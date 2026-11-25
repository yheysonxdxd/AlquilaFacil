package com.alquilafacil.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class YCExcel {
    public enum YCTipo { YC_TEXTO, YC_ENTERO, YC_MONEDA, YC_FECHA, YC_FECHA_HORA, YC_PORCENTAJE }

    private static final int YC_S_NORMAL = 0, YC_S_CABECERA = 1, YC_S_MONEDA = 2, YC_S_ENTERO = 3, YC_S_FECHA = 4,
            YC_S_FECHA_HORA = 5, YC_S_TITULO = 6, YC_S_TOTAL_TEXTO = 7, YC_S_TOTAL_MONEDA = 8, YC_S_PORCENTAJE = 9,
            YC_S_SUBTITULO = 10, YC_S_TOTAL_ENTERO = 11;

    private YCExcel() {
    }

    private record YCColumna(String ycNombre, YCTipo ycTipo, double ycAncho) {}

    public static final class YCHoja {
        private final String ycNombre;
        private final List<String> ycTitulos = new ArrayList<>();
        private final List<YCColumna> ycColumnas = new ArrayList<>();
        private final List<Object[]> ycFilas = new ArrayList<>();
        private Object[] ycTotales;

        public YCHoja(String ycNombre) {
            this.ycNombre = ycNombre;
        }

        public YCHoja ycTitulo(String ycLinea) {
            ycTitulos.add(ycLinea);
            return this;
        }

        public YCHoja ycColumna(String ycNombre, YCTipo ycTipo, double ycAncho) {
            ycColumnas.add(new YCColumna(ycNombre, ycTipo, ycAncho));
            return this;
        }

        public YCHoja ycFila(Object... ycValores) {
            ycFilas.add(ycValores);
            return this;
        }

        public YCHoja ycTotales(Object... ycValores) {
            ycTotales = ycValores;
            return this;
        }

        public int ycCantidadFilas() {
            return ycFilas.size();
        }
    }

    public static void ycEscribir(File ycDestino, List<YCHoja> ycHojas) throws IOException {
        try (OutputStream ycOut = new FileOutputStream(ycDestino)) {
            ycEscribir(ycOut, ycHojas);
        }
    }

    public static void ycEscribir(OutputStream ycOut, List<YCHoja> ycHojas) throws IOException {
        if (ycHojas.isEmpty()) {
            throw new IllegalArgumentException("El archivo necesita al menos una hoja");
        }
        List<String> ycNombres = ycNombresValidos(ycHojas);
        try (ZipOutputStream ycZip = new ZipOutputStream(ycOut)) {
            ycAgregar(ycZip, "[Content_Types].xml", ycContentTypes(ycHojas.size()));
            ycAgregar(ycZip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            ycAgregar(ycZip, "xl/workbook.xml", ycWorkbook(ycHojas, ycNombres));
            ycAgregar(ycZip, "xl/_rels/workbook.xml.rels", ycWorkbookRels(ycHojas.size()));
            ycAgregar(ycZip, "xl/styles.xml", YC_ESTILOS);
            for (int ycI = 0; ycI < ycHojas.size(); ycI++) {
                ycAgregar(ycZip, "xl/worksheets/sheet" + (ycI + 1) + ".xml", ycHoja(ycHojas.get(ycI)));
            }
        }
    }

    private static String ycContentTypes(int ycN) {
        StringBuilder ycSb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int ycI = 1; ycI <= ycN; ycI++) {
            ycSb.append("<Override PartName=\"/xl/worksheets/sheet").append(ycI)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        return ycSb.append("</Types>").toString();
    }

    private static String ycWorkbook(List<YCHoja> ycHojas, List<String> ycNombres) {
        StringBuilder ycSb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                """);
        for (int ycI = 0; ycI < ycHojas.size(); ycI++) {
            ycSb.append("<sheet name=\"").append(ycXml(ycNombres.get(ycI))).append("\" sheetId=\"").append(ycI + 1)
                    .append("\" r:id=\"rId").append(ycI + 1).append("\"/>\n");
        }
        ycSb.append("</sheets>\n<definedNames>\n");
        for (int ycI = 0; ycI < ycHojas.size(); ycI++) {
            YCHoja ycH = ycHojas.get(ycI);
            if (ycH.ycColumnas.isEmpty()) {
                continue;
            }
            int ycCab = ycFilaCabecera(ycH);
            int ycUltima = ycCab + Math.max(1, ycH.ycFilas.size());
            ycSb.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"").append(ycI)
                    .append("\" hidden=\"1\">'").append(ycXml(ycNombres.get(ycI).replace("'", "''"))).append("'!$A$").append(ycCab)
                    .append(":$").append(ycLetra(ycH.ycColumnas.size() - 1)).append("$").append(ycUltima).append("</definedName>\n");
        }
        return ycSb.append("</definedNames>\n</workbook>").toString();
    }

    private static String ycWorkbookRels(int ycN) {
        StringBuilder ycSb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        for (int ycI = 1; ycI <= ycN; ycI++) {
            ycSb.append("<Relationship Id=\"rId").append(ycI)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(ycI).append(".xml\"/>\n");
        }
        ycSb.append("<Relationship Id=\"rId").append(ycN + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        return ycSb.append("</Relationships>").toString();
    }

    private static int ycFilaCabecera(YCHoja ycH) {
        return ycH.ycTitulos.isEmpty() ? 1 : ycH.ycTitulos.size() + 2;
    }

    private static String ycHoja(YCHoja ycH) {
        int ycCab = ycFilaCabecera(ycH);
        StringBuilder ycSb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                """);
        if (!ycH.ycColumnas.isEmpty()) {
            ycSb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"").append(ycCab)
                    .append("\" topLeftCell=\"A").append(ycCab + 1)
                    .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>\n");
            ycSb.append("<cols>");
            for (int ycI = 0; ycI < ycH.ycColumnas.size(); ycI++) {
                ycSb.append("<col min=\"").append(ycI + 1).append("\" max=\"").append(ycI + 1).append("\" width=\"")
                        .append(ycH.ycColumnas.get(ycI).ycAncho()).append("\" customWidth=\"1\"/>");
            }
            ycSb.append("</cols>\n");
        }
        ycSb.append("<sheetData>\n");
        int ycFila = 1;
        for (int ycI = 0; ycI < ycH.ycTitulos.size(); ycI++, ycFila++) {
            ycSb.append("<row r=\"").append(ycFila).append("\">");
            ycCeldaTexto(ycSb, "A" + ycFila, ycH.ycTitulos.get(ycI), ycI == 0 ? YC_S_TITULO : YC_S_SUBTITULO);
            ycSb.append("</row>\n");
        }
        if (!ycH.ycColumnas.isEmpty()) {
            ycFila = ycCab;
            ycSb.append("<row r=\"").append(ycFila).append("\">");
            for (int ycC = 0; ycC < ycH.ycColumnas.size(); ycC++) {
                ycCeldaTexto(ycSb, ycLetra(ycC) + ycFila, ycH.ycColumnas.get(ycC).ycNombre(), YC_S_CABECERA);
            }
            ycSb.append("</row>\n");
            for (Object[] ycValores : ycH.ycFilas) {
                ycFila++;
                ycSb.append("<row r=\"").append(ycFila).append("\">");
                for (int ycC = 0; ycC < ycH.ycColumnas.size(); ycC++) {
                    ycCelda(ycSb, ycLetra(ycC) + ycFila, ycC < ycValores.length ? ycValores[ycC] : null, ycH.ycColumnas.get(ycC).ycTipo(), false);
                }
                ycSb.append("</row>\n");
            }
            if (ycH.ycTotales != null) {
                ycFila++;
                ycSb.append("<row r=\"").append(ycFila).append("\">");
                for (int ycC = 0; ycC < ycH.ycColumnas.size(); ycC++) {
                    ycCelda(ycSb, ycLetra(ycC) + ycFila, ycC < ycH.ycTotales.length ? ycH.ycTotales[ycC] : null, ycH.ycColumnas.get(ycC).ycTipo(), true);
                }
                ycSb.append("</row>\n");
            }
        }
        ycSb.append("</sheetData>\n");
        if (!ycH.ycColumnas.isEmpty()) {
            int ycUltima = ycCab + Math.max(1, ycH.ycFilas.size());
            ycSb.append("<autoFilter ref=\"A").append(ycCab).append(":").append(ycLetra(ycH.ycColumnas.size() - 1)).append(ycUltima).append("\"/>\n");
        }
        ycSb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>\n");
        return ycSb.append("</worksheet>").toString();
    }

    private static void ycCelda(StringBuilder ycSb, String ycRef, Object ycV, YCTipo ycTipo, boolean ycTotal) {
        if (ycV == null) {
            if (ycTotal) {
                ycSb.append("<c r=\"").append(ycRef).append("\" s=\"").append(YC_S_TOTAL_TEXTO).append("\"/>");
            }
            return;
        }
        if (ycV instanceof LocalDateTime ycT) {
            ycNumero(ycSb, ycRef, ycSerial(ycT), YC_S_FECHA_HORA);
        } else if (ycV instanceof LocalDate ycD) {
            ycNumero(ycSb, ycRef, String.valueOf(ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), ycD)), YC_S_FECHA);
        } else if (ycV instanceof Number ycN) {
            BigDecimal ycB = ycV instanceof BigDecimal ycBd ? ycBd : new BigDecimal(ycN.toString());
            int ycEstilo = switch (ycTipo) {
                case YC_MONEDA -> ycTotal ? YC_S_TOTAL_MONEDA : YC_S_MONEDA;
                case YC_ENTERO -> ycTotal ? YC_S_TOTAL_ENTERO : YC_S_ENTERO;
                case YC_PORCENTAJE -> YC_S_PORCENTAJE;
                default -> ycTotal ? YC_S_TOTAL_MONEDA : YC_S_NORMAL;
            };
            if (ycTipo == YCTipo.YC_PORCENTAJE) {
                ycB = ycB.divide(BigDecimal.valueOf(100));
            }
            ycNumero(ycSb, ycRef, ycB.stripTrailingZeros().toPlainString(), ycEstilo);
        } else {
            ycCeldaTexto(ycSb, ycRef, ycV.toString(), ycTotal ? YC_S_TOTAL_TEXTO : YC_S_NORMAL);
        }
    }

    private static void ycNumero(StringBuilder ycSb, String ycRef, String ycValor, int ycEstilo) {
        ycSb.append("<c r=\"").append(ycRef).append("\" s=\"").append(ycEstilo).append("\"><v>").append(ycValor).append("</v></c>");
    }

    private static void ycCeldaTexto(StringBuilder ycSb, String ycRef, String ycTexto, int ycEstilo) {
        ycSb.append("<c r=\"").append(ycRef).append("\" s=\"").append(ycEstilo).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(ycXml(ycTexto)).append("</t></is></c>");
    }

    private static String ycSerial(LocalDateTime ycT) {
        long ycDias = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), ycT.toLocalDate());
        double ycFraccion = ycT.toLocalTime().toSecondOfDay() / 86400.0;
        return new BigDecimal(ycDias + ycFraccion).setScale(10, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    static String ycLetra(int ycIndice) {
        StringBuilder ycSb = new StringBuilder();
        int ycN = ycIndice + 1;
        while (ycN > 0) {
            int ycR = (ycN - 1) % 26;
            ycSb.insert(0, (char) ('A' + ycR));
            ycN = (ycN - 1) / 26;
        }
        return ycSb.toString();
    }

    private static List<String> ycNombresValidos(List<YCHoja> ycHojas) {
        List<String> ycR = new ArrayList<>();
        Set<String> ycUsados = new HashSet<>();
        for (YCHoja ycH : ycHojas) {
            String ycBase = (ycH.ycNombre == null || ycH.ycNombre.isBlank() ? "Hoja" : ycH.ycNombre).replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
            if (ycBase.length() > 31) {
                ycBase = ycBase.substring(0, 31);
            }
            String ycNombre = ycBase;
            for (int ycI = 2; !ycUsados.add(ycNombre.toLowerCase()); ycI++) {
                String ycSufijo = " " + ycI;
                ycNombre = (ycBase.length() + ycSufijo.length() > 31 ? ycBase.substring(0, 31 - ycSufijo.length()) : ycBase) + ycSufijo;
            }
            ycR.add(ycNombre);
        }
        return ycR;
    }

    private static String ycXml(String ycS) {
        StringBuilder ycSb = new StringBuilder(ycS.length());
        for (char ycCh : ycS.toCharArray()) {
            switch (ycCh) {
                case '&' -> ycSb.append("&amp;");
                case '<' -> ycSb.append("&lt;");
                case '>' -> ycSb.append("&gt;");
                case '"' -> ycSb.append("&quot;");
                default -> {
                    if (ycCh >= 0x20 || ycCh == '\n' || ycCh == '\t') {
                        ycSb.append(ycCh);
                    }
                }
            }
        }
        return ycSb.toString();
    }

    private static void ycAgregar(ZipOutputStream ycZip, String ycNombre, String ycContenido) throws IOException {
        ycZip.putNextEntry(new ZipEntry(ycNombre));
        ycZip.write(ycContenido.getBytes(StandardCharsets.UTF_8));
        ycZip.closeEntry();
    }

    private static final String YC_ESTILOS = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <numFmts count="2">
            <numFmt numFmtId="164" formatCode="dd/mm/yyyy"/>
            <numFmt numFmtId="165" formatCode="dd/mm/yyyy hh:mm"/>
            </numFmts>
            <fonts count="4">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="14"/><color rgb="FF0D9488"/><name val="Calibri"/></font>
            <font><sz val="10"/><color rgb="FF656D76"/><name val="Calibri"/></font>
            </fonts>
            <fills count="4">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFDDF3F0"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFF2F4F7"/><bgColor indexed="64"/></patternFill></fill>
            </fills>
            <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border><left/><right/><top style="thin"><color rgb="FF9AA4AE"/></top><bottom/><diagonal/></border>
            </borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="12">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            <xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="1" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
            <xf numFmtId="4" fontId="1" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
            <xf numFmtId="9" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="1" fontId="1" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
            </cellXfs>
            <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
            </styleSheet>""";
}
