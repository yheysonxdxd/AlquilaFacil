package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCMetodoPago;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YCCierreCajaPdfService {
    private static final Color YC_ACENTO = new Color(13, 148, 136);
    private static final Color YC_SUAVE = new Color(240, 250, 249);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_GRIS = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(214, 228, 226);
    private static final Color YC_ROJO = new Color(190, 18, 60);
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter YC_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DecimalFormat YC_IMPORTE = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCDatosArrendador ycArrendador;

    private final Font ycFTitulo = ycF(15, Font.BOLD, YC_ACENTO);
    private final Font ycFSub = ycF(9, Font.NORMAL, YC_GRIS);
    private final Font ycFSeccion = ycF(9, Font.BOLD, YC_ACENTO);
    private final Font ycFNormal = ycF(8, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycF(8, Font.BOLD, YC_TEXTO);
    private final Font ycFCabecera = ycF(7, Font.BOLD, Color.WHITE);
    private final Font ycFRojo = ycF(8, Font.NORMAL, YC_ROJO);
    private final Font ycFGrande = ycF(11, Font.BOLD, YC_TEXTO);

    public void ycGenerar(YCCajaService.YCCierre ycC, BigDecimal ycContado, File ycDestino) throws IOException {
        try (OutputStream ycOut = new FileOutputStream(ycDestino)) {
            ycEscribir(ycC, ycContado, ycOut);
        }
    }

    void ycEscribir(YCCajaService.YCCierre ycC, BigDecimal ycContado, OutputStream ycOut) {
        Document ycDoc = new Document(PageSize.A4, 36, 36, 36, 40);
        try {
            PdfWriter.getInstance(ycDoc, ycOut);
            ycDoc.addTitle("Cierre de caja " + ycC.ycDia().format(YC_FECHA));
            ycDoc.open();

            ycDoc.add(new Paragraph(ycArrendador.getYcNombreHospedaje(), ycFTitulo));
            ycDoc.add(new Paragraph("CIERRE DE CAJA DEL " + ycC.ycDia().format(YC_FECHA)
                    + (ycC.ycUsuario() == null ? " · TODOS LOS USUARIOS" : " · USUARIO: " + ycC.ycUsuario().toUpperCase()), ycFGrande));
            ycDoc.add(new Paragraph("Impreso el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + (ycC.ycConGastos() ? "" : " · No incluye gastos"), ycFSub));
            ycDoc.add(ycEspacio(10));

            ycDoc.add(new Paragraph("RESUMEN POR FORMA DE PAGO", ycFSeccion));
            ycDoc.add(ycEspacio(4));
            PdfPTable ycResumen = new PdfPTable(new float[]{40, 20, 20, 20});
            ycResumen.setWidthPercentage(100);
            ycCabecera(ycResumen, "FORMA DE PAGO", "ENTRO", "SALIO", "NETO");
            for (YCMetodoPago ycM : YCMetodoPago.values()) {
                BigDecimal ycEn = ycC.ycEntradas(ycM);
                BigDecimal ycSa = ycC.ycSalidas(ycM);
                if (ycEn.signum() == 0 && ycSa.signum() == 0) {
                    continue;
                }
                ycCelda(ycResumen, ycM.ycGetDescripcion(), ycFNormal, Element.ALIGN_LEFT, null);
                ycCelda(ycResumen, YC_IMPORTE.format(ycEn), ycFNormal, Element.ALIGN_RIGHT, null);
                ycCelda(ycResumen, YC_IMPORTE.format(ycSa), ycFNormal, Element.ALIGN_RIGHT, null);
                ycCelda(ycResumen, YC_IMPORTE.format(ycEn.subtract(ycSa)), ycFNormal, Element.ALIGN_RIGHT, null);
            }
            ycCelda(ycResumen, "TOTAL", ycFNegrita, Element.ALIGN_LEFT, YC_SUAVE);
            ycCelda(ycResumen, YC_IMPORTE.format(ycC.ycTotalEntradas()), ycFNegrita, Element.ALIGN_RIGHT, YC_SUAVE);
            ycCelda(ycResumen, YC_IMPORTE.format(ycC.ycTotalSalidas()), ycFNegrita, Element.ALIGN_RIGHT, YC_SUAVE);
            ycCelda(ycResumen, YC_IMPORTE.format(ycC.ycNeto()), ycFNegrita, Element.ALIGN_RIGHT, YC_SUAVE);
            ycDoc.add(ycResumen);
            ycDoc.add(ycEspacio(8));

            PdfPTable ycArqueo = new PdfPTable(new float[]{60, 40});
            ycArqueo.setWidthPercentage(60);
            ycArqueo.setHorizontalAlignment(Element.ALIGN_LEFT);
            ycCelda(ycArqueo, "Efectivo que debe haber en caja", ycFNegrita, Element.ALIGN_LEFT, null);
            ycCelda(ycArqueo, "S/ " + YC_IMPORTE.format(ycC.ycEfectivoEnCaja()), ycFNegrita, Element.ALIGN_RIGHT, null);
            if (ycContado != null) {
                BigDecimal ycDif = ycContado.subtract(ycC.ycEfectivoEnCaja());
                ycCelda(ycArqueo, "Efectivo contado", ycFNormal, Element.ALIGN_LEFT, null);
                ycCelda(ycArqueo, "S/ " + YC_IMPORTE.format(ycContado), ycFNormal, Element.ALIGN_RIGHT, null);
                ycCelda(ycArqueo, ycDif.signum() == 0 ? "Diferencia" : ycDif.signum() > 0 ? "Sobra" : "Falta",
                        ycDif.signum() == 0 ? ycFNegrita : ycF(8, Font.BOLD, YC_ROJO), Element.ALIGN_LEFT, YC_SUAVE);
                ycCelda(ycArqueo, "S/ " + YC_IMPORTE.format(ycDif.abs()),
                        ycDif.signum() == 0 ? ycFNegrita : ycF(8, Font.BOLD, YC_ROJO), Element.ALIGN_RIGHT, YC_SUAVE);
            }
            ycDoc.add(ycArqueo);
            ycDoc.add(ycEspacio(10));

            Map<String, BigDecimal> ycPorUsuario = ycC.ycCobradoPorUsuario();
            if (ycC.ycUsuario() == null && !ycPorUsuario.isEmpty()) {
                ycDoc.add(new Paragraph("COBRADO POR CADA USUARIO", ycFSeccion));
                ycDoc.add(ycEspacio(4));
                PdfPTable ycUsuarios = new PdfPTable(new float[]{60, 40});
                ycUsuarios.setWidthPercentage(60);
                ycUsuarios.setHorizontalAlignment(Element.ALIGN_LEFT);
                ycCabecera(ycUsuarios, "USUARIO", "COBRO");
                ycPorUsuario.forEach((ycU, ycMonto) -> {
                    ycCelda(ycUsuarios, ycU, ycFNormal, Element.ALIGN_LEFT, null);
                    ycCelda(ycUsuarios, YC_IMPORTE.format(ycMonto), ycFNormal, Element.ALIGN_RIGHT, null);
                });
                ycDoc.add(ycUsuarios);
                ycDoc.add(ycEspacio(10));
            }

            ycDoc.add(new Paragraph("DETALLE DE MOVIMIENTOS (" + ycC.ycMovimientos().size() + ")", ycFSeccion));
            ycDoc.add(ycEspacio(4));
            PdfPTable ycDetalle = new PdfPTable(new float[]{8, 22, 36, 12, 12, 10});
            ycDetalle.setWidthPercentage(100);
            ycDetalle.setHeaderRows(1);
            ycCabecera(ycDetalle, "HORA", "CONCEPTO", "DETALLE", "FORMA", "MONTO", "USUARIO");
            if (ycC.ycMovimientos().isEmpty()) {
                PdfPCell ycVacio = new PdfPCell(new Phrase("Sin movimientos este dia.", ycFNormal));
                ycVacio.setColspan(6);
                ycVacio.setBorderColor(YC_BORDE);
                ycVacio.setPadding(6);
                ycDetalle.addCell(ycVacio);
            }
            for (YCCajaService.YCMovimiento ycM : ycC.ycMovimientos()) {
                Font ycFm = ycM.ycEntrada() ? ycFNormal : ycFRojo;
                ycCelda(ycDetalle, ycM.ycHora().format(YC_HORA), ycFm, Element.ALIGN_LEFT, null);
                ycCelda(ycDetalle, ycM.ycConcepto(), ycFm, Element.ALIGN_LEFT, null);
                ycCelda(ycDetalle, ycM.ycDetalle(), ycFm, Element.ALIGN_LEFT, null);
                ycCelda(ycDetalle, ycM.ycMetodo() == null ? "—" : ycM.ycMetodo().ycGetDescripcion(), ycFm, Element.ALIGN_LEFT, null);
                ycCelda(ycDetalle, (ycM.ycEntrada() ? "" : "- ") + YC_IMPORTE.format(ycM.ycMonto()), ycFm, Element.ALIGN_RIGHT, null);
                ycCelda(ycDetalle, ycM.ycUsuario() == null ? "—" : ycM.ycUsuario(), ycFm, Element.ALIGN_LEFT, null);
            }
            ycDoc.add(ycDetalle);

            if (!ycC.ycAnulaciones().isEmpty()) {
                ycDoc.add(ycEspacio(10));
                ycDoc.add(new Paragraph("ANULACIONES Y CANCELACIONES DEL DIA", ycFSeccion));
                ycDoc.add(ycEspacio(4));
                PdfPTable ycAnul = new PdfPTable(new float[]{8, 34, 12, 34, 12});
                ycAnul.setWidthPercentage(100);
                ycCabecera(ycAnul, "HORA", "QUE", "MONTO", "MOTIVO", "USUARIO");
                for (YCCajaService.YCAnulacion ycA : ycC.ycAnulaciones()) {
                    ycCelda(ycAnul, ycA.ycHora().format(YC_HORA), ycFNormal, Element.ALIGN_LEFT, null);
                    ycCelda(ycAnul, ycA.ycQue(), ycFNormal, Element.ALIGN_LEFT, null);
                    ycCelda(ycAnul, YC_IMPORTE.format(ycA.ycMonto()), ycFNormal, Element.ALIGN_RIGHT, null);
                    ycCelda(ycAnul, ycA.ycMotivo() == null ? "—" : ycA.ycMotivo(), ycFNormal, Element.ALIGN_LEFT, null);
                    ycCelda(ycAnul, ycA.ycUsuario() == null ? "—" : ycA.ycUsuario(), ycFNormal, Element.ALIGN_LEFT, null);
                }
                ycDoc.add(ycAnul);
            }

            ycDoc.add(ycEspacio(50));
            PdfPTable ycFirmas = new PdfPTable(new float[]{45, 10, 45});
            ycFirmas.setWidthPercentage(80);
            ycFirmas.addCell(ycFirma("ENTREGO"));
            ycFirmas.addCell(ycSinBorde());
            ycFirmas.addCell(ycFirma("RECIBIO"));
            ycDoc.add(ycFirmas);
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el cierre de caja", ycE);
        } finally {
            ycDoc.close();
        }
    }

    private void ycCabecera(PdfPTable ycT, String... ycTextos) {
        for (String ycS : ycTextos) {
            PdfPCell ycC = new PdfPCell(new Phrase(ycS, ycFCabecera));
            ycC.setBackgroundColor(YC_ACENTO);
            ycC.setBorderColor(YC_ACENTO);
            ycC.setPadding(5);
            ycT.addCell(ycC);
        }
    }

    private static void ycCelda(PdfPTable ycT, String ycTexto, Font ycF, int ycAlineacion, Color ycFondo) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto == null ? "" : ycTexto, ycF));
        ycC.setHorizontalAlignment(ycAlineacion);
        ycC.setBorderColor(YC_BORDE);
        ycC.setPadding(5);
        if (ycFondo != null) {
            ycC.setBackgroundColor(ycFondo);
        }
        ycT.addCell(ycC);
    }

    private PdfPCell ycFirma(String ycTexto) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFNegrita));
        ycC.setBorder(Rectangle.TOP);
        ycC.setBorderColor(YC_TEXTO);
        ycC.setHorizontalAlignment(Element.ALIGN_CENTER);
        ycC.setPaddingTop(6);
        return ycC;
    }

    private static PdfPCell ycSinBorde() {
        PdfPCell ycC = new PdfPCell();
        ycC.setBorder(Rectangle.NO_BORDER);
        return ycC;
    }

    private static Paragraph ycEspacio(float ycAlto) {
        Paragraph ycP = new Paragraph(" ");
        ycP.setLeading(ycAlto);
        return ycP;
    }

    private static Font ycF(float ycTam, int ycEstilo, Color ycColor) {
        return new Font(Font.HELVETICA, ycTam, ycEstilo, ycColor);
    }
}
