package com.alquilafacil.controller;

import com.alquilafacil.ui.YCTablas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class YCControles {
    static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter YC_FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", YCTablas.YC_LOCALE);

    private YCControles() {
    }

    static BigDecimal ycImporte(String ycTexto) {
        if (ycTexto == null || ycTexto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(ycTexto.trim().replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ycE) {
            return null;
        }
    }

    static BigDecimal ycImporteOCero(String ycTexto) {
        BigDecimal ycValor = ycImporte(ycTexto);
        return ycValor == null ? BigDecimal.ZERO.setScale(2) : ycValor;
    }

    static String ycMoneda(BigDecimal ycValor) {
        return YCTablas.YC_MONEDA.format(ycValor == null ? BigDecimal.ZERO : ycValor);
    }

    static String ycNombreMes(java.time.YearMonth ycMes) {
        return ycMes.getMonth().getDisplayName(java.time.format.TextStyle.FULL, YCTablas.YC_LOCALE);
    }

    static String ycFecha(LocalDate ycFecha) {
        return ycFecha == null ? "" : ycFecha.format(YC_FECHA);
    }

    static String ycCapitalizar(String ycS) {
        return ycS == null || ycS.isEmpty() ? ycS : Character.toUpperCase(ycS.charAt(0)) + ycS.substring(1);
    }

    static javafx.util.StringConverter<LocalDate> ycConvertidorFecha() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(LocalDate ycF) {
                return ycFecha(ycF);
            }

            @Override
            public LocalDate fromString(String ycT) {
                try {
                    return ycT == null || ycT.isBlank() ? null : LocalDate.parse(ycT.trim(), YC_FECHA);
                } catch (Exception ycE) {
                    return null;
                }
            }
        };
    }
}
