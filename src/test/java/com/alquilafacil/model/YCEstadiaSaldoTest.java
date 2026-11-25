package com.alquilafacil.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class YCEstadiaSaldoTest {
    private static YCEstadia ycEstadia(String ycAdelanto, Boolean ycSaldoPagado, boolean ycCancelada) {
        return YCEstadia.builder().ycTotal(new BigDecimal("240.00"))
                .ycAdelanto(ycAdelanto == null ? null : new BigDecimal(ycAdelanto))
                .ycSaldoPagado(ycSaldoPagado).ycCancelada(ycCancelada).build();
    }

    @Test
    void ycEstadiaAntiguaSinAdelantoCuentaComoPagada() {
        YCEstadia ycE = ycEstadia(null, null, false);
        assertThat(ycE.ycGetPagado()).isEqualByComparingTo("240");
        assertThat(ycE.ycTieneSaldo()).isFalse();
    }

    @Test
    void ycConAdelantoQuedaSaldo() {
        YCEstadia ycE = ycEstadia("50", false, false);
        assertThat(ycE.ycGetPagado()).isEqualByComparingTo("50");
        assertThat(ycE.ycGetSaldo()).isEqualByComparingTo("190");
        assertThat(ycE.ycTieneSaldo()).isTrue();
    }

    @Test
    void ycAlCobrarElSaldoYaNoDebe() {
        YCEstadia ycE = ycEstadia("50", true, false);
        assertThat(ycE.ycGetPagado()).isEqualByComparingTo("240");
        assertThat(ycE.ycTieneSaldo()).isFalse();
    }

    @Test
    void ycCanceladaNoTieneSaldo() {
        assertThat(ycEstadia("50", false, true).ycTieneSaldo()).isFalse();
    }
}
