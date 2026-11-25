package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.*;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCGastoRepository;
import com.alquilafacil.repository.YCLiquidacionRepository;
import com.alquilafacil.repository.YCPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YCCajaServiceTest {
    private static final LocalDate YC_HOY = LocalDate.of(2026, 11, 11);
    private static final LocalDateTime YC_A_LAS_10 = YC_HOY.atTime(10, 0);

    private final YCPagoRepository ycPagos = mock(YCPagoRepository.class);
    private final YCEstadiaRepository ycEstadias = mock(YCEstadiaRepository.class);
    private final YCLiquidacionRepository ycLiquidaciones = mock(YCLiquidacionRepository.class);
    private final YCGastoRepository ycGastos = mock(YCGastoRepository.class);
    private final YCSesion ycSesion = new YCSesion();
    private final com.alquilafacil.repository.YCCierreCajaRepository ycCierres = mock(com.alquilafacil.repository.YCCierreCajaRepository.class);
    private final YCCajaService ycService = new YCCajaService(ycPagos, ycEstadias, ycLiquidaciones, ycGastos, ycCierres, ycSesion,
            mock(YCAuditoriaService.class));

    private final YCInquilino ycJuan = YCInquilino.builder().ycNombre("Juan").ycApellido("Perez").build();
    private final YCCuarto ycCuarto = new YCCuarto();

    @BeforeEach
    void ycDatos() {
        YCPago ycNormal = ycPago(1L, "350", YCMetodoPago.YC_EFECTIVO, "maria", null);
        YCPago ycAnulado = ycPago(2L, "100", YCMetodoPago.YC_YAPE, "maria", null);
        ycAnulado.setYcAnulado(true);
        ycAnulado.setYcMotivoAnulacion("Mes equivocado");
        ycAnulado.setYcAnuladoPor("admin");
        ycAnulado.setYcFechaAnulacion(YC_A_LAS_10.plusHours(1));
        YCPago ycAlSalir = ycPago(3L, "300", YCMetodoPago.YC_EFECTIVO, "jose", "Pagado al retirarse (se uso la garantia)");
        when(ycPagos.ycFindByFechaPagoGreaterThanEqualAndFechaPagoLessThanOrderByFechaPago(any(), any()))
                .thenReturn(List.of(ycNormal, ycAnulado, ycAlSalir));
        when(ycPagos.ycFindByFechaAnulacionGreaterThanEqualAndFechaAnulacionLessThanOrderByFechaAnulacion(any(), any()))
                .thenReturn(List.of(ycAnulado));

        YCLiquidacion ycLiq = YCLiquidacion.builder().ycId(1L).ycInquilino(ycJuan).ycCuartoNumero("101").ycFecha(YC_A_LAS_10)
                .ycCobrado(new BigDecimal("50")).ycDevuelto(BigDecimal.ZERO).ycMetodo(YCMetodoPago.YC_EFECTIVO)
                .ycRegistradoPor("jose").build();
        when(ycLiquidaciones.ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFecha(any(), any())).thenReturn(List.of(ycLiq));

        YCEstadia ycE = YCEstadia.builder().ycId(7L).ycCuarto(ycCuarto).ycCuartoNumero("H-1").ycHuesped("Ana Diaz")
                .ycTotal(new BigDecimal("100")).ycAdelanto(new BigDecimal("60")).ycMetodo(YCMetodoPago.YC_YAPE)
                .ycFechaRegistro(YC_A_LAS_10).ycRegistradoPor("maria")
                .ycSaldoPagado(true).ycMontoSaldo(new BigDecimal("40")).ycMetodoSaldo(YCMetodoPago.YC_EFECTIVO)
                .ycFechaSaldo(YC_A_LAS_10.plusHours(2)).ycSaldoCobradoPor("jose")
                .ycDevuelto(new BigDecimal("20")).ycFechaDevolucion(YC_A_LAS_10.plusHours(3)).ycDevueltoPor("jose")
                .ycCancelada(false).build();
        when(ycEstadias.ycConMovimientosEntre(any(), any())).thenReturn(List.of(ycE));

        YCGasto ycG = YCGasto.builder().ycFecha(YC_HOY).ycCategoria(YCCategoriaGasto.values()[0]).ycNegocio(YCNegocio.YC_GENERAL)
                .ycMonto(new BigDecimal("30")).ycMetodo(YCMetodoPago.YC_EFECTIVO).ycRegistradoPor("admin")
                .ycFechaRegistro(YC_A_LAS_10).build();
        when(ycGastos.ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(any(), any())).thenReturn(List.of(ycG));
    }

    private YCPago ycPago(Long ycId, String ycTotal, YCMetodoPago ycM, String ycUsuario, String ycNota) {
        return YCPago.builder().ycId(ycId).ycInquilino(ycJuan).ycCuarto(ycCuarto).ycCuartoNumero("101")
                .ycPeriodo(YearMonth.of(2026, 11)).ycTotal(new BigDecimal(ycTotal)).ycMetodo(ycM)
                .ycFechaPago(YC_A_LAS_10).ycRegistradoPor(ycUsuario).ycNota(ycNota).build();
    }

    @Test
    void ycAdministradorVeTodoElDia() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(1L).ycUsuario("admin").ycRol(YCRol.YC_ADMINISTRADOR).build());
        YCCajaService.YCCierre ycC = ycService.ycCierre(YC_HOY, null);

        assertEquals(new BigDecimal("500"), ycC.ycTotalEntradas());
        assertEquals(new BigDecimal("50"), ycC.ycTotalSalidas());
        assertEquals(new BigDecimal("60"), ycC.ycEntradas(YCMetodoPago.YC_YAPE));
        assertEquals(new BigDecimal("390"), ycC.ycEfectivoEnCaja());
        assertEquals(1, ycC.ycAnulaciones().size());
        assertEquals("Mes equivocado", ycC.ycAnulaciones().get(0).ycMotivo());
        assertEquals(new BigDecimal("410"), ycC.ycCobradoPorUsuario().get("maria"));
        assertEquals(new BigDecimal("90"), ycC.ycCobradoPorUsuario().get("jose"));
    }

    @Test
    void ycRecepcionSoloVeLoSuyoYSinGastos() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(2L).ycUsuario("jose").ycRol(YCRol.YC_RECEPCION).build());
        YCCajaService.YCCierre ycC = ycService.ycCierre(YC_HOY, null);

        assertEquals("jose", ycC.ycUsuario());
        assertFalse(ycC.ycConGastos());
        assertEquals(new BigDecimal("90"), ycC.ycTotalEntradas());
        assertEquals(new BigDecimal("20"), ycC.ycTotalSalidas());
        assertTrue(ycC.ycAnulaciones().isEmpty());
    }

    @Test
    void ycGeneraElPdfDelCierre() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(1L).ycUsuario("admin").ycRol(YCRol.YC_ADMINISTRADOR).build());
        YCCajaService.YCCierre ycC = ycService.ycCierre(YC_HOY, null);
        java.io.ByteArrayOutputStream ycOut = new java.io.ByteArrayOutputStream();
        new YCCierreCajaPdfService(new com.alquilafacil.config.YCDatosArrendador()).ycEscribir(ycC, new BigDecimal("385"), ycOut);
        assertTrue(ycOut.size() > 1000);
        assertEquals("%PDF", new String(ycOut.toByteArray(), 0, 4));
    }

    @Test
    void ycElMotivoEsObligatorio() {
        assertThrows(YCNegocioException.class, () -> YCMotivos.ycExigir("  ", "anular el recibo"));
        assertThrows(YCNegocioException.class, () -> YCMotivos.ycExigir("no", "anular el recibo"));
        assertEquals("Mes equivocado", YCMotivos.ycExigir("  Mes   equivocado ", "anular el recibo"));
    }

    @Test
    void ycGuardarElCierrePideNotaSiNoCuadra() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(1L).ycUsuario("admin").ycRol(YCRol.YC_ADMINISTRADOR).build());
        when(ycCierres.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        YCNegocioException ycE = assertThrows(YCNegocioException.class,
                () -> ycService.ycGuardarCierre(YC_HOY, null, new BigDecimal("380"), ""));
        assertTrue(ycE.getMessage().contains("Faltan S/ 10"));

        var ycFalta = ycService.ycGuardarCierre(YC_HOY, null, new BigDecimal("380"), "Vuelto mal dado");
        assertEquals(new BigDecimal("-10"), ycFalta.getYcDiferencia());
        assertEquals("admin", ycFalta.getYcCerradoPor());

        var ycCuadra = ycService.ycGuardarCierre(YC_HOY, null, new BigDecimal("390"), null);
        assertEquals(0, ycCuadra.getYcDiferencia().signum());
        assertThrows(YCNegocioException.class, () -> ycService.ycGuardarCierre(YC_HOY, null, null, null));
    }

    @Test
    void ycRecepcionSoloVeSusCierres() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(2L).ycUsuario("jose").ycRol(YCRol.YC_RECEPCION).build());
        when(ycCierres.ycFindByDiaOrderByFechaCierreDesc(YC_HOY)).thenReturn(List.of(
                com.alquilafacil.model.YCCierreCaja.builder().ycCerradoPor("jose").build(),
                com.alquilafacil.model.YCCierreCaja.builder().ycCerradoPor("admin").build()));
        assertEquals(1, ycService.ycCierresDel(YC_HOY).size());
    }
}
