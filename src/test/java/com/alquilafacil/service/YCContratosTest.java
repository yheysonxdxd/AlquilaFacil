package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YCContratosTest {
    private final YCInquilinoRepository ycRepo = mock(YCInquilinoRepository.class);
    private final YCInquilinoService ycService = new YCInquilinoService(ycRepo, mock(YCPagoRepository.class), new YCSesion());
    private final LocalDate ycHoy = LocalDate.now();
    private YCCuarto ycCuarto;

    @BeforeEach
    void ycDatos() {
        ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Cuarto 101").ycPrecio(new BigDecimal("350")).ycDescripcion("Con baño propio").build();
        when(ycRepo.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
        when(ycRepo.ycFindByDniAndActivoTrue(any())).thenReturn(Optional.empty());
        when(ycRepo.ycFindByCuartoIdAndActivoTrue(any())).thenReturn(Optional.empty());
    }

    private YCInquilino ycInquilino(long ycId, LocalDate ycFin) {
        YCInquilino ycI = YCInquilino.builder().ycId(ycId).ycNombre("Juan").ycApellido("Perez " + ycId).ycDni("1234567" + ycId)
                .ycCuarto(ycCuarto).ycFechaIngreso(ycHoy.minusMonths(11)).ycDiaPago(5)
                .ycGarantia(new BigDecimal("350")).ycActivo(true).ycFechaFinContrato(ycFin).build();
        when(ycRepo.findById(ycId)).thenReturn(Optional.of(ycI));
        return ycI;
    }

    @Test
    void ycAvisaLosQueVencenEn30DiasOYaVencieronDelMasUrgenteAlUltimo() {
        YCInquilino ycLejos = ycInquilino(1, ycHoy.plusDays(90));
        YCInquilino ycPronto = ycInquilino(2, ycHoy.plusDays(20));
        YCInquilino ycVencido = ycInquilino(3, ycHoy.minusDays(5));
        YCInquilino ycSinFin = ycInquilino(4, null);
        when(ycRepo.ycFindByActivoTrueOrderByApellidoAscNombreAsc()).thenReturn(List.of(ycLejos, ycPronto, ycVencido, ycSinFin));

        assertEquals(List.of(ycVencido, ycPronto), ycService.ycContratosPorVencer(30));
        assertEquals(-5L, ycVencido.ycDiasParaFinContrato(ycHoy));
        assertNull(ycSinFin.ycDiasParaFinContrato(ycHoy));
    }

    @Test
    void ycRenovarSumaDesdeLaFechaDeFin() {
        YCInquilino ycI = ycInquilino(1, ycHoy.plusDays(10));
        ycService.ycRenovarContrato(1L, 12);
        assertEquals(ycHoy.plusDays(10).plusMonths(12), ycI.getYcFechaFinContrato());
    }

    @Test
    void ycRenovarUnContratoMuyVencidoOSinFechaCuentaDesdeHoy() {
        YCInquilino ycViejo = ycInquilino(1, ycHoy.minusMonths(3));
        ycService.ycRenovarContrato(1L, 12);
        assertEquals(ycHoy.plusMonths(12), ycViejo.getYcFechaFinContrato());

        YCInquilino ycSinFin = ycInquilino(2, null);
        ycService.ycRenovarContrato(2L, 6);
        assertEquals(ycHoy.plusMonths(6), ycSinFin.getYcFechaFinContrato());

        assertThrows(YCNegocioException.class, () -> ycService.ycRenovarContrato(2L, 0));
    }

    @Test
    void ycElFinDelContratoDebeSerDespuesDelIngreso() {
        YCInquilino ycI = YCInquilino.builder().ycNombre("Ana").ycApellido("Diaz").ycDni("44556677").ycCuarto(ycCuarto)
                .ycFechaIngreso(ycHoy).ycDiaPago(5).ycFechaFinContrato(ycHoy.minusDays(1)).build();
        YCNegocioException ycE = assertThrows(YCNegocioException.class, () -> ycService.ycGuardar(ycI));
        assertTrue(ycE.getMessage().contains("contrato"));

        ycI.setYcFechaFinContrato(ycHoy.plusYears(1));
        assertEquals(ycHoy.plusYears(1), ycService.ycGuardar(ycI).getYcFechaFinContrato());
    }

    @Test
    void ycElContratoTieneLosDatosDelAlquiler() {
        YCDatosArrendador ycArrendador = new YCDatosArrendador();
        ycArrendador.setYcNombre("Rosa Quispe");
        ycArrendador.setYcDni("40404040");
        ycArrendador.setYcDireccion("Av. Los Andes 123, Cusco");
        YCContratoPdfService ycPdf = new YCContratoPdfService(ycService, ycArrendador);
        YCInquilino ycI = ycInquilino(1, ycHoy.minusMonths(11).plusYears(1));

        String ycTexto = String.join("\n", ycPdf.ycClausulas(ycI).stream().map(ycC -> ycC[0] + " " + ycC[1]).toList());
        assertTrue(ycTexto.contains("Cuarto 101"));
        assertTrue(ycTexto.contains("Av. Los Andes 123, Cusco"));
        assertTrue(ycTexto.contains("S/ 350.00"));
        assertTrue(ycTexto.contains("TRESCIENTOS CINCUENTA") || ycTexto.toLowerCase().contains("trescientos cincuenta"));
        assertTrue(ycTexto.contains("dia 5"));
        assertTrue(ycTexto.contains("(12 meses)"));
        assertTrue(ycTexto.contains("garantia S/ 350.00"));

        ycI.setYcFechaFinContrato(null);
        ycI.setYcGarantia(BigDecimal.ZERO);
        String ycSinFin = String.join("\n", ycPdf.ycClausulas(ycI).stream().map(ycC -> ycC[1]).toList());
        assertTrue(ycSinFin.contains("plazo indeterminado"));
        assertTrue(ycSinFin.contains("no se entrega garantia"));

        ByteArrayOutputStream ycOut = new ByteArrayOutputStream();
        ycPdf.ycEscribir(ycI, ycHoy, ycOut);
        assertEquals("%PDF", new String(ycOut.toByteArray(), 0, 4));
        assertEquals("Contrato - Cuarto 101 - Juan Perez 1", YCContratoPdfService.ycNombreArchivo(ycI));
    }
}
