package com.alquilafacil.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class YCCalendarioPagosTest {
    @Test
    void ycVenceElUltimoDiaSiElMesEsMasCorto() {
        assertThat(YCCalendarioPagos.ycVencimiento(YearMonth.of(2026, 2), 31)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(YCCalendarioPagos.ycVencimiento(YearMonth.of(2026, 4), 31)).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(YCCalendarioPagos.ycVencimiento(YearMonth.of(2026, 11), 5)).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void ycListaLosMesesNoPagadosDesdeElIngreso() {
        LocalDate ycIngreso = LocalDate.of(2026, 8, 10);
        LocalDate ycHoy = LocalDate.of(2026, 11, 3);
        Set<YearMonth> ycPagados = Set.of(YearMonth.of(2026, 8));

        assertThat(YCCalendarioPagos.ycMesesAdeudados(ycIngreso, null, ycHoy, ycPagados, false))
                .containsExactly(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11));
    }

    @Test
    void ycPuedeIncluirElMesSiguienteParaPagoAdelantado() {
        LocalDate ycIngreso = LocalDate.of(2026, 11, 1);
        LocalDate ycHoy = LocalDate.of(2026, 11, 3);

        assertThat(YCCalendarioPagos.ycMesesAdeudados(ycIngreso, null, ycHoy, Set.of(YearMonth.of(2026, 11)), true))
                .containsExactly(YearMonth.of(2026, 12));
    }

    @Test
    void ycNoCuentaMesesDespuesDeLaSalida() {
        LocalDate ycIngreso = LocalDate.of(2026, 6, 1);
        LocalDate ycSalida = LocalDate.of(2026, 8, 15);
        LocalDate ycHoy = LocalDate.of(2026, 11, 3);

        assertThat(YCCalendarioPagos.ycMesesAdeudados(ycIngreso, ycSalida, ycHoy, Set.of(), true))
                .containsExactly(YearMonth.of(2026, 6), YearMonth.of(2026, 7), YearMonth.of(2026, 8));
    }

    @Test
    void ycTextoDelMesEnEspanol() {
        assertThat(YCCalendarioPagos.ycTexto(YearMonth.of(2026, 11))).isEqualTo("Noviembre 2026");
    }
}
