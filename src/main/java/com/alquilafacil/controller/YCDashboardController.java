package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.service.YCCalendarioPagos;
import com.alquilafacil.service.YCDashboardService;
import com.alquilafacil.service.YCDeudaPendiente;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCDashboardController implements YCRefrescable {
    private static final DateTimeFormatter YC_HOY =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));

    private final YCDashboardService ycDashboardService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCMainController ycMainController;
    private final YCPagoController ycPagoController;
    private final YCExportador ycExportador;
    private final YCNotificador ycNotificador;
    private final YCDatosArrendador ycArrendador;
    private final com.alquilafacil.service.YCReporteMensualService ycReporteService;
    private final com.alquilafacil.service.YCReporteAnualService ycReporteAnualService;
    private final com.alquilafacil.service.YCInquilinoService ycInquilinoService;
    private final YCInquilinoController ycInquilinoController;
    private final YCCobroYape ycCobroYape;

    @FXML private Label ycLblFecha;
    @FXML private Button ycBtnReporte;
    @FXML private Button ycBtnReporteAnual;
    @FXML private javafx.scene.layout.VBox ycPanelContratos;
    @FXML private Label ycLblContratos;
    @FXML private javafx.scene.layout.VBox ycListaContratos;
    @FXML private Label ycKpiCobrado;
    @FXML private Label ycKpiCobradoDetalle;
    @FXML private Label ycKpiDeuda;
    @FXML private Label ycKpiDeudaDetalle;
    @FXML private Label ycKpiAtrasados;
    @FXML private Label ycKpiAtrasadosDetalle;
    @FXML private Label ycKpiOcupacion;
    @FXML private Label ycKpiOcupacionDetalle;

    @FXML private TableView<YCDeudaPendiente> ycTablaPendientes;
    @FXML private TableColumn<YCDeudaPendiente, String> ycColInquilino;
    @FXML private TableColumn<YCDeudaPendiente, String> ycColCuarto;
    @FXML private TableColumn<YCDeudaPendiente, String> ycColMes;
    @FXML private TableColumn<YCDeudaPendiente, String> ycColVence;
    @FXML private TableColumn<YCDeudaPendiente, String> ycColEstado;
    @FXML private TableColumn<YCDeudaPendiente, BigDecimal> ycColMonto;
    @FXML private Button ycBtnCopiar;
    @FXML private Button ycBtnWhatsapp;
    @FXML private Button ycBtnCobrar;

    @FXML private BarChart<String, Number> ycGraficoIngresos;

    private final ObservableList<YCDeudaPendiente> ycPendientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ycColInquilino.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycInquilino().ycGetNombreCompleto()));
        ycColInquilino.setCellFactory(YCTablas.ycCeldaAvatar());
        ycColCuarto.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycInquilino().getYcCuarto().getYcNumero()));
        ycColMes.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycPeriodoTexto()
                + (ycC.getValue().ycPorDias() ? " (" + ycC.getValue().ycTextoDias() + ")" : "")));
        ycColVence.setCellValueFactory(ycC -> new SimpleStringProperty(YCControles.ycFecha(ycC.getValue().ycVence())));
        ycColEstado.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycEstado()));
        ycColEstado.setCellFactory(YCTablas.ycCeldaChip(ycV -> ycV.contains("atraso") ? "peligro"
                : ycV.startsWith("Vence") ? "advertencia" : "neutro"));
        ycColMonto.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().ycTotal()));
        ycColMonto.setCellFactory(YCTablas.ycCeldaMoneda());

        YCTablas.ycConfigurar(ycTablaPendientes, Feather.CHECK_CIRCLE, "Todo al dia",
                "Ningun inquilino tiene pagos pendientes este mes.", this::ycCobrar);
        ycTablaPendientes.setItems(ycPendientes);

        var ycSeleccion = ycTablaPendientes.getSelectionModel().selectedItemProperty();
        ycBtnCopiar.disableProperty().bind(ycSeleccion.isNull());
        ycBtnWhatsapp.disableProperty().bind(ycSeleccion.isNull());
        ycBtnCobrar.disableProperty().bind(ycSeleccion.isNull());
            ycSesion.ycSoloAdmin(ycBtnReporte, ycBtnReporteAnual);
    }

    @Override
    public void ycRefrescar() {
        YCDashboardService.YCResumen ycR = ycDashboardService.ycResumen();
        String ycHoy = LocalDate.now().format(YC_HOY);
        ycLblFecha.setText(Character.toUpperCase(ycHoy.charAt(0)) + ycHoy.substring(1));

        ycKpiCobrado.setText(ycSesion.ycAdminProperty().get() ? YCControles.ycMoneda(ycR.ycCobradoMes()) : "S/ •••••");
        ycKpiCobradoDetalle.setText(YCCalendarioPagos.ycTexto(YearMonth.now()));
        ycKpiDeuda.setText(YCControles.ycMoneda(ycR.ycDeudaVencida()));
        long ycMesesVencidos = ycR.ycPendientes().stream().filter(YCDeudaPendiente::ycAtrasado).count();
        ycKpiDeudaDetalle.setText(ycMesesVencidos == 1 ? "1 mes vencido sin pagar" : ycMesesVencidos + " meses vencidos sin pagar");
        ycKpiAtrasados.setText(String.valueOf(ycR.ycInquilinosAtrasados()));
        ycKpiAtrasadosDetalle.setText("de " + ycR.ycOcupados() + " inquilinos activos");
        ycKpiOcupacion.setText(ycR.ycOcupacion() + " %");
        ycKpiOcupacionDetalle.setText(ycR.ycOcupados() + " ocupados · " + ycR.ycLibres() + " libres");

        ycPendientes.setAll(ycR.ycPendientes());
        ycConstruirContratos();

        XYChart.Series<String, Number> ycSerie = new XYChart.Series<>();
        for (Map.Entry<YearMonth, BigDecimal> ycE : ycR.ycIngresosPorMes().entrySet()) {
            ycSerie.getData().add(new XYChart.Data<>(YCCalendarioPagos.ycCorto(ycE.getKey()), ycE.getValue()));
        }
        ycGraficoIngresos.getData().setAll(ycSerie);
        ycGraficoIngresos.setVisible(ycSesion.ycAdminProperty().get());
        if (!ycSesion.ycAdminProperty().get()) {
            ycKpiCobradoDetalle.setText("Solo lo ve el administrador");
        }

        Animations.fadeIn(ycTablaPendientes, Duration.millis(260)).play();
    }

    @FXML
    private void ycOnReporte() {
        javafx.scene.control.ComboBox<java.time.YearMonth> ycMes = new javafx.scene.control.ComboBox<>();
        java.time.YearMonth ycActual = java.time.YearMonth.now();
        for (int ycI = 0; ycI < 12; ycI++) {
            ycMes.getItems().add(ycActual.minusMonths(ycI));
        }
        ycMes.setValue(ycActual);
        ycMes.setMaxWidth(Double.MAX_VALUE);
        ycMes.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(java.time.YearMonth ycM) {
                return ycM == null ? "" : com.alquilafacil.service.YCCalendarioPagos.ycTexto(ycM);
            }

            @Override
            public java.time.YearMonth fromString(String ycS) {
                return null;
            }
        });
        org.kordamp.ikonli.javafx.FontIcon ycIcono = new org.kordamp.ikonli.javafx.FontIcon("fth-file-text");
        javafx.scene.layout.StackPane ycCirculo = new javafx.scene.layout.StackPane(ycIcono);
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Reporte mensual");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycTexto = new Label("Ingresos del alquiler mensual y de " + ycArrendador.getYcNombreHospedaje()
                + " por separado, con la ocupacion de cada habitacion. En PDF para imprimir o en Excel para sacar cuentas.");
        ycTexto.getStyleClass().add("dialogo-mensaje");
        ycTexto.setWrapText(true);
        Button ycExcel = new Button("Excel", new org.kordamp.ikonli.javafx.FontIcon("fth-grid"));
        ycExcel.getStyleClass().add("button-outlined");
        ycExcel.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            java.time.YearMonth ycM = ycMes.getValue();
            ycExportador.ycGuardar(ycLblFecha.getScene().getWindow(), "Guardar reporte en Excel",
                    "Reporte " + com.alquilafacil.service.YCCalendarioPagos.ycTexto(ycM), YCExportador.YCFormato.YC_EXCEL,
                    ycF -> ycReporteService.ycGenerarExcel(ycM, ycF));
        });
        Button ycPdf = new Button("PDF", new org.kordamp.ikonli.javafx.FontIcon("fth-file-text"));
        ycPdf.getStyleClass().add("accent");
        ycPdf.setDefaultButton(true);
        ycPdf.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            java.time.YearMonth ycM = ycMes.getValue();
            ycExportador.ycAbrirTemporal("Reporte " + com.alquilafacil.service.YCCalendarioPagos.ycTexto(ycM),
                    YCExportador.YCFormato.YC_PDF, ycF -> ycReporteService.ycGenerarPdf(ycM, ycF));
        });
        Button ycCancelar = new Button("Cancelar");
        ycCancelar.getStyleClass().add("flat");
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        javafx.scene.layout.Region ycEsp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(ycEsp, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox ycAcciones = new javafx.scene.layout.HBox(10, ycEsp, ycCancelar, ycExcel, ycPdf);
        ycAcciones.getStyleClass().add("dialogo-acciones");
        javafx.scene.layout.VBox ycDialogo = new javafx.scene.layout.VBox(14, ycCirculo, ycTitulo, ycTexto, ycMes, ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(460, javafx.scene.layout.Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    private void ycConstruirContratos() {
        List<com.alquilafacil.model.YCInquilino> ycLista =
                ycInquilinoService.ycContratosPorVencer(YCInquilinoController.YC_AVISO_CONTRATO_DIAS);
        ycListaContratos.getChildren().clear();
        ycPanelContratos.setVisible(!ycLista.isEmpty());
        ycPanelContratos.setManaged(!ycLista.isEmpty());
        long ycVencidos = ycLista.stream().filter(ycI -> ycI.ycDiasParaFinContrato(LocalDate.now()) < 0).count();
        ycLblContratos.setText((ycLista.size() == 1 ? "1 contrato por vencer" : ycLista.size() + " contratos por vencer")
                + (ycVencidos > 0 ? " · " + ycVencidos + (ycVencidos == 1 ? " ya vencio" : " ya vencieron") : ""));
        for (com.alquilafacil.model.YCInquilino ycI : ycLista) {
            javafx.scene.layout.StackPane ycAvatar = new javafx.scene.layout.StackPane(new Label(YCTablas.ycIniciales(ycI.ycGetNombreCompleto())));
            ycAvatar.getStyleClass().add("avatar");
            Label ycNombre = new Label(ycI.ycGetNombreCompleto());
            ycNombre.getStyleClass().add("ficha-dato-valor");
            Label ycDetalle = new Label(ycI.getYcCuarto().getYcNumero() + " · fin del contrato " + YCControles.ycFecha(ycI.getYcFechaFinContrato()));
            ycDetalle.getStyleClass().add("ficha-lateral-nota");
            javafx.scene.layout.VBox ycTextos = new javafx.scene.layout.VBox(1, ycNombre, ycDetalle);
            javafx.scene.layout.HBox.setHgrow(ycTextos, javafx.scene.layout.Priority.ALWAYS);
            String ycEstado = YCInquilinoController.ycTextoContrato(ycI);
            Label ycChip = new Label(ycEstado);
            ycChip.getStyleClass().addAll("chip", ycEstado.startsWith("Vencio") ? "peligro" : "advertencia");
            ycChip.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            Button ycContrato = new Button("Contrato", new org.kordamp.ikonli.javafx.FontIcon("fth-file-text"));
            ycContrato.getStyleClass().add("flat");
            ycContrato.setOnAction(ycE -> ycInquilinoController.ycVerContrato(ycI));
            Button ycRenovar = new Button("Renovar 1 año", new org.kordamp.ikonli.javafx.FontIcon("fth-refresh-cw"));
            ycRenovar.getStyleClass().addAll("accent", "button-outlined");
            ycRenovar.setOnAction(ycE -> ycRenovar(ycI));
            javafx.scene.layout.HBox ycFila = new javafx.scene.layout.HBox(12, ycAvatar, ycTextos, ycChip, ycContrato, ycRenovar);
            ycFila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            ycListaContratos.getChildren().add(ycFila);
        }
    }

    private void ycRenovar(com.alquilafacil.model.YCInquilino ycI) {
        try {
            com.alquilafacil.model.YCInquilino ycR = ycInquilinoService.ycRenovarContrato(ycI.getYcId(), 12);
            ycRefrescar();
            ycNotificador.ycExitoConAcciones("Contrato renovado",
                    "El contrato de " + ycR.ycGetNombreCompleto() + " ahora va hasta el "
                            + YCControles.ycFecha(ycR.getYcFechaFinContrato()) + ". ¿Imprimes el nuevo contrato para firmarlo?",
                    "Despues", null, "Ver contrato", () -> ycInquilinoController.ycVerContrato(ycR));
        } catch (com.alquilafacil.exception.YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        }
    }

    @FXML
    private void ycOnReporteAnual() {
        javafx.scene.control.ComboBox<Integer> ycAnio = new javafx.scene.control.ComboBox<>();
        ycAnio.getItems().setAll(com.alquilafacil.service.YCReporteAnualService.ycAnios(LocalDate.now().getYear() - 4));
        ycAnio.setValue(LocalDate.now().getYear());
        ycAnio.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.StackPane ycCirculo = new javafx.scene.layout.StackPane(
                new org.kordamp.ikonli.javafx.FontIcon("fth-calendar"));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Reporte anual para el contador");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycTexto = new Label("Mes a mes: lo cobrado por alquiler mensual y por hospedaje, los gastos y la ganancia. "
                + "Incluye un estimado referencial del impuesto del alquiler; el monto real lo confirma tu contador.");
        ycTexto.getStyleClass().add("dialogo-mensaje");
        ycTexto.setWrapText(true);
        Button ycExcel = new Button("Excel", new org.kordamp.ikonli.javafx.FontIcon("fth-grid"));
        ycExcel.getStyleClass().add("button-outlined");
        ycExcel.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            int ycA = ycAnio.getValue();
            ycExportador.ycGuardar(ycLblFecha.getScene().getWindow(), "Guardar reporte anual en Excel",
                    "Reporte anual " + ycA, YCExportador.YCFormato.YC_EXCEL, ycF -> ycReporteAnualService.ycGenerarExcel(ycA, ycF));
        });
        Button ycPdf = new Button("PDF", new org.kordamp.ikonli.javafx.FontIcon("fth-file-text"));
        ycPdf.getStyleClass().add("accent");
        ycPdf.setDefaultButton(true);
        ycPdf.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            int ycA = ycAnio.getValue();
            ycExportador.ycAbrirTemporal("Reporte anual " + ycA, YCExportador.YCFormato.YC_PDF,
                    ycF -> ycReporteAnualService.ycGenerarPdf(ycA, ycF));
        });
        Button ycCancelar = new Button("Cancelar");
        ycCancelar.getStyleClass().add("flat");
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        javafx.scene.layout.Region ycEsp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(ycEsp, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox ycAcciones = new javafx.scene.layout.HBox(10, ycEsp, ycCancelar, ycExcel, ycPdf);
        ycAcciones.getStyleClass().add("dialogo-acciones");
        javafx.scene.layout.VBox ycDialogo = new javafx.scene.layout.VBox(14, ycCirculo, ycTitulo, ycTexto, ycAnio, ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(460, javafx.scene.layout.Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    @FXML
    private void ycOnActualizar() {
        ycRefrescar();
        ycNotificador.ycInfo("Datos actualizados.");
    }

    @FXML
    private void ycOnRegistrarPago() {
        ycMainController.ycIrA("pago-view");
        ycPagoController.ycRegistrarPara(null, null);
    }

    @FXML
    private void ycOnCobrar() {
        ycCobrar(ycTablaPendientes.getSelectionModel().getSelectedItem());
    }

    private void ycCobrar(YCDeudaPendiente ycD) {
        if (ycD == null) {
            return;
        }
        ycMainController.ycIrA("pago-view");
        ycPagoController.ycRegistrarPara(ycD.ycInquilino().getYcId(), ycD.ycPeriodo());
    }

    @FXML
    private void ycOnCopiarRecordatorio() {
        YCDeudaPendiente ycD = ycTablaPendientes.getSelectionModel().getSelectedItem();
        if (ycD == null) {
            return;
        }
        ClipboardContent ycContenido = new ClipboardContent();
        ycContenido.putString(ycMensaje(ycD));
        Clipboard.getSystemClipboard().setContent(ycContenido);
        ycNotificador.ycExito("Mensaje copiado. Pegalo en WhatsApp o SMS con Ctrl+V.");
    }

    @FXML
    private void ycOnWhatsapp() {
        YCDeudaPendiente ycD = ycTablaPendientes.getSelectionModel().getSelectedItem();
        if (ycD == null) {
            return;
        }
        YCInquilino ycI = ycD.ycInquilino();
        String ycCelular = ycI.getYcTelefono() == null ? "" : ycI.getYcTelefono().replaceAll("\\D", "");
        if (ycCelular.length() != 9) {
            ycNotificador.ycAdvertencia(ycI.ycGetNombreCompleto() + " no tiene un celular registrado. Usa \"Copiar mensaje\".");
            return;
        }
        String ycTexto = URLEncoder.encode(ycMensaje(ycD), StandardCharsets.UTF_8).replace("+", "%20");
        boolean ycConQr = ycCobroYape.ycCopiarQr();
        ycExportador.ycAbrirEnlace("https://wa.me/51" + ycCelular + "?text=" + ycTexto);
        if (ycConQr) {
            ycNotificador.ycExito("Se abrio WhatsApp. Tu QR de Yape quedo copiado: en el chat presiona Ctrl+V para adjuntarlo.");
        }
    }

    private String ycMensaje(YCDeudaPendiente ycD) {
        StringBuilder ycDetalle = new StringBuilder("alquiler ").append(YCControles.ycMoneda(ycD.ycAlquiler()));
        if (ycD.ycLuz().signum() > 0) {
            ycDetalle.append(", luz ").append(YCControles.ycMoneda(ycD.ycLuz()));
        }
        if (ycD.ycAgua().signum() > 0) {
            ycDetalle.append(", agua ").append(YCControles.ycMoneda(ycD.ycAgua()));
        }
        String ycCuando = ycD.ycAtrasado()
                ? "que vencio el " + YCControles.ycFecha(ycD.ycVence())
                : "que vence el " + YCControles.ycFecha(ycD.ycVence());
        return "Hola " + ycD.ycInquilino().getYcNombre() + ", le saluda " + ycArrendador.getYcNombre()
                + ". Le recuerdo el pago de " + ycD.ycPeriodoTexto() + " del " + ycD.ycInquilino().getYcCuarto().getYcNumero()
                + " por " + YCControles.ycMoneda(ycD.ycTotal()) + " (" + ycDetalle + "), " + ycCuando + ". "
                + ycCobroYape.ycFrase() + "Muchas gracias.";
    }
}
