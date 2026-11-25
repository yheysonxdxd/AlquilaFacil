package com.alquilafacil.service;

import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YCConsultaDniService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCConsultaDniService.class);
    public enum YCFuente {
        YC_INQUILINO("Inquilino registrado"),
        YC_HUESPED("Huesped anterior"),
        YC_RENIEC("RENIEC");

        private final String ycTexto;

        YCFuente(String ycTexto) {
            this.ycTexto = ycTexto;
        }

        public String ycGetTexto() {
            return ycTexto;
        }
    }

    public record YCPersona(String ycNombres, String ycApellidos, String ycTelefono, YCFuente ycFuente) {
        public String ycNombreCompleto() {
            return (ycNombres + " " + ycApellidos).trim().replaceAll("\\s+", " ");
        }
    }

    public static class YCConsultaException extends RuntimeException {
        public YCConsultaException(String ycMensaje) {
            super(ycMensaje);
        }
    }

    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCEstadiaRepository ycEstadiaRepository;
    private final String ycUrl;
    private final String ycToken;
    private final HttpClient ycHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    public YCConsultaDniService(YCInquilinoRepository ycInquilinoRepository, YCEstadiaRepository ycEstadiaRepository,
                              @Value("${app.dni.url:}") String ycUrl, @Value("${app.dni.token:}") String ycToken) {
        this.ycInquilinoRepository = ycInquilinoRepository;
        this.ycEstadiaRepository = ycEstadiaRepository;
        this.ycUrl = ycUrl == null ? "" : ycUrl.trim();
        this.ycToken = ycToken == null ? "" : ycToken.trim();
    }

    public boolean ycReniecConfigurado() {
        return !ycUrl.isEmpty() && !ycToken.isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<YCPersona> ycBuscarLocal(String ycDni) {
        if (ycDni == null || ycDni.isBlank()) {
            return Optional.empty();
        }
        String ycD = ycDni.trim();
        Optional<YCInquilino> ycInquilino = ycInquilinoRepository.ycFindFirstByDniOrderByIdDesc(ycD);
        if (ycInquilino.isPresent()) {
            YCInquilino ycI = ycInquilino.get();
            return Optional.of(new YCPersona(ycI.getYcNombre(), ycI.getYcApellido(), ycI.getYcTelefono(), YCFuente.YC_INQUILINO));
        }
        YCEstadia ycE = ycEstadiaRepository.ycFindFirstByDocumentoIgnoreCaseOrderByFechaRegistroDesc(ycD);
        if (ycE != null) {
            String[] ycPartes = ycE.getYcHuesped().trim().split("\\s+");
            int ycCorte = ycPartes.length >= 4 ? 2 : 1;
            String ycNombres = String.join(" ", java.util.Arrays.copyOfRange(ycPartes, 0, Math.min(ycCorte, ycPartes.length)));
            String ycApellidos = ycPartes.length > ycCorte
                    ? String.join(" ", java.util.Arrays.copyOfRange(ycPartes, ycCorte, ycPartes.length)) : "";
            return Optional.of(new YCPersona(ycNombres, ycApellidos, ycE.getYcTelefono(), YCFuente.YC_HUESPED));
        }
        return Optional.empty();
    }

    public Optional<YCPersona> ycBuscarReniec(String ycDni) {
        if (!ycReniecConfigurado()) {
            throw new YCConsultaException("La consulta a RENIEC no esta configurada.");
        }
        if (ycDni == null || !ycDni.trim().matches("\\d{8}")) {
            return Optional.empty();
        }
        HttpRequest ycPedido = HttpRequest.newBuilder(URI.create(ycUrl.replace("{dni}", ycDni.trim())))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + ycToken)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> ycRespuesta;
        try {
            ycRespuesta = ycHttp.send(ycPedido, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ycE) {
            Thread.currentThread().interrupt();
            throw new YCConsultaException("Se interrumpio la consulta.");
        } catch (Exception ycE) {
            YC_LOG.warn("No se pudo consultar el DNI", ycE);
            throw new YCConsultaException("Sin conexion con el servicio de RENIEC. Revisa tu internet.");
        }
        int ycCodigo = ycRespuesta.statusCode();
        if (ycCodigo == 401 || ycCodigo == 403) {
            throw new YCConsultaException("El token de RENIEC no es valido o se acabaron las consultas del plan.");
        }
        if (ycCodigo == 404 || ycCodigo == 422) {
            return Optional.empty();
        }
        if (ycCodigo == 429) {
            throw new YCConsultaException("Demasiadas consultas seguidas. Espera un momento.");
        }
        if (ycCodigo >= 400) {
            throw new YCConsultaException("El servicio de RENIEC respondio con error " + ycCodigo + ".");
        }
        return ycInterpretar(ycRespuesta.body());
    }

    static Optional<YCPersona> ycInterpretar(String ycJson) {
        if (ycJson == null || ycJson.isBlank()) {
            return Optional.empty();
        }
        String ycNombres = ycCampo(ycJson, "nombres", "first_name", "nombre", "names");
        String ycPaterno = ycCampo(ycJson, "apellidoPaterno", "apellido_paterno", "first_last_name", "paternal_surname");
        String ycMaterno = ycCampo(ycJson, "apellidoMaterno", "apellido_materno", "second_last_name", "maternal_surname");
        if (ycNombres != null && (ycPaterno != null || ycMaterno != null)) {
            String ycApellidos = ((ycPaterno == null ? "" : ycPaterno) + " " + (ycMaterno == null ? "" : ycMaterno)).trim();
            return Optional.of(new YCPersona(ycTitulo(ycNombres), ycTitulo(ycApellidos), null, YCFuente.YC_RENIEC));
        }
        String ycCompleto = ycCampo(ycJson, "full_name", "nombre_completo", "nombreCompleto", "fullName");
        if (ycCompleto != null && !ycCompleto.isBlank()) {
            if (ycCompleto.contains(",")) {
                String[] ycP = ycCompleto.split(",", 2);
                return Optional.of(new YCPersona(ycTitulo(ycP[1].trim()), ycTitulo(ycP[0].trim()), null, YCFuente.YC_RENIEC));
            }
            String[] ycPartes = ycCompleto.trim().split("\\s+");
            if (ycPartes.length >= 3) {
                String ycApellidos = ycPartes[0] + " " + ycPartes[1];
                String ycNom = String.join(" ", java.util.Arrays.copyOfRange(ycPartes, 2, ycPartes.length));
                return Optional.of(new YCPersona(ycTitulo(ycNom), ycTitulo(ycApellidos), null, YCFuente.YC_RENIEC));
            }
            return Optional.of(new YCPersona(ycTitulo(ycCompleto), "", null, YCFuente.YC_RENIEC));
        }
        return Optional.empty();
    }

    private static String ycCampo(String ycJson, String... ycClaves) {
        for (String ycClave : ycClaves) {
            Matcher ycM = Pattern.compile("\"" + Pattern.quote(ycClave) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(ycJson);
            if (ycM.find()) {
                String ycValor = ycDesescapar(ycM.group(1)).trim();
                if (!ycValor.isEmpty()) {
                    return ycValor;
                }
            }
        }
        return null;
    }

    private static String ycDesescapar(String ycS) {
        StringBuilder ycSb = new StringBuilder();
        for (int ycI = 0; ycI < ycS.length(); ycI++) {
            char ycC = ycS.charAt(ycI);
            if (ycC == '\\' && ycI + 1 < ycS.length()) {
                char ycN = ycS.charAt(++ycI);
                if (ycN == 'u' && ycI + 4 < ycS.length()) {
                    ycSb.append((char) Integer.parseInt(ycS.substring(ycI + 1, ycI + 5), 16));
                    ycI += 4;
                } else {
                    ycSb.append(ycN == 'n' ? ' ' : ycN);
                }
            } else {
                ycSb.append(ycC);
            }
        }
        return ycSb.toString();
    }

    static String ycTitulo(String ycTexto) {
        if (ycTexto == null || ycTexto.isBlank()) {
            return "";
        }
        StringBuilder ycSb = new StringBuilder();
        for (String ycP : ycTexto.trim().toLowerCase(java.util.Locale.ROOT).split("\\s+")) {
            if (!ycSb.isEmpty()) ycSb.append(' ');
            ycSb.append(Character.toUpperCase(ycP.charAt(0))).append(ycP.substring(1));
        }
        return ycSb.toString();
    }
}
