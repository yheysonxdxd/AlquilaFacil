package com.alquilafacil.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class YCCalendarioPagos {
    private static final Locale YC_ES = Locale.forLanguageTag("es-PE");

    private YCCalendarioPagos() {
    }

    public static LocalDate ycVencimiento(YearMonth ycPeriodo, int ycDiaPago) {
        return ycPeriodo.atDay(Math.min(ycDiaPago, ycPeriodo.lengthOfMonth()));
    }

    public static List<YearMonth> ycMesesAdeudados(LocalDate ycFechaIngreso, LocalDate ycFechaSalida, LocalDate ycHoy,
                                                 Set<YearMonth> ycPagados, boolean ycIncluirSiguiente) {
        YearMonth ycDesde = YearMonth.from(ycFechaIngreso);
        YearMonth ycHasta = YearMonth.from(ycHoy);
        if (ycIncluirSiguiente) {
            ycHasta = ycHasta.plusMonths(1);
        }
        if (ycFechaSalida != null && YearMonth.from(ycFechaSalida).isBefore(ycHasta)) {
            ycHasta = YearMonth.from(ycFechaSalida);
        }
        List<YearMonth> ycMeses = new ArrayList<>();
        for (YearMonth ycM = ycDesde; !ycM.isAfter(ycHasta); ycM = ycM.plusMonths(1)) {
            if (!ycPagados.contains(ycM)) {
                ycMeses.add(ycM);
            }
        }
        return ycMeses;
    }

    public static String ycTexto(YearMonth ycPeriodo) {
        String ycMes = ycPeriodo.getMonth().getDisplayName(TextStyle.FULL, YC_ES);
        return Character.toUpperCase(ycMes.charAt(0)) + ycMes.substring(1) + " " + ycPeriodo.getYear();
    }

    public static String ycCorto(YearMonth ycPeriodo) {
        String ycMes = ycPeriodo.getMonth().getDisplayName(TextStyle.SHORT, YC_ES).replace(".", "");
        return Character.toUpperCase(ycMes.charAt(0)) + ycMes.substring(1) + " " + (ycPeriodo.getYear() % 100);
    }
}
