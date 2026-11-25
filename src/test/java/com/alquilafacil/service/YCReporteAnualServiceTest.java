package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YCReporteAnualServiceTest {
    @TempDir
    Path ycTmp;

    private final YCGastoService ycGastos = mock(YCGastoService.class);
    private final YCReporteAnualService ycService = new YCReporteAnualService(ycGastos, new YCDatosArrendador());

    private static YCGastoService.YCResultado ycMes(YearMonth ycM, String ycAlquiler, String ycHospedaje, String ycGasto) {
        return new YCGastoService.YCResultado(ycM, new BigDecimal(ycAlquiler), new BigDecimal(ycHospedaje),
                new BigDecimal(ycGasto), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void ycCalculaCadaMesYElEstimadoSoloSobreElAlquiler() {
        YCReporteAnualService.YCFila ycF = ycService.ycFila(ycMes(YearMonth.of(2025, 3), "1000", "600", "300"));
        assertEquals(new BigDecimal("1600"), ycF.ycIngresos());
        assertEquals(new BigDecimal("1300"), ycF.ycGanancia());
        assertEquals(new BigDecimal("50.00"), ycF.ycImpuestoAlquiler(), "5% de 1000, sin contar el hospedaje");
    }

    @Test
    void ycUnAnioPasadoTiene12MesesYSusTotales() throws Exception {
        when(ycGastos.ycResultado(any())).thenAnswer(ycI -> ycMes(ycI.getArgument(0), "100", "50", "30"));
        YCReporteAnualService.YCAnual ycA = ycService.ycDatos(2024);

        assertEquals(12, ycA.ycMeses().size());
        assertEquals(new BigDecimal("1200"), ycA.ycTotal(YCReporteAnualService.YCFila::ycAlquiler));
        assertEquals(new BigDecimal("1440"), ycA.ycTotal(YCReporteAnualService.YCFila::ycGanancia));
        assertEquals(new BigDecimal("60.00"), ycA.ycTotal(YCReporteAnualService.YCFila::ycImpuestoAlquiler));

        File ycCsv = ycTmp.resolve("anual.csv").toFile();
        ycService.ycGenerarCsv(2024, ycCsv);
        List<String> ycLineas = Files.readAllLines(ycCsv.toPath(), StandardCharsets.UTF_8);
        assertEquals(14, ycLineas.size(), "cabecera + 12 meses + total");
        assertTrue(ycLineas.get(13).startsWith("TOTAL 2024;1200,00;600,00;1800,00;360,00;1440,00;60,00"));

        ByteArrayOutputStream ycOut = new ByteArrayOutputStream();
        ycService.ycEscribirPdf(ycA, ycOut);
        assertEquals("%PDF", new String(ycOut.toByteArray(), 0, 4));
    }

    @Test
    void ycElAnioActualLlegaHastaElMesDeHoy() {
        when(ycGastos.ycResultado(any())).thenAnswer(ycI -> ycMes(ycI.getArgument(0), "0", "0", "0"));
        int ycHoy = YearMonth.now().getYear();
        assertEquals(YearMonth.now().getMonthValue(), ycService.ycDatos(ycHoy).ycMeses().size());
    }
}
