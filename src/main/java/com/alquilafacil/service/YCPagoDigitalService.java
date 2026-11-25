package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCAjuste;
import com.alquilafacil.repository.YCAjusteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class YCPagoDigitalService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCPagoDigitalService.class);
    private static final Set<String> YC_EXTENSIONES = Set.of("png", "jpg", "jpeg");
    private static final long YC_TAMANIO_MAXIMO = 5L * 1024 * 1024;

    private final YCAjusteRepository ycAjustes;
    private final YCDatosArrendador ycArrendador;
    private final YCSesion ycSesion;
    private final YCAuditoriaService ycAuditoria;
    private final Path ycCarpeta;

    public YCPagoDigitalService(YCAjusteRepository ycAjustes, YCDatosArrendador ycArrendador, YCSesion ycSesion,
                              YCAuditoriaService ycAuditoria, @Value("${app.carpeta:.}") String ycCarpeta) {
        this.ycAjustes = ycAjustes;
        this.ycArrendador = ycArrendador;
        this.ycSesion = ycSesion;
        this.ycAuditoria = ycAuditoria;
        this.ycCarpeta = Paths.get(ycCarpeta).toAbsolutePath().normalize();
    }

    @Transactional
    public void ycGuardar(String ycNumero, String ycTitular, Path ycNuevoQr, boolean ycQuitarQr) {
        ycSesion.ycExigirAdmin("cambiar los datos de Yape / Plin");
        String ycCel = ycNumero == null ? "" : ycNumero.replaceAll("\\D", "");
        if (!ycCel.isEmpty() && !ycCel.matches("9\\d{8}")) {
            throw new YCNegocioException("El celular de Yape / Plin debe tener 9 digitos y empezar con 9 (o dejalo vacio).");
        }
        String ycNombre = ycTitular == null ? "" : ycTitular.trim().replaceAll("\\s+", " ");
        if (ycNombre.length() > 60) {
            ycNombre = ycNombre.substring(0, 60);
        }
        String ycArchivo = ycArrendador.getYcQrPago() == null ? "" : ycArrendador.getYcQrPago();
        if (ycQuitarQr) {
            ycQr().ifPresent(YCPagoDigitalService::ycBorrar);
            ycArchivo = "";
        }
        if (ycNuevoQr != null) {
            ycArchivo = ycCopiarQr(ycNuevoQr);
        }
        String ycAntes = ycTexto();
        ycAjustes.save(new YCAjuste("yapeNumero", ycCel));
        ycAjustes.save(new YCAjuste("yapeTitular", ycNombre));
        ycAjustes.save(new YCAjuste("qrPago", ycArchivo));
        ycArrendador.setYcYapeNumero(ycCel);
        ycArrendador.setYcYapeTitular(ycNombre);
        ycArrendador.setYcQrPago(ycArchivo);
        ycAuditoria.ycRegistrar("Yape / Plin", "antes: " + (ycAntes.isEmpty() ? "—" : ycAntes) + " · ahora: "
                + (ycTexto().isEmpty() ? "—" : ycTexto()) + (ycNuevoQr != null ? " · QR nuevo" : ycQuitarQr ? " · QR quitado" : ""));
    }

    public Optional<Path> ycQr() {
        String ycNombre = ycArrendador.getYcQrPago();
        if (ycNombre == null || ycNombre.isBlank()) {
            return Optional.empty();
        }
        Path ycRuta = ycCarpeta.resolve(ycNombre).normalize();
        return ycRuta.startsWith(ycCarpeta) && Files.isRegularFile(ycRuta) ? Optional.of(ycRuta) : Optional.empty();
    }

    public String ycTexto() {
        String ycCel = ycArrendador.getYcYapeNumero();
        if (ycCel == null || ycCel.isBlank()) {
            return "";
        }
        String ycBonito = ycCel.length() == 9 ? ycCel.substring(0, 3) + " " + ycCel.substring(3, 6) + " " + ycCel.substring(6) : ycCel;
        String ycTitular = ycArrendador.getYcYapeTitular();
        return "Yape / Plin al " + ycBonito + (ycTitular == null || ycTitular.isBlank() ? "" : " (" + ycTitular + ")");
    }

    public boolean ycConfigurado() {
        return !ycTexto().isEmpty() || ycQr().isPresent();
    }

    private String ycCopiarQr(Path ycOrigen) {
        Optional<Path> ycActual = ycQr();
        if (ycActual.isPresent() && ycActual.get().equals(ycOrigen.toAbsolutePath().normalize())) {
            return ycArrendador.getYcQrPago();
        }
        String ycNombre = ycOrigen.getFileName().toString();
        int ycPunto = ycNombre.lastIndexOf('.');
        String ycExt = ycPunto < 0 ? "" : ycNombre.substring(ycPunto + 1).toLowerCase(Locale.ROOT);
        if (!YC_EXTENSIONES.contains(ycExt)) {
            throw new YCNegocioException("El QR debe ser una imagen PNG o JPG.");
        }
        try {
            if (Files.size(ycOrigen) > YC_TAMANIO_MAXIMO) {
                throw new YCNegocioException("La imagen del QR pesa mas de 5 MB. Usa una captura mas liviana.");
            }
            Files.createDirectories(ycCarpeta);
            ycQr().ifPresent(YCPagoDigitalService::ycBorrar);
            String ycDestino = "qr-pago." + (ycExt.equals("jpeg") ? "jpg" : ycExt);
            Files.copy(ycOrigen, ycCarpeta.resolve(ycDestino), StandardCopyOption.REPLACE_EXISTING);
            YC_LOG.info("QR de pago guardado: {}", ycDestino);
            return ycDestino;
        } catch (IOException ycE) {
            throw new YCNegocioException("No se pudo guardar el QR: " + ycE.getMessage());
        }
    }

    private static void ycBorrar(Path ycRuta) {
        try {
            Files.deleteIfExists(ycRuta);
        } catch (IOException ycE) {
            YC_LOG.warn("No se pudo borrar el QR {}", ycRuta, ycE);
        }
    }
}
