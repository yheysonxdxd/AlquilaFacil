package com.alquilafacil.model;

import com.alquilafacil.service.YCEstadiaService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YCEstadiaTest {
    private static final LocalDate YC_HOY = LocalDate.of(2026, 11, 3);

    private static YCEstadia ycEstadia(LocalDate ycEntrada, LocalDate ycSalida) {
        return YCEstadia.builder().ycEntrada(ycEntrada).ycSalida(ycSalida).ycCancelada(false).build();
    }

    @Test
    void ycReservaFuturaEsProxima() {
        assertEquals(YCEstadia.YCEstado.YC_PROXIMA, ycEstadia(YC_HOY.plusDays(2), YC_HOY.plusDays(4)).ycEstado(YC_HOY));
    }

    @Test
    void ycElDiaDeEntradaYaEstaHospedado() {
        assertEquals(YCEstadia.YCEstado.YC_EN_CURSO, ycEstadia(YC_HOY, YC_HOY.plusDays(1)).ycEstado(YC_HOY));
    }

    @Test
    void ycElDiaDeSalidaYaNoCuentaComoNoche() {
        assertEquals(YCEstadia.YCEstado.YC_TERMINADA, ycEstadia(YC_HOY.minusDays(2), YC_HOY).ycEstado(YC_HOY));
    }

    @Test
    void ycCanceladaGanaSobreLasFechas() {
        YCEstadia ycE = ycEstadia(YC_HOY.minusDays(1), YC_HOY.plusDays(2));
        ycE.setYcCancelada(true);
        assertEquals(YCEstadia.YCEstado.YC_CANCELADA, ycE.ycEstado(YC_HOY));
    }

    @Test
    void ycRegistroAntiguoSinDatoDeLlegadaSeConsideraLlegado() {
        YCEstadia ycE = ycEstadia(YC_HOY, YC_HOY.plusDays(2));
        assertTrue(ycE.ycIsLlego());
        assertTrue(ycE.ycHospedado(YC_HOY));
        assertFalse(ycE.ycEsperandoLlegada(YC_HOY));
    }

    @Test
    void ycReservaDeHoySinCheckInEstaEsperandoLlegada() {
        YCEstadia ycE = ycEstadia(YC_HOY, YC_HOY.plusDays(2));
        ycE.setYcLlego(false);
        assertTrue(ycE.ycEsperandoLlegada(YC_HOY));
        assertFalse(ycE.ycHospedado(YC_HOY));
        assertFalse(ycE.ycEsperandoLlegada(YC_HOY.minusDays(1)));
    }

    @Test
    void ycNoLlegoSeCancelaYNoDebeSaldo() {
        YCEstadia ycE = ycEstadia(YC_HOY, YC_HOY.plusDays(2));
        ycE.setYcTotal(new java.math.BigDecimal("160.00"));
        ycE.setYcAdelanto(new java.math.BigDecimal("50.00"));
        ycE.setYcLlego(false);
        ycE.setYcNoLlego(true);
        ycE.setYcCancelada(true);
        assertFalse(ycE.ycEsperandoLlegada(YC_HOY));
        assertEquals(0, ycE.ycGetSaldo().signum());
        assertEquals(new java.math.BigDecimal("50.00"), ycE.ycGetPagado());
    }

    @Test
    void ycNoSePuedeCancelarSiYaSeHospedoYDebe() {
        LocalDate ycHoy = LocalDate.now();
        YCEstadia ycE = ycEstadia(ycHoy.minusDays(1), ycHoy.plusDays(2));
        ycE.setYcTotal(new java.math.BigDecimal("240.00"));
        ycE.setYcAdelanto(new java.math.BigDecimal("80.00"));
        ycE.setYcSaldoPagado(false);
        assertTrue(YCEstadiaService.ycDebeAntesDeCancelar(ycE));

        ycE.setYcSaldoPagado(true);
        assertFalse(YCEstadiaService.ycDebeAntesDeCancelar(ycE));
    }

    @Test
    void ycReservaFuturaOSinLlegarSiSePuedeCancelarAunqueTengaSaldo() {
        LocalDate ycHoy = LocalDate.now();
        YCEstadia ycFutura = ycEstadia(ycHoy.plusDays(3), ycHoy.plusDays(5));
        ycFutura.setYcTotal(new java.math.BigDecimal("160.00"));
        ycFutura.setYcAdelanto(new java.math.BigDecimal("50.00"));
        assertFalse(YCEstadiaService.ycDebeAntesDeCancelar(ycFutura));

        YCEstadia ycNoLlego = ycEstadia(ycHoy, ycHoy.plusDays(2));
        ycNoLlego.setYcTotal(new java.math.BigDecimal("160.00"));
        ycNoLlego.setYcAdelanto(new java.math.BigDecimal("50.00"));
        ycNoLlego.setYcLlego(false);
        assertFalse(YCEstadiaService.ycDebeAntesDeCancelar(ycNoLlego));
    }

    @Test
    void ycCuentaLasNochesEntreFechas() {
        assertEquals(3, YCEstadiaService.ycNoches(YC_HOY, YC_HOY.plusDays(3)));
        assertEquals(0, YCEstadiaService.ycNoches(YC_HOY, YC_HOY));
        assertEquals(0, YCEstadiaService.ycNoches(YC_HOY.plusDays(1), YC_HOY));
    }
}
