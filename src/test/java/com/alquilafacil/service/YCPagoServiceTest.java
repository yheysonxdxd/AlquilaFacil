package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.*;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class YCPagoServiceTest {
    private final YCPagoRepository ycPagos = mock(YCPagoRepository.class);
    private final YCInquilinoRepository ycInquilinos = mock(YCInquilinoRepository.class);
    private final YCConsumoServicioRepository ycConsumos = mock(YCConsumoServicioRepository.class);
    private final YCSesion ycSesion = new YCSesion();
    private final YCPagoService ycService = new YCPagoService(ycPagos, ycInquilinos, ycConsumos, ycSesion);

    private final YearMonth ycMes = YearMonth.now();
    private YCInquilino ycJuan;

    @BeforeEach
    void ycDatos() {
        YCCuarto ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Cuarto 101").ycPrecio(new BigDecimal("350")).build();
        ycJuan = YCInquilino.builder().ycId(5L).ycNombre("Juan").ycApellido("Perez").ycDni("12345678").ycCuarto(ycCuarto)
                .ycFechaIngreso(ycMes.minusMonths(2).atDay(1)).ycDiaPago(5).ycActivo(true).build();
        when(ycInquilinos.findById(5L)).thenReturn(Optional.of(ycJuan));
        when(ycPagos.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
    }

    private void ycIngresar(YCRol ycRol, String ycUsuario) {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(ycRol == YCRol.YC_ADMINISTRADOR ? 1L : 2L).ycUsuario(ycUsuario).ycRol(ycRol).build());
    }

    private YCPago ycPagoGuardado() {
        return YCPago.builder().ycId(25L).ycInquilino(ycJuan).ycCuarto(ycJuan.getYcCuarto()).ycCuartoNumero("Cuarto 101")
                .ycPeriodo(ycMes).ycTotal(new BigDecimal("380")).ycAnulado(false).build();
    }

    @Test
    void ycRegistraElPagoConSuTotalYQuienCobro() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        YCPago ycP = ycService.ycRegistrar(5L, ycMes, new BigDecimal("350"), new BigDecimal("20.5"), new BigDecimal("9.5"),
                null, "  ");

        assertEquals(new BigDecimal("380.00"), ycP.getYcTotal());
        assertEquals(YCMetodoPago.YC_EFECTIVO, ycP.getYcMetodo(), "sin metodo se asume efectivo");
        assertEquals("maria", ycP.getYcRegistradoPor());
        assertEquals("Cuarto 101", ycP.getYcCuartoNumero());
        assertNull(ycP.getYcNota(), "una nota en blanco no se guarda");
        assertFalse(ycP.isYcAnulado());
    }

    @Test
    void ycNoSeCobraDosVecesElMismoMes() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        when(ycPagos.ycExistsByInquilinoIdAndPeriodoAndAnuladoFalse(5L, ycMes)).thenReturn(true);

        YCNegocioException ycE = assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(5L, ycMes,
                new BigDecimal("350"), null, null, YCMetodoPago.YC_YAPE, null));
        assertTrue(ycE.getMessage().contains("ya fue pagado"));
        verify(ycPagos, never()).save(any());
    }

    @Test
    void ycNoSeCobraAUnInquilinoQueYaSeFue() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        ycJuan.setYcActivo(false);
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(5L, ycMes, new BigDecimal("350"),
                null, null, YCMetodoPago.YC_EFECTIVO, null));
    }

    @Test
    void ycRechazaMontosNegativosOEnCero() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(5L, ycMes, new BigDecimal("-1"),
                null, null, YCMetodoPago.YC_EFECTIVO, null));
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(5L, ycMes, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, YCMetodoPago.YC_EFECTIVO, null));
        assertThrows(YCNegocioException.class, () -> ycService.ycRegistrar(5L, null, new BigDecimal("350"),
                null, null, YCMetodoPago.YC_EFECTIVO, null));
    }

    @Test
    void ycRecepcionNoPuedeAnular() {
        ycIngresar(YCRol.YC_RECEPCION, "maria");
        when(ycPagos.findById(25L)).thenReturn(Optional.of(ycPagoGuardado()));
        assertThrows(YCNegocioException.class, () -> ycService.ycAnular(25L, "Me equivoque de mes"));
    }

    @Test
    void ycAnularPideMotivo() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCPago ycP = ycPagoGuardado();
        when(ycPagos.findById(25L)).thenReturn(Optional.of(ycP));

        assertThrows(YCNegocioException.class, () -> ycService.ycAnular(25L, "  "));
        assertFalse(ycP.isYcAnulado(), "sin motivo el recibo sigue valido");
    }

    @Test
    void ycAnularGuardaMotivoQuienYCuando() {
        ycIngresar(YCRol.YC_ADMINISTRADOR, "admin");
        YCPago ycP = ycPagoGuardado();
        when(ycPagos.findById(25L)).thenReturn(Optional.of(ycP));

        ycService.ycAnular(25L, "Se registro en el mes equivocado");

        assertTrue(ycP.isYcAnulado());
        assertEquals("Se registro en el mes equivocado", ycP.getYcMotivoAnulacion());
        assertEquals("admin", ycP.getYcAnuladoPor());
        assertNotNull(ycP.getYcFechaAnulacion());
        assertThrows(YCNegocioException.class, () -> ycService.ycAnular(25L, "Otra vez"), "no se anula dos veces");
    }

    @Test
    void ycLosMesesNoPagadosQuedanPendientes() {
        YCPago ycPasado = ycPagoGuardado();
        ycPasado.setYcPeriodo(ycMes.minusMonths(1));
        when(ycPagos.ycFindByInquilinoIdAndAnuladoFalse(anyLong())).thenReturn(List.of(ycPasado));

        List<YCDeudaPendiente> ycDeudas = ycService.ycPendientes(ycJuan, false);

        assertEquals(List.of(ycMes.minusMonths(2), ycMes), ycDeudas.stream().map(YCDeudaPendiente::ycPeriodo).toList());
        assertEquals(new BigDecimal("350"), ycDeudas.get(0).ycTotal());
        assertTrue(ycDeudas.get(0).ycAtrasado());
        assertEquals(LocalDate.from(ycMes.minusMonths(2).atDay(5)), ycDeudas.get(0).ycVence());
    }
}
