package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCPago;
import com.alquilafacil.service.YCCalendarioPagos;
import com.alquilafacil.service.YCDeudaPendiente;
import com.alquilafacil.service.YCInquilinoService;
import com.alquilafacil.service.YCNumeroALetras;
import com.alquilafacil.service.YCPagoService;
import com.alquilafacil.service.YCReciboPdfService;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCPaginador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YCPagoController implements YCRefrescable {
    private static final DateTimeFormatter YC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final YCPagoService ycService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCInquilinoService ycInquilinoService;
    private final YCReciboPdfService ycReciboPdf;
    private final YCExportador ycExportador;
    private final YCNotificador ycNotificador;
    private final YCDatosArrendador ycArrendador;
    private final YCCobroYape ycCobroYape;

    @FXML private VBox ycPanelFormulario;
    @FXML private ComboBox<YCInquilino> ycCmbInquilino;
    @FXML private ComboBox<YCDeudaPendiente> ycCmbPeriodo;
    @FXML private Label ycLblAyudaPeriodo;
    @FXML private TextField ycTxtAlquiler;
    @FXML private TextField ycTxtLuz;
    @FXML private TextField ycTxtAgua;
    @FXML private ComboBox<YCMetodoPago> ycCmbMetodo;
    @FXML private TextField ycTxtNota;
    @FXML private Label ycLblTotal;
    @FXML private Label ycLblTotalLetras;
    @FXML private Button ycBtnRegistrar;

    @FXML private TextField ycTxtBuscar;
    @FXML private TableView<YCPago> ycTabla;
    @FXML private TableColumn<YCPago, String> ycColRecibo;
    @FXML private TableColumn<YCPago, String> ycColFecha;
    @FXML private TableColumn<YCPago, String> ycColInquilino;
    @FXML private TableColumn<YCPago, String> ycColCuarto;
    @FXML private TableColumn<YCPago, String> ycColMes;
    @FXML private TableColumn<YCPago, BigDecimal> ycColTotal;
    @FXML private TableColumn<YCPago, String> ycColMetodo;
    @FXML private TableColumn<YCPago, String> ycColEstado;
    @FXML private TableColumn<YCPago, String> ycColUsuario;
    @FXML private Button ycBtnVer;
    @FXML private Button ycBtnGuardarPdf;
    @FXML private Button ycBtnAnular;
    @FXML private YCPaginador ycPaginador;

    @FXML private VBox ycPanelDeudas;
    @FXML private StackPane ycIconoDeudas;
    @FXML private Label ycLblDeudas;
    @FXML private Label ycLblDeudasAyuda;
    @FXML private VBox ycListaDeudas;
    @FXML private ScrollPane ycScrollDeudas;

    private final ObservableList<YCPago> ycDatos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ycConfigurarTabla();
        ycConfigurarFormulario();
            ycSesion.ycSoloAdmin(ycBtnAnular);
    }

    private void ycConfigurarTabla() {
        ycColRecibo.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycGetNumeroRecibo()));
        ycColFecha.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcFechaPago().format(YC_FECHA_HORA)));
        ycColInquilino.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcInquilino().ycGetNombreCompleto()));
        ycColInquilino.setCellFactory(YCTablas.ycCeldaAvatar());
        ycColCuarto.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcCuartoNumero()));
        ycColMes.setCellValueFactory(ycC -> new SimpleStringProperty(YCCalendarioPagos.ycTexto(ycC.getValue().getYcPeriodo())));
        ycColTotal.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().getYcTotal()));
        ycColTotal.setCellFactory(YCTablas.ycCeldaMoneda());
        ycColMetodo.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcMetodo().ycGetDescripcion()));
        ycColMetodo.setCellFactory(YCTablas.ycCeldaChip("—"));
        ycColEstado.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().isYcAnulado() ? "Anulado" : "Pagado"));
        ycColEstado.setCellFactory(ycCol -> new TableCell<>() {
            private final Label ycChip = new Label();

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                YCPago ycP = ycVacio ? null : getTableRow().getItem();
                if (ycVacio || ycValor == null || ycP == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                ycChip.setText(ycValor);
                ycChip.getStyleClass().setAll("chip", ycP.isYcAnulado() ? "peligro" : "exito");
                setGraphic(ycChip);
                setTooltip(ycP.isYcAnulado() && ycP.getYcMotivoAnulacion() != null
                        ? new Tooltip("Motivo: " + ycP.getYcMotivoAnulacion()
                        + (ycP.getYcAnuladoPor() == null ? "" : "\nAnulado por " + ycP.getYcAnuladoPor())
                        + (ycP.getYcFechaAnulacion() == null ? "" : " el " + ycP.getYcFechaAnulacion().format(YC_FECHA_HORA)))
                        : null);
            }
        });
        ycColUsuario.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcRegistradoPor()));
        ycColUsuario.setCellFactory(YCTablas.ycCeldaOpcional());

        YCTablas.ycConfigurar(ycTabla, Feather.FILE_TEXT, "Sin pagos registrados",
                "Registra el primer cobro con el boton \"Registrar pago\".", this::ycVerRecibo);
        ycTabla.setItems(ycDatos);

        var ycSeleccion = ycTabla.getSelectionModel().selectedItemProperty();
        ycBtnVer.disableProperty().bind(ycSeleccion.isNull());
        ycBtnGuardarPdf.disableProperty().bind(ycSeleccion.isNull());
        ycBtnAnular.disableProperty().bind(ycSeleccion.isNull());

        ycPaginador.ycSetOnCambio(this::ycCargarTabla);
        ycTxtBuscar.textProperty().addListener((ycObs, ycA, ycB) -> ycPaginador.ycIrAPrimera());
    }

    private void ycConfigurarFormulario() {
        ycCmbInquilino.setConverter(new StringConverter<>() {
            @Override
            public String toString(YCInquilino ycI) {
                return ycI == null ? "" : ycI.ycGetNombreCompleto() + "  ·  " + ycI.getYcCuarto().getYcNumero();
            }

            @Override
            public YCInquilino fromString(String ycS) {
                return null;
            }
        });
        ycCmbPeriodo.setConverter(new StringConverter<>() {
            @Override
            public String toString(YCDeudaPendiente ycD) {
                if (ycD == null) {
                    return "";
                }
                String ycExtra = ycD.ycAtrasado() ? "  ·  atrasado" : ycD.ycAdelantado() ? "  ·  adelantado" : "";
                return ycD.ycPeriodoTexto() + (ycD.ycPorDias() ? " (" + ycD.ycTextoDias() + ")" : "") + ycExtra;
            }

            @Override
            public YCDeudaPendiente fromString(String ycS) {
                return null;
            }
        });
        ycCmbMetodo.setItems(FXCollections.observableArrayList(YCMetodoPago.values()));

        ycCmbInquilino.valueProperty().addListener((ycObs, ycA, ycInquilino) -> {
            YCFormularios.ycLimpiarError(ycCmbInquilino);
            ycCargarPeriodos(ycInquilino, null);
        });
        ycCmbPeriodo.valueProperty().addListener((ycObs, ycA, ycDeuda) -> {
            YCFormularios.ycLimpiarError(ycCmbPeriodo);
            ycCargarMontos(ycDeuda);
        });

        for (TextField ycT : List.of(ycTxtAlquiler, ycTxtLuz, ycTxtAgua)) {
            YCFormularios.ycSoloDecimal(ycT);
            ycT.textProperty().addListener((ycObs, ycA, ycB) -> ycActualizarTotal());
        }
        YCFormularios.ycLimpiarAlEditar(ycTxtAlquiler, ycTxtLuz, ycTxtAgua);
    }

    @Override
    public void ycRefrescar() {
        ycCargarTabla();
        ycConstruirDeudas();
    }

    private void ycConstruirDeudas() {
        List<YCDeudaPendiente> ycDeudas = ycService.ycPendientesDeTodos().stream()
                .sorted(java.util.Comparator.comparingLong(YCDeudaPendiente::ycDiasAtraso).reversed())
                .toList();
        ycListaDeudas.getChildren().clear();
        ycPanelDeudas.getStyleClass().remove("con-atrasos");
        ycIconoDeudas.getStyleClass().removeAll("rojo", "verde", "ambar");
        long ycAtrasados = ycDeudas.stream().filter(YCDeudaPendiente::ycAtrasado).count();
        BigDecimal ycTotal = ycDeudas.stream().map(YCDeudaPendiente::ycTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ycVencido = ycDeudas.stream().filter(YCDeudaPendiente::ycAtrasado)
                .map(YCDeudaPendiente::ycTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (ycDeudas.isEmpty()) {
            ycIconoDeudas.getStyleClass().add("verde");
            ycLblDeudas.setText("Nadie esta debiendo");
            ycLblDeudasAyuda.setText("Todos los inquilinos estan al dia.");
            ycScrollDeudas.setVisible(false);
            ycScrollDeudas.setManaged(false);
            return;
        }
        ycScrollDeudas.setVisible(true);
        ycScrollDeudas.setManaged(true);
        if (ycAtrasados > 0) {
            ycPanelDeudas.getStyleClass().add("con-atrasos");
            ycIconoDeudas.getStyleClass().add("rojo");
            ycLblDeudas.setText("Estan debiendo: " + ycAtrasados + (ycAtrasados == 1 ? " pago atrasado" : " pagos atrasados")
                    + " · " + YCControles.ycMoneda(ycVencido));
        } else {
            ycIconoDeudas.getStyleClass().add("ambar");
            ycLblDeudas.setText("Nadie esta atrasado");
        }
        ycLblDeudasAyuda.setText(ycDeudas.size() + (ycDeudas.size() == 1 ? " mes por cobrar" : " meses por cobrar")
                + " en total (" + YCControles.ycMoneda(ycTotal) + "). Pulsa \"Cobrar\" y escribe ahi la luz y el agua.");

        for (YCDeudaPendiente ycD : ycDeudas) {
            YCInquilino ycI = ycD.ycInquilino();
            StackPane ycAvatar = new StackPane(new Label(YCTablas.ycIniciales(ycI.ycGetNombreCompleto())));
            ycAvatar.getStyleClass().add("avatar");
            Label ycNombre = new Label(ycI.ycGetNombreCompleto());
            ycNombre.getStyleClass().add("ficha-dato-valor");
            Label ycDetalle = new Label(ycI.getYcCuarto().getYcNumero() + " · " + ycD.ycPeriodoTexto()
                    + " · vence el " + YCControles.ycFecha(ycD.ycVence()));
            ycDetalle.getStyleClass().add("ficha-lateral-nota");
            VBox ycTextos = new VBox(1, ycNombre, ycDetalle);
            HBox.setHgrow(ycTextos, Priority.ALWAYS);
            Label ycEstado = new Label(ycD.ycEstado());
            ycEstado.getStyleClass().addAll("chip", ycD.ycAtrasado() ? "peligro" : ycD.ycDiasAtraso() == 0 ? "advertencia" : "neutro");
            ycEstado.setMinWidth(Region.USE_PREF_SIZE);
            Label ycMonto = new Label(YCControles.ycMoneda(ycD.ycTotal()));
            ycMonto.getStyleClass().add("deuda-monto");
            ycMonto.setMinWidth(Region.USE_PREF_SIZE);

            Button ycWhatsapp = new Button(null, new FontIcon(Feather.MESSAGE_CIRCLE));
            ycWhatsapp.getStyleClass().addAll("success", "button-outlined", "button-circle");
            ycWhatsapp.setTooltip(new Tooltip("Recordarle por WhatsApp"));
            ycWhatsapp.setOnAction(ycEv -> ycRecordar(ycD));
            Button ycCobrar = new Button("Cobrar", new FontIcon(Feather.DOLLAR_SIGN));
            ycCobrar.getStyleClass().add("accent");
            ycCobrar.setOnAction(ycEv -> ycAbrirCobro(ycI.getYcId(), ycD.ycPeriodo()));
            HBox ycAcciones = new HBox(6, ycWhatsapp, ycCobrar);
            ycAcciones.setAlignment(Pos.CENTER_RIGHT);
            ycAcciones.setMinWidth(Region.USE_PREF_SIZE);

            HBox ycFila = new HBox(12, ycAvatar, ycTextos, ycEstado, ycMonto, ycAcciones);
            ycFila.setAlignment(Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            if (ycD.ycAtrasado()) {
                ycFila.getStyleClass().add("no-llego");
            }
            ycListaDeudas.getChildren().add(ycFila);
        }
    }

    private void ycRecordar(YCDeudaPendiente ycD) {
        YCInquilino ycI = ycD.ycInquilino();
        String ycCelular = ycI.getYcTelefono() == null ? "" : ycI.getYcTelefono().replaceAll("\\D", "");
        if (ycCelular.length() != 9) {
            ycNotificador.ycAdvertencia(ycI.ycGetNombreCompleto() + " no tiene un celular registrado.");
            return;
        }
        String ycCuando = ycD.ycAtrasado() ? "que vencio el " + YCControles.ycFecha(ycD.ycVence())
                : "que vence el " + YCControles.ycFecha(ycD.ycVence());
        String ycTexto = "Hola " + ycI.getYcNombre() + ", le saluda " + ycArrendador.getYcNombre()
                + ". Le recuerdo el pago de " + ycD.ycPeriodoTexto() + " del " + ycI.getYcCuarto().getYcNumero()
                + " por " + YCControles.ycMoneda(ycD.ycAlquiler()) + " de alquiler (mas luz y agua), " + ycCuando
                + ". " + ycCobroYape.ycFrase() + "Muchas gracias.";
        boolean ycConQr = ycCobroYape.ycCopiarQr();
        ycExportador.ycAbrirEnlace("https://wa.me/51" + ycCelular + "?text="
                + URLEncoder.encode(ycTexto, StandardCharsets.UTF_8).replace("+", "%20"));
        if (ycConQr) {
            ycNotificador.ycExito("Se abrio WhatsApp. Tu QR de Yape quedo copiado: en el chat presiona Ctrl+V para adjuntarlo.");
        }
    }

    @FXML
    private void ycOnNuevo() {
        ycAbrirCobro(null, null);
    }

    public void ycRegistrarPara(Long ycInquilinoId, YearMonth ycPeriodo) {
        ycAbrirCobro(ycInquilinoId, ycPeriodo);
    }

    private void ycAbrirCobro(Long ycInquilinoId, YearMonth ycPeriodo) {
        YCFormularios.ycLimpiarErrores(ycCmbInquilino, ycCmbPeriodo, ycTxtAlquiler, ycTxtLuz, ycTxtAgua);
        List<YCInquilino> ycActivos = ycInquilinoService.ycActivos();
        ycCmbInquilino.setItems(FXCollections.observableArrayList(ycActivos));
        ycCmbMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycTxtNota.clear();
        YCInquilino ycElegido = ycInquilinoId == null ? null
                : ycActivos.stream().filter(ycI -> ycI.getYcId().equals(ycInquilinoId)).findFirst().orElse(null);
        ycCmbInquilino.setValue(null);
        ycCmbInquilino.setValue(ycElegido);
        if (ycElegido != null && ycPeriodo != null) {
            ycCargarPeriodos(ycElegido, ycPeriodo);
        }
        if (ycActivos.isEmpty()) {
            ycNotificador.ycAdvertencia("No hay inquilinos activos. Registra uno en el modulo Inquilinos.");
        }
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(() -> (ycElegido == null ? ycCmbInquilino : ycTxtAlquiler).requestFocus());
    }

    private void ycCargarPeriodos(YCInquilino ycInquilino, YearMonth ycElegir) {
        if (ycInquilino == null) {
            ycCmbPeriodo.setItems(FXCollections.observableArrayList());
            ycCmbPeriodo.setValue(null);
            ycLblAyudaPeriodo.setText(" ");
            return;
        }
        List<YCDeudaPendiente> ycPendientes = ycService.ycPendientes(ycInquilino, true);
        ycCmbPeriodo.setItems(FXCollections.observableArrayList(ycPendientes));
        YCDeudaPendiente ycSeleccion = ycPendientes.stream()
                .filter(ycD -> ycD.ycPeriodo().equals(ycElegir))
                .findFirst()
                .orElse(ycPendientes.isEmpty() ? null : ycPendientes.get(0));
        ycCmbPeriodo.setValue(null);
        ycCmbPeriodo.setValue(ycSeleccion);

        long ycAtrasados = ycPendientes.stream().filter(YCDeudaPendiente::ycAtrasado).count();
        ycLblAyudaPeriodo.setText(ycPendientes.isEmpty()
                ? "Esta al dia, incluso con el mes siguiente."
                : ycAtrasados == 0
                ? "Paga el dia " + ycInquilino.getYcDiaPago() + " de cada mes. No tiene meses atrasados."
                : "Tiene " + ycAtrasados + (ycAtrasados == 1 ? " mes atrasado." : " meses atrasados.")
                + " Se sugiere cobrar primero el mas antiguo.");
    }

    private void ycCargarMontos(YCDeudaPendiente ycD) {
        ycTxtAlquiler.setText(ycD == null ? "" : ycD.ycAlquiler().setScale(2).toPlainString());
        ycTxtLuz.setText(ycD == null ? "" : ycD.ycLuz().setScale(2).toPlainString());
        ycTxtAgua.setText(ycD == null ? "" : ycD.ycAgua().setScale(2).toPlainString());
        ycActualizarTotal();
    }

    private BigDecimal ycTotalFormulario() {
        return YCControles.ycImporteOCero(ycTxtAlquiler.getText())
                .add(YCControles.ycImporteOCero(ycTxtLuz.getText()))
                .add(YCControles.ycImporteOCero(ycTxtAgua.getText()));
    }

    private void ycActualizarTotal() {
        BigDecimal ycTotal = ycTotalFormulario();
        ycLblTotal.setText(YCControles.ycMoneda(ycTotal));
        ycLblTotalLetras.setText(ycTotal.signum() > 0 ? "Son: " + YCNumeroALetras.ycImporte(ycTotal) : " ");
    }

    @FXML
    private void ycOnCerrarFormulario() {
        ycNotificador.ycCerrarPanel();
    }

    @FXML
    private void ycOnRegistrar() {
        YCFormularios.ycLimpiarErrores(ycCmbInquilino, ycCmbPeriodo, ycTxtAlquiler, ycTxtLuz, ycTxtAgua);
        YCInquilino ycInquilino = ycCmbInquilino.getValue();
        YCDeudaPendiente ycDeuda = ycCmbPeriodo.getValue();
        if (ycInquilino == null) {
            YCFormularios.ycMarcarError(ycCmbInquilino, "Selecciona el inquilino");
            return;
        }
        if (ycDeuda == null) {
            YCFormularios.ycMarcarError(ycCmbPeriodo, "No hay un mes pendiente para cobrar");
            return;
        }
        if (ycTotalFormulario().signum() <= 0) {
            YCFormularios.ycMarcarError(ycTxtAlquiler, "El total debe ser mayor a 0");
            return;
        }
        try {
            YCPago ycPago = ycService.ycRegistrar(ycInquilino.getYcId(), ycDeuda.ycPeriodo(),
                    YCControles.ycImporteOCero(ycTxtAlquiler.getText()),
                    YCControles.ycImporteOCero(ycTxtLuz.getText()),
                    YCControles.ycImporteOCero(ycTxtAgua.getText()),
                    ycCmbMetodo.getValue(), ycTxtNota.getText());
            ycNotificador.ycCerrarPanel();
            ycCargarTabla();
            ycConstruirDeudas();
            ycNotificador.ycExitoConAcciones("Pago registrado",
                    ycPago.ycGetNumeroRecibo() + " · " + ycInquilino.ycGetNombreCompleto() + " pago "
                            + YCControles.ycMoneda(ycPago.getYcTotal()) + " por " + YCCalendarioPagos.ycTexto(ycPago.getYcPeriodo()) + ".",
                    "Cerrar", null,
                    "Ver recibo", () -> ycVerRecibo(ycPago));
        } catch (YCNegocioException ycE) {
            ycNotificador.ycError(ycE.getMessage());
        } catch (Exception ycE) {
            ycNotificador.ycError("Error inesperado: " + ycE.getMessage());
        }
    }

    @FXML
    private void ycOnVerRecibo() {
        ycVerRecibo(ycTabla.getSelectionModel().getSelectedItem());
    }

    private void ycVerRecibo(YCPago ycPago) {
        if (ycPago == null) {
            return;
        }
        ycExportador.ycAbrirTemporal("Recibo_" + ycPago.ycGetNumeroRecibo(), YCExportador.YCFormato.YC_PDF,
                ycDestino -> ycReciboPdf.ycGenerar(ycPago.getYcId(), ycDestino));
    }

    @FXML
    private void ycOnGuardarPdf() {
        YCPago ycPago = ycTabla.getSelectionModel().getSelectedItem();
        if (ycPago == null) {
            return;
        }
        String ycNombre = "Recibo_" + ycPago.ycGetNumeroRecibo() + "_" + ycPago.getYcPeriodo() + "_"
                + ycPago.getYcInquilino().getYcApellido().split("\\s+")[0];
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar recibo", ycNombre, YCExportador.YCFormato.YC_PDF,
                ycDestino -> ycReciboPdf.ycGenerar(ycPago.getYcId(), ycDestino));
    }

    @FXML
    private void ycOnExcel() {
        List<YCPago> ycPagos = ycService.ycBuscarTodos(ycTxtBuscar.getText());
        com.alquilafacil.service.YCExcel.YCHoja ycH = new com.alquilafacil.service.YCExcel.YCHoja("Pagos y recibos")
                .ycTitulo("Pagos y recibos" + (ycTxtBuscar.getText().isBlank() ? "" : " · busqueda: " + ycTxtBuscar.getText().trim()))
                .ycTitulo("Generado el " + java.time.LocalDateTime.now().format(YC_FECHA_HORA) + " · " + ycPagos.size() + " recibos")
                .ycColumna("Recibo", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11).ycColumna("Fecha", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA_HORA, 17)
                .ycColumna("Inquilino", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 28).ycColumna("DNI", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Cuarto", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 14).ycColumna("Mes pagado", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 16)
                .ycColumna("Alquiler", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Luz", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 9)
                .ycColumna("Agua", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 9).ycColumna("Total", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11)
                .ycColumna("Forma", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 13).ycColumna("Cobro", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Estado", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 10).ycColumna("Motivo de anulacion", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 30);
        BigDecimal ycTotal = BigDecimal.ZERO;
        for (YCPago ycP : ycPagos) {
            ycH.ycFila(ycP.ycGetNumeroRecibo(), ycP.getYcFechaPago(), ycP.getYcInquilino().ycGetNombreCompleto(), ycP.getYcInquilino().getYcDni(),
                    ycP.getYcCuartoNumero(), YCCalendarioPagos.ycTexto(ycP.getYcPeriodo()), ycP.getYcAlquiler(), ycP.getYcLuz(), ycP.getYcAgua(),
                    ycP.getYcTotal(), ycP.getYcMetodo().ycGetDescripcion(), ycP.getYcRegistradoPor(), ycP.isYcAnulado() ? "Anulado" : "Pagado",
                    ycP.getYcMotivoAnulacion());
            if (!ycP.isYcAnulado()) {
                ycTotal = ycTotal.add(ycP.getYcTotal());
            }
        }
        ycH.ycTotales("Total sin anulados", null, null, null, null, null, null, null, null, ycTotal, null, null, null, null);
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar pagos en Excel",
                "Pagos " + java.time.LocalDate.now(), YCExportador.YCFormato.YC_EXCEL, ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, List.of(ycH)));
    }

    @FXML
    private void ycOnAnular() {
        YCPago ycPago = ycTabla.getSelectionModel().getSelectedItem();
        if (ycPago == null) {
            return;
        }
        if (ycPago.isYcAnulado()) {
            ycNotificador.ycInfo("El recibo " + ycPago.ycGetNumeroRecibo() + " ya esta anulado.");
            return;
        }
        ycNotificador.ycConfirmarConMotivo("¿Anular el recibo " + ycPago.ycGetNumeroRecibo() + "?",
                "El recibo quedara marcado como ANULADO y " + YCCalendarioPagos.ycTexto(ycPago.getYcPeriodo())
                        + " volvera a figurar como pendiente para " + ycPago.getYcInquilino().ycGetNombreCompleto() + ".",
                "Ej. Se registro en el mes equivocado",
                "Anular",
                ycMotivo -> {
                    try {
                        ycService.ycAnular(ycPago.getYcId(), ycMotivo);
                        ycNotificador.ycExito("Recibo " + ycPago.ycGetNumeroRecibo() + " anulado.");
                        ycCargarTabla();
                        ycConstruirDeudas();
                    } catch (YCNegocioException ycE) {
                        ycNotificador.ycError(ycE.getMessage());
                    }
                });
    }

    private void ycCargarTabla() {
        Page<YCPago> ycPagina = ycService.ycListar(ycTxtBuscar.getText(), ycPaginador.ycGetPagina(), ycPaginador.ycGetTamanio());
        ycDatos.setAll(ycPagina.getContent());
        ycPaginador.ycActualizar(ycPagina);
        Animations.fadeIn(ycTabla, Duration.millis(260)).play();
    }
}
