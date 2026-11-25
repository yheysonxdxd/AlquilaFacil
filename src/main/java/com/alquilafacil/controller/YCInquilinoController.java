package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.service.YCCuartoService;
import com.alquilafacil.service.YCDeudaPendiente;
import com.alquilafacil.service.YCInquilinoService;
import com.alquilafacil.service.YCPagoService;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCPaginador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCInquilinoController implements YCRefrescable {
    private final YCInquilinoService ycService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCCuartoService ycCuartoService;
    private final YCPagoService ycPagoService;
    private final YCNotificador ycNotificador;
    private final com.alquilafacil.ui.YCAutocompletarDni ycAutocompletarDni;
    private final com.alquilafacil.service.YCSalidaInquilinoService ycSalidaService;
    private final com.alquilafacil.service.YCLiquidacionPdfService ycLiquidacionPdf;
    private final com.alquilafacil.ui.YCExportador ycExportador;
    private final com.alquilafacil.service.YCContratoPdfService ycContratoPdf;

    static final int YC_AVISO_CONTRATO_DIAS = 30;

    @FXML private VBox ycPanelFormulario;
    @FXML private Label ycLblTituloFormulario;
    @FXML private Label ycLblSubtituloFormulario;
    @FXML private TextField ycTxtDni;
    @FXML private Label ycLblDniEstado;
    private String ycNombreAuto = "";
    private String ycApellidoAuto = "";
    private String ycTelefonoAuto = "";
    @FXML private TextField ycTxtNombre;
    @FXML private TextField ycTxtApellido;
    @FXML private TextField ycTxtTelefono;
    @FXML private ComboBox<YCCuarto> ycCmbCuarto;
    @FXML private DatePicker ycDpIngreso;
    @FXML private TextField ycTxtDiaPago;
    @FXML private TextField ycTxtGarantia;
    @FXML private TextField ycTxtRenta;
    @FXML private DatePicker ycDpFinContrato;

    @FXML private TextField ycTxtBuscar;
    @FXML private CheckBox ycChkRetirados;
    @FXML private TableView<YCInquilino> ycTabla;
    @FXML private TableColumn<YCInquilino, String> ycColNombre;
    @FXML private TableColumn<YCInquilino, String> ycColDni;
    @FXML private TableColumn<YCInquilino, String> ycColTelefono;
    @FXML private TableColumn<YCInquilino, String> ycColCuarto;
    @FXML private TableColumn<YCInquilino, String> ycColIngreso;
    @FXML private TableColumn<YCInquilino, String> ycColDiaPago;
    @FXML private TableColumn<YCInquilino, java.math.BigDecimal> ycColRenta;
    @FXML private TableColumn<YCInquilino, String> ycColEstado;
    @FXML private TableColumn<YCInquilino, String> ycColContrato;

    @FXML private Button ycBtnEditar;
    @FXML private Button ycBtnBaja;
    @FXML private Button ycBtnContrato;
    @FXML private Button ycBtnEliminar;
    @FXML private YCPaginador ycPaginador;

    private final ObservableList<YCInquilino> ycDatos = FXCollections.observableArrayList();
    private Long ycIdEditando;

    @FXML
    public void initialize() {
        ycColNombre.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycGetNombreCompleto()));
        ycColNombre.setCellFactory(YCTablas.ycCeldaAvatar());
        ycColDni.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcDni()));
        ycColTelefono.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcTelefono()));
        ycColTelefono.setCellFactory(YCTablas.ycCeldaOpcional());
        ycColCuarto.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcCuarto().getYcNumero()));
        ycColCuarto.setCellFactory(YCTablas.ycCeldaChip("—"));
        ycColIngreso.setCellValueFactory(ycC -> new SimpleStringProperty(YCControles.ycFecha(ycC.getValue().getYcFechaIngreso())));
        ycColDiaPago.setCellValueFactory(ycC -> new SimpleStringProperty("Dia " + ycC.getValue().getYcDiaPago()));
        ycColRenta.setCellValueFactory(ycC -> new javafx.beans.property.SimpleObjectProperty<>(ycC.getValue().ycGetRentaMensual()));
        ycColRenta.setCellFactory(YCTablas.ycCeldaMoneda());
        ycColEstado.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().isYcActivo()
                ? "Activo" : "Retirado " + YCControles.ycFecha(ycC.getValue().getYcFechaSalida())));
        ycColEstado.setCellFactory(YCTablas.ycCeldaChip(ycV -> "Activo".equals(ycV) ? "exito" : "neutro"));
        ycColContrato.setCellValueFactory(ycC -> new SimpleStringProperty(ycTextoContrato(ycC.getValue())));
        ycColContrato.setCellFactory(YCTablas.ycCeldaChip(ycV -> ycV.startsWith("Vencio") ? "peligro"
                : ycV.startsWith("Vence") ? "advertencia" : "neutro"));

        YCTablas.ycConfigurar(ycTabla, Feather.USERS, "Sin inquilinos",
                "Registra un inquilino o cambia el texto de busqueda.", this::ycEditar);
        ycTabla.setItems(ycDatos);

        var ycSeleccion = ycTabla.getSelectionModel().selectedItemProperty();
        ycBtnEditar.disableProperty().bind(ycSeleccion.isNull());
        ycBtnEliminar.disableProperty().bind(ycSeleccion.isNull());
        ycBtnBaja.disableProperty().bind(ycSeleccion.isNull());
        ycBtnContrato.disableProperty().bind(ycSeleccion.isNull());

        ycPaginador.ycSetOnCambio(this::ycCargarTabla);
        ycTxtBuscar.textProperty().addListener((ycObs, ycA, ycB) -> ycPaginador.ycIrAPrimera());
        ycChkRetirados.selectedProperty().addListener((ycObs, ycA, ycB) -> ycPaginador.ycIrAPrimera());

        ycCmbCuarto.setConverter(new StringConverter<>() {
            @Override
            public String toString(YCCuarto ycC) {
                return ycC == null ? "" : ycC.ycGetEtiqueta();
            }

            @Override
            public YCCuarto fromString(String ycS) {
                return null;
            }
        });
        ycDpIngreso.setConverter(new StringConverter<>() {
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
        ycDpIngreso.setPromptText("dd/mm/aaaa");
        ycDpFinContrato.setConverter(YCControles.ycConvertidorFecha());
        ycDpFinContrato.setPromptText("dd/mm/aaaa (opcional)");
        ycDpIngreso.valueProperty().addListener((ycObs, ycA, ycB) -> {
            if (ycIdEditando == null && ycB != null && (ycDpFinContrato.getValue() == null
                    || (ycA != null && ycDpFinContrato.getValue().equals(ycA.plusYears(1))))) {
                ycDpFinContrato.setValue(ycB.plusYears(1));
            }
        });
        ycDpFinContrato.valueProperty().addListener((ycObs, ycA, ycB) -> YCFormularios.ycLimpiarError(ycDpFinContrato));

        YCFormularios.ycSoloDigitos(ycTxtDni, 8);
        ycAutocompletarDni.ycConectar(ycTxtDni, ycLblDniEstado, ycP -> {
            if (ycTxtNombre.getText().isBlank() || ycTxtNombre.getText().equals(ycNombreAuto)) {
                ycTxtNombre.setText(ycP.ycNombres());
                ycNombreAuto = ycP.ycNombres();
            }
            if (ycTxtApellido.getText().isBlank() || ycTxtApellido.getText().equals(ycApellidoAuto)) {
                ycTxtApellido.setText(ycP.ycApellidos());
                ycApellidoAuto = ycP.ycApellidos();
            }
            if (ycP.ycTelefono() != null && (ycTxtTelefono.getText().isBlank() || ycTxtTelefono.getText().equals(ycTelefonoAuto))) {
                ycTxtTelefono.setText(ycP.ycTelefono());
                ycTelefonoAuto = ycP.ycTelefono();
            }
        });
        YCFormularios.ycSoloDigitos(ycTxtTelefono, 9);
        YCFormularios.ycSoloDigitos(ycTxtDiaPago, 2);
        YCFormularios.ycSoloDecimal(ycTxtGarantia);
        YCFormularios.ycLimpiarAlEditar(ycTxtNombre, ycTxtApellido, ycTxtDni, ycTxtTelefono, ycTxtDiaPago, ycTxtGarantia);
        ycCmbCuarto.valueProperty().addListener((ycObs, ycA, ycB) -> {
            YCFormularios.ycLimpiarError(ycCmbCuarto);
            String ycAnterior = ycA == null ? "" : ycA.getYcPrecio().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            if (ycB != null && (ycTxtRenta.getText().isBlank() || ycTxtRenta.getText().equals(ycAnterior))) {
                ycTxtRenta.setText(ycB.getYcPrecio().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
        });
        YCFormularios.ycSoloDecimal(ycTxtRenta);
        YCFormularios.ycLimpiarAlEditar(ycTxtRenta);
        ycDpIngreso.valueProperty().addListener((ycObs, ycA, ycB) -> YCFormularios.ycLimpiarError(ycDpIngreso));
            ycSesion.ycSoloAdmin(ycBtnEliminar);
    }

    @Override
    public void ycRefrescar() {
        ycCargarTabla();
    }

    @FXML
    private void ycOnNuevo() {
        ycTabla.getSelectionModel().clearSelection();
        ycLimpiarFormulario();
        ycIdEditando = null;
        ycCmbCuarto.setItems(FXCollections.observableArrayList(ycCuartoService.ycLibres()));
        if (ycCmbCuarto.getItems().isEmpty()) {
            ycNotificador.ycAdvertencia("No hay cuartos libres. Registra un cuarto o da de baja a un inquilino.");
        }
        ycDpFinContrato.setValue(null);
        ycDpIngreso.setValue(LocalDate.now());
        ycDpFinContrato.setValue(LocalDate.now().plusYears(1));
        ycTxtDiaPago.setText(String.valueOf(LocalDate.now().getDayOfMonth()));
        ycLblTituloFormulario.setText("Nuevo inquilino");
        ycLblSubtituloFormulario.setText("Completa los datos para registrarlo");
        ycAbrirFormulario();
    }

    public void ycNuevoPara(YCCuarto ycCuarto) {
        ycOnNuevo();
        ycCmbCuarto.getItems().stream()
                .filter(ycC -> ycC.getYcId().equals(ycCuarto.getYcId()))
                .findFirst()
                .ifPresent(ycC -> {
                    ycCmbCuarto.setValue(ycC);
                    if (ycTxtGarantia.getText().isBlank()) {
                        ycTxtGarantia.setText(ycC.getYcPrecio().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    }
                    ycLblSubtituloFormulario.setText("Alquiler de " + ycC.getYcNumero() + " · " + YCControles.ycMoneda(ycC.getYcPrecio()) + " al mes");
                });
        javafx.application.Platform.runLater(ycTxtDni::requestFocus);
    }

    @FXML
    private void ycOnEditar() {
        ycEditar(ycTabla.getSelectionModel().getSelectedItem());
    }

    private void ycEditar(YCInquilino ycI) {
        if (ycI == null) {
            return;
        }
        if (!ycI.isYcActivo()) {
            ycNotificador.ycInfo(ycI.ycGetNombreCompleto() + " ya se retiro; su registro queda solo como historial.");
            return;
        }
        ycLimpiarFormulario();
        ycIdEditando = ycI.getYcId();
        ycTxtDni.setText(ycI.getYcDni());
        ycTxtNombre.setText(ycI.getYcNombre());
        ycTxtApellido.setText(ycI.getYcApellido());
        com.alquilafacil.ui.YCAutocompletarDni.ycOcultar(ycLblDniEstado);
        ycTxtTelefono.setText(ycI.getYcTelefono() == null ? "" : ycI.getYcTelefono());
        ycCmbCuarto.setItems(FXCollections.observableArrayList(ycCuartoService.ycDisponiblesPara(ycI.getYcCuarto())));
        ycCmbCuarto.setValue(ycI.getYcCuarto());
        ycDpIngreso.setValue(ycI.getYcFechaIngreso());
        ycDpFinContrato.setValue(ycI.getYcFechaFinContrato());
        ycTxtDiaPago.setText(String.valueOf(ycI.getYcDiaPago()));
        ycTxtGarantia.setText(ycI.getYcGarantia() == null ? "" : ycI.getYcGarantia().toPlainString());
        ycTxtRenta.setText(ycI.ycGetRentaMensual().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        ycLblTituloFormulario.setText("Editar inquilino");
        ycLblSubtituloFormulario.setText("Registro #" + ycI.getYcId() + " · " + ycI.ycGetNombreCompleto());
        ycAbrirFormulario();
    }

    @FXML
    private void ycOnCerrarFormulario() {
        ycNotificador.ycCerrarPanel();
    }

    @FXML
    private void ycOnGuardar() {
        YCFormularios.ycLimpiarErrores(ycTxtNombre, ycTxtApellido, ycTxtDni, ycTxtTelefono, ycCmbCuarto, ycDpIngreso,
                ycTxtDiaPago, ycTxtGarantia);
        YCInquilino ycInquilino = YCInquilino.builder()
                .ycId(ycIdEditando)
                .ycDni(ycTxtDni.getText().trim())
                .ycNombre(ycTxtNombre.getText().trim())
                .ycApellido(ycTxtApellido.getText().trim())
                .ycTelefono(ycTxtTelefono.getText().trim())
                .ycCuarto(ycCmbCuarto.getValue())
                .ycFechaIngreso(ycDpIngreso.getValue())
                .ycDiaPago(ycTxtDiaPago.getText().isBlank() ? null : Integer.valueOf(ycTxtDiaPago.getText().trim()))
                .ycGarantia(YCControles.ycImporteOCero(ycTxtGarantia.getText()))
                .ycFechaFinContrato(ycDpFinContrato.getValue())
                .ycRenta(YCControles.ycImporte(ycTxtRenta.getText()))
                .build();
        try {
            boolean ycEsNuevo = ycInquilino.getYcId() == null;
            YCInquilino ycGuardado = ycService.ycGuardar(ycInquilino);
            ycNotificador.ycCerrarPanel();
            ycNotificador.ycExito(ycEsNuevo
                    ? ycGuardado.ycGetNombreCompleto() + " se registro en el " + ycGuardado.getYcCuarto().getYcNumero() + "."
                    : "Los cambios de " + ycGuardado.ycGetNombreCompleto() + " se guardaron.");
            ycCargarTabla();
        } catch (ConstraintViolationException ycE) {
            List<String> ycOtros = YCFormularios.ycMostrarViolaciones(ycE, Map.of(
                    "ycNombre", ycTxtNombre, "ycApellido", ycTxtApellido, "ycDni", ycTxtDni, "ycTelefono", ycTxtTelefono,
                    "ycCuarto", ycCmbCuarto, "ycFechaIngreso", ycDpIngreso, "ycDiaPago", ycTxtDiaPago, "ycGarantia", ycTxtGarantia, "ycRenta", ycTxtRenta));
            ycOtros.forEach(ycNotificador::ycAdvertencia);
        } catch (YCNegocioException ycE) {
            String ycM = ycE.getMessage();
            if (ycM.contains("renta")) {
                YCFormularios.ycMarcarError(ycTxtRenta, ycM);
            } else if (ycM.contains("contrato")) {
                YCFormularios.ycMarcarError(ycDpFinContrato, ycM);
            } else if (ycM.contains("DNI")) {
                YCFormularios.ycMarcarError(ycTxtDni, ycM);
            } else if (ycM.contains("ocupado")) {
                YCFormularios.ycMarcarError(ycCmbCuarto, ycM);
            } else if (ycM.contains("fecha")) {
                YCFormularios.ycMarcarError(ycDpIngreso, ycM);
            } else {
                ycNotificador.ycError(ycM);
            }
        } catch (Exception ycE) {
            ycNotificador.ycError("Error inesperado: " + ycE.getMessage());
        }
    }

    @FXML
    private void ycOnExcel() {
        List<YCInquilino> ycLista = ycService.ycListar(ycTxtBuscar.getText(), ycChkRetirados.isSelected(), 0, 100_000).getContent();
        com.alquilafacil.service.YCExcel.YCHoja ycH = new com.alquilafacil.service.YCExcel.YCHoja("Inquilinos")
                .ycTitulo("Inquilinos" + (ycChkRetirados.isSelected() ? " (incluye retirados)" : " activos"))
                .ycTitulo("Generado el " + YCControles.ycFecha(LocalDate.now()) + " · " + ycLista.size() + " inquilinos")
                .ycColumna("Inquilino", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 28).ycColumna("DNI", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11)
                .ycColumna("Celular", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11).ycColumna("Cuarto", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 14)
                .ycColumna("Renta", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Ingreso", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 11)
                .ycColumna("Paga el dia", com.alquilafacil.service.YCExcel.YCTipo.YC_ENTERO, 11).ycColumna("Garantia", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11)
                .ycColumna("Fin de contrato", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 14).ycColumna("Contrato", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 18)
                .ycColumna("Estado", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 10).ycColumna("Salida", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA, 11);
        BigDecimal ycRentas = BigDecimal.ZERO;
        for (YCInquilino ycI : ycLista) {
            ycH.ycFila(ycI.ycGetNombreCompleto(), ycI.getYcDni(), ycI.getYcTelefono(), ycI.getYcCuarto().getYcNumero(), ycI.ycGetRentaMensual(),
                    ycI.getYcFechaIngreso(), ycI.getYcDiaPago(), ycI.getYcGarantia(), ycI.getYcFechaFinContrato(), ycTextoContrato(ycI),
                    ycI.isYcActivo() ? "Activo" : "Retirado", ycI.getYcFechaSalida());
            if (ycI.isYcActivo()) {
                ycRentas = ycRentas.add(ycI.ycGetRentaMensual());
            }
        }
        ycH.ycTotales("Renta mensual de los activos", null, null, null, ycRentas, null, null, null, null, null, null, null);
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar inquilinos en Excel",
                "Inquilinos " + LocalDate.now(), com.alquilafacil.ui.YCExportador.YCFormato.YC_EXCEL,
                ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, List.of(ycH)));
    }

    @FXML
    private void ycOnContrato() {
        YCInquilino ycI = ycTabla.getSelectionModel().getSelectedItem();
        if (ycI != null) {
            ycVerContrato(ycI);
        }
    }

    void ycVerContrato(YCInquilino ycI) {
        ycExportador.ycAbrirTemporal(com.alquilafacil.service.YCContratoPdfService.ycNombreArchivo(ycI),
                com.alquilafacil.ui.YCExportador.YCFormato.YC_PDF, ycF -> ycContratoPdf.ycGenerar(ycI.getYcId(), ycF));
    }

    static String ycTextoContrato(YCInquilino ycI) {
        if (!ycI.isYcActivo()) {
            return "—";
        }
        Long ycFaltan = ycI.ycDiasParaFinContrato(LocalDate.now());
        if (ycFaltan == null) {
            return "Sin fecha de fin";
        }
        if (ycFaltan < 0) {
            return "Vencio hace " + (-ycFaltan) + (ycFaltan == -1 ? " dia" : " dias");
        }
        if (ycFaltan == 0) {
            return "Vence hoy";
        }
        if (ycFaltan <= YC_AVISO_CONTRATO_DIAS) {
            return "Vence en " + ycFaltan + (ycFaltan == 1 ? " dia" : " dias");
        }
        return "Hasta " + YCControles.ycFecha(ycI.getYcFechaFinContrato());
    }

    @FXML
    private void ycOnDarDeBaja() {
        YCInquilino ycI = ycTabla.getSelectionModel().getSelectedItem();
        if (ycI == null) {
            return;
        }
        if (!ycI.isYcActivo()) {
            ycNotificador.ycInfo(ycI.ycGetNombreCompleto() + " ya fue dado de baja.");
            return;
        }
        ycMostrarSalida(ycI);
    }

    private void ycMostrarSalida(YCInquilino ycI) {
        List<YCDeudaPendiente> ycPendientes = ycPagoService.ycPendientes(ycI, false, LocalDate.now());
        java.util.Map<YCDeudaPendiente, CheckBox> ycMarcas = new java.util.LinkedHashMap<>();

        Label ycTitulo = new Label("Salida de " + ycI.ycGetNombreCompleto());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label(ycI.getYcCuarto().getYcNumero() + " · vive aqui desde el " + YCControles.ycFecha(ycI.getYcFechaIngreso())
                + ". Antes de liberar el cuarto, revisa las cuentas.");
        ycSub.getStyleClass().add("dialogo-mensaje");
        ycSub.setWrapText(true);

        Label ycTMeses = new Label("MESES QUE DEBE");
        ycTMeses.getStyleClass().add("seccion-form");
        VBox ycMeses = new VBox(6);
        if (ycPendientes.isEmpty()) {
            Label ycOk = new Label("No debe ningun mes. Esta al dia.", new org.kordamp.ikonli.javafx.FontIcon(Feather.CHECK_CIRCLE));
            ycOk.getStyleClass().addAll("dni-estado", "ok");
            ycMeses.getChildren().add(ycOk);
        }
        for (YCDeudaPendiente ycD : ycPendientes) {
            CheckBox ycCb = new CheckBox(ycD.ycPeriodoTexto() + (ycD.ycPorDias() ? " (" + ycD.ycTextoDias() + ")" : "")
                    + "  ·  " + ycD.ycEstado());
            ycCb.setSelected(true);
            Label ycMonto = new Label(YCControles.ycMoneda(ycD.ycTotal()));
            ycMonto.getStyleClass().add("ficha-dato-valor");
            javafx.scene.layout.Region ycEsp = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(ycEsp, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox ycFila = new javafx.scene.layout.HBox(8, ycCb, ycEsp, ycMonto);
            ycFila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            ycMeses.getChildren().add(ycFila);
            ycMarcas.put(ycD, ycCb);
        }

        Label ycTExtra = new Label("DAÑOS Y GARANTIA");
        ycTExtra.getStyleClass().add("seccion-form");
        TextField ycDanos = new TextField();
        ycDanos.setPromptText("0.00");
        YCFormularios.ycSoloDecimal(ycDanos);
        TextField ycMotivo = new TextField();
        ycMotivo.setPromptText("Motivo (ej. vidrio roto, pintura)");
        TextField ycGarantia = new TextField(ycI.getYcGarantia() == null ? "0.00"
                : ycI.getYcGarantia().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        YCFormularios.ycSoloDecimal(ycGarantia);
        javafx.scene.layout.HBox.setHgrow(ycMotivo, javafx.scene.layout.Priority.ALWAYS);
        Label ycEDanos = new Label("Daños (S/)");
        ycEDanos.getStyleClass().add("campo-label");
        Label ycEGar = new Label("Garantia que dejo (S/)");
        ycEGar.getStyleClass().add("campo-label");
        ycDanos.setPrefWidth(110);
        ycGarantia.setPrefWidth(150);
        javafx.scene.layout.HBox ycFilaExtra = new javafx.scene.layout.HBox(10,
                new VBox(4, ycEDanos, ycDanos), new VBox(4, new Label(" "), ycMotivo), new VBox(4, ycEGar, ycGarantia));
        javafx.scene.layout.HBox.setHgrow(ycFilaExtra.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        Label ycLDeuda = new Label();
        Label ycLDanos = new Label();
        Label ycLGar = new Label();
        for (Label ycL : List.of(ycLDeuda, ycLDanos, ycLGar)) {
            ycL.getStyleClass().add("estadia-total-detalle");
        }
        Label ycLResultadoTitulo = new Label();
        ycLResultadoTitulo.getStyleClass().add("estadia-total-etiqueta");
        Label ycLResultado = new Label();
        ycLResultado.getStyleClass().add("estadia-total-monto");
        VBox ycDetalle = new VBox(2, ycLResultadoTitulo, ycLDeuda, ycLDanos, ycLGar);
        javafx.scene.layout.HBox.setHgrow(ycDetalle, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox ycCaja = new javafx.scene.layout.HBox(12, ycDetalle, ycLResultado);
        ycCaja.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        ycCaja.getStyleClass().add("estadia-total");

        ComboBox<com.alquilafacil.model.YCMetodoPago> ycMetodo = new ComboBox<>();
        ycMetodo.getItems().setAll(com.alquilafacil.model.YCMetodoPago.values());
        ycMetodo.setValue(com.alquilafacil.model.YCMetodoPago.YC_EFECTIVO);
        Label ycEMetodo = new Label("Paga con");
        ycEMetodo.getStyleClass().add("campo-label");
        javafx.scene.layout.HBox ycFilaMetodo = new javafx.scene.layout.HBox(10, ycEMetodo, ycMetodo);
        ycFilaMetodo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ycAviso = new Label();
        ycAviso.getStyleClass().addAll("dni-estado", "aviso");
        ycAviso.setWrapText(true);

        Button ycRegistrar = new Button("Registrar salida", new org.kordamp.ikonli.javafx.FontIcon(Feather.LOG_OUT));
        ycRegistrar.getStyleClass().add("accent");
        ycRegistrar.setDefaultButton(true);

        Runnable ycRecalcular = () -> {
            BigDecimal ycDeuda = ycMarcas.entrySet().stream().filter(ycE -> ycE.getValue().isSelected())
                    .map(ycE -> ycE.getKey().ycTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ycSinMarcar = ycMarcas.entrySet().stream().filter(ycE -> !ycE.getValue().isSelected())
                    .map(ycE -> ycE.getKey().ycTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
            var ycC = com.alquilafacil.service.YCSalidaInquilinoService.ycCalcular(
                    ycDeuda, YCControles.ycImporteOCero(ycDanos.getText()), YCControles.ycImporteOCero(ycGarantia.getText()));
            ycLDeuda.setText("Meses que paga: " + YCControles.ycMoneda(ycC.ycDeuda()));
            ycLDanos.setText("Daños: " + YCControles.ycMoneda(ycC.ycDanos()));
            ycLGar.setText("Menos garantia: −" + YCControles.ycMoneda(ycC.ycGarantia()));
            if (ycC.ycACobrar().signum() > 0) {
                ycLResultadoTitulo.setText("TE DEBE PAGAR");
                ycLResultado.setText(YCControles.ycMoneda(ycC.ycACobrar()));
                ycCaja.getStyleClass().removeAll("devolver", "a-mano");
                ycCaja.getStyleClass().add("cobrar");
                ycRegistrar.setText("Cobrar " + YCControles.ycMoneda(ycC.ycACobrar()) + " y registrar salida");
            } else if (ycC.ycADevolver().signum() > 0) {
                ycLResultadoTitulo.setText("DEVUELVELE DE SU GARANTIA");
                ycLResultado.setText(YCControles.ycMoneda(ycC.ycADevolver()));
                ycCaja.getStyleClass().removeAll("cobrar", "a-mano");
                ycCaja.getStyleClass().add("devolver");
                ycRegistrar.setText("Devolver " + YCControles.ycMoneda(ycC.ycADevolver()) + " y registrar salida");
            } else {
                ycLResultadoTitulo.setText("QUEDAN A MANO");
                ycLResultado.setText(YCControles.ycMoneda(BigDecimal.ZERO));
                ycCaja.getStyleClass().removeAll("cobrar", "devolver");
                ycCaja.getStyleClass().add("a-mano");
                ycRegistrar.setText("Registrar salida");
            }
            boolean ycPagaAlgo = ycDeuda.signum() > 0;
            ycFilaMetodo.setVisible(ycPagaAlgo);
            ycFilaMetodo.setManaged(ycPagaAlgo);
            boolean ycConDeuda = ycSinMarcar.signum() > 0;
            ycAviso.setText("Ojo: los meses sin marcar (" + YCControles.ycMoneda(ycSinMarcar)
                    + ") quedaran como deuda y se iran sin pagar. Quedara anotado en la liquidacion.");
            ycAviso.setGraphic(new org.kordamp.ikonli.javafx.FontIcon(Feather.ALERT_TRIANGLE));
            ycAviso.setVisible(ycConDeuda);
            ycAviso.setManaged(ycConDeuda);
        };
        ycMarcas.values().forEach(ycCb -> ycCb.selectedProperty().addListener((ycO, ycA, ycB) -> ycRecalcular.run()));
        ycDanos.textProperty().addListener((ycO, ycA, ycB) -> ycRecalcular.run());
        ycGarantia.textProperty().addListener((ycO, ycA, ycB) -> ycRecalcular.run());
        ycRecalcular.run();

        ycRegistrar.setOnAction(ycE -> {
            java.util.Set<java.time.YearMonth> ycElegidos = new java.util.HashSet<>();
            ycMarcas.forEach((ycD, ycCb) -> {
                if (ycCb.isSelected()) ycElegidos.add(ycD.ycPeriodo());
            });
            try {
                var ycL = ycSalidaService.ycRegistrar(ycI.getYcId(), ycElegidos, YCControles.ycImporteOCero(ycDanos.getText()),
                        ycMotivo.getText(), YCControles.ycImporteOCero(ycGarantia.getText()), ycMetodo.getValue());
                ycNotificador.ycCerrarDialogo();
                ycCargarTabla();
                String ycResultado = ycL.getYcCobrado().signum() > 0 ? "Pago " + YCControles.ycMoneda(ycL.getYcCobrado()) + " al salir."
                        : ycL.getYcDevuelto().signum() > 0 ? "Devuelvele " + YCControles.ycMoneda(ycL.getYcDevuelto()) + " de su garantia."
                        : "Quedaron a mano.";
                ycNotificador.ycExitoConAcciones("Salida registrada · " + ycL.ycGetNumero(),
                        ycI.ycGetNombreCompleto() + " ya no vive en " + ycL.getYcCuartoNumero() + " y el cuarto quedo libre. " + ycResultado
                                + " Imprime la liquidacion para que ambos la firmen.",
                        "Cerrar", null, "Ver liquidacion PDF",
                        () -> ycExportador.ycAbrirTemporal("Liquidacion " + ycL.ycGetNumero() + " - " + ycI.ycGetNombreCompleto(),
                                com.alquilafacil.ui.YCExportador.YCFormato.YC_PDF, ycF -> ycLiquidacionPdf.ycGenerar(ycL.getYcId(), ycF)));
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Button ycCancelar = new Button("Cancelar");
        ycCancelar.getStyleClass().add("button-outlined");
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        javafx.scene.layout.Region ycEsp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(ycEsp, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox ycPie = new javafx.scene.layout.HBox(10, ycFilaMetodo, ycEsp, ycCancelar, ycRegistrar);
        ycPie.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox ycDialogo = new VBox(12, ycTitulo, ycSub, ycTMeses, ycMeses, ycTExtra, ycFilaExtra, ycCaja, ycAviso, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(640);
        ycDialogo.setMaxSize(640, javafx.scene.layout.Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    @FXML
    private void ycOnEliminar() {
        YCInquilino ycI = ycTabla.getSelectionModel().getSelectedItem();
        if (ycI == null) {
            return;
        }
        ycNotificador.ycConfirmar("¿Eliminar a " + ycI.ycGetNombreCompleto() + "?",
                "Solo se puede eliminar si nunca registro pagos. Si se va, usa \"Dar de baja\".",
                "Eliminar",
                () -> {
                    try {
                        ycService.ycEliminar(ycI.getYcId());
                        ycNotificador.ycExito("Registro eliminado.");
                        ycCargarTabla();
                    } catch (YCNegocioException ycE) {
                        ycNotificador.ycError(ycE.getMessage());
                    }
                });
    }

    private void ycCargarTabla() {
        Page<YCInquilino> ycPagina = ycService.ycListar(ycTxtBuscar.getText(), ycChkRetirados.isSelected(),
                ycPaginador.ycGetPagina(), ycPaginador.ycGetTamanio());
        ycDatos.setAll(ycPagina.getContent());
        ycPaginador.ycActualizar(ycPagina);
        Animations.fadeIn(ycTabla, Duration.millis(260)).play();
    }

    private void ycAbrirFormulario() {
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(ycTxtDni::requestFocus);
    }

    private void ycLimpiarFormulario() {
        YCFormularios.ycLimpiarErrores(ycTxtNombre, ycTxtApellido, ycTxtDni, ycTxtTelefono, ycCmbCuarto, ycDpIngreso,
                ycTxtDiaPago, ycTxtGarantia);
        ycTxtDni.clear();
        ycNombreAuto = "";
        ycApellidoAuto = "";
        ycTelefonoAuto = "";
        com.alquilafacil.ui.YCAutocompletarDni.ycOcultar(ycLblDniEstado);
        ycTxtNombre.clear();
        ycTxtApellido.clear();
        ycTxtTelefono.clear();
        ycCmbCuarto.setValue(null);
        ycDpIngreso.setValue(null);
        ycDpFinContrato.setValue(null);
        YCFormularios.ycLimpiarError(ycDpFinContrato);
        ycTxtRenta.clear();
        YCFormularios.ycLimpiarError(ycTxtRenta);
        ycTxtDiaPago.clear();
        ycTxtGarantia.clear();
    }
}
