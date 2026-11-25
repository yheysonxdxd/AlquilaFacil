package com.alquilafacil.service;

import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YCProrrateoTest {
    private final YCPagoRepository ycPagos = mock(YCPagoRepository.class);
    private final YCPagoService ycService = new YCPagoService(ycPagos, mock(YCInquilinoRepository.class),
            mock(YCConsumoServicioRepository.class), new YCSesion());

    @Test
    void ycCuentaLosDiasQueViveEnElMes() {
        YearMonth ycNov = YearMonth.of(2026, 11);
        assertEquals(6, YCPagoService.ycDiasCobrables(ycNov, LocalDate.of(2026, 11, 25), null), "del 25 al 30");
        assertEquals(30, YCPagoService.ycDiasCobrables(ycNov, LocalDate.of(2026, 10, 3), null), "mes completo");
        assertEquals(10, YCPagoService.ycDiasCobrables(ycNov, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 10)), "se va el 10");
        assertEquals(6, YCPagoService.ycDiasCobrables(ycNov, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 10)), "del 5 al 10");
        assertEquals(1, YCPagoService.ycDiasCobrables(YearMonth.of(2026, 2), LocalDate.of(2026, 2, 28), null));
    }

    @Test
    void ycLaRentaSeCobraProporcionalALosDias() {
        YearMonth ycNov = YearMonth.of(2026, 11);
        assertEquals(new BigDecimal("70.00"), YCPagoService.ycRentaDelMes(new BigDecimal("350"), ycNov, 6));
        assertEquals(new BigDecimal("350"), YCPagoService.ycRentaDelMes(new BigDecimal("350"), ycNov, 30));
        assertEquals(new BigDecimal("116.67"), YCPagoService.ycRentaDelMes(new BigDecimal("350"), ycNov, 10));
    }

    @Test
    void ycElPrimerMesSeCobraPorDiasYUsaLaRentaPactada() {
        YearMonth ycMes = YearMonth.now();
        YCCuarto ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Cuarto 101").ycPrecio(new BigDecimal("400")).build();
        LocalDate ycIngreso = ycMes.minusMonths(1).atEndOfMonth();
        YCInquilino ycI = YCInquilino.builder().ycId(3L).ycNombre("Ana").ycApellido("Diaz").ycCuarto(ycCuarto)
                .ycFechaIngreso(ycIngreso).ycDiaPago(5).ycRenta(new BigDecimal("300")).ycActivo(true).build();
        when(ycPagos.ycFindByInquilinoIdAndAnuladoFalse(anyLong())).thenReturn(List.of());

        List<YCDeudaPendiente> ycD = ycService.ycPendientes(ycI, false);

        int ycDiasMesPasado = ycMes.minusMonths(1).lengthOfMonth();
        assertTrue(ycD.get(0).ycPorDias());
        assertEquals(1, ycD.get(0).ycDiasCobrados());
        assertEquals(new BigDecimal("300").divide(BigDecimal.valueOf(ycDiasMesPasado), 2, java.math.RoundingMode.HALF_UP),
                ycD.get(0).ycAlquiler());
        assertFalse(ycD.get(1).ycPorDias(), "el mes actual va completo");
        assertEquals(new BigDecimal("300"), ycD.get(1).ycAlquiler(), "renta pactada, no el precio nuevo del cuarto");
    }

    @Test
    void ycAlSalirElUltimoMesSeCobraHastaHoy() {
        YearMonth ycMes = YearMonth.now();
        YCCuarto ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Cuarto 101").ycPrecio(new BigDecimal("300")).build();
        YCInquilino ycI = YCInquilino.builder().ycId(3L).ycNombre("Ana").ycApellido("Diaz").ycCuarto(ycCuarto)
                .ycFechaIngreso(ycMes.minusMonths(3).atDay(1)).ycDiaPago(5).ycActivo(true).build();
        when(ycPagos.ycFindByInquilinoIdAndAnuladoFalse(anyLong())).thenReturn(List.of());

        LocalDate ycHoy = LocalDate.now();
        YCDeudaPendiente ycUltimo = ycService.ycPendientes(ycI, false, ycHoy).stream()
                .filter(ycX -> ycX.ycPeriodo().equals(ycMes)).findFirst().orElseThrow();

        int ycDias = ycHoy.getDayOfMonth();
        assertEquals(ycDias == ycMes.lengthOfMonth() ? null : (Integer) ycDias, ycUltimo.ycDiasCobrados());
        assertEquals(YCPagoService.ycRentaDelMes(new BigDecimal("300"), ycMes, ycDias), ycUltimo.ycAlquiler());
        assertEquals(new BigDecimal("300"), ycI.ycGetRentaMensual(), "sin renta pactada usa el precio del cuarto");
    }
}
