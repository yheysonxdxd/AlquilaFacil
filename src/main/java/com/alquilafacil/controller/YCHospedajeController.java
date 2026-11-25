package com.alquilafacil.controller;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCanalReserva;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCEstadoLimpieza;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.service.YCCuartoService;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCHospedajeController implements YCRefrescable {
    private final YCEstadiaService ycService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCCuartoService ycCuartoService;
    private final YCNotificador ycNotificador;
    private final YCExportador ycExportador;
    private final YCDatosArrendador ycArrendador;
    private final YCMainController ycMainController;
    private final YCAccionesEstadia ycAcciones;
    private final YCDetalleEstadia ycDetalle;
    private final YCMensajesHuesped ycMensajes;
    private final com.alquilafacil.ui.YCAutocompletarDni ycAutocompletarDni;
    private final YCConsumosEstadia ycConsumos;
    private final com.alquilafacil.service.YCConsumoExtraService ycConsumoService;
    private Map<Long, BigDecimal> ycConsumosPendientes = Map.of();
    @FXML private Button ycBtnConsumo;
    private String ycHuespedAuto = "";
    private String ycTelefonoAuto = "";

    @FXML private VBox ycPanelFormulario;
    @FXML private Label ycLblTituloForm;
    @FXML private Label ycLblSubtituloForm;
    @FXML private Button ycBtnGuardarForm;
    private Long ycIdEditando;
    @FXML private ComboBox<YCCuarto> ycCmbCuarto;
    @FXML private Label ycLblInfoHabitacion;
    @FXML private VBox ycListaExtras;
    @FXML private Button ycBtnAgregarHabitacion;
    @FXML private Label ycLblPersonas;
    private final List<YCFilaExtra> ycExtras = new java.util.ArrayList<>();

    private record YCFilaExtra(ComboBox<YCCuarto> ycCuarto, TextField ycPersonas, TextField ycPrecio, Node ycNodo) {}
    @FXML private DatePicker ycDpEntrada;
    @FXML private DatePicker ycDpSalida;
    @FXML private TextField ycTxtNoches;
    @FXML private TextField ycTxtHuesped;
    @FXML private TextField ycTxtDocumento;
    @FXML private TextField ycTxtTelefono;
    @FXML private TextField ycTxtPersonas;
    @FXML private TextField ycTxtPrecio;
    @FXML private ComboBox<YCMetodoPago> ycCmbMetodo;
    @FXML private TextField ycTxtNota;
    @FXML private TextField ycTxtAdelanto;
    @FXML private ComboBox<YCCanalReserva> ycCmbCanal;
    @FXML private Label ycLblAyudaCanal;
    @FXML private VBox ycPanelLlegadas;
    @FXML private StackPane ycIconoLlegadas;
    @FXML private Label ycLblLlegadas;
    @FXML private Label ycLblLlegadasAyuda;
    @FXML private VBox ycListaLlegadas;
    @FXML private Label ycLblCanales;
    @FXML private TableColumn<YCEstadia, String> ycColCanal;
    @FXML private Label ycLblFrecuente;
    @FXML private Label ycLblDniEstado;
    @FXML private Label ycLblSaldoForm;
    @FXML private VBox ycPanelRecordatorios;
    @FXML private Label ycLblRecordatorios;
    @FXML private VBox ycListaRecordatorios;
    @FXML private Button ycBtnSaldo;
    @FXML private TableColumn<YCEstadia, String> ycColSaldo;
    @FXML private Label ycLblDetalleTotal;
    @FXML private Label ycLblTotal;

    @FXML private StackPane ycHero;
    @FXML private Label ycLblResumen;
    @FXML private Label ycKpiHospedados;
    @FXML private Label ycKpiHospedadosDetalle;
    @FXML private Label ycKpiLlegadas;
    @FXML private Label ycKpiLlegadasDetalle;
    @FXML private Label ycKpiSalidas;
    @FXML private Label ycKpiSalidasDetalle;
    @FXML private Label ycKpiCobrado;
    @FXML private Label ycKpiCobradoDetalle;
    @FXML private Label ycLblRangoCalendario;
    @FXML private GridPane ycCalendario;

    @FXML private TextField ycTxtBuscar;
    @FXML private ToggleGroup ycGrupoFiltro;
    @FXML private ToggleButton ycFiltroHospedados;
    @FXML private ToggleButton ycFiltroProximas;
    @FXML private ToggleButton ycFiltroTerminadas;
    @FXML private ToggleButton ycFiltroTodas;
    @FXML private Button ycBtnDetalle;
    @FXML private Button ycBtnComprobante;
    @FXML private Button ycBtnWhatsapp;
    @FXML private Button ycBtnSalida;
    @FXML private Button ycBtnCancelar;

    @FXML private TableView<YCEstadia> ycTabla;
    @FXML private TableColumn<YCEstadia, String> ycColNumero;
    @FXML private TableColumn<YCEstadia, String> ycColCuarto;
    @FXML private TableColumn<YCEstadia, String> ycColHuesped;
    @FXML private TableColumn<YCEstadia, String> ycColPersonas;
    @FXML private TableColumn<YCEstadia, String> ycColEntrada;
    @FXML private TableColumn<YCEstadia, String> ycColSalida;
    @FXML private TableColumn<YCEstadia, String> ycColNoches;
    @FXML private TableColumn<YCEstadia, BigDecimal> ycColTotal;
    @FXML private TableColumn<YCEstadia, String> ycColEstado;

    private final ObservableList<YCEstadia> ycDatos = FXCollections.observableArrayList();
    private final FilteredList<YCEstadia> ycFiltradas = new FilteredList<>(ycDatos);
    private boolean ycSincronizando;
    private LocalDate ycInicioCalendario = LocalDate.now().minusDays(1);
    private YCCalendarioOcupacion ycCalendarioOcupacion;

    @FXML
    public void initialize() {
        ycConfigurarTabla();
        ycConfigurarFormulario();
        ycCalendarioOcupacion = new YCCalendarioOcupacion(ycCalendario, ycLblRangoCalendario, this::ycNuevaEstadia, this::ycMostrarDetalle);

        ycTxtBuscar.textProperty().addListener((ycObs, ycA, ycB) -> ycAplicarFiltro());
        ycGrupoFiltro.getToggles().forEach(ycT -> ((Region) ycT).setMinWidth(Region.USE_PREF_SIZE));
        for (Button ycB : List.of(ycBtnDetalle, ycBtnComprobante, ycBtnWhatsapp, ycBtnSaldo, ycBtnConsumo, ycBtnSalida, ycBtnCancelar)) {
            ycB.setMinWidth(Region.USE_PREF_SIZE);
        }
        ycTxtBuscar.setMinWidth(150);
        ycGrupoFiltro.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual) -> {
            if (ycActual == null) {
                ycGrupoFiltro.selectToggle(ycAnterior);
                return;
            }
            ycAplicarFiltro();
        });
        Rectangle ycRecorte = new Rectangle();
        ycRecorte.setArcWidth(36);
        ycRecorte.setArcHeight(36);
        ycRecorte.widthProperty().bind(ycHero.widthProperty());
        ycRecorte.heightProperty().bind(ycHero.heightProperty());
        ycHero.setClip(ycRecorte);
            ycSesion.ycSoloAdmin(ycBtnCancelar);
    }

    @Override
    public void ycRefrescar() {
        ycCargar();
    }

    public void ycNuevaEstadia(YCCuarto ycCuarto) {
        ycNuevaEstadia(ycCuarto, null);
    }

    public void ycNuevaEstadia(YCCuarto ycCuarto, LocalDate ycEntrada) {
        ycOnNueva();
        if (ycEntrada != null) {
            ycDpEntrada.setValue(ycEntrada);
        }
        if (ycCuarto != null) {
            ycCmbCuarto.getItems().stream()
                    .filter(ycC -> ycC.getYcId().equals(ycCuarto.getYcId()))
                    .findFirst()
                    .ifPresent(ycCmbCuarto::setValue);
            Platform.runLater(ycTxtHuesped::requestFocus);
        }
    }

    @FXML
    private void ycOnNueva() {
        ycLimpiarFormulario();
        ycCmbCuarto.getItems().setAll(ycCuartoService.ycDeHospedaje());
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(ycCmbCuarto::requestFocus);
    }

    @FXML
    private void ycOnVerHabitaciones() {
        ycMainController.ycIrA("habitacion-view");
    }

    @FXML
    private void ycOnCerrarFormulario() {
        ycNotificador.ycCerrarPanel();
    }

    @FXML
    private void ycOnGuardar() {
        YCFormularios.ycLimpiarErrores(ycCmbCuarto, ycDpEntrada, ycDpSalida, ycTxtHuesped, ycTxtPrecio);
        if (ycCmbCuarto.getValue() == null) {
            YCFormularios.ycMarcarError(ycCmbCuarto, "Elige la habitacion.");
            return;
        }
        if (ycDpEntrada.getValue() == null) {
            YCFormularios.ycMarcarError(ycDpEntrada, "Indica la fecha de entrada.");
            return;
        }
        if (ycDpSalida.getValue() == null || !ycDpSalida.getValue().isAfter(ycDpEntrada.getValue())) {
            YCFormularios.ycMarcarError(ycDpSalida, "Debe ser despues de la entrada.");
            return;
        }
        if (ycTxtHuesped.getText().isBlank()) {
            YCFormularios.ycMarcarError(ycTxtHuesped, "Escribe el nombre del huesped.");
            return;
        }
        BigDecimal ycPrecio = YCControles.ycImporte(ycTxtPrecio.getText());
        if (ycPrecio == null || ycPrecio.signum() <= 0) {
            YCFormularios.ycMarcarError(ycTxtPrecio, "Precio no valido.");
            return;
        }
        if (ycIdEditando != null) {
            ycGuardarEdicion(ycPrecio);
            return;
        }
        for (YCFilaExtra ycF : ycExtras) {
            if (ycF.ycCuarto().getValue() == null) {
                ycNotificador.ycError("Elige la habitacion en cada fila del grupo (o quita la fila vacia).");
                return;
            }
            BigDecimal ycP = YCControles.ycImporte(ycF.ycPrecio().getText());
            if (ycP == null || ycP.signum() <= 0) {
                ycNotificador.ycError("Precio no valido en " + ycF.ycCuarto().getValue().getYcNumero() + ".");
                return;
            }
        }
        BigDecimal ycAdelanto = null;
        if (!ycTxtAdelanto.getText().isBlank()) {
            ycAdelanto = YCControles.ycImporte(ycTxtAdelanto.getText());
            BigDecimal ycTotal = ycTotalFormulario();
            if (ycAdelanto == null || ycAdelanto.compareTo(ycTotal) > 0) {
                YCFormularios.ycMarcarError(ycTxtAdelanto, "Maximo " + YCControles.ycMoneda(ycTotal) + ".");
                return;
            }
        }
        if (!ycExtras.isEmpty()) {
            ycGuardarGrupo(ycPrecio, ycAdelanto);
            return;
        }
        try {
            YCEstadia ycE = ycService.ycRegistrar(new YCEstadiaService.YCSolicitud(
                    ycCmbCuarto.getValue().getYcId(), ycTxtHuesped.getText(), ycTxtDocumento.getText(),
                    ycTxtTelefono.getText(), ycEntero(ycTxtPersonas, 1), ycDpEntrada.getValue(), ycDpSalida.getValue(),
                    ycPrecio, ycCmbMetodo.getValue(), ycTxtNota.getText(), ycAdelanto, ycCmbCanal.getValue()));
            ycNotificador.ycCerrarPanel();
            String ycResumen = ycE.getYcHuesped() + " en " + ycE.getYcCuartoNumero() + " · " + ycE.getYcNoches()
                    + (ycE.getYcNoches() == 1 ? " noche" : " noches") + " · " + YCControles.ycMoneda(ycE.getYcTotal())
                    + (ycE.ycTieneSaldo() ? " (adelanto " + YCControles.ycMoneda(ycE.ycGetPagado()) + ", saldo "
                    + YCControles.ycMoneda(ycE.ycGetSaldo()) + ")" : "") + ".";
            YCEstadoLimpieza ycLimpieza = ycE.getYcCuarto().ycGetLimpieza();
            if (ycLimpieza != YCEstadoLimpieza.YC_LIMPIA && !ycE.getYcEntrada().isAfter(LocalDate.now())) {
                ycNotificador.ycAdvertencia(ycE.getYcCuartoNumero() + " figura como \"" + ycLimpieza.ycGetTexto().toLowerCase()
                        + "\". Preparala antes de que llegue el huesped.");
            }
            if (YCMensajesHuesped.ycTieneCelular(ycE)) {
                ycNotificador.ycExitoConAcciones("Estadia " + ycE.ycGetNumero() + " registrada",
                        ycResumen + " ¿Le envias su comprobante por WhatsApp?",
                        "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycE),
                        "Enviar por WhatsApp", () -> ycMensajes.ycEnviarWhatsapp(ycE));
            } else {
                ycNotificador.ycExitoConAcciones("Estadia " + ycE.ycGetNumero() + " registrada", ycResumen,
                        "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycE));
            }
            switch (ycE.ycEstado(LocalDate.now())) {
                case YC_EN_CURSO -> (ycE.ycIsLlego() ? ycFiltroHospedados : ycFiltroProximas).setSelected(true);
                case YC_PROXIMA -> ycFiltroProximas.setSelected(true);
                default -> ycFiltroTodas.setSelected(true);
            }
            ycCargar();
            ycTabla.getSelectionModel().select(ycE);
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        } catch (Exception ycEx) {
            ycNotificador.ycError("Error inesperado: " + ycEx.getMessage());
        }
    }

    @FXML
    private void ycOnAgregarHabitacion() {
        ComboBox<YCCuarto> ycCuarto = new ComboBox<>();
        ycCuarto.setConverter(ycCmbCuarto.getConverter());
        ycCuarto.getItems().setAll(ycCmbCuarto.getItems());
        ycCuarto.setPromptText("Otra habitacion");
        ycCuarto.setMaxWidth(Double.MAX_VALUE);
        TextField ycPersonas = new TextField("1");
        ycPersonas.setPrefWidth(70);
        YCFormularios.ycSoloDigitos(ycPersonas, 2);
        TextField ycPrecio = new TextField();
        ycPrecio.setPromptText("S/ noche");
        ycPrecio.setPrefWidth(90);
        YCFormularios.ycSoloDecimal(ycPrecio);
        ycPrecio.editableProperty().bind(ycSesion.ycAdminProperty());
        Button ycQuitar = new Button(null, new FontIcon(Feather.X));
        ycQuitar.getStyleClass().addAll("flat", "button-circle", "danger");
        ycQuitar.setTooltip(new Tooltip("Quitar esta habitacion"));

        Label ycEtCuarto = new Label("Habitacion " + (ycExtras.size() + 2));
        ycEtCuarto.getStyleClass().add("campo-label");
        Label ycEtPersonas = new Label("Pers.");
        ycEtPersonas.getStyleClass().add("campo-label");
        Label ycEtPrecio = new Label("S/ noche");
        ycEtPrecio.getStyleClass().add("campo-label");
        VBox ycCCuarto = new VBox(4, ycEtCuarto, ycCuarto);
        HBox.setHgrow(ycCCuarto, Priority.ALWAYS);
        VBox ycCPersonas = new VBox(4, ycEtPersonas, ycPersonas);
        ycCPersonas.setMinWidth(64);
        ycCPersonas.setPrefWidth(64);
        VBox ycCPrecio = new VBox(4, ycEtPrecio, ycPrecio);
        ycCPrecio.setMinWidth(90);
        ycCPrecio.setPrefWidth(90);
        HBox ycFila = new HBox(8, ycCCuarto, ycCPersonas, ycCPrecio, ycQuitar);
        ycFila.setAlignment(Pos.BOTTOM_LEFT);

        YCFilaExtra ycExtra = new YCFilaExtra(ycCuarto, ycPersonas, ycPrecio, ycFila);
        ycCuarto.valueProperty().addListener((ycObs, ycA, ycC) -> {
            if (ycC != null) {
                ycPrecio.setText(ycC.getYcPrecio().setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
            ycActualizarTotal();
        });
        ycPrecio.textProperty().addListener((ycObs, ycA, ycB) -> ycActualizarTotal());
        ycQuitar.setOnAction(ycEv -> {
            ycExtras.remove(ycExtra);
            ycListaExtras.getChildren().remove(ycFila);
            ycRenumerarExtras();
        });
        ycExtras.add(ycExtra);
        ycListaExtras.getChildren().add(ycFila);
        ycRenumerarExtras();
        Platform.runLater(ycCuarto::requestFocus);
    }

    private void ycRenumerarExtras() {
        for (int ycI = 0; ycI < ycExtras.size(); ycI++) {
            HBox ycFila = (HBox) ycExtras.get(ycI).ycNodo();
            ((Label) ((VBox) ycFila.getChildren().get(0)).getChildren().get(0)).setText("Habitacion " + (ycI + 2));
        }
        boolean ycHay = !ycExtras.isEmpty();
        ycListaExtras.setVisible(ycHay);
        ycListaExtras.setManaged(ycHay);
        ycLblPersonas.setText(ycHay ? "Personas (hab. 1)" : "Personas");
        ycActualizarTotal();
    }

    private BigDecimal ycTotalFormulario() {
        long ycNoches = YCEstadiaService.ycNoches(ycDpEntrada.getValue(), ycDpSalida.getValue());
        BigDecimal ycPrecio = YCControles.ycImporte(ycTxtPrecio.getText());
        BigDecimal ycTotal = ycPrecio == null ? BigDecimal.ZERO : ycPrecio.multiply(BigDecimal.valueOf(ycNoches));
        for (YCFilaExtra ycF : ycExtras) {
            BigDecimal ycP = YCControles.ycImporte(ycF.ycPrecio().getText());
            if (ycF.ycCuarto().getValue() != null && ycP != null) {
                ycTotal = ycTotal.add(ycP.multiply(BigDecimal.valueOf(ycNoches)));
            }
        }
        return ycTotal;
    }

    private void ycGuardarGrupo(BigDecimal ycPrecio, BigDecimal ycAdelanto) {
        List<YCEstadiaService.YCHabitacionGrupo> ycHabitaciones = new java.util.ArrayList<>();
        ycHabitaciones.add(new YCEstadiaService.YCHabitacionGrupo(ycCmbCuarto.getValue().getYcId(), ycEntero(ycTxtPersonas, 1), ycPrecio));
        for (YCFilaExtra ycF : ycExtras) {
            ycHabitaciones.add(new YCEstadiaService.YCHabitacionGrupo(ycF.ycCuarto().getValue().getYcId(), ycEntero(ycF.ycPersonas(), 1),
                    YCControles.ycImporte(ycF.ycPrecio().getText())));
        }
        try {
            List<YCEstadia> ycGrupo = ycService.ycRegistrarGrupo(new YCEstadiaService.YCSolicitud(
                    null, ycTxtHuesped.getText(), ycTxtDocumento.getText(), ycTxtTelefono.getText(), null,
                    ycDpEntrada.getValue(), ycDpSalida.getValue(), null, ycCmbMetodo.getValue(), ycTxtNota.getText(),
                    ycAdelanto, ycCmbCanal.getValue()), ycHabitaciones);
            ycNotificador.ycCerrarPanel();
            YCEstadia ycE = ycGrupo.get(0);
            BigDecimal ycTotal = ycGrupo.stream().map(YCEstadia::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ycSaldo = ycGrupo.stream().map(YCEstadia::ycGetSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            String ycResumen = ycE.getYcHuesped() + " alquila " + ycGrupo.size() + " habitaciones ("
                    + String.join(", ", ycGrupo.stream().map(YCEstadia::getYcCuartoNumero).toList()) + ") · "
                    + ycE.getYcNoches() + (ycE.getYcNoches() == 1 ? " noche" : " noches") + " · total " + YCControles.ycMoneda(ycTotal)
                    + (ycSaldo.signum() > 0 ? " · saldo " + YCControles.ycMoneda(ycSaldo) : "") + ".";
            if (YCMensajesHuesped.ycTieneCelular(ycE)) {
                ycNotificador.ycExitoConAcciones("Reserva de grupo " + ycE.ycGetNumero() + " registrada",
                        ycResumen + " ¿Le envias su comprobante por WhatsApp?",
                        "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycE),
                        "Enviar por WhatsApp", () -> ycMensajes.ycEnviarWhatsapp(ycE));
            } else {
                ycNotificador.ycExitoConAcciones("Reserva de grupo " + ycE.ycGetNumero() + " registrada", ycResumen,
                        "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycE));
            }
            switch (ycE.ycEstado(LocalDate.now())) {
                case YC_EN_CURSO -> (ycE.ycIsLlego() ? ycFiltroHospedados : ycFiltroProximas).setSelected(true);
                case YC_PROXIMA -> ycFiltroProximas.setSelected(true);
                default -> ycFiltroTodas.setSelected(true);
            }
            ycCargar();
            ycTabla.getSelectionModel().select(ycE);
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        } catch (Exception ycEx) {
            ycNotificador.ycError("Error inesperado: " + ycEx.getMessage());
        }
    }

    @FXML
    private void ycOnExcel() {
        com.alquilafacil.service.YCExcel.YCHoja ycH = new com.alquilafacil.service.YCExcel.YCHoja("Estadias")
                .ycTitulo(ycArrendador.getYcNombreHospedaje() + " · Estadias y reservas")
                .ycTitulo("Filtro: " + ((ToggleButton) ycGrupoFiltro.getSelectedToggle()).getText()
                        + (ycTxtBuscar.getText().isBlank() ? "" : " · busqueda: " + ycTxtBuscar.getText().trim())
                        + " · " + ycFiltradas.size() + " estadias")
                .ycColumna("N°", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 10).ycColumna("Huesped", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 28)
                .ycColumna("Documento", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 12).ycColumna("Celular", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Habitacion", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 16).ycColumna("Reservo por", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 13)
                .ycColumna("Entrada", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 11).ycColumna("Salida", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 11)
                .ycColumna("Noches", com.alquilafacil.service.YCExcel.YCTipo.YC_ENTERO, 8).ycColumna("Personas", com.alquilafacil.service.YCExcel.YCTipo.YC_ENTERO, 9)
                .ycColumna("Precio noche", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 12).ycColumna("Total", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11)
                .ycColumna("Pagado", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Saldo", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11)
                .ycColumna("Devuelto", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Estado", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 12)
                .ycColumna("Registro", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11);
        BigDecimal ycTotal = BigDecimal.ZERO, ycPagado = BigDecimal.ZERO, ycSaldo = BigDecimal.ZERO;
        for (YCEstadia ycE : ycFiltradas) {
            ycH.ycFila(ycE.ycGetNumero(), ycE.getYcHuesped(), ycE.getYcDocumento(), ycE.getYcTelefono(), ycE.getYcCuartoNumero(),
                    ycE.getYcCanal() == null ? null : ycE.getYcCanal().ycGetTexto(), ycE.getYcEntrada(), ycE.getYcSalida(), ycE.getYcNoches(),
                    ycE.getYcPersonas(), ycE.getYcPrecioNoche(), ycE.getYcTotal(), ycE.ycGetPagado(), ycE.ycGetSaldo(), ycE.getYcDevuelto(),
                    YCEstadoEstadia.ycTexto(ycE, ycArrendador.ycGetHoraLimiteLlegadaHora()), ycE.getYcRegistradoPor());
            if (!ycE.isYcCancelada()) {
                ycTotal = ycTotal.add(ycE.getYcTotal());
            }
            ycPagado = ycPagado.add(ycE.ycGetPagado());
            ycSaldo = ycSaldo.add(ycE.ycGetSaldo());
        }
        ycH.ycTotales("Totales", null, null, null, null, null, null, null, null, null, null, ycTotal, ycPagado, ycSaldo, null, null, null);
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar estadias en Excel",
                "Estadias " + LocalDate.now(), YCExportador.YCFormato.YC_EXCEL, ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, List.of(ycH)));
    }

    @FXML
    private void ycOnDetalle() {
        YCEstadia ycE = ycTabla.getSelectionModel().getSelectedItem();
        if (ycE != null) {
            ycMostrarDetalle(ycE);
        }
    }

    @FXML
    private void ycOnComprobante() {
        YCEstadia ycE = ycTabla.getSelectionModel().getSelectedItem();
        if (ycE != null) {
            ycMensajes.ycVerComprobante(ycE);
        }
    }

    @FXML
    private void ycOnWhatsapp() {
        YCEstadia ycE = ycTabla.getSelectionModel().getSelectedItem();
        if (ycE != null) {
            ycMensajes.ycEnviarWhatsapp(ycE);
        }
    }

    @FXML
    private void ycOnCobrarSaldo() {
        ycAcciones.ycCobrarSaldo(ycTabla.getSelectionModel().getSelectedItem(), this::ycCargar);
    }

    @FXML
    private void ycOnConsumo() {
        YCEstadia ycE = ycTabla.getSelectionModel().getSelectedItem();
        if (ycE != null) {
            ycConsumos.ycAgregar(ycE, this::ycCargar);
        }
    }

    @FXML
    private void ycOnSalida() {
        ycAcciones.ycRegistrarSalida(ycTabla.getSelectionModel().getSelectedItem(), this::ycCargar);
    }

    @FXML
    private void ycOnCancelar() {
        ycAcciones.ycCancelar(ycTabla.getSelectionModel().getSelectedItem(), this::ycCargar);
    }

    @FXML
    private void ycOnCalendarioAnterior() {
        ycInicioCalendario = ycInicioCalendario.minusDays(7);
        ycDibujarCalendario();
    }

    @FXML
    private void ycOnCalendarioSiguiente() {
        ycInicioCalendario = ycInicioCalendario.plusDays(7);
        ycDibujarCalendario();
    }

    @FXML
    private void ycOnCalendarioHoy() {
        ycInicioCalendario = LocalDate.now().minusDays(1);
        ycDibujarCalendario();
    }

    private void ycMostrarDetalle(YCEstadia ycE) {
        ycDetalle.ycMostrar(ycE, this::ycCargar, this::ycEditarEstadia);
    }

    private boolean ycAtrasado(YCEstadia ycE) {
        return YCEstadoEstadia.ycAtrasado(ycE, ycArrendador.ycGetHoraLimiteLlegadaHora());
    }

    private void ycDibujarCalendario() {
        ycCalendarioOcupacion.ycDibujar(ycInicioCalendario, ycCuartoService.ycDeHospedaje(),
                ycService.ycEntre(ycInicioCalendario, ycInicioCalendario.plusDays(YCCalendarioOcupacion.YC_DIAS_CALENDARIO)));
    }

    private void ycCargar() {
        ycConsumosPendientes = ycConsumoService.ycPendientesPorEstadia();
        ycDatos.setAll(ycService.ycTodas());
        LocalDate ycHoy = LocalDate.now();
        List<YCCuarto> ycCuartos = ycCuartoService.ycDeHospedaje();
        Map<Long, YCEstadia> ycEnCurso = ycService.ycEnCursoPorCuarto();

        long ycHospedados = ycDatos.stream().filter(ycE -> ycE.ycHospedado(ycHoy)).count();
        long ycProximas = ycDatos.stream().filter(ycE -> YCEstadoEstadia.ycPorLlegar(ycE, ycHoy)).count();
        int ycPersonas = ycDatos.stream().filter(ycE -> ycE.ycHospedado(ycHoy))
                .mapToInt(YCEstadia::getYcPersonas).sum();
        List<YCEstadia> ycLlegadas = ycDatos.stream()
                .filter(ycE -> !ycE.isYcCancelada() && ycE.getYcEntrada().equals(ycHoy)).toList();
        List<YCEstadia> ycSalidas = ycDatos.stream()
                .filter(ycE -> !ycE.isYcCancelada() && ycE.getYcSalida().equals(ycHoy)).toList();
        long ycLibres = ycCuartos.stream().filter(ycC -> !ycEnCurso.containsKey(ycC.getYcId())).count();

        ycLblResumen.setText(YCControles.ycCapitalizar(ycHoy.format(YCControles.YC_FECHA_LARGA)) + " · " + ycLibres + " de " + ycCuartos.size()
                + (ycCuartos.size() == 1 ? " habitacion libre" : " habitaciones libres") + " esta noche");
        ycKpiHospedados.setText(String.valueOf(ycHospedados));
        ycKpiHospedadosDetalle.setText(ycPersonas + (ycPersonas == 1 ? " persona" : " personas") + " en "
                + ycHospedados + " de " + ycCuartos.size() + " habitaciones");
        ycKpiLlegadas.setText(String.valueOf(ycLlegadas.size()));
        List<YCEstadia> ycFaltan = ycLlegadas.stream().filter(ycE -> !ycE.ycIsLlego()).toList();
        ycKpiLlegadasDetalle.setText(ycLlegadas.isEmpty() ? "Nadie llega hoy"
                : ycFaltan.isEmpty() ? "Todos llegaron · " + ycNombres(ycLlegadas)
                : ycFaltan.size() + " por llegar · " + ycNombres(ycFaltan));
        ycKpiSalidas.setText(String.valueOf(ycSalidas.size()));
        ycKpiSalidasDetalle.setText(ycSalidas.isEmpty() ? "Nadie sale hoy" : ycNombres(ycSalidas));
        YearMonth ycMes = YearMonth.from(ycHoy);
        ycKpiCobrado.setText(ycSesion.ycAdminProperty().get() ? YCControles.ycMoneda(ycService.ycCobradoEn(ycMes)) : "S/ •••••");
        long ycEstadiasMes = ycDatos.stream().filter(ycE -> !ycE.isYcCancelada()
                && YearMonth.from(ycE.getYcFechaRegistro()).equals(ycMes)).count();
        ycKpiCobradoDetalle.setText(ycEstadiasMes + (ycEstadiasMes == 1 ? " estadia" : " estadias") + " en "
                + YCControles.ycNombreMes(ycMes));

        ycFiltroHospedados.setText("Hospedados (" + ycHospedados + ")");
        ycFiltroProximas.setText("Proximas (" + ycProximas + ")");

        ycConstruirLlegadas();
        ycConstruirRecordatorios();
        ycConstruirCanales();
        ycDibujarCalendario();
        ycAplicarFiltro();
        ycTabla.refresh();
    }

    private void ycConstruirLlegadas() {
        List<YCEstadia> ycPendientes = ycService.ycEsperandoLlegada();
        ycListaLlegadas.getChildren().clear();
        ycPanelLlegadas.setVisible(!ycPendientes.isEmpty());
        ycPanelLlegadas.setManaged(!ycPendientes.isEmpty());
        if (ycPendientes.isEmpty()) {
            return;
        }
        long ycAtrasados = ycPendientes.stream().filter(this::ycAtrasado).count();
        String ycLimite = ycArrendador.ycGetHoraLimiteLlegadaHora().toString();
        ycPanelLlegadas.getStyleClass().remove("con-atrasos");
        ycIconoLlegadas.getStyleClass().removeAll("ambar", "rojo");
        if (ycAtrasados > 0) {
            ycPanelLlegadas.getStyleClass().add("con-atrasos");
            ycIconoLlegadas.getStyleClass().add("rojo");
            ycLblLlegadas.setText(ycAtrasados + (ycAtrasados == 1 ? " huesped no llego" : " huespedes no llegaron")
                    + (ycPendientes.size() > ycAtrasados ? " · " + (ycPendientes.size() - ycAtrasados) + " por llegar" : ""));
            ycLblLlegadasAyuda.setText("Ya pasaron las " + ycLimite + ". Llamales o escribeles; si no vienen, libera la "
                    + "habitacion para alquilarla a otra persona.");
        } else {
            ycIconoLlegadas.getStyleClass().add("ambar");
            ycLblLlegadas.setText("Por llegar hoy: " + ycPendientes.size()
                    + (ycPendientes.size() == 1 ? " huesped" : " huespedes"));
            ycLblLlegadasAyuda.setText("Cuando llegue, marca \"Llego\". Si a las " + ycLimite
                    + " no ha llegado, aqui te avisara.");
        }
        for (YCEstadia ycE : ycPendientes) {
            boolean ycTarde = ycAtrasado(ycE);
            StackPane ycAvatar = new StackPane(new Label(YCTablas.ycIniciales(ycE.getYcHuesped())));
            ycAvatar.getStyleClass().add("avatar");
            Label ycNombre = new Label(ycE.getYcHuesped());
            ycNombre.getStyleClass().add("ficha-dato-valor");
            String ycCuando = ycE.getYcEntrada().isBefore(LocalDate.now())
                    ? "debia llegar el " + YCControles.ycFecha(ycE.getYcEntrada())
                    : "check-in desde las " + ycE.getYcCuarto().ycGetHoraEntradaODefecto();
            Label ycDetalle = new Label(ycE.getYcCuartoNumero() + " · " + ycCuando
                    + (ycE.getYcCanal() != null ? " · reservo por " + ycE.getYcCanal().ycGetTexto().toLowerCase() : "")
                    + (ycE.ycGetPagado().signum() > 0 ? " · adelanto " + YCControles.ycMoneda(ycE.ycGetPagado()) : " · sin adelanto"));
            ycDetalle.getStyleClass().add("ficha-lateral-nota");
            ycDetalle.setWrapText(true);
            VBox ycTextos = new VBox(1, ycNombre, ycDetalle);
            HBox.setHgrow(ycTextos, Priority.ALWAYS);
            Label ycEstado = new Label(ycTarde ? "No llego" : "Esperando",
                    new FontIcon(ycTarde ? Feather.ALERT_TRIANGLE : Feather.CLOCK));
            ycEstado.getStyleClass().addAll("chip", ycTarde ? "peligro" : "advertencia");
            ycEstado.setMinWidth(Region.USE_PREF_SIZE);

            HBox ycBotones = new HBox(6);
            ycBotones.setAlignment(Pos.CENTER_RIGHT);
            ycBotones.setMinWidth(Region.USE_PREF_SIZE);
            if (YCMensajesHuesped.ycTieneCelular(ycE)) {
                Button ycEscribir = new Button(null, new FontIcon(Feather.MESSAGE_CIRCLE));
                ycEscribir.getStyleClass().addAll("success", "button-outlined", "button-circle");
                ycEscribir.setTooltip(new Tooltip("Preguntarle por WhatsApp si sigue en pie su reserva"));
                ycEscribir.setOnAction(ycEv -> ycMensajes.ycPreguntarLlegada(ycE));
                ycBotones.getChildren().add(ycEscribir);
            }
            if (ycTarde) {
                Button ycNoVino = new Button("No vino", new FontIcon(Feather.X_CIRCLE));
                ycNoVino.getStyleClass().addAll("flat", "danger");
                ycNoVino.setTooltip(new Tooltip("Liberar la habitacion"));
                ycSesion.ycSoloAdmin(ycNoVino);
                ycNoVino.setOnAction(ycEv -> ycAcciones.ycRegistrarNoLlego(ycE, this::ycCargar));
                ycBotones.getChildren().add(ycNoVino);
            }
            Button ycLlego = new Button("Llego", new FontIcon(Feather.CHECK));
            ycLlego.getStyleClass().add("accent");
            ycLlego.setOnAction(ycEv -> ycAcciones.ycRegistrarLlegada(ycE, this::ycCargar));
            ycBotones.getChildren().add(ycLlego);

            HBox ycFila = new HBox(12, ycAvatar, ycTextos, ycEstado, ycBotones);
            ycFila.setAlignment(Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            if (ycTarde) {
                ycFila.getStyleClass().add("no-llego");
            }
            ycListaLlegadas.getChildren().add(ycFila);
        }
    }

    private void ycConstruirCanales() {
        YearMonth ycMes = YearMonth.now();
        Map<YCCanalReserva, Long> ycConteo = ycService.ycPorCanalDesde(ycMes.atDay(1));
        ycLblCanales.setVisible(!ycConteo.isEmpty());
        ycLblCanales.setManaged(!ycConteo.isEmpty());
        if (ycConteo.isEmpty()) {
            return;
        }
        long ycTotal = ycConteo.values().stream().mapToLong(Long::longValue).sum();
        String ycDetalle = String.join("  ·  ", ycConteo.entrySet().stream()
                .sorted(Map.Entry.<YCCanalReserva, Long>comparingByValue().reversed())
                .map(ycEn -> ycEn.getKey().ycGetTexto() + " " + ycEn.getValue()
                        + " (" + Math.round(ycEn.getValue() * 100.0 / ycTotal) + "%)")
                .toList());
        ycLblCanales.setText("De donde vinieron en " + YCControles.ycNombreMes(ycMes) + ":  " + ycDetalle);
    }

    private void ycConstruirRecordatorios() {
        List<YCEstadia> ycManana = ycService.ycLleganManana();
        ycListaRecordatorios.getChildren().clear();
        ycPanelRecordatorios.setVisible(!ycManana.isEmpty());
        ycPanelRecordatorios.setManaged(!ycManana.isEmpty());
        long ycPendientes = ycManana.stream().filter(ycE -> !ycE.ycIsRecordatorioEnviado()).count();
        ycLblRecordatorios.setText("Llegan manana: " + ycManana.size() + (ycManana.size() == 1 ? " huesped" : " huespedes")
                + (ycPendientes > 0 ? " · " + ycPendientes + " sin recordatorio" : " · todos avisados"));
        for (YCEstadia ycE : ycManana) {
            StackPane ycAvatar = new StackPane(new Label(YCTablas.ycIniciales(ycE.getYcHuesped())));
            ycAvatar.getStyleClass().add("avatar");
            Label ycNombre = new Label(ycE.getYcHuesped());
            ycNombre.getStyleClass().add("ficha-dato-valor");
            Label ycDetalle = new Label(ycE.getYcCuartoNumero() + " · check-in desde las " + ycE.getYcCuarto().ycGetHoraEntradaODefecto()
                    + " · " + ycE.getYcNoches() + (ycE.getYcNoches() == 1 ? " noche" : " noches")
                    + (ycE.ycTieneSaldo() ? " · saldo " + YCControles.ycMoneda(ycE.ycGetSaldo()) : ""));
            ycDetalle.getStyleClass().add("ficha-lateral-nota");
            VBox ycTextos = new VBox(1, ycNombre, ycDetalle);
            HBox.setHgrow(ycTextos, Priority.ALWAYS);
            Node ycAccion;
            if (ycE.ycIsRecordatorioEnviado()) {
                Label ycEnviado = new Label("Recordatorio enviado", new FontIcon(Feather.CHECK));
                ycEnviado.getStyleClass().addAll("chip", "exito");
                Button ycOtra = new Button(null, new FontIcon(Feather.REPEAT));
                ycOtra.getStyleClass().addAll("flat", "button-circle");
                ycOtra.setTooltip(new Tooltip("Enviar de nuevo"));
                ycOtra.setOnAction(ycEv -> ycMensajes.ycEnviarRecordatorio(ycE, this::ycCargar));
                HBox ycCaja = new HBox(6, ycEnviado, ycOtra);
                ycCaja.setAlignment(Pos.CENTER_RIGHT);
                ycAccion = ycCaja;
            } else if (YCMensajesHuesped.ycTieneCelular(ycE)) {
                Button ycRecordar = new Button("Recordar por WhatsApp", new FontIcon(Feather.MESSAGE_CIRCLE));
                ycRecordar.getStyleClass().addAll("success", "button-outlined");
                ycRecordar.setOnAction(ycEv -> ycMensajes.ycEnviarRecordatorio(ycE, this::ycCargar));
                ycAccion = ycRecordar;
            } else {
                Label ycSin = new Label("Sin celular");
                ycSin.getStyleClass().addAll("chip", "neutro");
                ycAccion = ycSin;
            }
            HBox ycFila = new HBox(12, ycAvatar, ycTextos, ycAccion);
            ycFila.setAlignment(Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            ycListaRecordatorios.getChildren().add(ycFila);
        }
    }

    private void ycAplicarFiltro() {
        String ycT = ycTxtBuscar.getText() == null ? "" : ycTxtBuscar.getText().trim().toLowerCase();
        Toggle ycFiltro = ycGrupoFiltro.getSelectedToggle();
        LocalDate ycHoy = LocalDate.now();
        ycFiltradas.setPredicate(ycE -> {
            YCEstadia.YCEstado ycEstado = ycE.ycEstado(ycHoy);
            boolean ycPasa = ycFiltro == ycFiltroTodas
                    || ycFiltro == ycFiltroHospedados && ycE.ycHospedado(ycHoy)
                    || ycFiltro == ycFiltroProximas && YCEstadoEstadia.ycPorLlegar(ycE, ycHoy)
                    || ycFiltro == ycFiltroTerminadas && (ycEstado == YCEstadia.YCEstado.YC_TERMINADA
                    || ycEstado == YCEstadia.YCEstado.YC_CANCELADA);
            return ycPasa && (ycT.isEmpty()
                    || ycE.getYcHuesped().toLowerCase().contains(ycT)
                    || ycE.getYcCuartoNumero().toLowerCase().contains(ycT)
                    || (ycE.getYcDocumento() != null && ycE.getYcDocumento().toLowerCase().contains(ycT)));
        });
    }

    private void ycConfigurarTabla() {
        ycColNumero.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycGetNumero()));
        ycColNumero.setCellFactory(YCTablas.ycCeldaId());
        ycColCuarto.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcCuartoNumero()
                + (ycC.getValue().ycIsDeGrupo() ? " · grupo" : "")));
        ycColCuarto.setCellFactory(YCTablas.ycCeldaChip("—"));
        ycColHuesped.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcHuesped()));
        ycColHuesped.setCellFactory(YCTablas.ycCeldaAvatar());
        ycColPersonas.setCellValueFactory(ycC -> new SimpleStringProperty(String.valueOf(ycC.getValue().getYcPersonas())));
        ycColEntrada.setCellValueFactory(ycC -> new SimpleStringProperty(YCControles.ycFecha(ycC.getValue().getYcEntrada())));
        ycColSalida.setCellValueFactory(ycC -> new SimpleStringProperty(YCControles.ycFecha(ycC.getValue().getYcSalida())));
        ycColNoches.setCellValueFactory(ycC -> new SimpleStringProperty(String.valueOf(ycC.getValue().getYcNoches())));
        ycColTotal.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().getYcTotal()));
        ycColTotal.setCellFactory(YCTablas.ycCeldaMoneda());
        ycColSaldo.setCellValueFactory(ycC -> {
            YCEstadia ycE = ycC.getValue();
            BigDecimal ycDebe = ycE.ycGetSaldo().add(ycConsumosPendientes.getOrDefault(ycE.getYcId(), BigDecimal.ZERO));
            return new SimpleStringProperty(ycE.isYcCancelada() && ycDebe.signum() == 0 ? "—"
                    : ycDebe.signum() > 0 ? "Debe " + YCControles.ycMoneda(ycDebe) : "Pagado");
        });
        ycColSaldo.setCellFactory(YCTablas.ycCeldaChip(ycV -> ycV.startsWith("Debe") ? "peligro" : ycV.equals("Pagado") ? "exito" : "neutro"));
        ycColEstado.setCellValueFactory(ycC -> new SimpleStringProperty(YCEstadoEstadia.ycTexto(ycC.getValue(), ycArrendador.ycGetHoraLimiteLlegadaHora())));
        ycColEstado.setCellFactory(YCTablas.ycCeldaChip(YCEstadoEstadia::ycClase));
        ycColCanal.setCellValueFactory(ycC -> new SimpleStringProperty(
                ycC.getValue().getYcCanal() == null ? "—" : ycC.getValue().getYcCanal().ycGetTexto()));

        YCTablas.ycConfigurar(ycTabla, Feather.MOON, "Sin estadias",
                "Registra un huesped con \"Nueva estadia\" o cambia el filtro.", this::ycMostrarDetalle);
        ycTabla.setItems(ycFiltradas);

        var ycSeleccion = ycTabla.getSelectionModel().selectedItemProperty();
        ycBtnDetalle.disableProperty().bind(ycSeleccion.isNull());
        ycBtnComprobante.disableProperty().bind(ycSeleccion.isNull());
        ycBtnWhatsapp.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycSeleccion.get() == null || ycSeleccion.get().isYcCancelada(), ycSeleccion));
        ycBtnSaldo.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycSeleccion.get() == null || !ycSeleccion.get().ycTieneSaldo(), ycSeleccion));
        ycBtnConsumo.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycSeleccion.get() == null || ycSeleccion.get().isYcCancelada(), ycSeleccion));
        ycBtnSalida.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycSeleccion.get() == null || (ycSeleccion.get().ycEstado(LocalDate.now()) != YCEstadia.YCEstado.YC_EN_CURSO
                        && !(ycSeleccion.get().getYcSalida().equals(LocalDate.now()) && !ycSeleccion.get().isYcCancelada())),
                ycSeleccion));
        ycBtnCancelar.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycSeleccion.get() == null || ycSeleccion.get().isYcCancelada()
                        || ycSeleccion.get().ycEstado(LocalDate.now()) == YCEstadia.YCEstado.YC_TERMINADA,
                ycSeleccion));
    }

    private void ycConfigurarFormulario() {
        ycCmbCuarto.setConverter(new StringConverter<>() {
            @Override
            public String toString(YCCuarto ycC) {
                if (ycC == null) {
                    return "";
                }
                return ycC.getYcNumero() + (ycC.getYcTipoHabitacion() == null ? "" : " · " + ycC.getYcTipoHabitacion().ycGetTexto())
                        + " · S/ " + ycC.getYcPrecio().setScale(2, RoundingMode.HALF_UP).toPlainString() + " /noche"
                        + (ycC.getYcCapacidad() == null ? "" : " · " + ycC.getYcCapacidad() + " pers.");
            }

            @Override
            public YCCuarto fromString(String ycS) {
                return null;
            }
        });
        ycCmbCuarto.valueProperty().addListener((ycObs, ycA, ycC) -> {
            YCFormularios.ycLimpiarError(ycCmbCuarto);
            if (ycC != null) {
                ycTxtPrecio.setText(ycC.getYcPrecio().setScale(2, RoundingMode.HALF_UP).toPlainString());
                StringBuilder ycInfo = new StringBuilder("Check-in " + ycC.ycGetHoraEntradaODefecto()
                        + " · Check-out " + ycC.ycGetHoraSalidaODefecto());
                if (ycC.ycGetCamasTexto() != null) ycInfo.append(" · ").append(ycC.ycGetCamasTexto());
                if (ycC.getYcCapacidad() != null) ycInfo.append(" · max. ").append(ycC.getYcCapacidad())
                        .append(ycC.getYcCapacidad() == 1 ? " persona" : " personas");
                ycLblInfoHabitacion.setText(ycInfo.toString());
                ycLblInfoHabitacion.setGraphic(new FontIcon(Feather.INFO));
            }
            ycLblInfoHabitacion.setVisible(ycC != null);
            ycLblInfoHabitacion.setManaged(ycC != null);
            ycActualizarTotal();
        });
        ycCmbMetodo.getItems().setAll(YCMetodoPago.values());
        ycCmbCanal.getItems().setAll(YCCanalReserva.values());
        ycCmbCanal.valueProperty().addListener((ycObs, ycA, ycB) -> ycActualizarAyudaCanal());
        ycDpEntrada.valueProperty().addListener((ycObs, ycA, ycB) -> ycActualizarAyudaCanal());

        for (DatePicker ycDp : List.of(ycDpEntrada, ycDpSalida)) {
            ycDp.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate ycFecha) {
                    return YCControles.ycFecha(ycFecha);
                }

                @Override
                public LocalDate fromString(String ycTexto) {
                    try {
                        return ycTexto == null || ycTexto.isBlank() ? null : LocalDate.parse(ycTexto.trim(), YCControles.YC_FECHA);
                    } catch (Exception ycE) {
                        return null;
                    }
                }
            });
            ycDp.setPromptText("dd/mm/aaaa");
        }

        ycDpEntrada.valueProperty().addListener((ycObs, ycA, ycB) -> {
            YCFormularios.ycLimpiarError(ycDpEntrada);
            ycSalidaDesdeNoches();
        });
        ycTxtNoches.textProperty().addListener((ycObs, ycA, ycB) -> ycSalidaDesdeNoches());
        ycDpSalida.valueProperty().addListener((ycObs, ycA, ycB) -> {
            YCFormularios.ycLimpiarError(ycDpSalida);
            if (ycSincronizando) {
                return;
            }
            ycSincronizando = true;
            long ycN = YCEstadiaService.ycNoches(ycDpEntrada.getValue(), ycB);
            ycTxtNoches.setText(ycN > 0 ? String.valueOf(ycN) : "");
            ycSincronizando = false;
            ycActualizarTotal();
        });

        YCFormularios.ycSoloDigitos(ycTxtNoches, 2);
        YCFormularios.ycSoloDigitos(ycTxtPersonas, 2);
        YCFormularios.ycSoloDigitos(ycTxtTelefono, 9);
        ycTxtDocumento.setTextFormatter(new TextFormatter<>(ycCambio ->
                ycCambio.getControlNewText().matches("[A-Za-z0-9]{0,20}") ? ycCambio : null));
        YCFormularios.ycSoloDecimal(ycTxtPrecio);
        ycTxtPrecio.editableProperty().bind(ycSesion.ycAdminProperty());
        YCFormularios.ycLimpiarAlEditar(ycTxtHuesped, ycTxtPrecio);
        ycTxtPrecio.textProperty().addListener((ycObs, ycA, ycB) -> ycActualizarTotal());
        YCFormularios.ycSoloDecimal(ycTxtAdelanto);
        YCFormularios.ycLimpiarAlEditar(ycTxtAdelanto);
        ycTxtAdelanto.textProperty().addListener((ycObs, ycA, ycB) -> ycActualizarTotal());
        ycTxtDocumento.textProperty().addListener((ycObs, ycA, ycB) -> ycBuscarFrecuente(ycB));
        ycAutocompletarDni.ycConectar(ycTxtDocumento, ycLblDniEstado, ycP -> {
            if (ycTxtHuesped.getText().isBlank() || ycTxtHuesped.getText().equals(ycHuespedAuto)) {
                ycTxtHuesped.setText(ycP.ycNombreCompleto());
                ycHuespedAuto = ycP.ycNombreCompleto();
            }
            if (ycP.ycTelefono() != null && (ycTxtTelefono.getText().isBlank() || ycTxtTelefono.getText().equals(ycTelefonoAuto))) {
                ycTxtTelefono.setText(ycP.ycTelefono());
                ycTelefonoAuto = ycP.ycTelefono();
            }
        });
    }

    private void ycSalidaDesdeNoches() {
        if (ycSincronizando) {
            return;
        }
        ycSincronizando = true;
        int ycN = ycEntero(ycTxtNoches, 0);
        if (ycDpEntrada.getValue() != null && ycN > 0) {
            ycDpSalida.setValue(ycDpEntrada.getValue().plusDays(ycN));
        }
        ycSincronizando = false;
        ycActualizarTotal();
    }

    private void ycActualizarTotal() {
        BigDecimal ycPrecio = YCControles.ycImporte(ycTxtPrecio.getText());
        long ycNoches = YCEstadiaService.ycNoches(ycDpEntrada.getValue(), ycDpSalida.getValue());
        if (ycPrecio == null || ycNoches <= 0) {
            ycLblSaldoForm.setVisible(false);
            ycLblSaldoForm.setManaged(false);
            ycLblDetalleTotal.setText("Elige habitacion y fechas");
            ycLblTotal.setText(YCControles.ycMoneda(BigDecimal.ZERO));
            return;
        }
        BigDecimal ycTotal = ycTotalFormulario();
        long ycHabitaciones = 1 + ycExtras.stream().filter(ycF -> ycF.ycCuarto().getValue() != null).count();
        ycLblDetalleTotal.setText(ycHabitaciones > 1
                ? ycHabitaciones + " habitaciones · " + ycNoches + (ycNoches == 1 ? " noche" : " noches") + " · paga una sola persona"
                : ycNoches + (ycNoches == 1 ? " noche" : " noches") + " × " + YCControles.ycMoneda(ycPrecio));
        ycLblTotal.setText(YCControles.ycMoneda(ycTotal));
        BigDecimal ycAdelanto = YCControles.ycImporte(ycTxtAdelanto.getText());
        boolean ycConSaldo = ycAdelanto != null && ycAdelanto.compareTo(ycTotal) < 0;
        ycLblSaldoForm.setVisible(ycConSaldo);
        ycLblSaldoForm.setManaged(ycConSaldo);
        if (ycConSaldo) {
            ycLblSaldoForm.setText("Paga ahora " + YCControles.ycMoneda(ycAdelanto) + " · saldo al llegar "
                    + YCControles.ycMoneda(ycTotal.subtract(ycAdelanto)));
        }
    }

    void ycEditarEstadia(YCEstadia ycE) {
        ycOnNueva();
        ycIdEditando = ycE.getYcId();
        ycLblTituloForm.setText("Editar estadia " + ycE.ycGetNumero());
        ycLblSubtituloForm.setText("Corrige datos, fechas o habitacion. Lo cobrado no cambia.");
        ycBtnGuardarForm.setText("Guardar cambios");
        ycCmbCuarto.getItems().stream().filter(ycC -> ycC.getYcId().equals(ycE.getYcCuarto().getYcId())).findFirst()
                .ifPresent(ycCmbCuarto::setValue);
        ycSincronizando = true;
        ycDpEntrada.setValue(ycE.getYcEntrada());
        ycDpSalida.setValue(ycE.getYcSalida());
        ycTxtNoches.setText(String.valueOf(ycE.getYcNoches()));
        ycSincronizando = false;
        ycTxtDocumento.setText(ycE.getYcDocumento() == null ? "" : ycE.getYcDocumento());
        ycTxtHuesped.setText(ycE.getYcHuesped());
        ycTxtTelefono.setText(ycE.getYcTelefono() == null ? "" : ycE.getYcTelefono());
        ycTxtPersonas.setText(String.valueOf(ycE.getYcPersonas()));
        ycTxtPrecio.setText(ycE.getYcPrecioNoche().setScale(2, RoundingMode.HALF_UP).toPlainString());
        ycTxtNota.setText(ycE.getYcNota() == null ? "" : ycE.getYcNota());
        if (ycE.getYcCanal() != null) {
            ycCmbCanal.setValue(ycE.getYcCanal());
        }
        ycTxtAdelanto.setText(ycE.ycGetPagado().setScale(2, RoundingMode.HALF_UP).toPlainString());
        ycTxtAdelanto.setDisable(true);
        ycCmbMetodo.setValue(ycE.getYcMetodo());
        ycCmbMetodo.setDisable(true);
        ycBtnAgregarHabitacion.setVisible(false);
        ycBtnAgregarHabitacion.setManaged(false);
        if (ycE.ycIsDeGrupo()) {
            ycLblSubtituloForm.setText("Habitacion de una reserva de grupo. Lo cobrado no cambia.");
        }
        ycActualizarTotal();
        Platform.runLater(ycTxtHuesped::requestFocus);
    }

    private void ycGuardarEdicion(BigDecimal ycPrecio) {
        try {
            YCEstadia ycE = ycService.ycEditar(ycIdEditando, new YCEstadiaService.YCEdicion(ycCmbCuarto.getValue().getYcId(),
                    ycTxtHuesped.getText(), ycTxtDocumento.getText(), ycTxtTelefono.getText(), ycEntero(ycTxtPersonas, 1),
                    ycDpEntrada.getValue(), ycDpSalida.getValue(), ycPrecio, ycTxtNota.getText(), ycCmbCanal.getValue()));
            ycNotificador.ycCerrarPanel();
            ycNotificador.ycExito("Estadia " + ycE.ycGetNumero() + " actualizada: " + ycE.getYcCuartoNumero() + " del "
                    + YCControles.ycFecha(ycE.getYcEntrada()) + " al " + YCControles.ycFecha(ycE.getYcSalida()) + " · total "
                    + YCControles.ycMoneda(ycE.getYcTotal()) + (ycE.ycTieneSaldo() ? " · debe " + YCControles.ycMoneda(ycE.ycGetSaldo()) : "") + ".");
            ycCargar();
            ycTabla.getSelectionModel().select(ycE);
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        }
    }

    private void ycLimpiarFormulario() {
        ycIdEditando = null;
        ycLblTituloForm.setText("Nueva estadia");
        ycLblSubtituloForm.setText("Registra al huesped y cobra sus noches");
        ycBtnGuardarForm.setText("Registrar y cobrar");
        ycTxtAdelanto.setDisable(false);
        ycCmbMetodo.setDisable(false);
        ycExtras.clear();
        ycListaExtras.getChildren().clear();
        ycListaExtras.setVisible(false);
        ycListaExtras.setManaged(false);
        ycLblPersonas.setText("Personas");
        ycBtnAgregarHabitacion.setVisible(true);
        ycBtnAgregarHabitacion.setManaged(true);
        YCFormularios.ycLimpiarErrores(ycCmbCuarto, ycDpEntrada, ycDpSalida, ycTxtHuesped, ycTxtPrecio);
        ycSincronizando = true;
        ycCmbCuarto.setValue(null);
        ycDpEntrada.setValue(LocalDate.now());
        ycDpSalida.setValue(LocalDate.now().plusDays(1));
        ycTxtNoches.setText("1");
        ycSincronizando = false;
        ycTxtHuesped.clear();
        ycTxtDocumento.clear();
        ycTxtTelefono.clear();
        ycTxtPersonas.setText("1");
        ycTxtPrecio.clear();
        ycTxtNota.clear();
        ycTxtAdelanto.clear();
        ycHuespedAuto = "";
        ycTelefonoAuto = "";
        com.alquilafacil.ui.YCAutocompletarDni.ycOcultar(ycLblDniEstado);
        ycLblFrecuente.setVisible(false);
        ycLblFrecuente.setManaged(false);
        ycCmbMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycCmbCanal.setValue(YCCanalReserva.YC_EN_PERSONA);
        ycActualizarTotal();
        ycActualizarAyudaCanal();
    }

    private void ycActualizarAyudaCanal() {
        LocalDate ycEntrada = ycDpEntrada.getValue();
        boolean ycAqui = ycCmbCanal.getValue() == YCCanalReserva.YC_EN_PERSONA && ycEntrada != null
                && !ycEntrada.isAfter(LocalDate.now());
        ycLblAyudaCanal.setText(ycAqui
                ? "Esta aqui en persona: queda registrado como que ya llego (check-in)."
                : "Quedara \"por llegar\": cuando llegue, marca \"Llego\". Si a las "
                + ycArrendador.ycGetHoraLimiteLlegadaHora() + " del dia de entrada no ha llegado, te avisara.");
    }

    private void ycBuscarFrecuente(String ycDocumento) {
        var ycEncontrado = ycDocumento == null || ycDocumento.trim().length() < 6
                ? java.util.Optional.<YCEstadiaService.YCHuespedFrecuente>empty()
                : ycService.ycHuespedPorDocumento(ycDocumento);
        ycLblFrecuente.setVisible(ycEncontrado.isPresent());
        ycLblFrecuente.setManaged(ycEncontrado.isPresent());
        ycEncontrado.ifPresent(ycH -> {
            YCEstadia ycU = ycH.ycUltima();
            ycLblFrecuente.setText("Cliente frecuente: " + ycH.ycVisitas() + (ycH.ycVisitas() == 1 ? " estadia" : " estadias")
                    + " · la ultima en " + ycU.getYcCuartoNumero() + " (" + YCControles.ycFecha(ycU.getYcEntrada()) + ")");
            ycLblFrecuente.setGraphic(new FontIcon(Feather.STAR));
            if (!ycDocumento.trim().matches("[0-9]{8}")) {
                if (ycTxtHuesped.getText().isBlank()) {
                    ycTxtHuesped.setText(ycU.getYcHuesped());
                }
                if (ycTxtTelefono.getText().isBlank() && ycU.getYcTelefono() != null) {
                    ycTxtTelefono.setText(ycU.getYcTelefono());
                }
            }
        });
    }

    private static String ycNombres(List<YCEstadia> ycEstadias) {
        String ycTexto = String.join(", ", ycEstadias.stream()
                .map(ycE -> ycE.getYcHuesped().split(" ")[0] + " (" + ycE.getYcCuartoNumero().replace("Habitación ", "") + ")")
                .toList());
        return ycTexto.length() > 42 ? ycTexto.substring(0, 40) + "…" : ycTexto;
    }

    private static int ycEntero(TextField ycCampo, int ycPorDefecto) {
        try {
            return ycCampo.getText().isBlank() ? ycPorDefecto : Integer.parseInt(ycCampo.getText().trim());
        } catch (NumberFormatException ycE) {
            return ycPorDefecto;
        }
    }
}
