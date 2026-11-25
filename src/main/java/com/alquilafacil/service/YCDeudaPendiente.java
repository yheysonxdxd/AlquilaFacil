package com.alquilafacil.service;

import com.alquilafacil.model.YCInquilino;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record YCDeudaPendiente(YCInquilino ycInquilino, YearMonth ycPeriodo, LocalDate ycVence,
                             BigDecimal ycAlquiler, BigDecimal ycLuz, BigDecimal ycAgua, long ycDiasAtraso,
                             Integer ycDiasCobrados) {
    public YCDeudaPendiente(YCInquilino ycInquilino, YearMonth ycPeriodo, LocalDate ycVence, BigDecimal ycAlquiler,
                          BigDecimal ycLuz, BigDecimal ycAgua, long ycDiasAtraso) {
        this(ycInquilino, ycPeriodo, ycVence, ycAlquiler, ycLuz, ycAgua, ycDiasAtraso, null);
    }

    public boolean ycPorDias() {
        return ycDiasCobrados != null;
    }

    public String ycTextoDias() {
        return ycDiasCobrados == null ? "" : ycDiasCobrados + " de " + ycPeriodo.lengthOfMonth() + " dias";
    }

    public BigDecimal ycTotal() {
        return ycAlquiler.add(ycLuz).add(ycAgua);
    }

    public boolean ycAtrasado() {
        return ycDiasAtraso > 0;
    }

    public boolean ycAdelantado() {
        return ycPeriodo.isAfter(YearMonth.now());
    }

    public String ycEstado() {
        if (ycDiasAtraso > 0) {
            return ycDiasAtraso == 1 ? "1 dia de atraso" : ycDiasAtraso + " dias de atraso";
        }
        if (ycDiasAtraso == 0) {
            return "Vence hoy";
        }
        return -ycDiasAtraso == 1 ? "Falta 1 dia" : "Faltan " + (-ycDiasAtraso) + " dias";
    }

    public String ycPeriodoTexto() {
        return YCCalendarioPagos.ycTexto(ycPeriodo);
    }
}
