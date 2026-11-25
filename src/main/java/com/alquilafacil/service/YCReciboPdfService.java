package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCPago;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
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
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class YCReciboPdfService {
    private static final Color YC_ACENTO = new Color(227, 28, 95);
    private static final Color YC_ACENTO_SUAVE = new Color(255, 240, 243);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_TEXTO_SUAVE = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(216, 222, 228);
    private static final Color YC_FILA_ALTERNA = new Color(246, 248, 250);
    private static final Color YC_PELIGRO = new Color(207, 34, 46);

    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter YC_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DecimalFormat YC_IMPORTE =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCPagoService ycPagoService;
    private final YCDatosArrendador ycArrendador;

    private final Font ycFNombre = ycFuente(15, Font.BOLD, YC_ACENTO);
    private final Font ycFNormal = ycFuente(9, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycFuente(9, Font.BOLD, YC_TEXTO);
    private final Font ycFSuave = ycFuente(8, Font.NORMAL, YC_TEXTO_SUAVE);
    private final Font ycFEtiqueta = ycFuente(7, Font.BOLD, YC_TEXTO_SUAVE);
    private final Font ycFCabecera = ycFuente(8, Font.BOLD, Color.WHITE);
    private final Font ycFCajaTipo = ycFuente(9.5f, Font.BOLD, Color.WHITE);
    private final Font ycFCajaNumero = ycFuente(12, Font.BOLD, YC_ACENTO);
    private final Font ycFTotal = ycFuente(10.5f, Font.BOLD, Color.WHITE);

    public void ycGenerar(Long ycPagoId, File ycDestino) throws IOException {
        YCPago ycPago = ycPagoService.ycObtener(ycPagoId);
        try (OutputStream ycSalida = new FileOutputStream(ycDestino)) {
            ycEscribir(ycPago, ycSalida);
        }
    }

    void ycEscribir(YCPago ycPago, OutputStream ycSalida) {
        Document ycDoc = new Document(PageSize.A5, 30, 30, 30, 45);
        try {
            PdfWriter ycWriter = PdfWriter.getInstance(ycDoc, ycSalida);
            ycWriter.setPageEvent(new YCPiePagina(ycPago));
            ycDoc.addTitle("Recibo " + ycPago.ycGetNumeroRecibo());
            ycDoc.addAuthor(ycArrendador.getYcNombre());
            ycDoc.open();

            ycDoc.add(ycCabecera(ycPago));
            ycDoc.add(ycEspacio(12));
            ycDoc.add(ycDatosInquilino(ycPago));
            ycDoc.add(ycEspacio(12));
            ycDoc.add(ycConceptos(ycPago));
            ycDoc.add(ycEspacio(10));
            ycDoc.add(ycImporteEnLetras(ycPago.getYcTotal()));
            if (ycPago.getYcNota() != null) {
                ycDoc.add(ycEspacio(6));
                Paragraph ycNota = new Paragraph();
                ycNota.add(new Chunk("NOTA: ", ycFEtiqueta));
                ycNota.add(new Chunk(ycPago.getYcNota(), ycFNormal));
                ycDoc.add(ycNota);
            }
            if (ycPago.getYcRegistradoPor() != null) {
                ycDoc.add(ycEspacio(4));
                Paragraph ycAtendio = new Paragraph();
                ycAtendio.add(new Chunk("ATENDIDO POR: ", ycFEtiqueta));
                ycAtendio.add(new Chunk(ycPago.getYcRegistradoPor(), ycFNormal));
                ycDoc.add(ycAtendio);
            }
            if (ycPago.isYcAnulado() && ycPago.getYcMotivoAnulacion() != null) {
                ycDoc.add(ycEspacio(4));
                Paragraph ycAnulado = new Paragraph();
                ycAnulado.add(new Chunk("ANULADO: ", ycFEtiqueta));
                ycAnulado.add(new Chunk(ycPago.getYcMotivoAnulacion()
                        + (ycPago.getYcAnuladoPor() == null ? "" : " (por " + ycPago.getYcAnuladoPor()
                        + (ycPago.getYcFechaAnulacion() == null ? "" : ", " + ycPago.getYcFechaAnulacion().format(YC_FECHA)
                        + " " + ycPago.getYcFechaAnulacion().format(YC_HORA)) + ")"), ycFNormal));
                ycDoc.add(ycAnulado);
            }
            ycDoc.add(ycEspacio(46));
            ycDoc.add(ycFirma());
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ycE.getMessage(), ycE);
        } finally {
            ycDoc.close();
        }
    }

    private PdfPTable ycCabecera(YCPago ycPago) {
        PdfPTable ycTabla = new PdfPTable(new float[]{60, 40});
        ycTabla.setWidthPercentage(100);

        PdfPCell ycEmisor = ycSinBorde();
        ycEmisor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ycEmisor.addElement(new Paragraph(ycArrendador.getYcNombre(), ycFNombre));
        ycEmisor.addElement(new Paragraph("Alquiler de cuartos", ycFNegrita));
        if (!ycVacio(ycArrendador.getYcDireccion())) {
            ycEmisor.addElement(new Paragraph(ycArrendador.getYcDireccion(), ycFSuave));
        }
        StringBuilder ycContacto = new StringBuilder();
        if (!ycVacio(ycArrendador.getYcDni())) {
            ycContacto.append("DNI ").append(ycArrendador.getYcDni());
        }
        if (!ycVacio(ycArrendador.getYcTelefono())) {
            ycContacto.append(ycContacto.isEmpty() ? "" : "  ·  ").append("Cel. ").append(ycArrendador.getYcTelefono());
        }
        if (!ycContacto.isEmpty()) {
            ycEmisor.addElement(new Paragraph(ycContacto.toString(), ycFSuave));
        }
        ycTabla.addCell(ycEmisor);

        PdfPTable ycCaja = new PdfPTable(1);
        ycCaja.setWidthPercentage(100);
        ycCaja.addCell(ycCeldaCaja(new Phrase("RECIBO DE PAGO", ycFCajaTipo), YC_ACENTO, 7, 7));
        ycCaja.addCell(ycCeldaCaja(new Phrase(ycPago.ycGetNumeroRecibo(), ycFCajaNumero), Color.WHITE, 7, 9));

        PdfPCell ycContenedor = new PdfPCell(ycCaja);
        ycContenedor.setBorderColor(YC_ACENTO);
        ycContenedor.setBorderWidth(1.2f);
        ycContenedor.setPadding(0);
        ycTabla.addCell(ycContenedor);
        return ycTabla;
    }

    private PdfPTable ycDatosInquilino(YCPago ycPago) {
        YCInquilino ycI = ycPago.getYcInquilino();
        PdfPTable ycTabla = new PdfPTable(new float[]{22, 38, 18, 22});
        ycTabla.setWidthPercentage(100);

        ycAgregarDato(ycTabla, "RECIBÍ DE", ycI.ycGetNombreCompleto().toUpperCase());
        ycAgregarDato(ycTabla, "FECHA", ycPago.getYcFechaPago().format(YC_FECHA));
        ycAgregarDato(ycTabla, "DNI", ycI.getYcDni());
        ycAgregarDato(ycTabla, "HORA", ycPago.getYcFechaPago().format(YC_HORA));
        ycAgregarDato(ycTabla, "CUARTO", ycPago.getYcCuartoNumero());
        ycAgregarDato(ycTabla, "PAGO", ycPago.getYcMetodo().ycGetDescripcion().toUpperCase());
        ycAgregarDato(ycTabla, "MES QUE PAGA", YCCalendarioPagos.ycTexto(ycPago.getYcPeriodo()).toUpperCase());
        ycAgregarDato(ycTabla, "MONEDA", "SOLES");

        PdfPTable ycMarco = new PdfPTable(1);
        ycMarco.setWidthPercentage(100);
        PdfPCell ycCelda = new PdfPCell(ycTabla);
        ycCelda.setBorderColor(YC_BORDE);
        ycCelda.setPadding(8);
        ycCelda.setBackgroundColor(YC_FILA_ALTERNA);
        ycMarco.addCell(ycCelda);
        return ycMarco;
    }

    private PdfPTable ycConceptos(YCPago ycPago) {
        PdfPTable ycTabla = new PdfPTable(new float[]{70, 30});
        ycTabla.setWidthPercentage(100);
        ycTabla.addCell(ycCeldaCabecera("CONCEPTO", Element.ALIGN_LEFT));
        ycTabla.addCell(ycCeldaCabecera("IMPORTE (S/)", Element.ALIGN_RIGHT));

        String ycMes = YCCalendarioPagos.ycTexto(ycPago.getYcPeriodo());
        int ycFila = 0;
        ycFila = ycConcepto(ycTabla, "Alquiler del " + ycPago.getYcCuartoNumero() + " - " + ycMes, ycPago.getYcAlquiler(), ycFila, true);
        ycFila = ycConcepto(ycTabla, "Consumo de luz - " + ycMes, ycPago.getYcLuz(), ycFila, false);
        ycConcepto(ycTabla, "Consumo de agua - " + ycMes, ycPago.getYcAgua(), ycFila, false);

        PdfPCell ycE = new PdfPCell(new Phrase("TOTAL PAGADO", ycFTotal));
        PdfPCell ycV = new PdfPCell(new Phrase("S/ " + YC_IMPORTE.format(ycPago.getYcTotal()), ycFTotal));
        for (PdfPCell ycC : new PdfPCell[]{ycE, ycV}) {
            ycC.setBackgroundColor(YC_ACENTO);
            ycC.setBorder(Rectangle.NO_BORDER);
            ycC.setPaddingTop(8);
            ycC.setPaddingBottom(9);
            ycC.setPaddingLeft(6);
            ycC.setPaddingRight(6);
        }
        ycV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ycTabla.addCell(ycE);
        ycTabla.addCell(ycV);
        return ycTabla;
    }

    private int ycConcepto(PdfPTable ycTabla, String ycTexto, BigDecimal ycMonto, int ycFila, boolean ycSiempre) {
        if (!ycSiempre && (ycMonto == null || ycMonto.signum() == 0)) {
            return ycFila;
        }
        Color ycFondo = ycFila % 2 == 0 ? Color.WHITE : YC_FILA_ALTERNA;
        ycTabla.addCell(ycCeldaItem(ycTexto, Element.ALIGN_LEFT, ycFondo, ycFNormal));
        ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycMonto), Element.ALIGN_RIGHT, ycFondo, ycFNegrita));
        return ycFila + 1;
    }

    private PdfPTable ycImporteEnLetras(BigDecimal ycTotal) {
        PdfPTable ycT = new PdfPTable(1);
        ycT.setWidthPercentage(100);
        Phrase ycFrase = new Phrase();
        ycFrase.add(new Chunk("SON: ", ycFEtiqueta));
        ycFrase.add(new Chunk(YCNumeroALetras.ycImporte(ycTotal), ycFNegrita));
        PdfPCell ycC = new PdfPCell(ycFrase);
        ycC.setBorderColor(YC_BORDE);
        ycC.setBackgroundColor(YC_ACENTO_SUAVE);
        ycC.setPadding(8);
        ycT.addCell(ycC);
        return ycT;
    }

    private PdfPTable ycFirma() {
        PdfPTable ycT = new PdfPTable(new float[]{45, 55});
        ycT.setWidthPercentage(100);
        ycT.addCell(ycSinBorde());
        PdfPCell ycC = new PdfPCell();
        ycC.setBorder(Rectangle.TOP);
        ycC.setBorderColor(YC_TEXTO_SUAVE);
        ycC.setPaddingTop(4);
        Paragraph ycNombre = new Paragraph(ycArrendador.getYcNombre(), ycFNegrita);
        ycNombre.setAlignment(Element.ALIGN_CENTER);
        Paragraph ycRol = new Paragraph("Firma del arrendador", ycFSuave);
        ycRol.setAlignment(Element.ALIGN_CENTER);
        ycC.addElement(ycNombre);
        ycC.addElement(ycRol);
        ycT.addCell(ycC);
        return ycT;
    }

    private void ycAgregarDato(PdfPTable ycTabla, String ycEtiqueta, String ycValor) {
        PdfPCell ycE = ycSinBorde();
        ycE.setPhrase(new Phrase(ycEtiqueta, ycFEtiqueta));
        ycE.setPaddingBottom(5);
        ycTabla.addCell(ycE);
        PdfPCell ycV = ycSinBorde();
        ycV.setPhrase(new Phrase(ycValor == null ? "—" : ycValor, ycFNormal));
        ycV.setPaddingBottom(5);
        ycTabla.addCell(ycV);
    }

    private PdfPCell ycCeldaCabecera(String ycTexto, int ycAlineacion) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFCabecera));
        ycC.setBackgroundColor(YC_ACENTO);
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setHorizontalAlignment(ycAlineacion);
        ycC.setPaddingTop(7);
        ycC.setPaddingBottom(7);
        ycC.setPaddingLeft(6);
        ycC.setPaddingRight(6);
        return ycC;
    }

    private PdfPCell ycCeldaCaja(Phrase ycTexto, Color ycFondo, float ycArriba, float ycAbajo) {
        PdfPCell ycC = new PdfPCell(ycTexto);
        ycC.setHorizontalAlignment(Element.ALIGN_CENTER);
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setBackgroundColor(ycFondo);
        ycC.setPaddingTop(ycArriba);
        ycC.setPaddingBottom(ycAbajo);
        return ycC;
    }

    private PdfPCell ycCeldaItem(String ycTexto, int ycAlineacion, Color ycFondo, Font ycFuente) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFuente));
        ycC.setHorizontalAlignment(ycAlineacion);
        ycC.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ycC.setBackgroundColor(ycFondo);
        ycC.setBorder(Rectangle.BOTTOM);
        ycC.setBorderColor(YC_BORDE);
        ycC.setPaddingTop(6);
        ycC.setPaddingBottom(7);
        ycC.setPaddingLeft(6);
        ycC.setPaddingRight(6);
        return ycC;
    }

    private static PdfPCell ycSinBorde() {
        PdfPCell ycC = new PdfPCell();
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setPadding(0);
        return ycC;
    }

    private static Paragraph ycEspacio(float ycAlto) {
        Paragraph ycP = new Paragraph(" ");
        ycP.setLeading(ycAlto);
        return ycP;
    }

    private static boolean ycVacio(String ycTexto) {
        return ycTexto == null || ycTexto.isBlank();
    }

    private static Font ycFuente(float ycTamanio, int ycEstilo, Color ycColor) {
        return new Font(Font.HELVETICA, ycTamanio, ycEstilo, ycColor);
    }

    private class YCPiePagina extends PdfPageEventHelper {
        private final YCPago ycPago;

        YCPiePagina(YCPago ycPago) {
            this.ycPago = ycPago;
        }

        @Override
        public void onEndPage(PdfWriter ycWriter, Document ycDocument) {
            PdfContentByte ycCb = ycWriter.getDirectContent();
            float ycCentro = (ycDocument.left() + ycDocument.right()) / 2;
            float ycY = ycDocument.bottom() - 20;

            ycCb.setColorStroke(YC_BORDE);
            ycCb.setLineWidth(0.6f);
            ycCb.moveTo(ycDocument.left(), ycY + 12);
            ycCb.lineTo(ycDocument.right(), ycY + 12);
            ycCb.stroke();
            ColumnText.showTextAligned(ycCb, Element.ALIGN_CENTER,
                    new Phrase("Generado con " + ycArrendador.getYcNombreSistema() + "  ·  Gracias por su puntualidad", ycFSuave), ycCentro, ycY, 0);

            if (ycPago.isYcAnulado()) {
                PdfContentByte ycDebajo = ycWriter.getDirectContentUnder();
                ycDebajo.saveState();
                PdfGState ycEstado = new PdfGState();
                ycEstado.setFillOpacity(0.12f);
                ycDebajo.setGState(ycEstado);
                try {
                    ycDebajo.beginText();
                    ycDebajo.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 80);
                    ycDebajo.setColorFill(YC_PELIGRO);
                    ycDebajo.showTextAligned(Element.ALIGN_CENTER, "ANULADO",
                            ycCentro, (ycDocument.top() + ycDocument.bottom()) / 2, 40);
                    ycDebajo.endText();
                } catch (DocumentException | IOException ycE) {
                    throw new IllegalStateException(ycE);
                }
                ycDebajo.restoreState();
            }
        }
    }
}
