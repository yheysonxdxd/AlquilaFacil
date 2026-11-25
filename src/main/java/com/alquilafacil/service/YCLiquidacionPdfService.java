package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCLiquidacion;
import com.lowagie.text.Chunk;
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
import org.springframework.transaction.annotation.Transactional;

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
public class YCLiquidacionPdfService {
    private static final Color YC_ACENTO = new Color(227, 28, 95);
    private static final Color YC_SUAVE = new Color(255, 240, 243);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_GRIS = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(255, 214, 222);
    private static final Color YC_VERDE = new Color(22, 128, 61);
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat YC_IMPORTE = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCSalidaInquilinoService ycSalidaService;
    private final YCDatosArrendador ycArrendador;

    private final Font ycFNombre = ycF(15, Font.BOLD, YC_ACENTO);
    private final Font ycFNormal = ycF(9, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycF(9, Font.BOLD, YC_TEXTO);
    private final Font ycFSuave = ycF(8, Font.NORMAL, YC_GRIS);
    private final Font ycFEtiqueta = ycF(7, Font.BOLD, YC_GRIS);
    private final Font ycFCajaTipo = ycF(9, Font.BOLD, Color.WHITE);
    private final Font ycFCajaNumero = ycF(12, Font.BOLD, YC_ACENTO);
    private final Font ycFResultado = ycF(12, Font.BOLD, Color.WHITE);

    @Transactional(readOnly = true)
    public void ycGenerar(Long ycLiquidacionId, File ycDestino) throws IOException {
        YCLiquidacion ycL = ycSalidaService.ycObtener(ycLiquidacionId);
        try (OutputStream ycOut = new FileOutputStream(ycDestino)) {
            ycEscribir(ycL, ycOut);
        }
    }

    void ycEscribir(YCLiquidacion ycL, OutputStream ycOut) {
        YCInquilino ycI = ycL.getYcInquilino();
        Document ycDoc = new Document(PageSize.A5, 30, 30, 30, 36);
        try {
            PdfWriter.getInstance(ycDoc, ycOut);
            ycDoc.addTitle("Liquidacion " + ycL.ycGetNumero());
            ycDoc.open();

            PdfPTable ycCab = new PdfPTable(new float[]{60, 40});
            ycCab.setWidthPercentage(100);
            PdfPCell ycEmisor = ycSinBorde();
            ycEmisor.addElement(new Paragraph(ycArrendador.getYcNombre(), ycFNombre));
            ycEmisor.addElement(new Paragraph("Alquiler de cuartos", ycFNegrita));
            if (!ycVacio(ycArrendador.getYcDireccion())) ycEmisor.addElement(new Paragraph(ycArrendador.getYcDireccion(), ycFSuave));
            ycCab.addCell(ycEmisor);
            PdfPTable ycCaja = new PdfPTable(1);
            ycCaja.addCell(ycCeldaCaja(new Phrase("LIQUIDACION DE SALIDA", ycFCajaTipo), YC_ACENTO));
            ycCaja.addCell(ycCeldaCaja(new Phrase(ycL.ycGetNumero(), ycFCajaNumero), Color.WHITE));
            PdfPCell ycCont = new PdfPCell(ycCaja);
            ycCont.setBorderColor(YC_ACENTO);
            ycCont.setBorderWidth(1.2f);
            ycCont.setPadding(0);
            ycCab.addCell(ycCont);
            ycDoc.add(ycCab);
            ycDoc.add(ycEspacio(10));

            PdfPTable ycDatos = new PdfPTable(new float[]{22, 38, 18, 22});
            ycDatos.setWidthPercentage(100);
            ycDato(ycDatos, "INQUILINO", ycI.ycGetNombreCompleto().toUpperCase());
            ycDato(ycDatos, "FECHA", ycL.getYcFecha().format(YC_FECHA));
            ycDato(ycDatos, "DNI", ycI.getYcDni());
            ycDato(ycDatos, "CUARTO", ycL.getYcCuartoNumero());
            ycDato(ycDatos, "INGRESO", ycI.getYcFechaIngreso() == null ? "—" : ycI.getYcFechaIngreso().format(YC_FECHA));
            ycDato(ycDatos, "SALIDA", ycI.getYcFechaSalida() == null ? ycL.getYcFecha().format(YC_FECHA) : ycI.getYcFechaSalida().format(YC_FECHA));
            PdfPTable ycMarco = new PdfPTable(1);
            ycMarco.setWidthPercentage(100);
            PdfPCell ycM = new PdfPCell(ycDatos);
            ycM.setBorderColor(YC_BORDE);
            ycM.setBackgroundColor(new Color(255, 248, 249));
            ycM.setPadding(8);
            ycMarco.addCell(ycM);
            ycDoc.add(ycMarco);
            ycDoc.add(ycEspacio(10));

            PdfPTable ycCuentas = new PdfPTable(new float[]{70, 30});
            ycCuentas.setWidthPercentage(100);
            ycFila(ycCuentas, "Meses pagados al salir" + (ycL.getYcMesesPagados() == null ? "" : ": " + ycL.getYcMesesPagados()),
                    ycL.getYcDeudaPagada(), false);
            if (ycL.getYcDanos().signum() > 0) {
                ycFila(ycCuentas, "Descuento por daños: " + ycL.getYcMotivoDanos(), ycL.getYcDanos(), false);
            }
            ycFila(ycCuentas, "Total a cubrir", ycL.getYcDeudaPagada().add(ycL.getYcDanos()), true);
            ycFila(ycCuentas, "Menos garantia entregada al ingresar", ycL.getYcGarantia().negate(), false);
            ycDoc.add(ycCuentas);
            ycDoc.add(ycEspacio(8));

            String ycTexto;
            BigDecimal ycMonto;
            Color ycColor;
            if (ycL.getYcCobrado().signum() > 0) {
                ycTexto = "EL INQUILINO PAGO AL SALIR (" + ycL.getYcMetodo().ycGetDescripcion().toUpperCase() + ")";
                ycMonto = ycL.getYcCobrado();
                ycColor = YC_ACENTO;
            } else if (ycL.getYcDevuelto().signum() > 0) {
                ycTexto = "SE DEVUELVE AL INQUILINO";
                ycMonto = ycL.getYcDevuelto();
                ycColor = YC_VERDE;
            } else {
                ycTexto = "QUEDAN A MANO (SIN SALDO)";
                ycMonto = BigDecimal.ZERO;
                ycColor = new Color(55, 65, 81);
            }
            PdfPTable ycRes = new PdfPTable(new float[]{65, 35});
            ycRes.setWidthPercentage(100);
            PdfPCell ycA = new PdfPCell(new Phrase(ycTexto, ycFResultado));
            PdfPCell ycB = new PdfPCell(new Phrase("S/ " + YC_IMPORTE.format(ycMonto), ycFResultado));
            for (PdfPCell ycC : new PdfPCell[]{ycA, ycB}) {
                ycC.setBackgroundColor(ycColor);
                ycC.setBorder(Rectangle.NO_BORDER);
                ycC.setPadding(9);
            }
            ycB.setHorizontalAlignment(Element.ALIGN_RIGHT);
            ycRes.addCell(ycA);
            ycRes.addCell(ycB);
            ycDoc.add(ycRes);
            if (ycMonto.signum() > 0) {
                Paragraph ycLetras = new Paragraph();
                ycLetras.add(new Chunk("SON: ", ycFEtiqueta));
                ycLetras.add(new Chunk(YCNumeroALetras.ycImporte(ycMonto), ycFNegrita));
                ycLetras.setSpacingBefore(6);
                ycDoc.add(ycLetras);
            }
            if (ycL.getYcDeudaPendiente().signum() > 0) {
                Paragraph ycP = new Paragraph();
                ycP.setSpacingBefore(6);
                ycP.add(new Chunk("QUEDA DEBIENDO: ", ycF(8, Font.BOLD, YC_ACENTO)));
                ycP.add(new Chunk(ycL.getYcMesesPendientes() + " · S/ " + YC_IMPORTE.format(ycL.getYcDeudaPendiente()), ycFNormal));
                ycDoc.add(ycP);
            }
            Paragraph ycConformidad = new Paragraph("Ambas partes declaran estar conformes con estas cuentas y que el cuarto "
                    + "fue entregado en la fecha indicada.", ycFSuave);
            ycConformidad.setSpacingBefore(10);
            ycDoc.add(ycConformidad);
            ycDoc.add(ycEspacio(42));

            PdfPTable ycFirmas = new PdfPTable(new float[]{45, 10, 45});
            ycFirmas.setWidthPercentage(100);
            ycFirmas.addCell(ycFirma(ycArrendador.getYcNombre(), "Arrendador"));
            ycFirmas.addCell(ycSinBorde());
            ycFirmas.addCell(ycFirma(ycI.ycGetNombreCompleto(), "Inquilino · DNI " + ycI.getYcDni()));
            ycDoc.add(ycFirmas);
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ycE.getMessage(), ycE);
        } finally {
            ycDoc.close();
        }
    }

    private void ycFila(PdfPTable ycT, String ycConcepto, BigDecimal ycMonto, boolean ycNegrita) {
        Font ycFu = ycNegrita ? ycFNegrita : ycFNormal;
        PdfPCell ycC = new PdfPCell(new Phrase(ycConcepto, ycFu));
        PdfPCell ycV = new PdfPCell(new Phrase(YC_IMPORTE.format(ycMonto), ycFu));
        for (PdfPCell ycX : new PdfPCell[]{ycC, ycV}) {
            ycX.setBorder(Rectangle.BOTTOM);
            ycX.setBorderColor(YC_BORDE);
            ycX.setPadding(6);
            if (ycNegrita) ycX.setBackgroundColor(YC_SUAVE);
        }
        ycV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ycT.addCell(ycC);
        ycT.addCell(ycV);
    }

    private PdfPCell ycFirma(String ycNombre, String ycRol) {
        PdfPCell ycC = new PdfPCell();
        ycC.setBorder(Rectangle.TOP);
        ycC.setBorderColor(YC_GRIS);
        ycC.setPaddingTop(4);
        Paragraph ycN = new Paragraph(ycNombre, ycFNegrita);
        ycN.setAlignment(Element.ALIGN_CENTER);
        Paragraph ycR = new Paragraph(ycRol, ycFSuave);
        ycR.setAlignment(Element.ALIGN_CENTER);
        ycC.addElement(ycN);
        ycC.addElement(ycR);
        return ycC;
    }

    private void ycDato(PdfPTable ycT, String ycEt, String ycVal) {
        PdfPCell ycE = ycSinBorde();
        ycE.setPhrase(new Phrase(ycEt, ycFEtiqueta));
        ycE.setPaddingBottom(5);
        ycT.addCell(ycE);
        PdfPCell ycV = ycSinBorde();
        ycV.setPhrase(new Phrase(ycVacio(ycVal) ? "—" : ycVal, ycFNormal));
        ycV.setPaddingBottom(5);
        ycT.addCell(ycV);
    }

    private static PdfPCell ycCeldaCaja(Phrase ycP, Color ycFondo) {
        PdfPCell ycC = new PdfPCell(ycP);
        ycC.setHorizontalAlignment(Element.ALIGN_CENTER);
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setBackgroundColor(ycFondo);
        ycC.setPaddingTop(7);
        ycC.setPaddingBottom(8);
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

    private static boolean ycVacio(String ycS) {
        return ycS == null || ycS.isBlank();
    }

    private static Font ycF(float ycT, int ycE, Color ycC) {
        return new Font(Font.HELVETICA, ycT, ycE, ycC);
    }
}
