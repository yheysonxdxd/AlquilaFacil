package com.alquilafacil.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.arrendador")
@Getter
@Setter
public class YCDatosArrendador {
    private String ycNombre = "Propietario";
    private String ycDni = "";
    private String ycDireccion = "";
    private String ycTelefono = "";
    private String ycNombreHospedaje = "Hospedaje Supply";
    private String ycNombreSistema = "Hospedaje Supply";
    private String ycHoraLimiteLlegada = "20:00";
    private java.math.BigDecimal ycTasaImpuestoAlquiler = new java.math.BigDecimal("5");
    private int ycMinutosInactividad = 15;
    private String ycYapeNumero = "";
    private String ycYapeTitular = "";
    private String ycQrPago = "";

    public java.time.LocalTime ycGetHoraLimiteLlegadaHora() {
        try {
            return java.time.LocalTime.parse(ycHoraLimiteLlegada.trim());
        } catch (Exception ycE) {
            return java.time.LocalTime.of(20, 0);
        }
    }
}
