package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.*;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YCEstadiaServiceTest {
    private final YCEstadiaRepository ycEstadias = mock(YCEstadiaRepository.class);
    private final YCCuartoRepository ycCuartos = mock(YCCuartoRepository.class);
    private final YCSesion ycSesion = new YCSesion();
    private final YCEstadiaService ycService = new YCEstadiaService(ycEstadias, ycCuartos, ycSesion);

    private final LocalDate ycHoy = LocalDate.now();
    private YCCuarto ycHabitacion;

    @BeforeEach
    void ycDatos() {
        ycHabitacion = YCCuarto.builder().ycId(1L).ycNumero("Habitacion 401").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("100")).ycCapacidad(2).build();
        when(ycCuartos.findById(1L)).thenReturn(Optional.of(ycHabitacion));
        when(ycCuartos.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        when(ycEstadias.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        when(ycEstadias.ycCruzadas(any(), any(), any(), any())).thenReturn(List.of());
    }

    private void ycIngresar(YCRol ycRol, String ycUsuario) {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(ycRol == YCRol.YC_ADMINISTRADOR ? 1L : 2L).ycUsuario(ycUsuario).ycRol(ycRol).build());
    }

    private YCEstadiaService.YCSolicitud ycSolicitud(LocalDate ycEntrada, LocalDate ycSalida, int ycPersonas, BigDecimal ycAdelanto) {
        return new YCEstadiaService.YCSolicitud(1L, "Ana Diaz", "44556677", "987654321", ycPersonas, ycEntrada, ycSalida,
                null, YCMetodoPago.YC_YAPE, null, ycAdelanto, YCCanalReserva.YC_EN_PERSONA);
    }

    private YCEstadia ycEstadiaConSaldo() {
        YCEstadia ycE = YCEstadia.builder().ycId(9L).ycCuarto(ycHabitacion).ycCuartoNumero("Habitacion 401").ycHuesped("Ana Diaz")
                .ycPersonas(1).ycEntrada(ycHoy).ycSalida(ycHoy.plusDays(3)).ycNoches(3).ycPrecioNoche(new BigDecimal("100.00"))
                .ycTotal(new BigDecimal("300.00")).ycAdelanto(new BigDecimal("100.00")).ycSaldoPagado(false)
                .ycMetodo(YCMetodoPago.YC_YAPE).ycCancelada(false).ycLlego(true).ycNoLlego(false)
                .ycFechaRegistro(LocalDateTime.now()).build();
        when(ycEstadias.findById(9L)).thenReturn(Optional.of(ycE));
        return ycE;
    }

    @Test
    void ycRegistraNochesTotalSaldoYQuienLaRegistro() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(3), 2, new BigDecimal("100")));

        assertEquals(3, ycE.getYcNoches());
        assertEquals(new BigDecimal("300.00"), ycE.getYcTotal());
        assertEquals(new BigDecimal("200.00"), ycE.ycGetSaldo());
        assertTrue(ycE.ycIsLlego(), "en persona y desde hoy: ya llego");
        assertEquals("maria", ycE.getYcRegistradoPor());
    }

    @Test
    void ycSinAdelantoSeCobraTodo() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(2), 1, null));
        assertFalse(ycE.ycTieneSaldo());
        assertEquals(new BigDecimal("200.00"), ycE.ycGetPagado());
    }

    @Test
    void ycNoSePuedeReservarUnaHabitacionYaOcupadaEsasFechas() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycOtra = YCEstadia.builder().ycHuesped("Luis Rojas").ycEntrada(ycHoy.plusDays(1)).ycSalida(ycHoy.plusDays(4)).build();
        when(ycEstadias.ycCruzadas(1L, ycHoy, ycHoy.plusDays(3), null)).thenReturn(List.of(ycOtra));

        YCNegocioException ycEx = assertThrows(YCNegocioException.class,
                () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(3), 1, null)));
        assertTrue(ycEx.getMessage().contains("Luis Rojas"));
        verify(ycEstadias, never()).save(any());
    }

    @Test
    void ycRechazaDatosInvalidos() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(1), 3, null)));
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy, 1, null)));
        assertThrows(YCNegocioException.class,
                () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(1), 1, new BigDecimal("500"))));
        assertThrows(YCNegocioException.class,
                () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(YCEstadiaService.YC_MAXIMO_NOCHES + 1), 1, null)));
        ycHabitacion.setYcModalidad(YCModalidad.YC_MENSUAL);
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(ycSolicitud(ycHoy, ycHoy.plusDays(1), 1, null)));
    }

    @Test
    void ycCobrarElSaldoDejaTodoPagadoYGuardaQuienCobro() {
        ycIngresar(YCRol.YC_RECEPCION, "jose");
        YCEstadia ycE = ycEstadiaConSaldo();

        ycService.ycCobrarSaldo(9L, YCMetodoPago.YC_EFECTIVO);

        assertFalse(ycE.ycTieneSaldo());
        assertEquals(new BigDecimal("200.00"), ycE.getYcMontoSaldo());
        assertEquals(YCMetodoPago.YC_EFECTIVO, ycE.getYcMetodoSaldo());
        assertEquals("jose", ycE.getYcSaldoCobradoPor());
        assertEquals(new BigDecimal("300.00"), ycE.ycGetPagado());
        assertThrows(YCNegocioException.class, () -> ycService.ycCobrarSaldo(9L, YCMetodoPago.YC_EFECTIVO), "ya no hay saldo");
    }

    @Test
    void ycCancelarEsSoloDelAdministradorYPideMotivo() {
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcEntrada(ycHoy.plusDays(2));
        ycE.setYcSalida(ycHoy.plusDays(5));

        ycIngresar(YCRol.YC_RECEPCION, "maria");
        assertThrows(YCNegocioException.class, () -> ycService.ycCancelar(9L, "El huesped no viaja"));

        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        assertThrows(YCNegocioException.class, () -> ycService.ycCancelar(9L, ""));
        ycService.ycCancelar(9L, "El huesped no viaja");
        assertTrue(ycE.isYcCancelada());
        assertEquals("El huesped no viaja", ycE.getYcMotivoCancelacion());
        assertEquals("admin", ycE.getYcCanceladaPor());
        assertEquals(BigDecimal.ZERO, ycE.ycGetSaldo(), "cancelada no debe nada");
    }

    @Test
    void ycNoSeCancelaUnHospedadoQueAunDebe() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCEstadia ycE = ycEstadiaConSaldo();
        assertThrows(YCNegocioException.class, () -> ycService.ycCancelar(9L, "Se fue sin pagar"));
        assertFalse(ycE.isYcCancelada());
    }

    @Test
    void ycNoVinoConDevolucionDelAdelanto() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcLlego(false);

        ycService.ycRegistrarNoLlego(9L, true);

        assertTrue(ycE.isYcCancelada());
        assertTrue(ycE.ycIsNoLlego());
        assertEquals(new BigDecimal("100.00"), ycE.getYcDevuelto());
        assertEquals("admin", ycE.getYcDevueltoPor());
        assertEquals("No se presento", ycE.getYcMotivoCancelacion());
        assertEquals(BigDecimal.ZERO.setScale(2), ycE.ycGetPagado());
    }

    @Test
    void ycNoVinoSinDevolverElAdelantoSeLoQueda() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcLlego(false);

        ycService.ycRegistrarNoLlego(9L, false);

        assertNull(ycE.getYcDevuelto());
        assertEquals(new BigDecimal("100.00"), ycE.ycGetPagado(), "el adelanto queda como penalidad");
    }

    @Test
    void ycSalidaAnticipadaDevuelveLasNochesNoUsadas() {
        ycIngresar(YCRol.YC_RECEPCION, "jose");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcEntrada(ycHoy.minusDays(1));
        ycE.setYcSalida(ycHoy.plusDays(3));
        ycE.setYcNoches(4);
        ycE.setYcTotal(new BigDecimal("400.00"));
        ycE.setYcAdelanto(new BigDecimal("400.00"));
        ycE.setYcSaldoPagado(true);

        var ycR = ycService.ycRegistrarSalidaAnticipada(9L, ycHoy.plusDays(1), true, null);

        assertEquals(2, ycR.ycNochesAhora());
        assertEquals(new BigDecimal("200.00"), ycR.ycADevolver());
        assertEquals(new BigDecimal("200.00"), ycE.getYcDevuelto());
        assertEquals("jose", ycE.getYcDevueltoPor());
        assertEquals(ycHoy.plusDays(4).minusDays(3), ycE.getYcSalida());
        assertEquals(new BigDecimal("200.00"), ycE.ycGetPagado());
    }

    @Test
    void ycSalidaAnticipadaConDeudaExigeCobrar() {
        ycIngresar(YCRol.YC_RECEPCION, "jose");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcEntrada(ycHoy.minusDays(2));
        ycE.setYcSalida(ycHoy.plusDays(2));

        ycE.setYcNoches(4);
        ycE.setYcTotal(new BigDecimal("400.00"));
        assertThrows(YCNegocioException.class,
                () -> ycService.ycRegistrarSalidaAnticipada(9L, ycHoy.plusDays(1), true, null));

        var ycR = ycService.ycRegistrarSalidaAnticipada(9L, ycHoy.plusDays(1), true, YCMetodoPago.YC_PLIN);
        assertEquals(new BigDecimal("200.00"), ycR.ycACobrar());
        assertFalse(ycE.ycTieneSaldo());
        assertEquals("jose", ycE.getYcSaldoCobradoPor());
        assertEquals(YCMetodoPago.YC_PLIN, ycE.getYcMetodoSaldo());
    }

    private YCEstadiaService.YCEdicion ycEdicion(YCEstadia ycE, LocalDate ycEntrada, LocalDate ycSalida, String ycPrecio) {
        return new YCEstadiaService.YCEdicion(1L, ycE.getYcHuesped(), "44556677", ycE.getYcTelefono(), ycE.getYcPersonas(),
                ycEntrada, ycSalida, ycPrecio == null ? null : new BigDecimal(ycPrecio), null, null);
    }

    @Test
    void ycAlAlargarLaEstadiaSubeElTotalYQuedaSaldo() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(5), null));
        assertEquals(5, ycE.getYcNoches());
        assertEquals(new BigDecimal("500.00"), ycE.getYcTotal());
        assertEquals(new BigDecimal("400.00"), ycE.ycGetSaldo());
        verify(ycEstadias).ycCruzadas(1L, ycHoy, ycHoy.plusDays(5), 9L);
    }

    @Test
    void ycUnaEstadiaPagadaCompletaQueSeAlargaQuedaDebiendoLaDiferencia() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcAdelanto(new BigDecimal("300.00"));
        ycE.setYcSaldoPagado(true);
        ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(4), null));
        assertEquals(new BigDecimal("100.00"), ycE.ycGetSaldo());
        assertEquals(new BigDecimal("300.00"), ycE.ycGetPagado());
    }

    @Test
    void ycNoSeBajaElTotalPorDebajoDeLoPagadoNiSeSubeSiElSaldoYaSeCobro() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycService.ycCobrarSaldo(9L, YCMetodoPago.YC_EFECTIVO);
        YCNegocioException ycBaja = assertThrows(YCNegocioException.class,
                () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(2), null)));
        assertTrue(ycBaja.getMessage().contains("Registrar salida"));
        assertThrows(YCNegocioException.class, () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(4), null)));
        ycService.ycEditar(9L, new YCEstadiaService.YCEdicion(1L, "Ana Maria Diaz", null, null, 1, ycHoy, ycHoy.plusDays(3),
                null, "Llega tarde", null));
        assertEquals("Ana Maria Diaz", ycE.getYcHuesped());
        assertEquals("Llega tarde", ycE.getYcNota());
    }

    @Test
    void ycNoSeEditaUnaCanceladaNiSeCruzaConOtraNiRecepcionCambiaPrecio() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycEstadiaConSaldo();
        assertThrows(YCNegocioException.class, () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(3), "150")),
                "recepcion no cambia precios");
        when(ycEstadias.ycCruzadas(1L, ycHoy, ycHoy.plusDays(6), 9L)).thenReturn(List.of(
                YCEstadia.builder().ycHuesped("Luis").ycEntrada(ycHoy.plusDays(4)).ycSalida(ycHoy.plusDays(7)).build()));
        assertThrows(YCNegocioException.class, () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(6), null)));
        ycE.setYcCancelada(true);
        assertThrows(YCNegocioException.class, () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy, ycHoy.plusDays(3), null)));
    }

    @Test
    void ycUnaEstadiaTerminadaSoloCorrigeDatosDelHuesped() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcEntrada(ycHoy.minusDays(5));
        ycE.setYcSalida(ycHoy.minusDays(2));
        assertThrows(YCNegocioException.class,
                () -> ycService.ycEditar(9L, ycEdicion(ycE, ycHoy.minusDays(5), ycHoy.minusDays(1), null)));
        ycService.ycEditar(9L, new YCEstadiaService.YCEdicion(1L, "Ana Diaz Rojas", "44556677", "999888777", 1,
                ycHoy.minusDays(5), ycHoy.minusDays(2), null, null, null));
        assertEquals("999888777", ycE.getYcTelefono());
    }

    @Test
    void ycLaDevolucionGuardaConQueSeDevolvio() {
        ycIngresar(YCRol.YC_RECEPCION, "jose");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcEntrada(ycHoy.minusDays(1));
        ycE.setYcSalida(ycHoy.plusDays(3));
        ycE.setYcNoches(4);
        ycE.setYcTotal(new BigDecimal("400.00"));
        ycE.setYcAdelanto(new BigDecimal("400.00"));
        ycE.setYcSaldoPagado(true);

        ycService.ycRegistrarSalidaAnticipada(9L, ycHoy.plusDays(1), true, null, YCMetodoPago.YC_YAPE);

        assertEquals(YCMetodoPago.YC_YAPE, ycE.getYcMetodoDevolucion());
        assertEquals(new BigDecimal("200.00"), ycE.getYcDevuelto());
        assertEquals("jose", ycE.getYcDevueltoPor());
        assertNotNull(ycE.getYcFechaDevolucion());
    }

    @Test
    void ycNoVinoDevuelveElAdelantoPorPlin() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCEstadia ycE = ycEstadiaConSaldo();
        ycE.setYcLlego(false);
        ycService.ycRegistrarNoLlego(9L, true, YCMetodoPago.YC_PLIN);
        assertEquals(YCMetodoPago.YC_PLIN, ycE.getYcMetodoDevolucion());
        assertEquals(new BigDecimal("100.00"), ycE.getYcDevuelto());
    }

    private void ycPreparaGrupo() {
        YCCuarto ycOtra = YCCuarto.builder().ycId(2L).ycNumero("Habitacion 402").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("60")).ycCapacidad(3).build();
        when(ycCuartos.findById(2L)).thenReturn(Optional.of(ycOtra));
        java.util.concurrent.atomic.AtomicLong ycIds = new java.util.concurrent.atomic.AtomicLong(20);
        when(ycEstadias.save(any())).thenAnswer(ycI -> {
            YCEstadia ycE = ycI.getArgument(0);
            if (ycE.getYcId() == null) {
                ycE.setYcId(ycIds.incrementAndGet());
            }
            return ycE;
        });
    }

    private YCEstadiaService.YCSolicitud ycDatosGrupo(BigDecimal ycAdelanto) {
        return new YCEstadiaService.YCSolicitud(null, "Ana Diaz", "44556677", "987654321", null, ycHoy, ycHoy.plusDays(2),
                null, YCMetodoPago.YC_EFECTIVO, null, ycAdelanto, YCCanalReserva.YC_EN_PERSONA);
    }

    @Test
    void ycUnaPersonaAlquilaVariasHabitacionesYPagaTodo() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        ycPreparaGrupo();
        List<YCEstadia> ycGrupo = ycService.ycRegistrarGrupo(ycDatosGrupo(null), List.of(
                new YCEstadiaService.YCHabitacionGrupo(1L, 2, null),
                new YCEstadiaService.YCHabitacionGrupo(2L, 3, null)));

        assertEquals(2, ycGrupo.size());
        assertEquals(ycGrupo.get(0).getYcId(), ycGrupo.get(0).getYcGrupo());
        assertEquals(ycGrupo.get(0).getYcId(), ycGrupo.get(1).getYcGrupo(), "las dos quedan unidas en el mismo grupo");
        assertEquals("Ana Diaz", ycGrupo.get(1).getYcHuesped());
        assertEquals(new BigDecimal("200.00"), ycGrupo.get(0).getYcTotal());
        assertEquals(new BigDecimal("120.00"), ycGrupo.get(1).getYcTotal());
        assertTrue(ycGrupo.stream().noneMatch(YCEstadia::ycTieneSaldo), "sin adelanto: paga todo");
    }

    @Test
    void ycElAdelantoDelGrupoSeRepartePorHabitacion() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        ycPreparaGrupo();
        List<YCEstadia> ycGrupo = ycService.ycRegistrarGrupo(ycDatosGrupo(new BigDecimal("250")), List.of(
                new YCEstadiaService.YCHabitacionGrupo(1L, 1, null),
                new YCEstadiaService.YCHabitacionGrupo(2L, 1, null)));

        assertEquals(new BigDecimal("200.00"), ycGrupo.get(0).ycGetPagado());
        assertEquals(new BigDecimal("50.00"), ycGrupo.get(1).ycGetPagado());
        assertEquals(new BigDecimal("70.00"), ycGrupo.get(1).ycGetSaldo());

        when(ycEstadias.ycFindByGrupoOrderByIdAsc(ycGrupo.get(0).getYcGrupo())).thenReturn(ycGrupo);
        when(ycEstadias.findById(ycGrupo.get(1).getYcId())).thenReturn(Optional.of(ycGrupo.get(1)));
        ycService.ycCobrarSaldoGrupo(ycGrupo.get(0), YCMetodoPago.YC_YAPE);
        assertTrue(ycGrupo.stream().noneMatch(YCEstadia::ycTieneSaldo), "un solo cobro paga el saldo de todo el grupo");
    }

    @Test
    void ycElAdelantoNoPuedePasarDelTotalDelGrupo() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        ycPreparaGrupo();
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrarGrupo(ycDatosGrupo(new BigDecimal("321")), List.of(
                new YCEstadiaService.YCHabitacionGrupo(1L, 1, null),
                new YCEstadiaService.YCHabitacionGrupo(2L, 1, null))));
    }

    @Test
    void ycNoSeEligeLaMismaHabitacionDosVecesEnUnGrupo() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        ycPreparaGrupo();
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrarGrupo(ycDatosGrupo(null), List.of(
                new YCEstadiaService.YCHabitacionGrupo(1L, 1, null),
                new YCEstadiaService.YCHabitacionGrupo(1L, 1, null))));
    }

    @Test
    void ycRegistrosAntiguosSinMetodoSeTomanComoEfectivo() {
        YCEstadia ycE = YCEstadia.builder().build();
        assertEquals(YCMetodoPago.YC_EFECTIVO, ycE.ycGetMetodoDevolucionODefecto());
    }
}
