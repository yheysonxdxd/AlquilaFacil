package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCPago;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCPagoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCReporteMensualService {
    private static final Color YC_MENSUAL = new Color(227, 28, 95);
    private static final Color YC_MENSUAL_SUAVE = new Color(255, 240, 243);
    private static final Color YC_HOSP = new Color(34, 34, 34);
    private static final Color YC_HOSP_SUAVE = new Color(242, 242, 242);
    private static final Color YC_TEXTO = new Color(31, 35, 40);
    private static final Color YC_TEXTO_SUAVE = new Color(101, 109, 118);
    private static final Color YC_BORDE = new Color(222, 226, 232);
    private static final Color YC_OSCURO = new Color(15, 31, 46);

    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat YC_IMPORTE =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat YC_CSV_IMPORTE =
            new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final YCPagoRepository ycPagoRepository;
    private final YCEstadiaRepository ycEstadiaRepository;
    private final YCCuartoRepository ycCuartoRepository;
    private final YCEstadiaService ycEstadiaService;
    private final YCDatosArrendador ycArrendador;
    private final YCGastoService ycGastoService;

    public record YCOcupacion(String ycHabitacion, int ycNoches, int ycDias, BigDecimal ycIngresos) {
        public int ycPorcentaje() {
            return ycDias == 0 ? 0 : (int) Math.round(ycNoches * 100.0 / ycDias);
        }
    }

    public record YCDatos(YearMonth ycMes, List<YCPago> ycPagos, BigDecimal ycTotalMensual, BigDecimal ycAlquileres,
                        BigDecimal ycLuz, BigDecimal ycAgua, List<YCEstadia> ycEstadias, BigDecimal ycCobradoHospedaje,
                        BigDecimal ycSaldosPendientes, List<YCOcupacion> ycOcupacion, int ycOcupacionTotal,
                        List<com.alquilafacil.model.YCGasto> ycGastos, YCGastoService.YCResultado ycResultado) {
        public BigDecimal ycTotal() {
            return ycTotalMensual.add(ycCobradoHospedaje);
        }
    }

    public YCDatos ycDatos(YearMonth ycMes) {
        LocalDateTime ycDesde = ycMes.atDay(1).atStartOfDay();
        LocalDateTime ycHasta = ycMes.plusMonths(1).atDay(1).atStartOfDay();
        List<YCPago> ycPagos = ycPagoRepository
                .ycFindByAnuladoFalseAndFechaPagoGreaterThanEqualAndFechaPagoLessThanOrderByFechaPago(ycDesde, ycHasta);
        BigDecimal ycAlquileres = ycSuma(ycPagos.stream().map(YCPago::getYcAlquiler).toList());
        BigDecimal ycLuz = ycSuma(ycPagos.stream().map(YCPago::getYcLuz).toList());
        BigDecimal ycAgua = ycSuma(ycPagos.stream().map(YCPago::getYcAgua).toList());
        BigDecimal ycTotalMensual = ycSuma(ycPagos.stream().map(YCPago::getYcTotal).toList());

        LocalDate ycInicio = ycMes.atDay(1);
        LocalDate ycFin = ycMes.plusMonths(1).atDay(1);
        List<YCEstadia> ycEstadias = ycEstadiaRepository.ycEntre(ycInicio, ycFin);
        BigDecimal ycSaldos = ycSuma(ycEstadias.stream().map(YCEstadia::ycGetSaldo).toList());

        List<YCOcupacion> ycOcupacion = new ArrayList<>();
        int ycNochesTotal = 0;
        List<YCCuarto> ycHabitaciones = ycCuartoRepository.ycFindAllByOrderByNumeroAsc().stream().filter(YCCuarto::ycIsHospedaje).toList();
        for (YCCuarto ycC : ycHabitaciones) {
            int ycNoches = 0;
            BigDecimal ycIngresos = BigDecimal.ZERO;
            for (YCEstadia ycE : ycEstadias) {
                if (!ycE.getYcCuarto().getYcId().equals(ycC.getYcId())) {
                    continue;
                }
                int ycN = ycNochesEnMes(ycE, ycInicio, ycFin);
                ycNoches += ycN;
                ycIngresos = ycIngresos.add(ycE.getYcPrecioNoche().multiply(BigDecimal.valueOf(ycN)));
            }
            ycNochesTotal += ycNoches;
            ycOcupacion.add(new YCOcupacion(ycC.getYcNumero(), ycNoches, ycMes.lengthOfMonth(), ycIngresos));
        }
        int ycPosibles = ycHabitaciones.size() * ycMes.lengthOfMonth();
        int ycOcupacionTotal = ycPosibles == 0 ? 0 : (int) Math.round(ycNochesTotal * 100.0 / ycPosibles);

        return new YCDatos(ycMes, ycPagos, ycTotalMensual, ycAlquileres, ycLuz, ycAgua, ycEstadias,
                ycEstadiaService.ycCobradoEn(ycMes), ycSaldos, ycOcupacion, ycOcupacionTotal,
                ycGastoService.ycDelMes(ycMes), ycGastoService.ycResultado(ycMes));
    }

    public void ycGenerarExcel(YearMonth ycMes, File ycDestino) throws IOException {
        YCDatos ycD = ycDatos(ycMes);
        String ycTitulo = ycArrendador.getYcNombreHospedaje() + " · Reporte de " + YCCalendarioPagos.ycTexto(ycMes);
        YCGastoService.YCResultado ycR = ycD.ycResultado();

        YCExcel.YCHoja ycResumen = new YCExcel.YCHoja("Resumen").ycTitulo(ycTitulo)
                .ycColumna("Concepto", YCExcel.YCTipo.YC_TEXTO, 38).ycColumna("Monto (S/)", YCExcel.YCTipo.YC_MONEDA, 16);
        ycResumen.ycFila("Alquiler mensual (cobrado)", ycD.ycTotalMensual());
        ycResumen.ycFila(ycArrendador.getYcNombreHospedaje() + " (cobrado)", ycD.ycCobradoHospedaje());
        ycResumen.ycFila("TOTAL INGRESOS DEL MES", ycD.ycTotal());
        ycResumen.ycFila("Gastos del mes", ycR.ycGastos());
        ycResumen.ycFila("Ganancia alquiler mensual", ycR.ycGananciaMensual());
        ycResumen.ycFila("Ganancia " + ycArrendador.getYcNombreHospedaje(), ycR.ycGananciaHospedaje());
        ycResumen.ycFila("Gastos de los dos negocios", ycR.ycGastosGenerales());
        ycResumen.ycFila("Saldos de hospedaje por cobrar", ycD.ycSaldosPendientes());
        ycResumen.ycTotales("GANANCIA DEL MES (margen " + ycR.ycMargen() + "%, ocupacion " + ycD.ycOcupacionTotal() + "%)", ycR.ycGanancia());

        YCExcel.YCHoja ycPagos = new YCExcel.YCHoja("Alquiler mensual").ycTitulo(ycTitulo).ycTitulo("Pagos de alquiler cobrados en el mes")
                .ycColumna("Fecha", YCExcel.YCTipo.YC_FECHA_HORA, 17).ycColumna("Recibo", YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Inquilino", YCExcel.YCTipo.YC_TEXTO, 28).ycColumna("Cuarto", YCExcel.YCTipo.YC_TEXTO, 14)
                .ycColumna("Mes que paga", YCExcel.YCTipo.YC_TEXTO, 16).ycColumna("Alquiler", YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Luz", YCExcel.YCTipo.YC_MONEDA, 10).ycColumna("Agua", YCExcel.YCTipo.YC_MONEDA, 10)
                .ycColumna("Total", YCExcel.YCTipo.YC_MONEDA, 12).ycColumna("Forma", YCExcel.YCTipo.YC_TEXTO, 13)
                .ycColumna("Cobro", YCExcel.YCTipo.YC_TEXTO, 12);
        for (YCPago ycP : ycD.ycPagos()) {
            ycPagos.ycFila(ycP.getYcFechaPago(), ycP.ycGetNumeroRecibo(), ycP.getYcInquilino().ycGetNombreCompleto(), ycP.getYcCuartoNumero(),
                    YCCalendarioPagos.ycTexto(ycP.getYcPeriodo()), ycP.getYcAlquiler(), ycP.getYcLuz(), ycP.getYcAgua(), ycP.getYcTotal(),
                    ycP.getYcMetodo().ycGetDescripcion(), ycP.getYcRegistradoPor());
        }
        ycPagos.ycTotales("Totales", null, null, null, null, ycD.ycAlquileres(), ycD.ycLuz(), ycD.ycAgua(), ycD.ycTotalMensual(), null, null);

        YCExcel.YCHoja ycEstadias = new YCExcel.YCHoja("Hospedaje").ycTitulo(ycTitulo).ycTitulo("Estadias con noches en el mes")
                .ycColumna("N°", YCExcel.YCTipo.YC_TEXTO, 10).ycColumna("Huesped", YCExcel.YCTipo.YC_TEXTO, 28)
                .ycColumna("Documento", YCExcel.YCTipo.YC_TEXTO, 12).ycColumna("Habitacion", YCExcel.YCTipo.YC_TEXTO, 16)
                .ycColumna("Entrada", YCExcel.YCTipo.YC_FECHA, 12).ycColumna("Salida", YCExcel.YCTipo.YC_FECHA, 12)
                .ycColumna("Noches", YCExcel.YCTipo.YC_ENTERO, 8).ycColumna("Total", YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Pagado", YCExcel.YCTipo.YC_MONEDA, 12).ycColumna("Saldo", YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Forma", YCExcel.YCTipo.YC_TEXTO, 13);
        BigDecimal ycTotalEst = BigDecimal.ZERO, ycPagadoEst = BigDecimal.ZERO;
        for (YCEstadia ycE : ycD.ycEstadias()) {
            ycEstadias.ycFila(ycE.ycGetNumero(), ycE.getYcHuesped(), ycE.getYcDocumento(), ycE.getYcCuartoNumero(), ycE.getYcEntrada(), ycE.getYcSalida(),
                    ycE.getYcNoches(), ycE.getYcTotal(), ycE.ycGetPagado(), ycE.ycGetSaldo(), ycE.getYcMetodo().ycGetDescripcion());
            ycTotalEst = ycTotalEst.add(ycE.getYcTotal());
            ycPagadoEst = ycPagadoEst.add(ycE.ycGetPagado());
        }
        ycEstadias.ycTotales("Totales", null, null, null, null, null, null, ycTotalEst, ycPagadoEst, ycD.ycSaldosPendientes(), null);

        YCExcel.YCHoja ycGastos = new YCExcel.YCHoja("Gastos").ycTitulo(ycTitulo)
                .ycColumna("Fecha", YCExcel.YCTipo.YC_FECHA, 12).ycColumna("Categoria", YCExcel.YCTipo.YC_TEXTO, 22)
                .ycColumna("Negocio", YCExcel.YCTipo.YC_TEXTO, 18).ycColumna("Descripcion", YCExcel.YCTipo.YC_TEXTO, 34)
                .ycColumna("Pagado con", YCExcel.YCTipo.YC_TEXTO, 13).ycColumna("Monto", YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Registro", YCExcel.YCTipo.YC_TEXTO, 12);
        for (com.alquilafacil.model.YCGasto ycG : ycD.ycGastos()) {
            ycGastos.ycFila(ycG.getYcFecha(), ycG.getYcCategoria().ycGetTexto(), ycG.getYcNegocio().ycGetTexto(), ycG.getYcDescripcion(),
                    ycG.getYcMetodo() == null ? null : ycG.getYcMetodo().ycGetDescripcion(), ycG.getYcMonto(), ycG.getYcRegistradoPor());
        }
        ycGastos.ycTotales("Total", null, null, null, null, ycR.ycGastos(), null);

        YCExcel.YCHoja ycOcupacion = new YCExcel.YCHoja("Ocupacion").ycTitulo(ycTitulo).ycTitulo("Ocupacion por habitacion de hospedaje")
                .ycColumna("Habitacion", YCExcel.YCTipo.YC_TEXTO, 20).ycColumna("Noches ocupadas", YCExcel.YCTipo.YC_ENTERO, 15)
                .ycColumna("Dias del mes", YCExcel.YCTipo.YC_ENTERO, 12).ycColumna("Ocupacion", YCExcel.YCTipo.YC_PORCENTAJE, 11)
                .ycColumna("Ingresos de esas noches", YCExcel.YCTipo.YC_MONEDA, 20);
        for (YCOcupacion ycO : ycD.ycOcupacion()) {
            ycOcupacion.ycFila(ycO.ycHabitacion(), ycO.ycNoches(), ycO.ycDias(), ycO.ycPorcentaje(), ycO.ycIngresos());
        }
        YCExcel.ycEscribir(ycDestino, List.of(ycResumen, ycPagos, ycEstadias, ycGastos, ycOcupacion));
    }

    public void ycGenerarCsv(YearMonth ycMes, File ycDestino) throws IOException {
        YCDatos ycD = ycDatos(ycMes);
        try (OutputStream ycOs = new FileOutputStream(ycDestino);
             PrintWriter ycW = new PrintWriter(new OutputStreamWriter(ycOs, StandardCharsets.UTF_8))) {
            ycW.print('﻿');
            ycW.println("REPORTE MENSUAL;" + YCCalendarioPagos.ycTexto(ycMes));
            ycW.println();
            ycW.println("RESUMEN");
            ycW.println("Alquiler mensual (cobrado);" + ycCsv(ycD.ycTotalMensual()));
            ycW.println(ycArrendador.getYcNombreHospedaje() + " (cobrado);" + ycCsv(ycD.ycCobradoHospedaje()));
            ycW.println("TOTAL INGRESOS DEL MES;" + ycCsv(ycD.ycTotal()));
            ycW.println("Gastos del mes;" + ycCsv(ycD.ycResultado().ycGastos()));
            ycW.println("GANANCIA DEL MES;" + ycCsv(ycD.ycResultado().ycGanancia()));
            ycW.println("Margen;" + ycD.ycResultado().ycMargen() + "%");
            ycW.println("Ganancia alquiler mensual;" + ycCsv(ycD.ycResultado().ycGananciaMensual()));
            ycW.println("Ganancia " + ycArrendador.getYcNombreHospedaje() + ";" + ycCsv(ycD.ycResultado().ycGananciaHospedaje()));
            ycW.println("Gastos de los dos negocios;" + ycCsv(ycD.ycResultado().ycGastosGenerales()));
            ycW.println("Saldos de hospedaje por cobrar;" + ycCsv(ycD.ycSaldosPendientes()));
            ycW.println("Ocupacion del hospedaje;" + ycD.ycOcupacionTotal() + "%");
            ycW.println();
            ycW.println("ALQUILER MENSUAL");
            ycW.println("Fecha;Recibo;Inquilino;Cuarto;Mes que paga;Alquiler;Luz;Agua;Total;Metodo");
            for (YCPago ycP : ycD.ycPagos()) {
                ycW.println(String.join(";", ycP.getYcFechaPago().format(YC_FECHA), ycP.ycGetNumeroRecibo(),
                        ycTexto(ycP.getYcInquilino().ycGetNombreCompleto()), ycTexto(ycP.getYcCuartoNumero()),
                        YCCalendarioPagos.ycTexto(ycP.getYcPeriodo()), ycCsv(ycP.getYcAlquiler()), ycCsv(ycP.getYcLuz()),
                        ycCsv(ycP.getYcAgua()), ycCsv(ycP.getYcTotal()), ycP.getYcMetodo().ycGetDescripcion()));
            }
            ycW.println(";;;;Totales;" + ycCsv(ycD.ycAlquileres()) + ";" + ycCsv(ycD.ycLuz()) + ";" + ycCsv(ycD.ycAgua()) + ";"
                    + ycCsv(ycD.ycTotalMensual()) + ";");
            ycW.println();
            ycW.println(ycArrendador.getYcNombreHospedaje().toUpperCase() + " - ESTADIAS");
            ycW.println("N°;Huesped;Documento;Habitacion;Entrada;Salida;Noches;Total;Pagado;Saldo;Metodo");
            for (YCEstadia ycE : ycD.ycEstadias()) {
                ycW.println(String.join(";", ycE.ycGetNumero(), ycTexto(ycE.getYcHuesped()), ycTexto(ycE.getYcDocumento()),
                        ycTexto(ycE.getYcCuartoNumero()), ycE.getYcEntrada().format(YC_FECHA), ycE.getYcSalida().format(YC_FECHA),
                        String.valueOf(ycE.getYcNoches()), ycCsv(ycE.getYcTotal()), ycCsv(ycE.ycGetPagado()), ycCsv(ycE.ycGetSaldo()),
                        ycE.getYcMetodo().ycGetDescripcion()));
            }
            ycW.println();
            ycW.println("GASTOS");
            ycW.println("Fecha;Categoria;Negocio;Descripcion;Pagado con;Monto;Registrado por");
            for (com.alquilafacil.model.YCGasto ycG : ycD.ycGastos()) {
                ycW.println(String.join(";", ycG.getYcFecha().format(YC_FECHA), ycG.getYcCategoria().ycGetTexto(), ycG.getYcNegocio().ycGetTexto(),
                        ycTexto(ycG.getYcDescripcion()), ycG.getYcMetodo() == null ? "" : ycG.getYcMetodo().ycGetDescripcion(),
                        ycCsv(ycG.getYcMonto()), ycTexto(ycG.getYcRegistradoPor())));
            }
            ycW.println(";;;;Total;" + ycCsv(ycD.ycResultado().ycGastos()) + ";");
            ycW.println();
            ycW.println("OCUPACION POR HABITACION");
            ycW.println("Habitacion;Noches ocupadas;Dias del mes;Ocupacion %;Ingresos de esas noches");
            for (YCOcupacion ycO : ycD.ycOcupacion()) {
                ycW.println(String.join(";", ycTexto(ycO.ycHabitacion()), String.valueOf(ycO.ycNoches()), String.valueOf(ycO.ycDias()),
                        ycO.ycPorcentaje() + "%", ycCsv(ycO.ycIngresos())));
            }
        }
    }

    private final Font ycFTitulo = ycFuente(20, Font.BOLD, YC_OSCURO);
    private final Font ycFSub = ycFuente(10, Font.NORMAL, YC_TEXTO_SUAVE);
    private final Font ycFSeccion = ycFuente(13, Font.BOLD, Color.WHITE);
    private final Font ycFNormal = ycFuente(8.5f, Font.NORMAL, YC_TEXTO);
    private final Font ycFNegrita = ycFuente(8.5f, Font.BOLD, YC_TEXTO);
    private final Font ycFCab = ycFuente(7.5f, Font.BOLD, Color.WHITE);
    private final Font ycFEtiqueta = ycFuente(7.5f, Font.BOLD, YC_TEXTO_SUAVE);
    private final Font ycFKpi = ycFuente(15, Font.BOLD, YC_TEXTO);
    private final Font ycFKpiBlanco = ycFuente(15, Font.BOLD, Color.WHITE);
    private final Font ycFEtiquetaBlanca = ycFuente(7.5f, Font.BOLD, Color.WHITE);
    private final Font ycFSuave = ycFuente(8, Font.NORMAL, YC_TEXTO_SUAVE);

    public void ycGenerarPdf(YearMonth ycMes, File ycDestino) throws IOException {
        YCDatos ycD = ycDatos(ycMes);
        try (OutputStream ycSalida = new FileOutputStream(ycDestino)) {
            ycEscribirPdf(ycD, ycSalida);
        }
    }

    private void ycEscribirPdf(YCDatos ycD, OutputStream ycSalida) {
        YearMonth ycMes = ycD.ycMes();
        Document ycDoc = new Document(PageSize.A4, 36, 36, 36, 46);
        try {
            PdfWriter ycWriter = PdfWriter.getInstance(ycDoc, ycSalida);
            ycWriter.setPageEvent(new YCPie());
            ycDoc.addTitle("Reporte " + YCCalendarioPagos.ycTexto(ycMes));
            ycDoc.open();

            ycDoc.add(new Paragraph("Reporte de " + YCCalendarioPagos.ycTexto(ycMes), ycFTitulo));
            ycDoc.add(new Paragraph(ycArrendador.getYcNombre() + "  ·  Alquiler mensual y " + ycArrendador.getYcNombreHospedaje()
                    + "  ·  Generado el " + LocalDate.now().format(YC_FECHA), ycFSub));
            ycDoc.add(ycEspacio(12));

            PdfPTable ycResumen = new PdfPTable(4);
            ycResumen.setWidthPercentage(100);
            ycResumen.addCell(ycKpi("INGRESOS", "S/ " + YC_IMPORTE.format(ycD.ycTotal()), YC_MENSUAL_SUAVE, false));
            ycResumen.addCell(ycKpi("GASTOS", "S/ " + YC_IMPORTE.format(ycD.ycResultado().ycGastos()), new Color(254, 243, 226), false));
            ycResumen.addCell(ycKpi("GANANCIA", "S/ " + YC_IMPORTE.format(ycD.ycResultado().ycGanancia()), YC_OSCURO, true));
            ycResumen.addCell(ycKpi("MARGEN", ycD.ycResultado().ycMargen() + " %", new Color(246, 248, 250), false));
            ycDoc.add(ycResumen);
            if (ycD.ycTotal().signum() > 0) {
                int ycPm = ycD.ycTotalMensual().multiply(BigDecimal.valueOf(100)).divide(ycD.ycTotal(), 0, RoundingMode.HALF_UP).intValue();
                ycDoc.add(ycEspacio(6));
                ycDoc.add(new Paragraph("El alquiler mensual aporto el " + ycPm + " % de los ingresos y "
                        + ycArrendador.getYcNombreHospedaje() + " el " + (100 - ycPm) + " %."
                        + (ycD.ycSaldosPendientes().signum() > 0
                        ? "  Saldos de hospedaje por cobrar: S/ " + YC_IMPORTE.format(ycD.ycSaldosPendientes()) + "." : ""), ycFSuave));
                ycDoc.add(new Paragraph("Ganancia del alquiler mensual: S/ " + YC_IMPORTE.format(ycD.ycResultado().ycGananciaMensual())
                        + "  ·  Ganancia de " + ycArrendador.getYcNombreHospedaje() + ": S/ "
                        + YC_IMPORTE.format(ycD.ycResultado().ycGananciaHospedaje())
                        + (ycD.ycResultado().ycGastosGenerales().signum() > 0 ? "  ·  Gastos de los dos negocios: S/ "
                        + YC_IMPORTE.format(ycD.ycResultado().ycGastosGenerales()) : ""), ycFSuave));
            }
            ycDoc.add(ycEspacio(14));

            ycDoc.add(ycTituloSeccion("Alquiler mensual  ·  " + ycD.ycPagos().size() + (ycD.ycPagos().size() == 1 ? " pago" : " pagos"), YC_MENSUAL));
            PdfPTable ycPagos = new PdfPTable(new float[]{10, 10, 21, 12, 13, 11, 7, 7, 9});
            ycPagos.setWidthPercentage(100);
            for (String ycC : new String[]{"FECHA", "RECIBO", "INQUILINO", "CUARTO", "MES", "ALQUILER", "LUZ", "AGUA", "TOTAL"}) {
                ycPagos.addCell(ycCabecera(ycC, YC_MENSUAL));
            }
            int ycI = 0;
            for (YCPago ycP : ycD.ycPagos()) {
                Color ycFondo = ycI++ % 2 == 0 ? Color.WHITE : YC_MENSUAL_SUAVE;
                ycPagos.addCell(ycCelda(ycP.getYcFechaPago().format(YC_FECHA), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycPagos.addCell(ycCelda(ycP.ycGetNumeroRecibo(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycPagos.addCell(ycCelda(ycP.getYcInquilino().ycGetNombreCompleto(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycPagos.addCell(ycCelda(ycP.getYcCuartoNumero(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycPagos.addCell(ycCelda(YCCalendarioPagos.ycTexto(ycP.getYcPeriodo()), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycP.getYcAlquiler()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
                ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycP.getYcLuz()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
                ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycP.getYcAgua()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
                ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycP.getYcTotal()), ycFondo, Element.ALIGN_RIGHT, ycFNegrita));
            }
            if (ycD.ycPagos().isEmpty()) {
                PdfPCell ycVacio = ycCelda("No hubo pagos este mes.", Color.WHITE, Element.ALIGN_LEFT, ycFSuave);
                ycVacio.setColspan(9);
                ycPagos.addCell(ycVacio);
            }
            PdfPCell ycEt = ycCelda("TOTALES", YC_MENSUAL_SUAVE, Element.ALIGN_RIGHT, ycFNegrita);
            ycEt.setColspan(5);
            ycPagos.addCell(ycEt);
            ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycD.ycAlquileres()), YC_MENSUAL_SUAVE, Element.ALIGN_RIGHT, ycFNegrita));
            ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycD.ycLuz()), YC_MENSUAL_SUAVE, Element.ALIGN_RIGHT, ycFNegrita));
            ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycD.ycAgua()), YC_MENSUAL_SUAVE, Element.ALIGN_RIGHT, ycFNegrita));
            ycPagos.addCell(ycCelda(YC_IMPORTE.format(ycD.ycTotalMensual()), YC_MENSUAL_SUAVE, Element.ALIGN_RIGHT, ycFNegrita));
            ycDoc.add(ycPagos);
            ycDoc.add(ycEspacio(16));

            ycDoc.add(ycTituloSeccion(ycArrendador.getYcNombreHospedaje() + "  ·  " + ycD.ycEstadias().size()
                    + (ycD.ycEstadias().size() == 1 ? " estadia" : " estadias"), YC_HOSP));
            PdfPTable ycEst = new PdfPTable(new float[]{9, 19, 14, 11, 11, 7, 9, 10, 10});
            ycEst.setWidthPercentage(100);
            for (String ycC : new String[]{"N°", "HUESPED", "HABITACION", "ENTRADA", "SALIDA", "NOCH.", "TOTAL", "PAGADO", "SALDO"}) {
                ycEst.addCell(ycCabecera(ycC, YC_HOSP));
            }
            ycI = 0;
            for (YCEstadia ycE : ycD.ycEstadias()) {
                Color ycFondo = ycI++ % 2 == 0 ? Color.WHITE : YC_HOSP_SUAVE;
                ycEst.addCell(ycCelda(ycE.ycGetNumero(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycEst.addCell(ycCelda(ycE.getYcHuesped(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycEst.addCell(ycCelda(ycE.getYcCuartoNumero(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycEst.addCell(ycCelda(ycE.getYcEntrada().format(YC_FECHA), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycEst.addCell(ycCelda(ycE.getYcSalida().format(YC_FECHA), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycEst.addCell(ycCelda(String.valueOf(ycE.getYcNoches()), ycFondo, Element.ALIGN_CENTER, ycFNormal));
                ycEst.addCell(ycCelda(YC_IMPORTE.format(ycE.getYcTotal()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
                ycEst.addCell(ycCelda(YC_IMPORTE.format(ycE.ycGetPagado()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
                ycEst.addCell(ycCelda(ycE.ycTieneSaldo() ? YC_IMPORTE.format(ycE.ycGetSaldo()) : "—", ycFondo, Element.ALIGN_RIGHT,
                        ycE.ycTieneSaldo() ? ycFNegrita : ycFNormal));
            }
            if (ycD.ycEstadias().isEmpty()) {
                PdfPCell ycVacio = ycCelda("No hubo estadias este mes.", Color.WHITE, Element.ALIGN_LEFT, ycFSuave);
                ycVacio.setColspan(9);
                ycEst.addCell(ycVacio);
            }
            ycDoc.add(ycEst);
            ycDoc.add(ycEspacio(12));

            PdfPTable ycOcup = new PdfPTable(new float[]{34, 22, 22, 22});
            ycOcup.setWidthPercentage(70);
            ycOcup.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String ycC : new String[]{"HABITACION", "NOCHES OCUPADAS", "OCUPACION", "INGRESOS"}) {
                ycOcup.addCell(ycCabecera(ycC, YC_HOSP));
            }
            ycI = 0;
            for (YCOcupacion ycO : ycD.ycOcupacion()) {
                Color ycFondo = ycI++ % 2 == 0 ? Color.WHITE : YC_HOSP_SUAVE;
                ycOcup.addCell(ycCelda(ycO.ycHabitacion(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycOcup.addCell(ycCelda(ycO.ycNoches() + " de " + ycO.ycDias(), ycFondo, Element.ALIGN_CENTER, ycFNormal));
                ycOcup.addCell(ycCelda(ycO.ycPorcentaje() + " %", ycFondo, Element.ALIGN_CENTER, ycFNegrita));
                ycOcup.addCell(ycCelda("S/ " + YC_IMPORTE.format(ycO.ycIngresos()), ycFondo, Element.ALIGN_RIGHT, ycFNormal));
            }
            ycDoc.add(ycOcup);
            ycDoc.add(ycEspacio(16));

            Color ycGastoColor = new Color(180, 83, 9);
            Color ycGastoSuave = new Color(254, 243, 226);
            ycDoc.add(ycTituloSeccion("Gastos  ·  S/ " + YC_IMPORTE.format(ycD.ycResultado().ycGastos()), ycGastoColor));
            PdfPTable ycGst = new PdfPTable(new float[]{12, 22, 16, 34, 16});
            ycGst.setWidthPercentage(100);
            for (String ycC : new String[]{"FECHA", "CATEGORIA", "NEGOCIO", "DESCRIPCION", "MONTO"}) {
                ycGst.addCell(ycCabecera(ycC, ycGastoColor));
            }
            ycI = 0;
            for (com.alquilafacil.model.YCGasto ycG : ycD.ycGastos()) {
                Color ycFondo = ycI++ % 2 == 0 ? Color.WHITE : ycGastoSuave;
                ycGst.addCell(ycCelda(ycG.getYcFecha().format(YC_FECHA), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycGst.addCell(ycCelda(ycG.getYcCategoria().ycGetTexto(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycGst.addCell(ycCelda(ycG.getYcNegocio().ycGetTexto(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycGst.addCell(ycCelda(ycG.getYcDescripcion() == null ? "" : ycG.getYcDescripcion(), ycFondo, Element.ALIGN_LEFT, ycFNormal));
                ycGst.addCell(ycCelda(YC_IMPORTE.format(ycG.getYcMonto()), ycFondo, Element.ALIGN_RIGHT, ycFNegrita));
            }
            if (ycD.ycGastos().isEmpty()) {
                PdfPCell ycVacio = ycCelda("No se registraron gastos este mes.", Color.WHITE, Element.ALIGN_LEFT, ycFSuave);
                ycVacio.setColspan(5);
                ycGst.addCell(ycVacio);
            }
            ycDoc.add(ycGst);
        } catch (DocumentException ycE) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ycE.getMessage(), ycE);
        } finally {
            if (ycDoc.isOpen()) {
                ycDoc.close();
            }
        }
    }

    private static int ycNochesEnMes(YCEstadia ycE, LocalDate ycInicio, LocalDate ycFin) {
        LocalDate ycDesde = ycE.getYcEntrada().isBefore(ycInicio) ? ycInicio : ycE.getYcEntrada();
        LocalDate ycHasta = ycE.getYcSalida().isAfter(ycFin) ? ycFin : ycE.getYcSalida();
        return (int) Math.max(0, ChronoUnit.DAYS.between(ycDesde, ycHasta));
    }

    private static BigDecimal ycSuma(List<BigDecimal> ycValores) {
        return ycValores.stream().filter(ycV -> ycV != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String ycCsv(BigDecimal ycV) {
        return YC_CSV_IMPORTE.format(ycV == null ? BigDecimal.ZERO : ycV);
    }

    private static String ycTexto(String ycT) {
        return ycT == null ? "" : ycT.replace(";", ",").replace("\n", " ");
    }

    private PdfPTable ycTituloSeccion(String ycTexto, Color ycColor) {
        PdfPTable ycT = new PdfPTable(1);
        ycT.setWidthPercentage(100);
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFSeccion));
        ycC.setBackgroundColor(ycColor);
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setPadding(8);
        ycC.setPaddingLeft(10);
        ycT.addCell(ycC);
        ycT.setSpacingAfter(4);
        return ycT;
    }

    private PdfPCell ycKpi(String ycEtiqueta, String ycValor, Color ycFondo, boolean ycOscuro) {
        PdfPCell ycC = new PdfPCell();
        ycC.setBackgroundColor(ycFondo);
        ycC.setBorderColor(Color.WHITE);
        ycC.setBorderWidth(3);
        ycC.setPadding(10);
        ycC.addElement(new Paragraph(ycEtiqueta, ycOscuro ? ycFEtiquetaBlanca : ycFEtiqueta));
        ycC.addElement(new Paragraph(ycValor, ycOscuro ? ycFKpiBlanco : ycFKpi));
        return ycC;
    }

    private PdfPCell ycCabecera(String ycTexto, Color ycColor) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto, ycFCab));
        ycC.setBackgroundColor(ycColor);
        ycC.setBorder(Rectangle.NO_BORDER);
        ycC.setPadding(5);
        return ycC;
    }

    private static PdfPCell ycCelda(String ycTexto, Color ycFondo, int ycAlineacion, Font ycFuente) {
        PdfPCell ycC = new PdfPCell(new Phrase(ycTexto == null ? "" : ycTexto, ycFuente));
        ycC.setBackgroundColor(ycFondo);
        ycC.setBorder(Rectangle.BOTTOM);
        ycC.setBorderColor(YC_BORDE);
        ycC.setHorizontalAlignment(ycAlineacion);
        ycC.setPadding(5);
        return ycC;
    }

    private static Paragraph ycEspacio(float ycAlto) {
        Paragraph ycP = new Paragraph(" ");
        ycP.setLeading(ycAlto);
        return ycP;
    }

    private static Font ycFuente(float ycTamanio, int ycEstilo, Color ycColor) {
        return new Font(Font.HELVETICA, ycTamanio, ycEstilo, ycColor);
    }

    private class YCPie extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter ycWriter, Document ycDocument) {
            ColumnText.showTextAligned(ycWriter.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(ycArrendador.getYcNombreSistema() + "  ·  Página " + ycWriter.getPageNumber(), ycFSuave),
                    (ycDocument.left() + ycDocument.right()) / 2, ycDocument.bottom() - 22, 0);
        }
    }
}
