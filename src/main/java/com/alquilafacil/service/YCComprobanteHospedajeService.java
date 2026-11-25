package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCComodidad;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCNorma;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YCComprobanteHospedajeService {
    private static final Color YC_ACENTO = new Color(227, 28, 95);
    private static final Color YC_ACENTO_SUAVE = new Color(255, 240, 243);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_TEXTO_SUAVE = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(255, 214, 222);
    private static final Color YC_FILA_ALTERNA = new Color(255, 248, 249);
    private static final Color YC_PELIGRO = new Color(207, 34, 46);

    private static final Locale YC_ES = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter YC_FECHA_LARGA = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", YC_ES);
    private static final DateTimeFormatter YC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat YC_IMPORTE =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCEstadiaService ycEstadiaService;
    private final YCDatosArrendador ycArrendador;

    private final Font ycFNombre = ycFuente(16, Font.BOLD, YC_ACENTO);
    private final Font ycFNormal = ycFuente(9, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycFuente(9, Font.BOLD, YC_TEXTO);
    private final Font ycFGrande = ycFuente(11, Font.BOLD, YC_TEXTO);
    private final Font ycFSuave = ycFuente(8, Font.NORMAL, YC_TEXTO_SUAVE);
    private final Font ycFEtiqueta = ycFuente(7, Font.BOLD, YC_TEXTO_SUAVE);
    private final Font ycFEtiquetaAcento = ycFuente(7, Font.BOLD, YC_ACENTO);
    private final Font ycFCabecera = ycFuente(8, Font.BOLD, Color.WHITE);
    private final Font ycFCajaTipo = ycFuente(8.5f, Font.BOLD, Color.WHITE);
    private final Font ycFCajaNumero = ycFuente(12, Font.BOLD, YC_ACENTO);
    private final Font ycFTotal = ycFuente(10.5f, Font.BOLD, Color.WHITE);

    private YCConsumoExtraService ycConsumos;
    private YCPagoDigitalService ycPagoDigital;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetConsumos(YCConsumoExtraService ycConsumos) {
        this.ycConsumos = ycConsumos;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetPagoDigital(YCPagoDigitalService ycPagoDigital) {
        this.ycPagoDigital = ycPagoDigital;
    }

    private List<com.alquilafacil.model.YCConsumoExtra> ycConsumosDe(YCEstadia ycE) {
        if (ycConsumos == null || ycE.getYcId() == null) {
            return List.of();
        }
        return ycConsumos.ycDeEstadia(ycE).stream().filter(ycC -> !ycC.isYcAnulado()).toList();
    }

    private PdfPTable ycTablaConsumos(List<com.alquilafacil.model.YCConsumoExtra> ycLista, boolean ycConHabitacion) {
        if (ycLista.isEmpty()) {
            return null;
        }
        PdfPTable ycTabla = new PdfPTable(new float[]{52, 14, 16, 18});
        ycTabla.setWidthPercentage(100);
        ycTabla.addCell(ycCeldaCabecera("CONSUMOS EXTRA", Element.ALIGN_LEFT));
        ycTabla.addCell(ycCeldaCabecera("CANT.", Element.ALIGN_CENTER));
        ycTabla.addCell(ycCeldaCabecera("P. UNIT.", Element.ALIGN_RIGHT));
        ycTabla.addCell(ycCeldaCabecera("IMPORTE", Element.ALIGN_RIGHT));
        BigDecimal ycTotal = BigDecimal.ZERO, ycPendiente = BigDecimal.ZERO;
        for (var ycC : ycLista) {
            String ycTexto = ycC.getYcConcepto() + (ycConHabitacion ? " · " + ycC.getYcEstadia().getYcCuartoNumero() : "")
                    + "\n" + ycC.getYcFechaRegistro().format(YC_FECHA) + (ycC.isYcPagado() ? " · pagado ("
                    + (ycC.getYcMetodo() == null ? "—" : ycC.getYcMetodo().ycGetDescripcion()) + ")" : " · a la cuenta");
            ycTabla.addCell(ycCeldaItem(ycTexto, Element.ALIGN_LEFT, ycFNormal));
            ycTabla.addCell(ycCeldaItem(String.valueOf(ycC.getYcCantidad()), Element.ALIGN_CENTER, ycFNormal));
            ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycC.getYcPrecioUnitario()), Element.ALIGN_RIGHT, ycFNormal));
            ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycC.getYcTotal()), Element.ALIGN_RIGHT, ycFNegrita));
            ycTotal = ycTotal.add(ycC.getYcTotal());
            if (ycC.ycIsPendiente()) {
                ycPendiente = ycPendiente.add(ycC.getYcTotal());
            }
        }
        if (ycPendiente.signum() > 0) {
            ycFilaPago(ycTabla, "CONSUMOS POR PAGAR", ycPendiente, ycFNegrita);
        }
        PdfPCell ycEt = new PdfPCell(new Phrase(ycPendiente.signum() > 0 ? "TOTAL CONSUMOS" : "TOTAL CONSUMOS PAGADO", ycFTotal));
        ycEt.setColspan(3);
        PdfPCell ycV = new PdfPCell(new Phrase("S/ " + YC_IMPORTE.format(ycTotal), ycFTotal));
        for (PdfPCell ycC : new PdfPCell[]{ycEt, ycV}) {
            ycC.setBackgroundColor(YC_TEXTO);
            ycC.setBorder(Rectangle.NO_BORDER);
            ycC.setPaddingTop(6);
            ycC.setPaddingBottom(7);
            ycC.setPaddingLeft(6);
            ycC.setPaddingRight(6);
        }
        ycV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ycTabla.addCell(ycEt);
        ycTabla.addCell(ycV);
        return ycTabla;
    }

    private PdfPTable ycBloqueYape(BigDecimal ycPorPagar) {
        if (ycPagoDigital == null || !ycPagoDigital.ycConfigurado()) {
            return null;
        }
        PdfPTable ycT = new PdfPTable(new float[]{24, 76});
        ycT.setWidthPercentage(100);
        PdfPCell ycCeldaQr = ycSinBorde();
        ycPagoDigital.ycQr().ifPresent(ycRuta -> {
            try {
                com.lowagie.text.Image ycImg = com.lowagie.text.Image.getInstance(ycRuta.toString());
                ycImg.scaleToFit(72, 72);
                ycCeldaQr.addElement(ycImg);
            } catch (IOException | RuntimeException ycEx) {
                ycCeldaQr.addElement(new Paragraph("(no se pudo leer el QR)", ycFSuave));
            }
        });
        ycT.addCell(ycCeldaQr);
        PdfPCell ycTexto = ycSinBorde();
        ycTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ycTexto.setPaddingLeft(8);
        ycTexto.addElement(new Paragraph("PAGUE CON YAPE O PLIN", ycFEtiquetaAcento));
        if (!ycPagoDigital.ycTexto().isEmpty()) {
            ycTexto.addElement(new Paragraph(ycPagoDigital.ycTexto(), ycFGrande));
        }
        ycTexto.addElement(new Paragraph(ycPorPagar.signum() > 0
                ? "Por pagar: S/ " + YC_IMPORTE.format(ycPorPagar) + ". Envie la captura del pago por WhatsApp."
                : "Escanee el codigo con su app para pagar consumos o futuras reservas.", ycFSuave));
        ycT.addCell(ycTexto);
        return ycMarco(ycT, Color.WHITE, YC_BORDE);
    }

    private BigDecimal ycConsumosYPago(Document ycDoc, YCEstadia ycE, BigDecimal ycSaldoHospedaje, boolean ycGrupo) throws DocumentException {
        List<com.alquilafacil.model.YCConsumoExtra> ycLista = ycConsumosDe(ycE);
        PdfPTable ycTabla = ycTablaConsumos(ycLista, ycGrupo);
        if (ycTabla != null) {
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycTabla);
        }
        return ycSaldoHospedaje.add(ycLista.stream().filter(com.alquilafacil.model.YCConsumoExtra::ycIsPendiente)
                .map(com.alquilafacil.model.YCConsumoExtra::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void ycCierre(Document ycDoc, YCEstadia ycE, BigDecimal ycDevuelto, BigDecimal ycPorPagar) throws DocumentException {
        boolean ycConDevolucion = ycDevuelto != null && ycDevuelto.signum() > 0;
        boolean ycConYape = ycPagoDigital != null && ycPagoDigital.ycConfigurado();
        if (ycConYape && ycConDevolucion) {
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycBloqueYape(ycPorPagar));
        }
        ycDoc.add(ycEspacio(ycConYape && !ycConDevolucion ? 4 : 14));
        ycDoc.add(ycFirma(ycE, ycDevuelto, ycConYape && !ycConDevolucion ? ycYapeCompacto(ycPorPagar) : null));
    }

    private PdfPCell ycYapeCompacto(BigDecimal ycPorPagar) {
        PdfPTable ycT = new PdfPTable(new float[]{40, 60});
        ycT.setWidthPercentage(100);
        PdfPCell ycCeldaQr = ycSinBorde();
        ycPagoDigital.ycQr().ifPresent(ycRuta -> {
            try {
                com.lowagie.text.Image ycImg = com.lowagie.text.Image.getInstance(ycRuta.toString());
                ycImg.scaleToFit(54, 54);
                ycCeldaQr.addElement(ycImg);
            } catch (IOException | RuntimeException ycEx) {
                ycCeldaQr.addElement(new Paragraph("(QR no disponible)", ycFSuave));
            }
        });
        PdfPCell ycTexto = ycSinBorde();
        ycTexto.setPaddingLeft(4);
        ycTexto.addElement(new Paragraph("PAGUE CON YAPE O PLIN", ycFEtiquetaAcento));
        String ycCel = ycArrendador.getYcYapeNumero();
        if (ycCel != null && ycCel.length() == 9) {
            ycTexto.addElement(new Paragraph(ycCel.substring(0, 3) + " " + ycCel.substring(3, 6) + " " + ycCel.substring(6), ycFNegrita));
        }
        if (ycArrendador.getYcYapeTitular() != null && !ycArrendador.getYcYapeTitular().isBlank()) {
            ycTexto.addElement(new Paragraph(ycArrendador.getYcYapeTitular(), ycFSuave));
        }
        if (ycPorPagar.signum() > 0) {
            ycTexto.addElement(new Paragraph("Por pagar: S/ " + YC_IMPORTE.format(ycPorPagar), ycFSuave));
        }
        if (ycPagoDigital.ycQr().isPresent()) {
            ycT.addCell(ycCeldaQr);
            ycT.addCell(ycTexto);
        } else {
            ycTexto.setColspan(2);
            ycT.addCell(ycTexto);
        }
        PdfPCell ycC = ycSinBorde();
        ycC.addElement(ycT);
        return ycC;
    }

    public static String ycNombreArchivo(YCEstadia ycE) {
        String ycHuesped = ycE.getYcHuesped().replaceAll("[\\\\/:*?\"<>|]", "").trim();
        return "Comprobante " + ycE.ycGetNumero() + " - " + ycHuesped;
    }

    public void ycGenerar(Long ycEstadiaId, File ycDestino) throws IOException {
        YCEstadia ycEstadia = ycEstadiaService.ycObtener(ycEstadiaId);
        List<YCEstadia> ycGrupo = ycEstadiaService.ycDelGrupo(ycEstadia);
        try (OutputStream ycSalida = new FileOutputStream(ycDestino)) {
            if (ycGrupo.size() > 1) {
                ycEscribirGrupo(ycGrupo, ycSalida);
            } else {
                ycEscribir(ycEstadia, ycSalida);
            }
        }
    }

    void ycEscribirGrupo(List<YCEstadia> ycGrupo, OutputStream ycSalida) {
        YCEstadia ycE = ycGrupo.get(0);
        boolean ycTodasCanceladas = ycGrupo.stream().allMatch(YCEstadia::isYcCancelada);
        Document ycDoc = new Document(PageSize.A5, 30, 30, 30, 45);
        try {
            PdfWriter ycWriter = PdfWriter.getInstance(ycDoc, ycSalida);
            ycWriter.setPageEvent(new YCPiePagina(ycTodasCanceladas));
            ycDoc.addTitle("Comprobante de hospedaje " + ycE.ycGetNumero());
            ycDoc.addAuthor(ycArrendador.getYcNombreHospedaje());
            ycDoc.open();

            ycDoc.add(ycCabecera(ycE));
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycDatosHuespedGrupo(ycGrupo));
            ycDoc.add(ycEspacio(10));
            LocalDate ycEntrada = ycGrupo.stream().map(YCEstadia::getYcEntrada).min(LocalDate::compareTo).orElseThrow();
            LocalDate ycSalidaGrupo = ycGrupo.stream().map(YCEstadia::getYcSalida).max(LocalDate::compareTo).orElseThrow();
            ycDoc.add(ycFechas(ycE.getYcCuarto(), ycEntrada, ycSalidaGrupo, (int) YCEstadiaService.ycNoches(ycEntrada, ycSalidaGrupo)));
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycConceptosGrupo(ycGrupo));
            BigDecimal ycTotal = ycGrupo.stream().map(YCComprobanteHospedajeService::ycImporteGrupo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycImporteEnLetras(ycTotal));
            BigDecimal ycPorPagar = ycConsumosYPago(ycDoc, ycE,
                    ycGrupo.stream().map(YCEstadia::ycGetSaldo).reduce(BigDecimal.ZERO, BigDecimal::add), true);

            YCCuarto ycC = ycE.getYcCuarto();
            if (!ycC.ycGetNormas().isEmpty()) {
                ycDoc.add(ycEspacio(8));
                ycDoc.add(ycLista("NORMAS DE LA CASA: ", ycC.ycGetNormas().stream()
                        .map(YCNorma::ycGetTexto).collect(Collectors.joining(" · "))));
            }
            if (ycE.getYcNota() != null) {
                ycDoc.add(ycEspacio(4));
                ycDoc.add(ycLista("NOTA: ", ycE.getYcNota()));
            }
            BigDecimal ycDevuelto = ycGrupo.stream().map(ycX -> ycX.getYcDevuelto() == null ? BigDecimal.ZERO : ycX.getYcDevuelto())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ycCierre(ycDoc, ycE, ycDevuelto, ycPorPagar);
        } catch (DocumentException ycEx) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ycEx.getMessage(), ycEx);
        } finally {
            ycDoc.close();
        }
    }

    private static BigDecimal ycImporteGrupo(YCEstadia ycX) {
        return ycX.isYcCancelada() ? ycX.ycGetPagado() : ycX.getYcTotal();
    }

    private PdfPTable ycDatosHuespedGrupo(List<YCEstadia> ycGrupo) {
        YCEstadia ycE = ycGrupo.get(0);
        int ycPersonas = ycGrupo.stream().filter(ycX -> !ycX.isYcCancelada()).mapToInt(YCEstadia::getYcPersonas).sum();
        PdfPTable ycTabla = new PdfPTable(new float[]{22, 38, 18, 22});
        ycTabla.setWidthPercentage(100);
        ycAgregarDato(ycTabla, "HUESPED", ycE.getYcHuesped().toUpperCase());
        ycAgregarDato(ycTabla, "EMITIDO", ycE.getYcFechaRegistro().format(YC_FECHA_HORA));
        ycAgregarDato(ycTabla, "DOCUMENTO", ycE.getYcDocumento());
        ycAgregarDato(ycTabla, "PERSONAS", String.valueOf(ycPersonas));
        ycAgregarDato(ycTabla, "RESERVA", "Grupo de " + ycGrupo.size() + " habitaciones");
        ycAgregarDato(ycTabla, "PAGO", ycE.getYcMetodo().ycGetDescripcion().toUpperCase());
        ycAgregarDato(ycTabla, "CELULAR", ycE.getYcTelefono());
        ycAgregarDato(ycTabla, "MONEDA", "SOLES");
        return ycMarco(ycTabla, YC_FILA_ALTERNA, YC_BORDE);
    }

    private PdfPTable ycConceptosGrupo(List<YCEstadia> ycGrupo) {
        PdfPTable ycTabla = new PdfPTable(new float[]{52, 14, 16, 18});
        ycTabla.setWidthPercentage(100);
        ycTabla.addCell(ycCeldaCabecera("HABITACION", Element.ALIGN_LEFT));
        ycTabla.addCell(ycCeldaCabecera("NOCHES", Element.ALIGN_CENTER));
        ycTabla.addCell(ycCeldaCabecera("P. NOCHE", Element.ALIGN_RIGHT));
        ycTabla.addCell(ycCeldaCabecera("IMPORTE", Element.ALIGN_RIGHT));
        BigDecimal ycTotal = BigDecimal.ZERO, ycRecibido = BigDecimal.ZERO, ycDevuelto = BigDecimal.ZERO, ycSaldo = BigDecimal.ZERO;
        for (YCEstadia ycX : ycGrupo) {
            String ycConcepto = ycX.getYcCuartoNumero() + " · " + ycX.getYcPersonas() + (ycX.getYcPersonas() == 1 ? " persona" : " personas")
                    + "\ndel " + ycX.getYcEntrada().format(YC_FECHA) + " al " + ycX.getYcSalida().format(YC_FECHA)
                    + (ycX.ycIsNoLlego() ? " · NO SE PRESENTO" : ycX.isYcCancelada() ? " · CANCELADA"
                    : ycX.ycSalioAntes() ? " · salio antes" : "");
            ycTabla.addCell(ycCeldaItem(ycConcepto, Element.ALIGN_LEFT, ycFNormal));
            ycTabla.addCell(ycCeldaItem(String.valueOf(ycX.getYcNoches()), Element.ALIGN_CENTER, ycFNormal));
            ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycX.getYcPrecioNoche()), Element.ALIGN_RIGHT, ycFNormal));
            ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycImporteGrupo(ycX)), Element.ALIGN_RIGHT, ycFNegrita));
            ycTotal = ycTotal.add(ycImporteGrupo(ycX));
            ycRecibido = ycRecibido.add(ycX.ycGetRecibido());
            ycDevuelto = ycDevuelto.add(ycX.getYcDevuelto() == null ? BigDecimal.ZERO : ycX.getYcDevuelto());
            ycSaldo = ycSaldo.add(ycX.ycGetSaldo());
        }
        if (ycSaldo.signum() > 0 || ycDevuelto.signum() > 0) {
            ycFilaPago(ycTabla, "Pagado por " + ycGrupo.get(0).getYcHuesped(), ycRecibido, ycFNormal);
        }
        if (ycDevuelto.signum() > 0) {
            ycFilaPago(ycTabla, "Devuelto", ycDevuelto.negate(), ycFNegrita);
        }
        if (ycSaldo.signum() > 0) {
            ycFilaPago(ycTabla, "SALDO PENDIENTE (a pagar al llegar)", ycSaldo, ycFNegrita);
        }
        String ycEtiquetaTotal = ycGrupo.stream().allMatch(YCEstadia::isYcCancelada) ? "TOTAL (CANCELADA)"
                : ycSaldo.signum() > 0 ? "TOTAL DEL GRUPO" : "TOTAL PAGADO";
        PdfPCell ycEt = new PdfPCell(new Phrase(ycEtiquetaTotal, ycFTotal));
        ycEt.setColspan(3);
        PdfPCell ycV = new PdfPCell(new Phrase("S/ " + YC_IMPORTE.format(ycTotal), ycFTotal));
        for (PdfPCell ycC : new PdfPCell[]{ycEt, ycV}) {
            ycC.setBackgroundColor(YC_ACENTO);
            ycC.setBorder(Rectangle.NO_BORDER);
            ycC.setPaddingTop(8);
            ycC.setPaddingBottom(9);
            ycC.setPaddingLeft(6);
            ycC.setPaddingRight(6);
        }
        ycV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ycTabla.addCell(ycEt);
        ycTabla.addCell(ycV);
        return ycTabla;
    }

    void ycEscribir(YCEstadia ycE, OutputStream ycSalida) {
        Document ycDoc = new Document(PageSize.A5, 30, 30, 30, 45);
        try {
            PdfWriter ycWriter = PdfWriter.getInstance(ycDoc, ycSalida);
            ycWriter.setPageEvent(new YCPiePagina(ycE));
            ycDoc.addTitle("Comprobante de hospedaje " + ycE.ycGetNumero());
            ycDoc.addAuthor(ycArrendador.getYcNombreHospedaje());
            ycDoc.open();

            ycDoc.add(ycCabecera(ycE));
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycDatosHuesped(ycE));
            ycDoc.add(ycEspacio(10));
            ycDoc.add(ycFechas(ycE));
            if (ycE.ycSalioAntes()) {
                Paragraph ycP = new Paragraph();
                ycP.setSpacingBefore(4);
                ycP.add(new Chunk("SALIDA ANTICIPADA: ", ycFEtiquetaAcento));
                ycP.add(new Chunk("habia reservado hasta el " + ycE.getYcSalidaOriginal().format(YC_FECHA) + " ("
                        + ycE.getYcNochesOriginales() + " noches) y se retiro el " + ycE.getYcSalida().format(YC_FECHA)
                        + ". Se cobran solo las noches de estadia.", ycFSuave));
                ycDoc.add(ycP);
            }
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycConceptos(ycE));
            ycDoc.add(ycEspacio(8));
            ycDoc.add(ycImporteEnLetras(ycE.getYcTotal()));
            BigDecimal ycPorPagar = ycConsumosYPago(ycDoc, ycE, ycE.ycGetSaldo(), false);

            YCCuarto ycC = ycE.getYcCuarto();
            if (!ycC.ycGetComodidades().isEmpty()) {
                ycDoc.add(ycEspacio(8));
                ycDoc.add(ycLista("SU HABITACION INCLUYE: ", ycC.ycGetComodidades().stream()
                        .map(YCComodidad::ycGetTexto).collect(Collectors.joining(" · "))));
            }
            if (!ycC.ycGetNormas().isEmpty()) {
                ycDoc.add(ycEspacio(4));
                ycDoc.add(ycLista("NORMAS DE LA CASA: ", ycC.ycGetNormas().stream()
                        .map(YCNorma::ycGetTexto).collect(Collectors.joining(" · "))));
            }
            if (ycE.getYcNota() != null) {
                ycDoc.add(ycEspacio(4));
                ycDoc.add(ycLista("NOTA: ", ycE.getYcNota()));
            }
            ycCierre(ycDoc, ycE, ycE.getYcDevuelto(), ycPorPagar);
        } catch (DocumentException ycEx) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ycEx.getMessage(), ycEx);
        } finally {
            ycDoc.close();
        }
    }

    private PdfPTable ycCabecera(YCEstadia ycE) {
        PdfPTable ycTabla = new PdfPTable(new float[]{58, 42});
        ycTabla.setWidthPercentage(100);

        PdfPCell ycEmisor = ycSinBorde();
        ycEmisor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ycEmisor.addElement(new Paragraph(ycArrendador.getYcNombreHospedaje(), ycFNombre));
        ycEmisor.addElement(new Paragraph("Hospedaje por noche", ycFNegrita));
        if (!ycVacio(ycArrendador.getYcDireccion())) {
            ycEmisor.addElement(new Paragraph(ycArrendador.getYcDireccion(), ycFSuave));
        }
        StringBuilder ycContacto = new StringBuilder();
        if (!ycVacio(ycArrendador.getYcTelefono())) {
            ycContacto.append("Cel. / WhatsApp ").append(ycArrendador.getYcTelefono());
        }
        if (!ycVacio(ycArrendador.getYcDni())) {
            ycContacto.append(ycContacto.isEmpty() ? "" : "  ·  ").append("DNI ").append(ycArrendador.getYcDni());
        }
        if (!ycContacto.isEmpty()) {
            ycEmisor.addElement(new Paragraph(ycContacto.toString(), ycFSuave));
        }
        ycTabla.addCell(ycEmisor);

        PdfPTable ycCaja = new PdfPTable(1);
        ycCaja.setWidthPercentage(100);
        ycCaja.addCell(ycCeldaCaja(new Phrase("COMPROBANTE DE HOSPEDAJE", ycFCajaTipo), YC_ACENTO, 7, 7));
        ycCaja.addCell(ycCeldaCaja(new Phrase(ycE.ycGetNumero(), ycFCajaNumero), Color.WHITE, 7, 9));
        PdfPCell ycContenedor = new PdfPCell(ycCaja);
        ycContenedor.setBorderColor(YC_ACENTO);
        ycContenedor.setBorderWidth(1.2f);
        ycContenedor.setPadding(0);
        ycTabla.addCell(ycContenedor);
        return ycTabla;
    }

    private PdfPTable ycDatosHuesped(YCEstadia ycE) {
        YCCuarto ycC = ycE.getYcCuarto();
        PdfPTable ycTabla = new PdfPTable(new float[]{22, 38, 18, 22});
        ycTabla.setWidthPercentage(100);
        ycAgregarDato(ycTabla, "HUESPED", ycE.getYcHuesped().toUpperCase());
        ycAgregarDato(ycTabla, "EMITIDO", ycE.getYcFechaRegistro().format(YC_FECHA_HORA));
        ycAgregarDato(ycTabla, "DOCUMENTO", ycE.getYcDocumento());
        ycAgregarDato(ycTabla, "PERSONAS", String.valueOf(ycE.getYcPersonas()));
        ycAgregarDato(ycTabla, "HABITACION", ycE.getYcCuartoNumero()
                + (ycC.getYcTipoHabitacion() == null ? "" : " (" + ycC.getYcTipoHabitacion().ycGetTexto() + ")"));
        ycAgregarDato(ycTabla, "PAGO", ycE.getYcMetodo().ycGetDescripcion().toUpperCase());
        ycAgregarDato(ycTabla, "CELULAR", ycE.getYcTelefono());
        ycAgregarDato(ycTabla, "MONEDA", "SOLES");
        return ycMarco(ycTabla, YC_FILA_ALTERNA, YC_BORDE);
    }

    private PdfPTable ycFechas(YCEstadia ycE) {
        return ycFechas(ycE.getYcCuarto(), ycE.getYcEntrada(), ycE.getYcSalida(), ycE.getYcNoches());
    }

    private PdfPTable ycFechas(YCCuarto ycC, LocalDate ycEntrada, LocalDate ycSalida, int ycCuantas) {
        PdfPTable ycTabla = new PdfPTable(new float[]{42, 16, 42});
        ycTabla.setWidthPercentage(100);
        ycTabla.addCell(ycBloqueFecha("ENTRADA (CHECK-IN)", ycEntrada.format(YC_FECHA_LARGA),
                "desde las " + ycC.ycGetHoraEntradaODefecto()));
        PdfPCell ycMedio = ycSinBorde();
        ycMedio.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph ycNoches = new Paragraph(ycCuantas + (ycCuantas == 1 ? " noche" : " noches"), ycFEtiquetaAcento);
        ycNoches.setAlignment(Element.ALIGN_CENTER);
        Paragraph ycFlecha = new Paragraph("»", ycFGrande);
        ycFlecha.setAlignment(Element.ALIGN_CENTER);
        ycMedio.addElement(ycNoches);
        ycMedio.addElement(ycFlecha);
        ycTabla.addCell(ycMedio);
        ycTabla.addCell(ycBloqueFecha("SALIDA (CHECK-OUT)", ycSalida.format(YC_FECHA_LARGA),
                "hasta las " + ycC.ycGetHoraSalidaODefecto()));
        return ycMarco(ycTabla, YC_ACENTO_SUAVE, YC_BORDE);
    }

    private PdfPCell ycBloqueFecha(String ycEtiqueta, String ycFecha, String ycHora) {
        PdfPCell ycCelda = ycSinBorde();
        ycCelda.addElement(new Paragraph(ycEtiqueta, ycFEtiquetaAcento));
        ycCelda.addElement(new Paragraph(ycFecha, ycFGrande));
        ycCelda.addElement(new Paragraph(ycHora, ycFSuave));
        return ycCelda;
    }

    private PdfPTable ycConceptos(YCEstadia ycE) {
        PdfPTable ycTabla = new PdfPTable(new float[]{52, 14, 16, 18});
        ycTabla.setWidthPercentage(100);
        ycTabla.addCell(ycCeldaCabecera("CONCEPTO", Element.ALIGN_LEFT));
        ycTabla.addCell(ycCeldaCabecera("NOCHES", Element.ALIGN_CENTER));
        ycTabla.addCell(ycCeldaCabecera("P. NOCHE", Element.ALIGN_RIGHT));
        ycTabla.addCell(ycCeldaCabecera("IMPORTE", Element.ALIGN_RIGHT));

        String ycConcepto = "Hospedaje en " + ycE.getYcCuartoNumero() + "\ndel " + ycE.getYcEntrada().format(YC_FECHA)
                + " al " + ycE.getYcSalida().format(YC_FECHA);
        ycTabla.addCell(ycCeldaItem(ycConcepto, Element.ALIGN_LEFT, ycFNormal));
        ycTabla.addCell(ycCeldaItem(String.valueOf(ycE.getYcNoches()), Element.ALIGN_CENTER, ycFNormal));
        ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycE.getYcPrecioNoche()), Element.ALIGN_RIGHT, ycFNormal));
        ycTabla.addCell(ycCeldaItem(YC_IMPORTE.format(ycE.getYcTotal()), Element.ALIGN_RIGHT, ycFNegrita));

        boolean ycConAdelanto = ycE.getYcAdelanto() != null && ycE.getYcAdelanto().compareTo(ycE.getYcTotal()) < 0 && !ycE.isYcCancelada();
        if (ycConAdelanto) {
            ycFilaPago(ycTabla, "Adelanto recibido (" + ycE.getYcMetodo().ycGetDescripcion() + ") el "
                    + ycE.getYcFechaRegistro().format(YC_FECHA), ycE.getYcAdelanto(), ycFNormal);
            if (Boolean.TRUE.equals(ycE.getYcSaldoPagado())) {
                ycFilaPago(ycTabla, "Saldo pagado (" + (ycE.getYcMetodoSaldo() == null ? "—" : ycE.getYcMetodoSaldo().ycGetDescripcion())
                        + ")" + (ycE.getYcFechaSaldo() == null ? "" : " el " + ycE.getYcFechaSaldo().format(YC_FECHA)),
                        ycE.getYcTotal().subtract(ycE.getYcAdelanto()), ycFNormal);
            } else {
                ycFilaPago(ycTabla, "SALDO PENDIENTE (a pagar al llegar)", ycE.ycGetSaldo(), ycFNegrita);
            }
        }
        if (ycE.getYcDevuelto() != null && ycE.getYcDevuelto().signum() > 0) {
            ycFilaPago(ycTabla, "Pagado por el huesped", ycE.ycGetRecibido(), ycFNormal);
            ycFilaPago(ycTabla, (ycE.ycIsNoLlego() ? "Adelanto devuelto (" : "Devuelto por salida anticipada (")
                    + ycE.ycGetMetodoDevolucionODefecto().ycGetDescripcion()
                    + ")" + (ycE.getYcFechaDevolucion() == null ? "" : " el " + ycE.getYcFechaDevolucion().format(YC_FECHA)),
                    ycE.getYcDevuelto().negate(), ycFNegrita);
        }
        String ycEtiquetaTotal = ycE.isYcCancelada() ? "TOTAL (CANCELADA)"
                : ycE.ycTieneSaldo() ? "TOTAL DE LA ESTADIA" : "TOTAL PAGADO";
        PdfPCell ycEt = new PdfPCell(new Phrase(ycEtiquetaTotal, ycFTotal));
        ycEt.setColspan(3);
        PdfPCell ycV = new PdfPCell(new Phrase("S/ " + YC_IMPORTE.format(ycE.getYcTotal()), ycFTotal));
        for (PdfPCell ycC : new PdfPCell[]{ycEt, ycV}) {
            ycC.setBackgroundColor(YC_ACENTO);
            ycC.setBorder(Rectangle.NO_BORDER);
            ycC.setPaddingTop(8);
            ycC.setPaddingBottom(9);
            ycC.setPaddingLeft(6);
            ycC.setPaddingRight(6);
        }
        ycV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ycTabla.addCell(ycEt);
        ycTabla.addCell(ycV);
        return ycTabla;
    }

    private void ycFilaPago(PdfPTable ycTabla, String ycTexto, BigDecimal ycMonto, Font ycFuente) {
        PdfPCell ycC = ycCeldaItem(ycTexto, Element.ALIGN_LEFT, ycFuente);
        ycC.setColspan(3);
        ycC.setBackgroundColor(YC_FILA_ALTERNA);
        ycTabla.addCell(ycC);
        PdfPCell ycV = ycCeldaItem(YC_IMPORTE.format(ycMonto), Element.ALIGN_RIGHT, ycFuente);
        ycV.setBackgroundColor(YC_FILA_ALTERNA);
        ycTabla.addCell(ycV);
    }

    private PdfPTable ycImporteEnLetras(BigDecimal ycTotal) {
        Phrase ycFrase = new Phrase();
        ycFrase.add(new Chunk("SON: ", ycFEtiqueta));
        ycFrase.add(new Chunk(YCNumeroALetras.ycImporte(ycTotal), ycFNegrita));
        PdfPTable ycT = new PdfPTable(1);
        ycT.setWidthPercentage(100);
        PdfPCell ycC = new PdfPCell(ycFrase);
        ycC.setBorderColor(YC_BORDE);
        ycC.setBackgroundColor(YC_ACENTO_SUAVE);
        ycC.setPadding(8);
        ycT.addCell(ycC);
        return ycT;
    }

    private Paragraph ycLista(String ycEtiqueta, String ycTexto) {
        Paragraph ycP = new Paragraph();
        ycP.setLeading(11);
        ycP.add(new Chunk(ycEtiqueta, ycFEtiquetaAcento));
        ycP.add(new Chunk(ycTexto, ycFSuave));
        return ycP;
    }

    private PdfPTable ycFirma(YCEstadia ycE, BigDecimal ycDevuelto, PdfPCell ycIzquierda) {
        PdfPTable ycT = new PdfPTable(new float[]{45, 10, 45});
        ycT.setWidthPercentage(100);
        if (ycIzquierda != null) {
            ycT.addCell(ycIzquierda);
        } else if (ycDevuelto != null && ycDevuelto.signum() > 0) {
            PdfPCell ycH = new PdfPCell();
            ycH.setBorder(Rectangle.TOP);
            ycH.setBorderColor(YC_TEXTO_SUAVE);
            ycH.setPaddingTop(4);
            Paragraph ycRecibi = new Paragraph("Recibi conforme S/ " + YC_IMPORTE.format(ycDevuelto) + " de devolucion", ycFNegrita);
            ycRecibi.setAlignment(Element.ALIGN_CENTER);
            Paragraph ycQuien = new Paragraph(ycE.getYcHuesped() + (ycE.getYcDocumento() == null ? "" : " · " + ycE.getYcDocumento()), ycFSuave);
            ycQuien.setAlignment(Element.ALIGN_CENTER);
            ycH.addElement(ycRecibi);
            ycH.addElement(ycQuien);
            ycT.addCell(ycH);
        } else {
            ycT.addCell(ycSinBorde());
        }
        ycT.addCell(ycSinBorde());
        PdfPCell ycC = new PdfPCell();
        ycC.setBorder(Rectangle.TOP);
        ycC.setBorderColor(YC_TEXTO_SUAVE);
        ycC.setPaddingTop(4);
        Paragraph ycNombre = new Paragraph(ycArrendador.getYcNombreHospedaje(), ycFNegrita);
        ycNombre.setAlignment(Element.ALIGN_CENTER);
        Paragraph ycRol = new Paragraph("Recepcion", ycFSuave);
        ycRol.setAlignment(Element.ALIGN_CENTER);
        ycC.addElement(ycNombre);
        ycC.addElement(ycRol);
        ycT.addCell(ycC);
        return ycT;
    }

    private static PdfPTable ycMarco(PdfPTable ycContenido, Color ycFondo, Color ycBorde) {
        PdfPTable ycMarco = new PdfPTable(1);
        ycMarco.setWidthPercentage(100);
        PdfPCell ycCelda = new PdfPCell(ycContenido);
        ycCelda.setBorderColor(ycBorde);
        ycCelda.setPadding(8);
        ycCelda.setBackgroundColor(ycFondo);
        ycMarco.addCell(ycCelda);
        return ycMarco;
    }

    private void ycAgregarDato(PdfPTable ycTabla, String ycEtiqueta, String ycValor) {
        PdfPCell ycE = ycSinBorde();
        ycE.setPhrase(new Phrase(ycEtiqueta, ycFEtiqueta));
        ycE.setPaddingBottom(5);
        ycTabla.addCell(ycE);
        PdfPCell ycV = ycSinBorde();
        ycV.setPhrase(new Phrase(ycVacio(ycValor) ? "—" : ycValor, ycFNormal));
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

    private PdfPCell ycCeldaItem(String ycTexto, int ycAlineacion, Font ycFuente) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFuente));
        ycC.setHorizontalAlignment(ycAlineacion);
        ycC.setVerticalAlignment(Element.ALIGN_MIDDLE);
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
        private final boolean ycCancelada;

        YCPiePagina(YCEstadia ycEstadia) {
            this(ycEstadia.isYcCancelada());
        }

        YCPiePagina(boolean ycCancelada) {
            this.ycCancelada = ycCancelada;
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
                    new Phrase("Gracias por elegir " + ycArrendador.getYcNombreHospedaje() + "  ·  ¡Vuelva pronto!", ycFSuave),
                    ycCentro, ycY, 0);

            if (ycCancelada) {
                PdfContentByte ycDebajo = ycWriter.getDirectContentUnder();
                ycDebajo.saveState();
                PdfGState ycEstado = new PdfGState();
                ycEstado.setFillOpacity(0.12f);
                ycDebajo.setGState(ycEstado);
                try {
                    ycDebajo.beginText();
                    ycDebajo.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 70);
                    ycDebajo.setColorFill(YC_PELIGRO);
                    ycDebajo.showTextAligned(Element.ALIGN_CENTER, "CANCELADA",
                            ycCentro, (ycDocument.top() + ycDocument.bottom()) / 2, 40);
                    ycDebajo.endText();
                } catch (DocumentException | IOException ycEx) {
                    throw new IllegalStateException(ycEx);
                }
                ycDebajo.restoreState();
            }
        }
    }
}
