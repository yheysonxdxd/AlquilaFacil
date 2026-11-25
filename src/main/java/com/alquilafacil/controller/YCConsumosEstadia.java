package com.alquilafacil.controller;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCConsumoExtra;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.service.YCConsumoExtraService;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCNotificador;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
class YCConsumosEstadia {
    private static final DateTimeFormatter YC_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final YCConsumoExtraService ycService;
    private final YCSesion ycSesion;
    private final YCNotificador ycNotificador;
    private final YCMensajesHuesped ycMensajes;

    Node ycSeccion(YCEstadia ycE, Runnable ycReabrir) {
        List<YCConsumoExtra> ycLista = ycService.ycDeEstadia(ycE);
        boolean ycGrupo = ycLista.stream().map(ycC -> ycC.getYcEstadia().getYcId()).distinct().count() > 1 || ycE.ycIsDeGrupo();
        BigDecimal ycPendiente = ycLista.stream().filter(YCConsumoExtra::ycIsPendiente).map(YCConsumoExtra::getYcTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Label ycTitulo = new Label("CONSUMOS EXTRA", new FontIcon(Feather.COFFEE));
        ycTitulo.getStyleClass().add("ficha-lateral-etiqueta");
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycCabecera = new HBox(8, ycTitulo, ycEsp);
        ycCabecera.setAlignment(Pos.CENTER_LEFT);
        if (ycPendiente.signum() > 0) {
            Button ycCobrar = new Button("Cobrar consumos " + YCControles.ycMoneda(ycPendiente), new FontIcon(Feather.DOLLAR_SIGN));
            ycCobrar.getStyleClass().add("success");
            ycCobrar.setOnAction(ycEv -> ycCobrar(ycE, ycReabrir));
            ycCabecera.getChildren().add(ycCobrar);
        }
        if (!ycE.isYcCancelada()) {
            Button ycAgregar = new Button("Agregar consumo", new FontIcon(Feather.PLUS));
            ycAgregar.getStyleClass().add("button-outlined");
            ycAgregar.setOnAction(ycEv -> ycAgregar(ycE, ycReabrir));
            ycCabecera.getChildren().add(ycAgregar);
        }

        VBox ycFilas = new VBox(4);
        if (ycLista.isEmpty()) {
            Label ycVacio = new Label("Sin consumos. Agrega desayunos, lavanderia, bebidas o cochera para cobrarlos aqui.");
            ycVacio.getStyleClass().add("campo-ayuda");
            ycVacio.setWrapText(true);
            ycFilas.getChildren().add(ycVacio);
        }
        for (YCConsumoExtra ycC : ycLista) {
            Label ycNombre = new Label(ycC.ycGetDescripcion() + (ycGrupo ? " · " + ycC.getYcEstadia().getYcCuartoNumero() : ""));
            ycNombre.getStyleClass().add("ficha-dato-valor");
            Label ycCuando = new Label(ycC.getYcFechaRegistro().format(YC_HORA)
                    + (ycC.getYcRegistradoPor() == null ? "" : " · " + ycC.getYcRegistradoPor())
                    + (ycC.isYcAnulado() ? " · anulado: " + ycC.getYcMotivoAnulacion() : ""));
            ycCuando.getStyleClass().add("ficha-lateral-nota");
            VBox ycTextos = new VBox(1, ycNombre, ycCuando);
            HBox.setHgrow(ycTextos, Priority.ALWAYS);
            Label ycMonto = new Label(YCControles.ycMoneda(ycC.getYcTotal()));
            ycMonto.getStyleClass().add("ficha-dato-valor");
            Label ycChip = new Label(ycC.isYcAnulado() ? "Anulado" : ycC.isYcPagado()
                    ? "Pagado" + (ycC.getYcMetodo() == null ? "" : " · " + ycC.getYcMetodo()) : "A la cuenta");
            ycChip.getStyleClass().addAll("chip", ycC.isYcAnulado() ? "neutro" : ycC.isYcPagado() ? "exito" : "advertencia");
            ycChip.setMinWidth(Region.USE_PREF_SIZE);
            HBox ycFila = new HBox(10, ycTextos, ycMonto, ycChip);
            ycFila.setAlignment(Pos.CENTER_LEFT);
            ycFila.getStyleClass().add("recordatorio-fila");
            if (!ycC.isYcAnulado()) {
                Button ycAnular = new Button(null, new FontIcon(Feather.X));
                ycAnular.getStyleClass().addAll("flat", "button-circle", "danger");
                ycAnular.setTooltip(new Tooltip("Anular este consumo"));
                ycSesion.ycSoloAdmin(ycAnular);
                ycAnular.setOnAction(ycEv -> ycAnular(ycC, ycReabrir));
                ycFila.getChildren().add(ycAnular);
            }
            ycFilas.getChildren().add(ycFila);
        }
        return new VBox(8, ycCabecera, ycFilas);
    }

    void ycAgregar(YCEstadia ycE, Runnable ycDespues) {
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.COFFEE));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Consumo de " + ycE.getYcHuesped());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label(ycE.ycGetNumero() + " · " + ycE.getYcCuartoNumero());
        ycSub.getStyleClass().add("dialogo-mensaje");

        ComboBox<String> ycConcepto = new ComboBox<>();
        ycConcepto.setEditable(true);
        ycConcepto.getItems().setAll(ycService.ycSugerencias());
        ycConcepto.setPromptText("Ej. Desayuno");
        ycConcepto.setMaxWidth(Double.MAX_VALUE);
        TextField ycCantidad = new TextField("1");
        YCFormularios.ycSoloDigitos(ycCantidad, 2);
        ycCantidad.setPrefWidth(70);
        TextField ycPrecio = new TextField();
        ycPrecio.setPromptText("S/ c/u");
        YCFormularios.ycSoloDecimal(ycPrecio);
        ycPrecio.setPrefWidth(100);
        Label ycTotal = new Label(YCControles.ycMoneda(BigDecimal.ZERO));
        ycTotal.getStyleClass().add("estadia-total-monto");

        Runnable ycRecalcular = () -> {
            BigDecimal ycP = YCControles.ycImporte(ycPrecio.getText());
            int ycN = ycEntero(ycCantidad.getText());
            ycTotal.setText(YCControles.ycMoneda(ycP == null ? BigDecimal.ZERO : ycP.multiply(BigDecimal.valueOf(ycN))));
        };
        ycConcepto.valueProperty().addListener((ycO, ycA, ycB) -> {
            BigDecimal ycUltimo = ycService.ycPrecioSugerido(ycB);
            if (ycUltimo != null) {
                ycPrecio.setText(ycUltimo.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        });
        ycPrecio.textProperty().addListener((ycO, ycA, ycB) -> ycRecalcular.run());
        ycCantidad.textProperty().addListener((ycO, ycA, ycB) -> ycRecalcular.run());

        ToggleGroup ycPago = new ToggleGroup();
        RadioButton ycCuenta = new RadioButton("A la cuenta (lo paga despues)");
        ycCuenta.setToggleGroup(ycPago);
        ycCuenta.setSelected(true);
        RadioButton ycAhora = new RadioButton("Paga ahora");
        ycAhora.setToggleGroup(ycPago);
        ComboBox<YCMetodoPago> ycMetodo = new ComboBox<>();
        ycMetodo.getItems().setAll(YCMetodoPago.values());
        ycMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodo.disableProperty().bind(ycAhora.selectedProperty().not());

        VBox ycCajaCantidad = ycCaja("Cantidad", ycCantidad);
        ycCajaCantidad.setMinWidth(80);
        ycCajaCantidad.setPrefWidth(80);
        VBox ycCajaPrecio = ycCaja("Precio c/u", ycPrecio);
        ycCajaPrecio.setMinWidth(110);
        ycCajaPrecio.setPrefWidth(110);
        HBox ycCampos = new HBox(10, ycCaja("¿Que consumio?", ycConcepto), ycCajaCantidad, ycCajaPrecio);
        HBox.setHgrow(ycCampos.getChildren().get(0), Priority.ALWAYS);
        HBox ycFilaPago = new HBox(12, ycCuenta, ycAhora, ycMetodo);
        ycFilaPago.setAlignment(Pos.CENTER_LEFT);
        Label ycEtTotal = new Label("TOTAL");
        ycEtTotal.getStyleClass().add("estadia-total-etiqueta");
        Region ycEspTotal = new Region();
        HBox.setHgrow(ycEspTotal, Priority.ALWAYS);
        HBox ycCajaTotal = new HBox(12, ycEtTotal, ycEspTotal, ycTotal);
        ycCajaTotal.setAlignment(Pos.CENTER_LEFT);
        ycCajaTotal.getStyleClass().add("estadia-total");

        Button ycVolver = new Button("Cancelar");
        ycVolver.getStyleClass().add("button-outlined");
        ycVolver.setCancelButton(true);
        ycVolver.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            ycDespues.run();
        });
        Button ycGuardar = new Button("Agregar", new FontIcon(Feather.CHECK));
        ycGuardar.getStyleClass().add("accent");
        ycGuardar.setDefaultButton(true);
        ycGuardar.setOnAction(ycEv -> {
            String ycQue = ycConcepto.getEditor().getText();
            try {
                YCConsumoExtra ycC = ycService.ycAgregar(ycE.getYcId(), ycQue, ycEntero(ycCantidad.getText()),
                        YCControles.ycImporte(ycPrecio.getText()), ycAhora.isSelected() ? ycMetodo.getValue() : null);
                ycNotificador.ycCerrarDialogo();
                ycNotificador.ycExito(ycC.ycGetDescripcion() + " · " + YCControles.ycMoneda(ycC.getYcTotal())
                        + (ycC.isYcPagado() ? " cobrado en " + ycC.getYcMetodo() + "." : " quedo a la cuenta de " + ycE.getYcHuesped() + "."));
                ycDespues.run();
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycEsp, ycVolver, ycGuardar);
        ycPie.getStyleClass().add("dialogo-acciones");

        VBox ycDialogo = new VBox(12, ycCirculo, ycTitulo, ycSub, ycCampos, ycFilaPago, ycCajaTotal, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(520);
        ycDialogo.setMaxSize(520, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
        Platform.runLater(ycConcepto::requestFocus);
    }

    void ycCobrar(YCEstadia ycE, Runnable ycDespues) {
        List<YCConsumoExtra> ycPendientes = ycService.ycDeEstadia(ycE).stream().filter(YCConsumoExtra::ycIsPendiente).toList();
        if (ycPendientes.isEmpty()) {
            return;
        }
        BigDecimal ycSuma = ycPendientes.stream().map(YCConsumoExtra::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.DOLLAR_SIGN));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Cobrar consumos de " + ycE.getYcHuesped());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycDetalle = new Label(String.join("\n", ycPendientes.stream()
                .map(ycC -> ycC.ycGetDescripcion() + " · " + YCControles.ycMoneda(ycC.getYcTotal())).toList()));
        ycDetalle.getStyleClass().add("dialogo-mensaje");
        ycDetalle.setWrapText(true);
        Label ycMonto = new Label(YCControles.ycMoneda(ycSuma));
        ycMonto.getStyleClass().add("estadia-total-monto");
        ComboBox<YCMetodoPago> ycMetodo = new ComboBox<>();
        ycMetodo.getItems().setAll(YCMetodoPago.values());
        ycMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodo.setMaxWidth(Double.MAX_VALUE);
        Button ycVolver = new Button("Cancelar");
        ycVolver.getStyleClass().add("button-outlined");
        ycVolver.setCancelButton(true);
        ycVolver.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            ycDespues.run();
        });
        Button ycCobrar = new Button("Cobrar " + YCControles.ycMoneda(ycSuma), new FontIcon(Feather.CHECK));
        ycCobrar.getStyleClass().add("accent");
        ycCobrar.setDefaultButton(true);
        ycCobrar.setOnAction(ycEv -> {
            try {
                BigDecimal ycCobrado = ycService.ycCobrarPendientes(ycE, ycMetodo.getValue());
                ycNotificador.ycCerrarDialogo();
                ycDespues.run();
                ycNotificador.ycExitoConAcciones("Consumos cobrados", ycE.getYcHuesped() + " pago " + YCControles.ycMoneda(ycCobrado)
                                + " de consumos (" + ycMetodo.getValue() + ").",
                        "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycE));
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycEsp, ycVolver, ycCobrar);
        ycPie.getStyleClass().add("dialogo-acciones");
        VBox ycDialogo = new VBox(12, ycCirculo, ycTitulo, ycDetalle, ycMonto, ycCaja("Metodo de pago", ycMetodo), ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(440, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
        Platform.runLater(ycCobrar::requestFocus);
    }

    private void ycAnular(YCConsumoExtra ycC, Runnable ycDespues) {
        ycNotificador.ycConfirmarConMotivo("¿Anular " + ycC.ycGetDescripcion() + "?",
                YCControles.ycMoneda(ycC.getYcTotal()) + (ycC.isYcPagado() ? ". Ya estaba pagado: deja de contar en la caja y "
                        + "devuelve el dinero aparte." : ". Ya no se le cobrara.") + " Queda en el historial.",
                "Ej. Se cargo por error",
                "Anular consumo",
                ycMotivo -> {
                    try {
                        ycService.ycAnular(ycC.getYcId(), ycMotivo);
                        ycNotificador.ycExito("Consumo anulado.");
                        ycDespues.run();
                    } catch (YCNegocioException ycEx) {
                        ycNotificador.ycError(ycEx.getMessage());
                    }
                });
    }

    private static VBox ycCaja(String ycEtiqueta, Control ycCampo) {
        Label ycL = new Label(ycEtiqueta);
        ycL.getStyleClass().add("campo-label");
        return new VBox(4, ycL, ycCampo);
    }

    private static int ycEntero(String ycTexto) {
        try {
            return ycTexto == null || ycTexto.isBlank() ? 1 : Integer.parseInt(ycTexto.trim());
        } catch (NumberFormatException ycE) {
            return 1;
        }
    }
}
