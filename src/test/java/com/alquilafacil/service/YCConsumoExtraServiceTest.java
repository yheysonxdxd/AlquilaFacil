package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.*;
import com.alquilafacil.repository.YCAuditoriaRepository;
import com.alquilafacil.repository.YCConsumoExtraRepository;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class YCConsumoExtraServiceTest {
    private final YCEstadiaRepository ycEstadias = mock(YCEstadiaRepository.class);
    private final YCCuartoRepository ycCuartos = mock(YCCuartoRepository.class);
    private final YCConsumoExtraRepository ycRepo = mock(YCConsumoExtraRepository.class);
    private final YCSesion ycSesion = new YCSesion();
    private final YCEstadiaService ycEstadiaService = new YCEstadiaService(ycEstadias, ycCuartos, ycSesion);
    private final YCAuditoriaService ycAuditoria = new YCAuditoriaService(mock(YCAuditoriaRepository.class), ycSesion);
    private final YCConsumoExtraService ycService = new YCConsumoExtraService(ycRepo, ycEstadiaService, ycSesion, ycAuditoria);

    private final List<YCConsumoExtra> ycGuardados = new ArrayList<>();
    private YCEstadia ycEstadia;

    @BeforeEach
    void ycDatos() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(2L).ycUsuario("maria").ycRol(YCRol.YC_RECEPCION).build());
        YCCuarto ycC = YCCuarto.builder().ycId(1L).ycNumero("Habitacion 401").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("100")).build();
        LocalDate ycHoy = LocalDate.now();
        ycEstadia = YCEstadia.builder().ycId(9L).ycCuarto(ycC).ycCuartoNumero("Habitacion 401").ycHuesped("Ana Diaz").ycPersonas(1)
                .ycEntrada(ycHoy.minusDays(1)).ycSalida(ycHoy.plusDays(1)).ycNoches(2).ycPrecioNoche(new BigDecimal("100.00"))
                .ycTotal(new BigDecimal("200.00")).ycAdelanto(new BigDecimal("200.00")).ycSaldoPagado(true)
                .ycMetodo(YCMetodoPago.YC_EFECTIVO).ycCancelada(false).ycLlego(true).ycNoLlego(false)
                .ycFechaRegistro(LocalDateTime.now()).build();
        when(ycEstadias.findById(9L)).thenReturn(Optional.of(ycEstadia));
        when(ycCuartos.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        when(ycEstadias.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        when(ycRepo.save(any())).thenAnswer(ycI -> {
            YCConsumoExtra ycX = ycI.getArgument(0);
            if (ycX.getYcId() == null) {
                ycX.setYcId((long) ycGuardados.size() + 1);
                ycGuardados.add(ycX);
            }
            return ycX;
        });
        when(ycRepo.ycFindByEstadiaIdInOrderByFechaRegistroAscIdAsc(anyCollection())).thenAnswer(ycI -> List.copyOf(ycGuardados));
        when(ycRepo.findById(any())).thenAnswer(ycI -> ycGuardados.stream().filter(ycX -> ycX.getYcId().equals(ycI.getArgument(0))).findFirst());
        ycEstadiaService.ycSetConsumos(ycRepo);
    }

    @Test
    void ycUnConsumoALaCuentaQuedaPendiente() {
        YCConsumoExtra ycC = ycService.ycAgregar(9L, " Desayuno ", 2, new BigDecimal("12.5"), null);
        assertEquals("Desayuno x2", ycC.ycGetDescripcion());
        assertEquals(new BigDecimal("25.00"), ycC.getYcTotal());
        assertTrue(ycC.ycIsPendiente());
        assertEquals("maria", ycC.getYcRegistradoPor());
        assertEquals(new BigDecimal("25.00"), ycService.ycPendienteDe(ycEstadia));
    }

    @Test
    void ycSiPagaAhoraQuedaCobradoConSuMetodo() {
        YCConsumoExtra ycC = ycService.ycAgregar(9L, "Lavanderia", 1, new BigDecimal("15"), YCMetodoPago.YC_YAPE);
        assertTrue(ycC.isYcPagado());
        assertEquals(YCMetodoPago.YC_YAPE, ycC.getYcMetodo());
        assertEquals("maria", ycC.getYcCobradoPor());
        assertEquals(BigDecimal.ZERO, ycService.ycPendienteDe(ycEstadia));
    }

    @Test
    void ycUnCobroPagaTodoLoPendiente() {
        ycService.ycAgregar(9L, "Desayuno", 2, new BigDecimal("10"), null);
        ycService.ycAgregar(9L, "Gaseosa", 3, new BigDecimal("4"), null);
        BigDecimal ycCobrado = ycService.ycCobrarPendientes(ycEstadia, YCMetodoPago.YC_PLIN);
        assertEquals(new BigDecimal("32.00"), ycCobrado);
        assertTrue(ycGuardados.stream().allMatch(YCConsumoExtra::isYcPagado));
        assertTrue(ycGuardados.stream().allMatch(ycC -> ycC.getYcMetodo() == YCMetodoPago.YC_PLIN));
    }

    @Test
    void ycNoSeVaConConsumosSinPagar() {
        ycService.ycAgregar(9L, "Cochera", 1, new BigDecimal("10"), null);
        YCNegocioException ycEx = assertThrows(YCNegocioException.class,
                () -> ycEstadiaService.ycRegistrarSalidaAnticipada(9L, LocalDate.now(), true, null));
        assertTrue(ycEx.getMessage().contains("consumos"));
    }

    @Test
    void ycSoloElAdministradorAnulaYConMotivo() {
        YCConsumoExtra ycC = ycService.ycAgregar(9L, "Agua", 1, new BigDecimal("3"), null);
        assertThrows(YCNegocioException.class, () -> ycService.ycAnular(ycC.getYcId(), "Se cargo por error"));

        ycSesion.ycIniciar(YCUsuario.builder().ycId(1L).ycUsuario("admin").ycRol(YCRol.YC_ADMINISTRADOR).build());
        assertThrows(YCNegocioException.class, () -> ycService.ycAnular(ycC.getYcId(), "x"));
        ycService.ycAnular(ycC.getYcId(), "Se cargo por error");
        assertTrue(ycC.isYcAnulado());
        assertFalse(ycC.ycIsPendiente(), "anulado ya no se cobra");
    }

    @Test
    void ycValidaPrecioYCantidad() {
        assertThrows(YCNegocioException.class, () -> ycService.ycAgregar(9L, "Desayuno", 1, BigDecimal.ZERO, null));
        assertThrows(YCNegocioException.class, () -> ycService.ycAgregar(9L, "Desayuno", 0, BigDecimal.TEN, null));
        assertThrows(YCNegocioException.class, () -> ycService.ycAgregar(9L, "", 1, BigDecimal.TEN, null));
        ycEstadia.setYcCancelada(true);
        assertThrows(YCNegocioException.class, () -> ycService.ycAgregar(9L, "Desayuno", 1, BigDecimal.TEN, null));
    }
}
