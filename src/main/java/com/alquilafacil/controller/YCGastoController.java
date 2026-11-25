package com.alquilafacil.controller;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCategoriaGasto;
import com.alquilafacil.model.YCGasto;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCNegocio;
import com.alquilafacil.service.YCCalendarioPagos;
import com.alquilafacil.service.YCGastoService;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCGastoController implements YCRefrescable {
    private final YCGastoService ycService;
    private final YCNotificador ycNotificador;
    private final YCSesion ycSesion;
    private final com.alquilafacil.ui.YCExportador ycExportador;

    @FXML private VBox ycPanelFormulario;
    @FXML private Label ycLblTituloFormulario;
    @FXML private TextField ycTxtMonto;
    @FXML private DatePicker ycDpFecha;
    @FXML private ComboBox<YCCategoriaGasto> ycCmbCategoria;
    @FXML private TextField ycTxtDescripcion;
    @FXML private ToggleGroup ycGrupoNegocio;
    @FXML private ToggleButton ycNegMensual;
    @FXML private ToggleButton ycNegHospedaje;
    @FXML private ToggleButton ycNegGeneral;
    @FXML private ComboBox<YCMetodoPago> ycCmbMetodo;

    @FXML private Label ycLblSubtitulo;
    @FXML private ComboBox<YearMonth> ycCmbMes;
    @FXML private Label ycKpiIngresos;
    @FXML private Label ycKpiIngresosDetalle;
    @FXML private Label ycKpiGastos;
    @FXML private Label ycKpiGastosDetalle;
    @FXML private Label ycKpiGanancia;
    @FXML private Label ycKpiGananciaDetalle;
    @FXML private Label ycKpiMargen;
    @FXML private VBox ycPanelNegocios;
    @FXML private VBox ycPanelCategorias;
    @FXML private Label ycLblLista;
    @FXML private Button ycBtnEditar;
    @FXML private Button ycBtnEliminar;

    @FXML private TableView<YCGasto> ycTabla;
    @FXML private TableColumn<YCGasto, String> ycColFecha;
    @FXML private TableColumn<YCGasto, String> ycColCategoria;
    @FXML private TableColumn<YCGasto, String> ycColNegocio;
    @FXML private TableColumn<YCGasto, String> ycColDescripcion;
    @FXML private TableColumn<YCGasto, String> ycColMetodo;
    @FXML private TableColumn<YCGasto, BigDecimal> ycColMonto;
    @FXML private TableColumn<YCGasto, String> ycColUsuario;

    private final ObservableList<YCGasto> ycDatos = FXCollections.observableArrayList();
    private Long ycIdEditando;

    @FXML
    public void initialize() {
        YearMonth ycActual = YearMonth.now();
        for (int ycI = 0; ycI < 24; ycI++) {
            ycCmbMes.getItems().add(ycActual.minusMonths(ycI));
        }
        ycCmbMes.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth ycM) {
                return ycM == null ? "" : YCCalendarioPagos.ycTexto(ycM);
            }

            @Override
            public YearMonth fromString(String ycS) {
                return null;
            }
        });
        ycCmbMes.setValue(ycActual);
        ycCmbMes.valueProperty().addListener((ycO, ycA, ycB) -> ycCargar());

        ycCmbCategoria.getItems().setAll(YCCategoriaGasto.values());
        ycCmbMetodo.getItems().setAll(YCMetodoPago.values());
        ycGrupoNegocio.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual2) -> {
            if (ycActual2 == null) {
                ycGrupoNegocio.selectToggle(ycAnterior);
            }
        });
        ycDpFecha.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate ycF) {
                return YCControles.ycFecha(ycF);
            }

            @Override
            public LocalDate fromString(String ycT) {
                try {
                    return ycT == null || ycT.isBlank() ? null : LocalDate.parse(ycT.trim(), YCControles.YC_FECHA);
                } catch (Exception ycE) {
                    return null;
                }
            }
        });
        YCFormularios.ycSoloDecimal(ycTxtMonto);
        YCFormularios.ycLimpiarAlEditar(ycTxtMonto, ycTxtDescripcion);
        ycCmbCategoria.valueProperty().addListener((ycO, ycA, ycB) -> YCFormularios.ycLimpiarError(ycCmbCategoria));
        ycConfigurarTabla();
    }

    @Override
    public void ycRefrescar() {
        boolean ycAdmin = ycSesion.ycEsAdmin();
        for (Button ycB : List.of(ycBtnEditar, ycBtnEliminar)) {
            ycB.setVisible(ycAdmin);
            ycB.setManaged(ycAdmin);
        }
        ycCargar();
    }

    @FXML
    private void ycOnNuevo() {
        ycLimpiar();
        ycIdEditando = null;
        ycLblTituloFormulario.setText("Nuevo gasto");
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(ycTxtMonto::requestFocus);
    }

    @FXML
    private void ycOnEditar() {
        YCGasto ycG = ycTabla.getSelectionModel().getSelectedItem();
        if (ycG == null) {
            return;
        }
        ycLimpiar();
        ycIdEditando = ycG.getYcId();
        ycTxtMonto.setText(ycG.getYcMonto().toPlainString());
        ycDpFecha.setValue(ycG.getYcFecha());
        ycCmbCategoria.setValue(ycG.getYcCategoria());
        ycTxtDescripcion.setText(ycG.getYcDescripcion() == null ? "" : ycG.getYcDescripcion());
        ycCmbMetodo.setValue(ycG.getYcMetodo());
        (switch (ycG.getYcNegocio()) {
            case YC_MENSUAL -> ycNegMensual;
            case YC_HOSPEDAJE -> ycNegHospedaje;
            case YC_GENERAL -> ycNegGeneral;
        }).setSelected(true);
        ycLblTituloFormulario.setText("Editar gasto");
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
    }

    @FXML
    private void ycOnExcel() {
        YearMonth ycMes = ycCmbMes.getValue();
        com.alquilafacil.service.YCExcel.YCHoja ycH = new com.alquilafacil.service.YCExcel.YCHoja("Gastos")
                .ycTitulo("Gastos de " + YCCalendarioPagos.ycTexto(ycMes))
                .ycTitulo(ycDatos.size() + (ycDatos.size() == 1 ? " gasto" : " gastos"))
                .ycColumna("Fecha", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 11).ycColumna("Categoria", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 22)
                .ycColumna("Negocio", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 18).ycColumna("Descripcion", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 34)
                .ycColumna("Pagado con", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 13).ycColumna("Monto", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Registro", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11);
        BigDecimal ycTotal = BigDecimal.ZERO;
        for (YCGasto ycG : ycDatos) {
            ycH.ycFila(ycG.getYcFecha(), ycG.getYcCategoria().ycGetTexto(), ycG.getYcNegocio().ycGetTexto(), ycG.getYcDescripcion(),
                    ycG.getYcMetodo() == null ? null : ycG.getYcMetodo().ycGetDescripcion(), ycG.getYcMonto(), ycG.getYcRegistradoPor());
            ycTotal = ycTotal.add(ycG.getYcMonto());
        }
        ycH.ycTotales("Total", null, null, null, null, ycTotal, null);
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar gastos en Excel", "Gastos " + ycMes,
                com.alquilafacil.ui.YCExportador.YCFormato.YC_EXCEL, ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, List.of(ycH)));
    }

    @FXML
    private void ycOnEliminar() {
        YCGasto ycG = ycTabla.getSelectionModel().getSelectedItem();
        if (ycG == null) {
            return;
        }
        ycNotificador.ycConfirmar("¿Eliminar este gasto?",
                ycG.getYcCategoria().ycGetTexto() + " · " + YCControles.ycMoneda(ycG.getYcMonto()) + " del " + YCControles.ycFecha(ycG.getYcFecha()),
                "Eliminar", () -> {
                    try {
                        ycService.ycEliminar(ycG.getYcId());
                        ycNotificador.ycExito("Gasto eliminado.");
                        ycCargar();
                    } catch (YCNegocioException ycE) {
                        ycNotificador.ycError(ycE.getMessage());
                    }
                });
    }

    @FXML
    private void ycOnCerrarFormulario() {
        ycNotificador.ycCerrarPanel();
    }

    @FXML
    private void ycOnGuardar() {
        YCFormularios.ycLimpiarErrores(ycTxtMonto, ycDpFecha, ycCmbCategoria, ycTxtDescripcion);
        if (ycCmbCategoria.getValue() == null) {
            YCFormularios.ycMarcarError(ycCmbCategoria, "Elige la categoria.");
            return;
        }
        try {
            YCGasto ycG = ycService.ycGuardar(YCGasto.builder()
                    .ycId(ycIdEditando)
                    .ycMonto(YCControles.ycImporte(ycTxtMonto.getText()))
                    .ycFecha(ycDpFecha.getValue())
                    .ycCategoria(ycCmbCategoria.getValue())
                    .ycDescripcion(ycTxtDescripcion.getText().isBlank() ? null : ycTxtDescripcion.getText().trim())
                    .ycNegocio(ycNegMensual.isSelected() ? YCNegocio.YC_MENSUAL : ycNegHospedaje.isSelected() ? YCNegocio.YC_HOSPEDAJE : YCNegocio.YC_GENERAL)
                    .ycMetodo(ycCmbMetodo.getValue())
                    .ycRegistradoPor(ycIdEditando == null ? null : ycTabla.getSelectionModel().getSelectedItem().getYcRegistradoPor())
                    .ycFechaRegistro(ycIdEditando == null ? null : ycTabla.getSelectionModel().getSelectedItem().getYcFechaRegistro())
                    .build());
            ycNotificador.ycCerrarPanel();
            ycNotificador.ycExito("Gasto guardado: " + ycG.getYcCategoria().ycGetTexto() + " · " + YCControles.ycMoneda(ycG.getYcMonto()));
            ycCmbMes.setValue(YearMonth.from(ycG.getYcFecha()));
            ycCargar();
        } catch (ConstraintViolationException ycE) {
            YCFormularios.ycMostrarViolaciones(ycE, Map.of("ycMonto", ycTxtMonto, "ycFecha", ycDpFecha,
                    "ycCategoria", ycCmbCategoria, "ycDescripcion", ycTxtDescripcion)).forEach(ycNotificador::ycAdvertencia);
        } catch (YCNegocioException ycE) {
            ycNotificador.ycError(ycE.getMessage());
        }
    }

    private void ycCargar() {
        YearMonth ycMes = ycCmbMes.getValue();
        YCGastoService.YCResultado ycR = ycService.ycResultado(ycMes);
        List<YCGasto> ycGastos = ycService.ycDelMes(ycMes);
        ycDatos.setAll(ycGastos);

        ycLblSubtitulo.setText("Lo que entra, lo que sale y lo que te queda en " + YCCalendarioPagos.ycTexto(ycMes).toLowerCase());
        ycKpiIngresos.setText(YCControles.ycMoneda(ycR.ycIngresos()));
        ycKpiIngresosDetalle.setText("Mensual " + YCControles.ycMoneda(ycR.ycIngresosMensual()) + " · Hospedaje " + YCControles.ycMoneda(ycR.ycIngresosHospedaje()));
        ycKpiGastos.setText(YCControles.ycMoneda(ycR.ycGastos()));
        ycKpiGastosDetalle.setText(ycGastos.size() + (ycGastos.size() == 1 ? " gasto registrado" : " gastos registrados"));
        ycKpiGanancia.setText(YCControles.ycMoneda(ycR.ycGanancia()));
        ycKpiGananciaDetalle.setText(ycR.ycGanancia().signum() < 0 ? "Este mes se gasto mas de lo que entro" : "Ingresos menos gastos");
        ycKpiMargen.setText(ycR.ycMargen() + " %");
        ycLblLista.setText("Gastos de " + YCCalendarioPagos.ycTexto(ycMes).toLowerCase() + " (" + ycGastos.size() + ")");

        ycPanelNegocios.getChildren().setAll(
                ycFilaNegocio("Alquiler mensual", "fth-key", ycR.ycIngresosMensual(), ycR.ycGastosMensual(), ycR.ycGananciaMensual(), false),
                ycFilaNegocio("Hospedaje Supply", "fth-moon", ycR.ycIngresosHospedaje(), ycR.ycGastosHospedaje(), ycR.ycGananciaHospedaje(), true));
        if (ycR.ycGastosGenerales().signum() > 0) {
            Label ycGenerales = new Label("Gastos de los dos negocios (edificio, impuestos…): " + YCControles.ycMoneda(ycR.ycGastosGenerales())
                    + ". Se restan de la ganancia total.", new FontIcon(Feather.INFO));
            ycGenerales.getStyleClass().add("campo-ayuda");
            ycGenerales.setWrapText(true);
            ycPanelNegocios.getChildren().add(ycGenerales);
        }

        Map<YCCategoriaGasto, BigDecimal> ycPorCategoria = new EnumMap<>(YCCategoriaGasto.class);
        ycGastos.forEach(ycG -> ycPorCategoria.merge(ycG.getYcCategoria(), ycG.getYcMonto(), BigDecimal::add));
        ycPanelCategorias.getChildren().clear();
        if (ycPorCategoria.isEmpty()) {
            Label ycVacio = new Label("Aun no hay gastos este mes. Registralos con \"Nuevo gasto\" para ver tu ganancia real.");
            ycVacio.getStyleClass().add("vacio-texto");
            ycVacio.setWrapText(true);
            ycPanelCategorias.getChildren().add(ycVacio);
        }
        BigDecimal ycTotal = ycR.ycGastos().signum() == 0 ? BigDecimal.ONE : ycR.ycGastos();
        ycPorCategoria.entrySet().stream()
                .sorted(Map.Entry.<YCCategoriaGasto, BigDecimal>comparingByValue().reversed())
                .forEach(ycE -> {
                    Label ycNombre = new Label(ycE.getKey().ycGetTexto(), new FontIcon(ycE.getKey().ycGetIcono()));
                    ycNombre.getStyleClass().add("gasto-categoria");
                    Region ycEsp = new Region();
                    HBox.setHgrow(ycEsp, Priority.ALWAYS);
                    Label ycMonto = new Label(YCControles.ycMoneda(ycE.getValue()));
                    ycMonto.getStyleClass().add("ficha-dato-valor");
                    HBox ycCab = new HBox(8, ycNombre, ycEsp, ycMonto);
                    ycCab.setAlignment(Pos.CENTER_LEFT);
                    ProgressBar ycBarra = new ProgressBar(ycE.getValue().divide(ycTotal, 4, RoundingMode.HALF_UP).doubleValue());
                    ycBarra.getStyleClass().add("barra-fina");
                    ycBarra.setMaxWidth(Double.MAX_VALUE);
                    ycPanelCategorias.getChildren().add(new VBox(4, ycCab, ycBarra));
                });
        ycTabla.refresh();
    }

    private Node ycFilaNegocio(String ycTitulo, String ycIcono, BigDecimal ycIngresos, BigDecimal ycGastos, BigDecimal ycGanancia, boolean ycHosp) {
        Label ycNombre = new Label(ycTitulo, new FontIcon(ycIcono));
        ycNombre.getStyleClass().addAll("negocio-titulo", ycHosp ? "hosp" : "mensual");
        HBox ycNumeros = new HBox(24,
                ycCifra("ENTRO", ycIngresos, "positivo"),
                ycCifra("SALIO", ycGastos, "negativo"),
                ycCifra("TE QUEDA", ycGanancia, ycGanancia.signum() < 0 ? "negativo" : "ganancia"));
        VBox ycCaja = new VBox(8, ycNombre, ycNumeros);
        ycCaja.getStyleClass().add("negocio-fila");
        return ycCaja;
    }

    private static Node ycCifra(String ycEtiqueta, BigDecimal ycValor, String ycClase) {
        Label ycEt = new Label(ycEtiqueta);
        ycEt.getStyleClass().add("ficha-lateral-etiqueta");
        Label ycV = new Label(YCControles.ycMoneda(ycValor));
        ycV.getStyleClass().addAll("negocio-cifra", ycClase);
        return new VBox(2, ycEt, ycV);
    }

    private void ycConfigurarTabla() {
        ycColFecha.setCellValueFactory(ycC -> new SimpleStringProperty(YCControles.ycFecha(ycC.getValue().getYcFecha())));
        ycColCategoria.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcCategoria().ycGetTexto()));
        ycColCategoria.setCellFactory(YCTablas.ycCeldaChip("—"));
        ycColNegocio.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcNegocio().ycGetTexto()));
        ycColDescripcion.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcDescripcion()));
        ycColDescripcion.setCellFactory(YCTablas.ycCeldaOpcional());
        ycColMetodo.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcMetodo() == null ? "" : ycC.getValue().getYcMetodo().toString()));
        ycColMetodo.setCellFactory(YCTablas.ycCeldaOpcional());
        ycColMonto.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().getYcMonto()));
        ycColMonto.setCellFactory(YCTablas.ycCeldaMoneda());
        ycColUsuario.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcRegistradoPor()));
        ycColUsuario.setCellFactory(YCTablas.ycCeldaOpcional());
        YCTablas.ycConfigurar(ycTabla, Feather.TRENDING_DOWN, "Sin gastos este mes",
                "Registra lo que pagas (luz del edificio, limpieza, arreglos…) con \"Nuevo gasto\".", ycG -> {
                    if (ycSesion.ycEsAdmin()) {
                        ycOnEditar();
                    }
                });
        ycTabla.setItems(ycDatos);
        var ycSel = ycTabla.getSelectionModel().selectedItemProperty();
        ycBtnEditar.disableProperty().bind(ycSel.isNull());
        ycBtnEliminar.disableProperty().bind(ycSel.isNull());
    }

    private void ycLimpiar() {
        YCFormularios.ycLimpiarErrores(ycTxtMonto, ycDpFecha, ycCmbCategoria, ycTxtDescripcion);
        ycTxtMonto.clear();
        ycTxtDescripcion.clear();
        ycDpFecha.setValue(LocalDate.now());
        ycCmbCategoria.setValue(null);
        ycCmbMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycNegGeneral.setSelected(true);
    }
}
