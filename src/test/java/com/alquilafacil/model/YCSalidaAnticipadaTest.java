package com.alquilafacil.model;

import com.alquilafacil.service.YCEstadiaService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class YCSalidaAnticipadaTest {
    private static YCEstadia ycReserva(String ycAdelanto) {
        return YCEstadia.builder().ycEntrada(LocalDate.of(2026, 11, 3)).ycSalida(LocalDate.of(2026, 11, 11)).ycNoches(8)
                .ycPrecioNoche(new BigDecimal("80.00")).ycTotal(new BigDecimal("640.00"))
                .ycAdelanto(ycAdelanto == null ? null : new BigDecimal(ycAdelanto)).ycSaldoPagado(ycAdelanto == null)
                .ycCancelada(false).build();
    }

    @Test
    void ycPagoTodoYSeVaElSieteSeLeDevuelve() {
        var ycR = YCEstadiaService.ycCalcularSalida(ycReserva(null), LocalDate.of(2026, 11, 7), true);
        assertThat(ycR.ycNochesAhora()).isEqualTo(4);
        assertThat(ycR.ycTotalAhora()).isEqualByComparingTo("320");
        assertThat(ycR.ycADevolver()).isEqualByComparingTo("320");
        assertThat(ycR.ycACobrar()).isEqualByComparingTo("0");
    }

    @Test
    void ycDejoAdelantoYAunDebeLaDiferencia() {
        var ycR = YCEstadiaService.ycCalcularSalida(ycReserva("100"), LocalDate.of(2026, 11, 7), true);
        assertThat(ycR.ycTotalAhora()).isEqualByComparingTo("320");
        assertThat(ycR.ycACobrar()).isEqualByComparingTo("220");
        assertThat(ycR.ycADevolver()).isEqualByComparingTo("0");
    }

    @Test
    void ycSinRecalcularSeCobraTodoIgual() {
        var ycR = YCEstadiaService.ycCalcularSalida(ycReserva(null), LocalDate.of(2026, 11, 7), false);
        assertThat(ycR.ycTotalAhora()).isEqualByComparingTo("640");
        assertThat(ycR.ycADevolver()).isEqualByComparingTo("0");
    }

    @Test
    void ycElPagadoDescuentaLaDevolucion() {
        YCEstadia ycE = ycReserva("640");
        ycE.setYcSaldoPagado(true);
        ycE.setYcMontoSaldo(BigDecimal.ZERO);
        ycE.setYcTotal(new BigDecimal("320"));
        ycE.setYcDevuelto(new BigDecimal("320"));
        assertThat(ycE.ycGetRecibido()).isEqualByComparingTo("640");
        assertThat(ycE.ycGetPagado()).isEqualByComparingTo("320");
        assertThat(ycE.ycTieneSaldo()).isFalse();
    }
}
