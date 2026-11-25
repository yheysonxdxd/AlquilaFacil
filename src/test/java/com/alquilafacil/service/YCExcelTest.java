package com.alquilafacil.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class YCExcelTest {
    @TempDir
    Path ycTmp;

    static List<YCExcel.YCHoja> ycMuestra() {
        YCExcel.YCHoja ycPagos = new YCExcel.YCHoja("Pagos: noviembre/2026")
                .ycTitulo("Hospedaje Supply · Pagos de noviembre 2026")
                .ycTitulo("Generado el 11/11/2026")
                .ycColumna("Fecha", YCExcel.YCTipo.YC_FECHA_HORA, 17)
                .ycColumna("Recibo", YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Inquilino", YCExcel.YCTipo.YC_TEXTO, 28)
                .ycColumna("Vence", YCExcel.YCTipo.YC_FECHA, 12)
                .ycColumna("Noches", YCExcel.YCTipo.YC_ENTERO, 9)
                .ycColumna("Total", YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Ocupacion", YCExcel.YCTipo.YC_PORCENTAJE, 11);
        ycPagos.ycFila(LocalDateTime.of(2026, 11, 4, 12, 11), "R-000039", "María Ñahui <\"&\">", LocalDate.of(2026, 11, 5),
                3, new BigDecimal("650.50"), 75);
        ycPagos.ycFila(LocalDateTime.of(2026, 11, 5, 9, 0), "R-000040", null, null, null, new BigDecimal("320"), null);
        ycPagos.ycTotales("Total", null, null, null, 3, new BigDecimal("970.50"), null);
        YCExcel.YCHoja ycVacia = new YCExcel.YCHoja("Pagos: noviembre/2026").ycColumna("A", YCExcel.YCTipo.YC_TEXTO, 10);
        return List.of(ycPagos, ycVacia);
    }

    @Test
    void ycCreaUnXlsxValido() throws Exception {
        File ycF = ycTmp.resolve("prueba.xlsx").toFile();
        YCExcel.ycEscribir(ycF, ycMuestra());
        try (ZipFile ycZip = new ZipFile(ycF)) {
            assertNotNull(ycZip.getEntry("[Content_Types].xml"));
            assertNotNull(ycZip.getEntry("xl/workbook.xml"));
            assertNotNull(ycZip.getEntry("xl/worksheets/sheet2.xml"));
            String ycLibro = new String(ycZip.getInputStream(ycZip.getEntry("xl/workbook.xml")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(ycLibro.contains("name=\"Pagos  noviembre 2026\""), "sin caracteres prohibidos");
            assertTrue(ycLibro.contains("name=\"Pagos  noviembre 2026 2\""), "sin nombres repetidos");
            String ycHoja = new String(ycZip.getInputStream(ycZip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(ycHoja.contains("María Ñahui &lt;&quot;&amp;&quot;&gt;"), "texto escapado");
            assertTrue(ycHoja.contains("<v>650.5</v>"), "montos como numero");
            assertTrue(ycHoja.contains("<v>46331</v>"), "fecha 05/11/2026 como fecha de Excel");
            assertTrue(ycHoja.contains("<v>0.75</v>"), "porcentaje");
            assertTrue(ycHoja.contains("autoFilter ref=\"A4:G6\""));
        }
        String ycDestino = System.getProperty("excel.muestra");
        if (ycDestino != null) {
            YCExcel.ycEscribir(new File(ycDestino), ycMuestra());
        }
    }

    @Test
    void ycLetrasDeColumna() {
        assertEquals("A", YCExcel.ycLetra(0));
        assertEquals("Z", YCExcel.ycLetra(25));
        assertEquals("AA", YCExcel.ycLetra(26));
        assertEquals("AZ", YCExcel.ycLetra(51));
    }
}
