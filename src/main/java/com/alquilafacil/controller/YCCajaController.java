package com.alquilafacil.controller;

import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCUsuario;
import com.alquilafacil.service.YCCajaService;
import com.alquilafacil.service.YCCierreCajaPdfService;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.service.YCUsuarioService;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCCajaController implements YCRefrescable {
    private static final String YC_TODOS = "Todos los usuarios";
    private static final DateTimeFormatter YC_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final YCCajaService ycService;
    private final YCCierreCajaPdfService ycPdf;
    private final YCUsuarioService ycUsuarioService;
    private final YCSesion ycSesion;
    private final YCExportador ycExportador;
    private final com.alquilafacil.ui.YCNotificador ycNotificador;

    @FXML private Label ycLblSubtitulo;
    @FXML private ComboBox<String> ycCmbUsuario;
    @FXML private DatePicker ycDpDia;
    @FXML private Label ycKpiEntro;
    @FXML private Label ycKpiEntroDetalle;
    @FXML private Label ycKpiSalio;
    @FXML private Label ycKpiSalioDetalle;
    @FXML private Label ycKpiNeto;
    @FXML private Label ycKpiEfectivo;
    @FXML private GridPane ycGridMetodos;
    @FXML private VBox ycPanelUsuarios;
    @FXML private TextField ycTxtContado;
    @FXML private Label ycLblDiferencia;
    @FXML private TextField ycTxtNotaCierre;
    @FXML private Button ycBtnGuardarCierre;
    @FXML private VBox ycListaCierres;
    @FXML private VBox ycPanelAnulaciones;
    @FXML private Label ycLblAnulaciones;
    @FXML private VBox ycListaAnulaciones;
    @FXML private Label ycLblLista;

    @FXML private TableView<YCCajaService.YCMovimiento> ycTabla;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColHora;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColTipo;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColConcepto;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColDetalle;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColMetodo;
    @FXML private TableColumn<YCCajaService.YCMovimiento, BigDecimal> ycColMonto;
    @FXML private TableColumn<YCCajaService.YCMovimiento, String> ycColUsuario;

    private YCCajaService.YCCierre ycCierre;
    private boolean ycCargando;

    @FXML
    public void initialize() {
        ycDpDia.setConverter(new StringConverter<>() {
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
        ycDpDia.setValue(LocalDate.now());
        ycDpDia.valueProperty().addListener((ycO, ycA, ycB) -> {
            if (ycB != null) {
                ycTxtContado.clear();
                ycCargar();
            }
        });
        ycCmbUsuario.valueProperty().addListener((ycO, ycA, ycB) -> {
            if (!ycCargando) {
                ycTxtContado.clear();
                ycCargar();
            }
        });
        YCFormularios.ycSoloDecimal(ycTxtContado);
        ycTxtContado.textProperty().addListener((ycO, ycA, ycB) -> ycMostrarDiferencia());
        ycConfigurarTabla();
    }

    @Override
    public void ycRefrescar() {
        boolean ycAdmin = ycSesion.ycEsAdmin();
        ycCmbUsuario.setVisible(ycAdmin);
        ycCmbUsuario.setManaged(ycAdmin);
        if (ycAdmin) {
            ycCargando = true;
            String ycElegido = ycCmbUsuario.getValue();
            ycCmbUsuario.getItems().setAll(YC_TODOS);
            ycUsuarioService.ycListar().stream().map(YCUsuario::getYcUsuario).forEach(ycCmbUsuario.getItems()::add);
            ycCmbUsuario.setValue(ycElegido != null && ycCmbUsuario.getItems().contains(ycElegido) ? ycElegido : YC_TODOS);
            ycCargando = false;
        }
        ycCargar();
    }

    @FXML
    private void ycOnImprimir() {
        if (ycCierre == null) {
            return;
        }
        YCCajaService.YCCierre ycC = ycCierre;
        BigDecimal ycContado = YCControles.ycImporte(ycTxtContado.getText());
        ycExportador.ycAbrirTemporal("Cierre_de_caja_" + ycC.ycDia() + (ycC.ycUsuario() == null ? "" : "_" + ycC.ycUsuario()),
                YCExportador.YCFormato.YC_PDF, ycDestino -> ycPdf.ycGenerar(ycC, ycContado, ycDestino));
    }

    @FXML
    private void ycOnExcel() {
        if (ycCierre == null) {
            return;
        }
        YCCajaService.YCCierre ycC = ycCierre;
        String ycTitulo = "Caja del " + YCControles.ycFecha(ycC.ycDia()) + (ycC.ycUsuario() == null ? " · todos los usuarios" : " · " + ycC.ycUsuario());
        com.alquilafacil.service.YCExcel.YCHoja ycMovs = new com.alquilafacil.service.YCExcel.YCHoja("Movimientos").ycTitulo(ycTitulo)
                .ycColumna("Hora", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA_HORA, 17).ycColumna("Concepto", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 30)
                .ycColumna("Detalle", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 40).ycColumna("Forma", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 13)
                .ycColumna("Entro", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Salio", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11)
                .ycColumna("Usuario", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11);
        for (YCCajaService.YCMovimiento ycM : ycC.ycMovimientos()) {
            ycMovs.ycFila(ycM.ycHora(), ycM.ycConcepto(), ycM.ycDetalle(), ycM.ycMetodo() == null ? null : ycM.ycMetodo().ycGetDescripcion(),
                    ycM.ycEntrada() ? ycM.ycMonto() : null, ycM.ycEntrada() ? null : ycM.ycMonto(), ycM.ycUsuario());
        }
        ycMovs.ycTotales("Totales", null, null, null, ycC.ycTotalEntradas(), ycC.ycTotalSalidas(), null);
        com.alquilafacil.service.YCExcel.YCHoja ycFormas = new com.alquilafacil.service.YCExcel.YCHoja("Por forma de pago").ycTitulo(ycTitulo)
                .ycTitulo("Efectivo que debe haber en caja: S/ " + ycC.ycEfectivoEnCaja())
                .ycColumna("Forma", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 16).ycColumna("Entro", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 12)
                .ycColumna("Salio", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 12).ycColumna("Neto", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 12);
        for (YCMetodoPago ycMp : YCMetodoPago.values()) {
            ycFormas.ycFila(ycMp.ycGetDescripcion(), ycC.ycEntradas(ycMp), ycC.ycSalidas(ycMp), ycC.ycEntradas(ycMp).subtract(ycC.ycSalidas(ycMp)));
        }
        ycFormas.ycTotales("Total", ycC.ycTotalEntradas(), ycC.ycTotalSalidas(), ycC.ycNeto());
        List<com.alquilafacil.service.YCExcel.YCHoja> ycHojas = new java.util.ArrayList<>(List.of(ycMovs, ycFormas));
        if (!ycC.ycAnulaciones().isEmpty()) {
            com.alquilafacil.service.YCExcel.YCHoja ycAnul = new com.alquilafacil.service.YCExcel.YCHoja("Anulaciones").ycTitulo(ycTitulo)
                    .ycColumna("Hora", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA_HORA, 17).ycColumna("Que", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 40)
                    .ycColumna("Monto", com.alquilafacil.service.YCExcel.YCTipo.YC_MONEDA, 11).ycColumna("Motivo", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 34)
                    .ycColumna("Usuario", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 11);
            for (YCCajaService.YCAnulacion ycA : ycC.ycAnulaciones()) {
                ycAnul.ycFila(ycA.ycHora(), ycA.ycQue(), ycA.ycMonto(), ycA.ycMotivo(), ycA.ycUsuario());
            }
            ycHojas.add(ycAnul);
        }
        ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar caja en Excel", "Caja " + ycC.ycDia()
                + (ycC.ycUsuario() == null ? "" : " " + ycC.ycUsuario()), YCExportador.YCFormato.YC_EXCEL, ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, ycHojas));
    }

    @FXML
    private void ycOnGuardarCierre() {
        if (ycCierre == null) {
            return;
        }
        BigDecimal ycContado = YCControles.ycImporte(ycTxtContado.getText());
        if (ycContado == null) {
            ycNotificador.ycAdvertencia("Primero escribe cuanto efectivo contaste.");
            ycTxtContado.requestFocus();
            return;
        }
        try {
            var ycGuardado = ycService.ycGuardarCierre(ycCierre.ycDia(), ycCierre.ycUsuario(), ycContado, ycTxtNotaCierre.getText());
            ycNotificador.ycExito("Cierre guardado" + (ycGuardado.getYcDiferencia().signum() == 0 ? ": la caja cuadra."
                    : ycGuardado.getYcDiferencia().signum() > 0 ? ": sobran " + YCControles.ycMoneda(ycGuardado.getYcDiferencia()) + "."
                    : ": faltan " + YCControles.ycMoneda(ycGuardado.getYcDiferencia().abs()) + "."));
            ycTxtNotaCierre.clear();
            ycMostrarCierres();
        } catch (com.alquilafacil.exception.YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
            if (ycEx.getMessage().contains("nota")) {
                ycTxtNotaCierre.requestFocus();
            }
        }
    }

    private void ycMostrarCierres() {
        ycListaCierres.getChildren().clear();
        if (ycCierre == null) {
            return;
        }
        var ycCierres = ycService.ycCierresDel(ycCierre.ycDia());
        if (ycCierres.isEmpty()) {
            Label ycNada = new Label("Aun no se guardo el cierre de este dia.");
            ycNada.getStyleClass().add("campo-ayuda");
            ycListaCierres.getChildren().add(ycNada);
            return;
        }
        Label ycTitulo = new Label("CIERRES GUARDADOS");
        ycTitulo.getStyleClass().add("caja-metodo-cabecera");
        ycListaCierres.getChildren().add(ycTitulo);
        for (var ycX : ycCierres) {
            BigDecimal ycD = ycX.getYcDiferencia();
            String ycEstado = ycD.signum() == 0 ? "cuadro" : ycD.signum() > 0 ? "sobro " + YCControles.ycMoneda(ycD)
                    : "falto " + YCControles.ycMoneda(ycD.abs());
            Label ycL = new Label(ycX.getYcFechaCierre().format(YC_HORA) + " · " + ycX.getYcCerradoPor()
                    + (ycX.getYcUsuarioCaja() == null ? " (todos)" : " (caja de " + ycX.getYcUsuarioCaja() + ")")
                    + " · contado " + YCControles.ycMoneda(ycX.getYcEfectivoContado()) + " · " + ycEstado
                    + (ycX.getYcNota() == null ? "" : "\nNota: " + ycX.getYcNota()),
                    new FontIcon(ycD.signum() == 0 ? Feather.CHECK_CIRCLE : Feather.ALERT_TRIANGLE));
            ycL.getStyleClass().add(ycD.signum() == 0 ? "campo-ayuda" : "estadia-info");
            ycL.setWrapText(true);
            ycListaCierres.getChildren().add(ycL);
        }
    }

    private void ycCargar() {
        LocalDate ycDia = ycDpDia.getValue() == null ? LocalDate.now() : ycDpDia.getValue();
        boolean ycAdmin = ycSesion.ycEsAdmin();
        String ycUsuario = ycAdmin ? (YC_TODOS.equals(ycCmbUsuario.getValue()) ? null : ycCmbUsuario.getValue()) : null;
        ycCierre = ycService.ycCierre(ycDia, ycUsuario);
        YCCajaService.YCCierre ycC = ycCierre;

        boolean ycHoy = ycDia.equals(LocalDate.now());
        ycLblSubtitulo.setText((ycHoy ? "Hoy, " : "") + YCControles.ycFecha(ycDia)
                + (ycC.ycUsuario() == null ? " · todos los usuarios" : " · cobrado por " + ycC.ycUsuario())
                + (ycC.ycConGastos() ? "" : " · sin gastos"));

        long ycEntradas = ycC.ycMovimientos().stream().filter(YCCajaService.YCMovimiento::ycEntrada).count();
        ycKpiEntro.setText(YCControles.ycMoneda(ycC.ycTotalEntradas()));
        ycKpiEntroDetalle.setText(ycEntradas + (ycEntradas == 1 ? " cobro" : " cobros"));
        ycKpiSalio.setText(YCControles.ycMoneda(ycC.ycTotalSalidas()));
        ycKpiSalioDetalle.setText(ycC.ycConGastos() ? "Gastos y devoluciones" : "Devoluciones");
        ycKpiNeto.setText(YCControles.ycMoneda(ycC.ycNeto()));
        ycKpiEfectivo.setText(YCControles.ycMoneda(ycC.ycEfectivoEnCaja()));

        ycGridMetodos.getChildren().clear();
        String[] ycCab = {"FORMA", "ENTRO", "SALIO", "NETO"};
        for (int ycI = 0; ycI < ycCab.length; ycI++) {
            Label ycL = new Label(ycCab[ycI]);
            ycL.getStyleClass().add("caja-metodo-cabecera");
            ycGridMetodos.add(ycL, ycI, 0);
            if (ycI > 0) GridPane.setHalignment(ycL, HPos.RIGHT);
        }
        int ycFila = 1;
        for (YCMetodoPago ycM : YCMetodoPago.values()) {
            BigDecimal ycEn = ycC.ycEntradas(ycM);
            BigDecimal ycSa = ycC.ycSalidas(ycM);
            Label ycNombre = new Label(ycM.ycGetDescripcion());
            Label ycLEn = new Label(YCControles.ycMoneda(ycEn));
            Label ycLSa = new Label(ycSa.signum() == 0 ? "—" : "- " + YCControles.ycMoneda(ycSa));
            if (ycSa.signum() > 0) ycLSa.getStyleClass().addAll("caja-monto", "salida");
            Label ycLNeto = new Label(YCControles.ycMoneda(ycEn.subtract(ycSa)));
            ycLNeto.getStyleClass().add("caja-monto");
            ycGridMetodos.addRow(ycFila++, ycNombre, ycLEn, ycLSa, ycLNeto);
            for (Label ycL : List.of(ycLEn, ycLSa, ycLNeto)) GridPane.setHalignment(ycL, HPos.RIGHT);
        }

        ycPanelUsuarios.getChildren().clear();
        Map<String, BigDecimal> ycPorUsuario = ycC.ycCobradoPorUsuario();
        if (ycC.ycUsuario() == null && !ycPorUsuario.isEmpty()) {
            Label ycTitulo = new Label("COBRADO POR CADA USUARIO");
            ycTitulo.getStyleClass().add("caja-metodo-cabecera");
            ycPanelUsuarios.getChildren().add(ycTitulo);
            ycPorUsuario.forEach((ycU, ycMonto) -> {
                Label ycL = new Label(ycU + ":  " + YCControles.ycMoneda(ycMonto), new FontIcon(Feather.USER));
                ycPanelUsuarios.getChildren().add(ycL);
            });
        }

        ycListaAnulaciones.getChildren().clear();
        boolean ycHayAnulaciones = !ycC.ycAnulaciones().isEmpty();
        ycPanelAnulaciones.setVisible(ycHayAnulaciones);
        ycPanelAnulaciones.setManaged(ycHayAnulaciones);
        ycLblAnulaciones.setText("Anulaciones y cancelaciones del día (" + ycC.ycAnulaciones().size() + ")");
        for (YCCajaService.YCAnulacion ycA : ycC.ycAnulaciones()) {
            Label ycL = new Label(ycA.ycHora().format(YC_HORA) + "  ·  " + ycA.ycQue() + "  ·  " + YCControles.ycMoneda(ycA.ycMonto())
                    + "\nMotivo: " + (ycA.ycMotivo() == null ? "—" : ycA.ycMotivo())
                    + (ycA.ycUsuario() == null ? "" : "  ·  por " + ycA.ycUsuario()), new FontIcon(Feather.X_CIRCLE));
            ycL.getStyleClass().add("estadia-info");
            ycL.setWrapText(true);
            ycL.setMaxWidth(Double.MAX_VALUE);
            ycListaAnulaciones.getChildren().add(ycL);
        }

        ycLblLista.setText("Movimientos del día (" + ycC.ycMovimientos().size() + ")");
        ycTabla.getItems().setAll(ycC.ycMovimientos());
        ycMostrarDiferencia();
        ycMostrarCierres();
    }

    private void ycMostrarDiferencia() {
        ycLblDiferencia.getStyleClass().removeAll("cuadra", "no-cuadra");
        BigDecimal ycContado = YCControles.ycImporte(ycTxtContado.getText());
        if (ycContado == null || ycCierre == null) {
            ycLblDiferencia.setText("Debe haber " + (ycCierre == null ? "" : YCControles.ycMoneda(ycCierre.ycEfectivoEnCaja()))
                    + " en efectivo.");
            return;
        }
        BigDecimal ycDif = ycContado.subtract(ycCierre.ycEfectivoEnCaja());
        if (ycDif.signum() == 0) {
            ycLblDiferencia.setText("✔ La caja cuadra.");
            ycLblDiferencia.getStyleClass().add("cuadra");
        } else {
            ycLblDiferencia.setText((ycDif.signum() > 0 ? "Sobran " : "Faltan ") + YCControles.ycMoneda(ycDif.abs()) + ".");
            ycLblDiferencia.getStyleClass().add("no-cuadra");
        }
    }

    private void ycConfigurarTabla() {
        ycColHora.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycHora().format(YC_HORA)));
        ycColTipo.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycEntrada() ? "Entrada" : "Salida"));
        ycColTipo.setCellFactory(YCTablas.ycCeldaChip(ycV -> "Entrada".equals(ycV) ? "exito" : "peligro"));
        ycColConcepto.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycConcepto()));
        ycColDetalle.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycDetalle()));
        ycColMetodo.setCellValueFactory(ycC -> new SimpleStringProperty(
                ycC.getValue().ycMetodo() == null ? "" : ycC.getValue().ycMetodo().ycGetDescripcion()));
        ycColMetodo.setCellFactory(YCTablas.ycCeldaChip("—"));
        ycColMonto.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().ycMonto()));
        ycColMonto.setCellFactory(ycCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-importe");
            }

            @Override
            protected void updateItem(BigDecimal ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                YCCajaService.YCMovimiento ycM = ycVacio ? null : getTableRow().getItem();
                getStyleClass().remove("salida");
                if (ycVacio || ycValor == null || ycM == null) {
                    setText(null);
                    return;
                }
                setText((ycM.ycEntrada() ? "" : "- ") + YCTablas.YC_MONEDA.format(ycValor));
                if (!ycM.ycEntrada()) getStyleClass().add("salida");
            }
        });
        ycColUsuario.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().ycUsuario()));
        ycColUsuario.setCellFactory(YCTablas.ycCeldaOpcional());
        YCTablas.ycConfigurar(ycTabla, Feather.INBOX, "Sin movimientos",
                "Aqui aparecen los cobros, devoluciones y gastos del dia elegido.", ycM -> { });
    }
}
