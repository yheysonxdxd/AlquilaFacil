package com.alquilafacil.controller;

import com.alquilafacil.model.YCEstadia;

import java.time.LocalDate;
import java.time.LocalTime;

final class YCEstadoEstadia {
    private YCEstadoEstadia() {
    }

    static boolean ycPorLlegar(YCEstadia ycE, LocalDate ycHoy) {
        return ycE.ycEstado(ycHoy) == YCEstadia.YCEstado.YC_PROXIMA || ycE.ycEsperandoLlegada(ycHoy);
    }

    static boolean ycAtrasado(YCEstadia ycE, LocalTime ycHoraLimite) {
        return ycE.getYcEntrada().isBefore(LocalDate.now()) || !LocalTime.now().isBefore(ycHoraLimite);
    }

    static String ycTexto(YCEstadia ycE, LocalTime ycHoraLimite) {
        LocalDate ycHoy = LocalDate.now();
        if (ycE.ycIsNoLlego()) {
            return "No vino";
        }
        if (ycE.ycEsperandoLlegada(ycHoy)) {
            return ycAtrasado(ycE, ycHoraLimite) ? "No llego" : "Por llegar";
        }
        return ycE.ycEstado(ycHoy).ycGetTexto();
    }

    static String ycClase(String ycTexto) {
        return switch (ycTexto) {
            case "Hospedado" -> "exito";
            case "Proxima", "Por llegar" -> "advertencia";
            case "Cancelada", "No llego", "No vino" -> "peligro";
            default -> "neutro";
        };
    }
}
