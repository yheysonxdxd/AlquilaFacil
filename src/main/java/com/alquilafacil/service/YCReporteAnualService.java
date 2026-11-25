package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class YCReporteAnualService {
    private static final Color YC_ACENTO = new Color(13, 148, 136);
    private static final Color YC_SUAVE = new Color(240, 250, 249);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_GRIS = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(214, 228, 226);
    private static final DecimalFormat YC_IMPORTE = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public record YCFila(YearMonth ycMes, BigDecimal ycAlquiler, BigDecimal ycHospedaje, BigDecimal ycGastos,
                       BigDecimal ycImpuestoAlquiler) {
        public BigDecimal ycIngresos() {
            return ycAlquiler.add(ycHospedaje);
        }

        public BigDecimal ycGanancia() {
            return ycIngresos().subtract(ycGastos);
        }
    }

    public record YCAnual(int ycAnio, BigDecimal ycTasaAlquiler, List<YCFila> ycMeses) {
        public BigDecimal ycTotal(Function<YCFila, BigDecimal> ycCampo) {
            return ycMeses.stream().map(ycCampo).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private final YCGastoService ycGastoService;
    private final YCDatosArrendador ycArrendador;

    public YCReporteAnualService(YCGastoService ycGastoService, YCDatosArrendador ycArrendador) {
        this.ycGastoService = ycGastoService;
        this.ycArrendador = ycArrendador;
    }

    private BigDecimal ycTasaAlquiler() {
        BigDecimal ycT = ycArrendador.getYcTasaImpuestoAlquiler();
        return ycT == null ? BigDecimal.valueOf(5) : ycT;
    }

    public YCAnual ycDatos(int ycAnio) {
        YearMonth ycHoy = YearMonth.now();
        List<YCFila> ycFilas = new ArrayList<>();
        for (int ycM = 1; ycM <= 12; ycM++) {
            YearMonth ycMes = YearMonth.of(ycAnio, ycM);
            if (ycMes.isAfter(ycHoy)) {
                break;
            }
            ycFilas.add(ycFila(ycGastoService.ycResultado(ycMes)));
        }
        return new YCAnual(ycAnio, ycTasaAlquiler(), ycFilas);
    }

    YCFila ycFila(YCGastoService.YCResultado ycR) {
        BigDecimal ycImpuesto = ycR.ycIngresosMensual().multiply(ycTasaAlquiler())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new YCFila(ycR.ycMes(), ycR.ycIngresosMensual(), ycR.ycIngresosHospedaje(), ycR.ycGastos(), ycImpuesto);
    }

    public void ycGenerarExcel(int ycAnio, File ycDestino) throws IOException {
        YCAnual ycA = ycDatos(ycAnio);
        String ycTasa = ycA.ycTasaAlquiler().stripTrailingZeros().toPlainString();
        YCExcel.YCHoja ycH = new YCExcel.YCHoja("Reporte " + ycAnio)
                .ycTitulo(ycArrendador.getYcNombreHospedaje() + " · Reporte anual " + ycAnio + " para el contador")
                .ycTitulo(ycArrendador.getYcNombre() + (ycArrendador.getYcDni() == null || ycArrendador.getYcDni().isBlank() ? ""
                        : " · DNI " + ycArrendador.getYcDni()))
                .ycTitulo("Impuesto del alquiler: estimado REFERENCIAL al " + ycTasa + "% de lo cobrado por alquiler mensual. "
                        + "El monto real lo confirma su contador.")
                .ycColumna("Mes", YCExcel.YCTipo.YC_TEXTO, 16).ycColumna("Alquiler mensual", YCExcel.YCTipo.YC_MONEDA, 16)
                .ycColumna("Hospedaje", YCExcel.YCTipo.YC_MONEDA, 14).ycColumna("Total ingresos", YCExcel.YCTipo.YC_MONEDA, 15)
                .ycColumna("Gastos", YCExcel.YCTipo.YC_MONEDA, 13).ycColumna("Ganancia", YCExcel.YCTipo.YC_MONEDA, 14)
                .ycColumna("Impuesto alquiler (" + ycTasa + "%)", YCExcel.YCTipo.YC_MONEDA, 20);
        for (YCFila ycF : ycA.ycMeses()) {
            ycH.ycFila(YCCalendarioPagos.ycTexto(ycF.ycMes()), ycF.ycAlquiler(), ycF.ycHospedaje(), ycF.ycIngresos(), ycF.ycGastos(), ycF.ycGanancia(),
                    ycF.ycImpuestoAlquiler());
        }
        ycH.ycTotales("TOTAL " + ycAnio, ycA.ycTotal(YCFila::ycAlquiler), ycA.ycTotal(YCFila::ycHospedaje), ycA.ycTotal(YCFila::ycIngresos),
                ycA.ycTotal(YCFila::ycGastos), ycA.ycTotal(YCFila::ycGanancia), ycA.ycTotal(YCFila::ycImpuestoAlquiler));
        YCExcel.ycEscribir(ycDestino, List.of(ycH));
    }

    public void ycGenerarCsv(int ycAnio, File ycDestino) throws IOException {
        YCAnual ycA = ycDatos(ycAnio);
        try (PrintWriter ycW = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ycDestino), StandardCharsets.UTF_8))) {
            ycW.print('﻿');
            ycW.println("Mes;Alquiler mensual;Hospedaje;Total ingresos;Gastos;Ganancia;Impuesto alquiler estimado ("
                    + ycA.ycTasaAlquiler().stripTrailingZeros().toPlainString() + "%)");
            for (YCFila ycF : ycA.ycMeses()) {
                ycW.println(String.join(";", YCCalendarioPagos.ycTexto(ycF.ycMes()), ycN(ycF.ycAlquiler()), ycN(ycF.ycHospedaje()),
                        ycN(ycF.ycIngresos()), ycN(ycF.ycGastos()), ycN(ycF.ycGanancia()), ycN(ycF.ycImpuestoAlquiler())));
            }
            ycW.println(String.join(";", "TOTAL " + ycAnio, ycN(ycA.ycTotal(YCFila::ycAlquiler)), ycN(ycA.ycTotal(YCFila::ycHospedaje)),
                    ycN(ycA.ycTotal(YCFila::ycIngresos)), ycN(ycA.ycTotal(YCFila::ycGastos)), ycN(ycA.ycTotal(YCFila::ycGanancia)),
                    ycN(ycA.ycTotal(YCFila::ycImpuestoAlquiler))));
        }
    }

    public void ycGenerarPdf(int ycAnio, File ycDestino) throws IOException {
        try (OutputStream ycOut = new FileOutputStream(ycDestino)) {
            ycEscribirPdf(ycDatos(ycAnio), ycOut);
        }
    }

    void ycEscribirPdf(YCAnual ycA, OutputStream ycOut) {
        Document ycDoc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(ycDoc, ycOut);
            ycDoc.addTitle("Reporte anual " + ycA.ycAnio());
            ycDoc.open();
            ycDoc.add(new Paragraph(ycArrendador.getYcNombreHospedaje(), ycF(15, Font.BOLD, YC_ACENTO)));
            ycDoc.add(new Paragraph("REPORTE ANUAL DE INGRESOS Y GASTOS " + ycA.ycAnio() + " · PARA EL CONTADOR", ycF(11, Font.BOLD, YC_TEXTO)));
            ycDoc.add(new Paragraph(ycArrendador.getYcNombre()
                    + (ycArrendador.getYcDni() == null || ycArrendador.getYcDni().isBlank() ? "" : " · DNI " + ycArrendador.getYcDni())
                    + " · Impreso el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    ycF(9, Font.NORMAL, YC_GRIS)));
            Paragraph ycSep = new Paragraph(" ");
            ycSep.setLeading(10);
            ycDoc.add(ycSep);

            String ycTasa = ycA.ycTasaAlquiler().stripTrailingZeros().toPlainString();
            PdfPTable ycT = new PdfPTable(new float[]{16, 14, 14, 14, 14, 14, 14});
            ycT.setWidthPercentage(100);
            ycT.setHeaderRows(1);
            for (String ycC : new String[]{"MES", "ALQUILER MENSUAL", "HOSPEDAJE", "TOTAL INGRESOS", "GASTOS",
                    "GANANCIA", "IMPUESTO ALQUILER (" + ycTasa + "%) *"}) {
                PdfPCell ycCel = new PdfPCell(new Phrase(ycC, ycF(8, Font.BOLD, Color.WHITE)));
                ycCel.setBackgroundColor(YC_ACENTO);
                ycCel.setBorderColor(YC_ACENTO);
                if (!ycC.equals("MES")) {
                    ycCel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                ycCel.setPadding(6);
                ycT.addCell(ycCel);
            }
            for (YCFila ycFl : ycA.ycMeses()) {
                ycFila(ycT, YCCalendarioPagos.ycTexto(ycFl.ycMes()), false, ycFl.ycAlquiler(), ycFl.ycHospedaje(), ycFl.ycIngresos(),
                        ycFl.ycGastos(), ycFl.ycGanancia(), ycFl.ycImpuestoAlquiler());
            }
            ycFila(ycT, "TOTAL " + ycA.ycAnio(), true, ycA.ycTotal(YCFila::ycAlquiler), ycA.ycTotal(YCFila::ycHospedaje),
                    ycA.ycTotal(YCFila::ycIngresos), ycA.ycTotal(YCFila::ycGastos), ycA.ycTotal(YCFila::ycGanancia), ycA.ycTotal(YCFila::ycImpuestoAlquiler));
            ycDoc.add(ycT);

            Paragraph ycNota = new Paragraph("* Estimado REFERENCIAL: " + ycTasa + "% de lo cobrado por alquiler mensual "
                    + "(renta de primera categoria). No incluye el hospedaje, que tributa como negocio segun su "
                    + "regimen. Los montos reales y la forma de declarar los confirma su contador.\n"
                    + "Ingresos = lo efectivamente cobrado en cada mes (recibos validos, adelantos y saldos de "
                    + "hospedaje, menos devoluciones). Gastos = los registrados en \"Gastos y ganancia\".",
                    ycF(8, Font.NORMAL, YC_GRIS));
            ycNota.setSpacingBefore(10);
            ycDoc.add(ycNota);
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el reporte anual", ycE);
        } finally {
            ycDoc.close();
        }
    }

    private static void ycFila(PdfPTable ycT, String ycMes, boolean ycTotal, BigDecimal... ycValores) {
        Font ycFuente = ycF(9, ycTotal ? Font.BOLD : Font.NORMAL, YC_TEXTO);
        PdfPCell ycM = new PdfPCell(new Phrase(ycMes, ycFuente));
        ycEstilo(ycM, ycTotal);
        ycT.addCell(ycM);
        for (BigDecimal ycV : ycValores) {
            PdfPCell ycC = new PdfPCell(new Phrase(YC_IMPORTE.format(ycV), ycFuente));
            ycC.setHorizontalAlignment(Element.ALIGN_RIGHT);
            ycEstilo(ycC, ycTotal);
            ycT.addCell(ycC);
        }
    }

    private static void ycEstilo(PdfPCell ycC, boolean ycTotal) {
        ycC.setBorderColor(YC_BORDE);
        ycC.setPadding(5);
        if (ycTotal) {
            ycC.setBackgroundColor(YC_SUAVE);
        }
    }

    private static String ycN(BigDecimal ycV) {
        return ycV.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static Font ycF(float ycTam, int ycEstilo, Color ycColor) {
        return new Font(Font.HELVETICA, ycTam, ycEstilo, ycColor);
    }

    public static List<Integer> ycAnios(int ycDesde) {
        List<Integer> ycR = new ArrayList<>();
        for (int ycA = Year.now().getValue(); ycA >= ycDesde; ycA--) {
            ycR.add(ycA);
        }
        return ycR;
    }
}
