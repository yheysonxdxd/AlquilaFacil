package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class YCContratoPdfService {
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_GRIS = new Color(101, 109, 118);
    private static final DateTimeFormatter YC_FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));
    private static final DecimalFormat YC_IMPORTE = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCInquilinoService ycInquilinoService;
    private final YCDatosArrendador ycArrendador;

    private final Font ycFTitulo = ycF(14, Font.BOLD, YC_TEXTO);
    private final Font ycFNormal = ycF(10, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycF(10, Font.BOLD, YC_TEXTO);
    private final Font ycFSuave = ycF(8, Font.NORMAL, YC_GRIS);

    @Transactional(readOnly = true)
    public void ycGenerar(Long ycInquilinoId, File ycDestino) throws IOException {
        YCInquilino ycI = ycInquilinoService.ycObtener(ycInquilinoId);
        try (OutputStream ycOut = new FileOutputStream(ycDestino)) {
            ycEscribir(ycI, LocalDate.now(), ycOut);
        }
    }

    public static String ycNombreArchivo(YCInquilino ycI) {
        return "Contrato - " + ycI.getYcCuarto().getYcNumero() + " - " + ycI.ycGetNombreCompleto();
    }

    List<String[]> ycClausulas(YCInquilino ycI) {
        YCCuarto ycC = ycI.getYcCuarto();
        BigDecimal ycRenta = ycI.ycGetRentaMensual().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ycGarantia = ycI.getYcGarantia() == null ? BigDecimal.ZERO : ycI.getYcGarantia().setScale(2, RoundingMode.HALF_UP);
        String ycDireccion = ycVacio(ycArrendador.getYcDireccion()) ? "el inmueble del ARRENDADOR" : ycArrendador.getYcDireccion();

        List<String[]> ycL = new ArrayList<>();
        ycL.add(new String[]{"PRIMERA: OBJETO",
                "EL ARRENDADOR da en alquiler a EL ARRENDATARIO la habitacion denominada \"" + ycC.getYcNumero()
                        + "\", ubicada en " + ycDireccion + ", para ser usada unicamente como vivienda."
                        + (ycVacio(ycC.getYcDescripcion()) ? "" : " Descripcion: " + ycC.getYcDescripcion().trim() + ".")});
        ycL.add(new String[]{"SEGUNDA: PLAZO", ycPlazo(ycI)});
        ycL.add(new String[]{"TERCERA: RENTA Y FORMA DE PAGO",
                "La renta mensual es de S/ " + YC_IMPORTE.format(ycRenta) + " (" + YCNumeroALetras.ycImporte(ycRenta) + "), "
                        + "que EL ARRENDATARIO pagara por adelantado hasta el dia " + ycI.getYcDiaPago()
                        + " de cada mes (si el mes tiene menos dias, el ultimo dia del mes). Cada pago se acredita "
                        + "con un recibo numerado entregado por EL ARRENDADOR."});
        ycL.add(new String[]{"CUARTA: SERVICIOS",
                "El consumo de luz y agua no esta incluido en la renta. Se cobrara cada mes junto con la renta, "
                        + "segun el consumo que corresponda a la habitacion."});
        ycL.add(new String[]{"QUINTA: GARANTIA",
                ycGarantia.signum() > 0
                        ? "EL ARRENDATARIO entrega en garantia S/ " + YC_IMPORTE.format(ycGarantia) + " ("
                        + YCNumeroALetras.ycImporte(ycGarantia) + "). Al terminar el contrato se le devolvera, descontando "
                        + "las rentas o servicios que deba y los daños que hubiera causado, de lo cual se dejara "
                        + "constancia en una liquidacion firmada por ambas partes."
                        : "Las partes acuerdan que no se entrega garantia."});
        ycL.add(new String[]{"SEXTA: OBLIGACIONES DEL ARRENDATARIO",
                "a) Pagar puntualmente la renta y los servicios. b) Cuidar la habitacion, sus muebles y las areas "
                        + "comunes, y devolverlos en el estado en que los recibio, salvo el desgaste normal. "
                        + "c) No subarrendar ni ceder la habitacion, ni alojar a otras personas de forma permanente sin "
                        + "permiso de EL ARRENDADOR. d) Respetar la tranquilidad de los demas inquilinos y las normas "
                        + "de la casa. e) Avisar con anticipacion cualquier desperfecto."});
        ycL.add(new String[]{"SETIMA: TERMINO DEL CONTRATO",
                "Cualquiera de las partes puede dar por terminado el contrato avisando a la otra con al menos "
                        + "treinta (30) dias de anticipacion. La falta de pago de dos (2) meses de renta permite a "
                        + "EL ARRENDADOR dar por resuelto el contrato. Al retirarse, EL ARRENDATARIO entregara la "
                        + "habitacion y las llaves."});
        ycL.add(new String[]{"OCTAVA: CONFORMIDAD",
                "Las partes declaran estar de acuerdo con todas las clausulas y firman en señal de conformidad."});
        return ycL;
    }

    private String ycPlazo(YCInquilino ycI) {
        String ycInicio = ycI.getYcFechaIngreso().format(YC_FECHA_LARGA);
        if (ycI.getYcFechaFinContrato() == null) {
            return "El contrato rige desde el " + ycInicio + " y es de plazo indeterminado.";
        }
        Period ycP = Period.between(ycI.getYcFechaIngreso(), ycI.getYcFechaFinContrato());
        int ycMeses = ycP.getYears() * 12 + ycP.getMonths();
        String ycDuracion = ycMeses >= 1 ? " (" + ycMeses + (ycMeses == 1 ? " mes" : " meses") + ")" : "";
        return "El contrato rige desde el " + ycInicio + " hasta el " + ycI.getYcFechaFinContrato().format(YC_FECHA_LARGA)
                + ycDuracion + ". Puede renovarse por acuerdo de ambas partes.";
    }

    void ycEscribir(YCInquilino ycI, LocalDate ycHoy, OutputStream ycOut) {
        Document ycDoc = new Document(PageSize.A4, 56, 56, 50, 50);
        try {
            PdfWriter.getInstance(ycDoc, ycOut);
            ycDoc.addTitle(ycNombreArchivo(ycI));
            ycDoc.open();

            Paragraph ycTitulo = new Paragraph("CONTRATO DE ARRENDAMIENTO DE HABITACION", ycFTitulo);
            ycTitulo.setAlignment(Element.ALIGN_CENTER);
            ycTitulo.setSpacingAfter(16);
            ycDoc.add(ycTitulo);

            Paragraph ycPartes = new Paragraph();
            ycPartes.setLeading(15);
            ycPartes.setAlignment(Element.ALIGN_JUSTIFIED);
            ycPartes.add(new Chunk("Conste por el presente documento el contrato de arrendamiento que celebran, de una parte, ", ycFNormal));
            ycPartes.add(new Chunk(ycArrendador.getYcNombre().toUpperCase(), ycFNegrita));
            if (!ycVacio(ycArrendador.getYcDni())) ycPartes.add(new Chunk(", identificado(a) con DNI N° " + ycArrendador.getYcDni(), ycFNormal));
            if (!ycVacio(ycArrendador.getYcDireccion())) ycPartes.add(new Chunk(", con domicilio en " + ycArrendador.getYcDireccion(), ycFNormal));
            ycPartes.add(new Chunk(", a quien se le denominara EL ARRENDADOR; y de la otra parte, ", ycFNormal));
            ycPartes.add(new Chunk(ycI.ycGetNombreCompleto().toUpperCase(), ycFNegrita));
            ycPartes.add(new Chunk(", identificado(a) con DNI N° " + ycI.getYcDni()
                    + (ycVacio(ycI.getYcTelefono()) ? "" : ", celular " + ycI.getYcTelefono())
                    + ", a quien se le denominara EL ARRENDATARIO; en los terminos siguientes:", ycFNormal));
            ycPartes.setSpacingAfter(10);
            ycDoc.add(ycPartes);

            for (String[] ycCl : ycClausulas(ycI)) {
                Paragraph ycP = new Paragraph();
                ycP.setLeading(15);
                ycP.setAlignment(Element.ALIGN_JUSTIFIED);
                ycP.add(new Chunk(ycCl[0] + ". ", ycFNegrita));
                ycP.add(new Chunk(ycCl[1], ycFNormal));
                ycP.setSpacingAfter(8);
                ycDoc.add(ycP);
            }

            Paragraph ycLugar = new Paragraph("Firmado el " + ycHoy.format(YC_FECHA_LARGA) + ".", ycFNormal);
            ycLugar.setSpacingBefore(6);
            ycDoc.add(ycLugar);

            Paragraph ycEspacio = new Paragraph(" ");
            ycEspacio.setSpacingBefore(60);
            ycDoc.add(ycEspacio);
            PdfPTable ycFirmas = new PdfPTable(new float[]{45, 10, 45});
            ycFirmas.setWidthPercentage(90);
            ycFirmas.addCell(ycFirma("EL ARRENDADOR", ycArrendador.getYcNombre(), ycArrendador.getYcDni()));
            PdfPCell ycMedio = new PdfPCell();
            ycMedio.setBorder(Rectangle.NO_BORDER);
            ycFirmas.addCell(ycMedio);
            ycFirmas.addCell(ycFirma("EL ARRENDATARIO", ycI.ycGetNombreCompleto(), ycI.getYcDni()));
            ycDoc.add(ycFirmas);
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el contrato", ycE);
        } finally {
            ycDoc.close();
        }
    }

    private PdfPCell ycFirma(String ycRol, String ycNombre, String ycDni) {
        Phrase ycTexto = new Phrase();
        ycTexto.add(new Chunk(ycRol + "\n", ycFNegrita));
        ycTexto.add(new Chunk(ycNombre + (ycVacio(ycDni) ? "" : "\nDNI " + ycDni), ycFSuave));
        PdfPCell ycC = new PdfPCell(ycTexto);
        ycC.setBorder(Rectangle.TOP);
        ycC.setBorderColor(YC_TEXTO);
        ycC.setHorizontalAlignment(Element.ALIGN_CENTER);
        ycC.setPaddingTop(6);
        return ycC;
    }

    private static boolean ycVacio(String ycS) {
        return ycS == null || ycS.isBlank();
    }

    private static Font ycF(float ycTam, int ycEstilo, Color ycColor) {
        return new Font(Font.HELVETICA, ycTam, ycEstilo, ycColor);
    }
}
