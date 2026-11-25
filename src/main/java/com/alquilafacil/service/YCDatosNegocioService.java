package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCAjuste;
import com.alquilafacil.repository.YCAjusteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Order(1)
public class YCDatosNegocioService implements ApplicationRunner {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCDatosNegocioService.class);
    static final String YC_NOMBRE_DE_FABRICA = "Nombre del Propietario";
    static final String YC_DIRECCION_DE_FABRICA = "Av. Principal 123";

    public record YCDatos(String ycNombre, String ycDni, String ycDireccion, String ycTelefono, String ycNombreHospedaje,
                        String ycHoraLimiteLlegada, BigDecimal ycTasaImpuestoAlquiler, Integer ycMinutosInactividad) {}

    private final YCAjusteRepository ycRepository;
    private final YCDatosArrendador ycArrendador;
    private final YCSesion ycSesion;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments ycArgs) {
        try {
            Map<String, String> ycGuardados = new LinkedHashMap<>();
            ycRepository.findAll().forEach(ycA -> ycGuardados.put(ycA.getYcClave(), ycA.getYcValor()));
            ycAplicar(ycGuardados);
        } catch (RuntimeException ycE) {
            YC_LOG.warn("No se pudieron leer los datos del negocio; se usan los de fabrica", ycE);
        }
    }

    public YCDatos ycActuales() {
        return new YCDatos(ycArrendador.getYcNombre(), ycArrendador.getYcDni(), ycArrendador.getYcDireccion(),
                ycArrendador.getYcTelefono(), ycArrendador.getYcNombreHospedaje(), ycArrendador.getYcHoraLimiteLlegada(),
                ycArrendador.getYcTasaImpuestoAlquiler(), ycArrendador.getYcMinutosInactividad());
    }

    public boolean ycFaltanDatos() {
        return ycVacio(ycArrendador.getYcNombre()) || YC_NOMBRE_DE_FABRICA.equals(ycArrendador.getYcNombre().trim())
                || ycVacio(ycArrendador.getYcDireccion()) || YC_DIRECCION_DE_FABRICA.equals(ycArrendador.getYcDireccion().trim());
    }

    @Transactional
    public void ycGuardar(YCDatos ycD) {
        ycSesion.ycExigirAdmin("cambiar los datos del negocio");
        Map<String, String> ycV = ycValidar(ycD);
        YCDatos ycAntes = ycActuales();
        String ycCambios = new YCAuditoriaService.YCCambios()
                .ycCampo("nombre", ycAntes.ycNombre(), ycV.get("nombre"))
                .ycCampo("DNI", ycAntes.ycDni(), ycV.get("dni"))
                .ycCampo("direccion", ycAntes.ycDireccion(), ycV.get("direccion"))
                .ycCampo("celular", ycAntes.ycTelefono(), ycV.get("telefono"))
                .ycCampo("hospedaje", ycAntes.ycNombreHospedaje(), ycV.get("nombreHospedaje"))
                .ycCampo("hora limite", ycAntes.ycHoraLimiteLlegada(), ycV.get("horaLimiteLlegada"))
                .ycCampo("cierre por inactividad (min)", ycAntes.ycMinutosInactividad(), ycV.get("minutosInactividad"))
                .ycCampo("tasa %", ycAntes.ycTasaImpuestoAlquiler() == null ? null
                        : ycAntes.ycTasaImpuestoAlquiler().stripTrailingZeros().toPlainString(), ycV.get("tasaImpuestoAlquiler"))
                .toString();
        ycV.forEach((ycClave, ycValor) -> ycRepository.save(new YCAjuste(ycClave, ycValor)));
        if (!ycCambios.isEmpty()) {
            ycAuditar("Datos del negocio", ycCambios);
        }
        ycAplicar(ycV);
        YC_LOG.info("Datos del negocio actualizados por {}", ycSesion.ycNombreUsuario());
    }

    static Map<String, String> ycValidar(YCDatos ycD) {
        String ycNombre = ycLimpio(ycD.ycNombre());
        if (ycNombre.length() < 3 || YC_NOMBRE_DE_FABRICA.equalsIgnoreCase(ycNombre)) {
            throw new YCNegocioException("Escribe tu nombre completo (sale en contratos y recibos).");
        }
        String ycDni = ycLimpio(ycD.ycDni());
        if (!ycDni.isEmpty() && !ycDni.matches("\\d{8}")) {
            throw new YCNegocioException("El DNI debe tener 8 digitos (o dejalo vacio).");
        }
        String ycDireccion = ycLimpio(ycD.ycDireccion());
        if (ycDireccion.length() < 5 || YC_DIRECCION_DE_FABRICA.equalsIgnoreCase(ycDireccion)) {
            throw new YCNegocioException("Escribe la direccion del inmueble (sale en el contrato).");
        }
        String ycTelefono = ycLimpio(ycD.ycTelefono()).replace(" ", "");
        if (!ycTelefono.isEmpty() && !ycTelefono.matches("\\d{9}")) {
            throw new YCNegocioException("El celular debe tener 9 digitos (o dejalo vacio).");
        }
        String ycHospedaje = ycLimpio(ycD.ycNombreHospedaje());
        if (ycHospedaje.length() < 2) {
            throw new YCNegocioException("Escribe el nombre comercial del hospedaje.");
        }
        String ycHora = ycLimpio(ycD.ycHoraLimiteLlegada());
        try {
            ycHora = LocalTime.parse(ycHora.length() == 4 ? "0" + ycHora : ycHora).toString();
        } catch (Exception ycE) {
            throw new YCNegocioException("La hora limite de llegada debe ser como 20:00.");
        }
        BigDecimal ycTasa = ycD.ycTasaImpuestoAlquiler();
        if (ycTasa == null || ycTasa.signum() < 0 || ycTasa.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new YCNegocioException("La tasa del impuesto va de 0 a 50 %.");
        }
        Integer ycMinutos = ycD.ycMinutosInactividad() == null ? 15 : ycD.ycMinutosInactividad();
        if (ycMinutos < 0 || ycMinutos > 480) {
            throw new YCNegocioException("El cierre por inactividad va de 0 (nunca) a 480 minutos.");
        }
        Map<String, String> ycV = new LinkedHashMap<>();
        ycV.put("nombre", ycNombre);
        ycV.put("dni", ycDni);
        ycV.put("direccion", ycDireccion);
        ycV.put("telefono", ycTelefono);
        ycV.put("nombreHospedaje", ycHospedaje);
        ycV.put("horaLimiteLlegada", ycHora);
        ycV.put("tasaImpuestoAlquiler", ycTasa.stripTrailingZeros().toPlainString());
        ycV.put("minutosInactividad", String.valueOf(ycMinutos));
        return ycV;
    }

    private void ycAplicar(Map<String, String> ycV) {
        ycV.forEach((ycClave, ycValor) -> {
            switch (ycClave) {
                case "nombre" -> ycArrendador.setYcNombre(ycValor);
                case "dni" -> ycArrendador.setYcDni(ycValor);
                case "direccion" -> ycArrendador.setYcDireccion(ycValor);
                case "telefono" -> ycArrendador.setYcTelefono(ycValor);
                case "nombreHospedaje" -> ycArrendador.setYcNombreHospedaje(ycValor);
                case "horaLimiteLlegada" -> ycArrendador.setYcHoraLimiteLlegada(ycValor);
                case "tasaImpuestoAlquiler" -> {
                    try {
                        ycArrendador.setYcTasaImpuestoAlquiler(new BigDecimal(ycValor));
                    } catch (NumberFormatException ycE) {
                        YC_LOG.warn("Tasa guardada no valida: {}", ycValor);
                    }
                }
                case "minutosInactividad" -> {
                    try {
                        ycArrendador.setYcMinutosInactividad(Integer.parseInt(ycValor));
                    } catch (NumberFormatException ycE) {
                        YC_LOG.warn("Minutos de inactividad no validos: {}", ycValor);
                    }
                }
                case "yapeNumero" -> ycArrendador.setYcYapeNumero(ycValor);
                case "yapeTitular" -> ycArrendador.setYcYapeTitular(ycValor);
                case "qrPago" -> ycArrendador.setYcQrPago(ycValor);
                default -> YC_LOG.debug("Ajuste desconocido: {}", ycClave);
            }
        });
    }

    private static String ycLimpio(String ycS) {
        return ycS == null ? "" : ycS.trim().replaceAll("\\s+", " ");
    }

    private static boolean ycVacio(String ycS) {
        return ycS == null || ycS.isBlank();
    }

    private YCAuditoriaService ycAuditoria;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetAuditoria(YCAuditoriaService ycAuditoria) {
        this.ycAuditoria = ycAuditoria;
    }

    private void ycAuditar(String ycAccion, String ycDetalle) {
        if (ycAuditoria != null) {
            ycAuditoria.ycRegistrar(ycAccion, ycDetalle);
        }
    }
}
