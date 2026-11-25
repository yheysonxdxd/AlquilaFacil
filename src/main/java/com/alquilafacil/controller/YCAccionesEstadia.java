package com.alquilafacil.controller;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.ui.YCNotificador;
import javafx.application.Platform;
import javafx.geometry.Pos;
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
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class YCAccionesEstadia {
    private final YCEstadiaService ycService;
    private final YCNotificador ycNotificador;
    private final YCMensajesHuesped ycMensajes;
    private final YCConsumosEstadia ycConsumos;
    private final com.alquilafacil.service.YCConsumoExtraService ycConsumoService;

    void ycCobrarSaldo(YCEstadia ycE, Runnable ycRecargar) {
        if (ycE == null) {
            return;
        }
        List<YCEstadia> ycConSaldo = ycService.ycDelGrupo(ycE).stream().filter(YCEstadia::ycTieneSaldo).toList();
        if (ycConSaldo.isEmpty()) {
            return;
        }
        if (ycConSaldo.size() > 1) {
            ycCobrarSaldoGrupo(ycE, ycConSaldo, ycRecargar);
            return;
        }
        ycE = ycConSaldo.get(0);
        final YCEstadia ycEst = ycE;
        FontIcon ycIcono = new FontIcon(Feather.DOLLAR_SIGN);
        StackPane ycCirculo = new StackPane(ycIcono);
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Cobrar saldo de " + ycE.getYcHuesped());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycMensaje = new Label(ycE.ycGetNumero() + " · " + ycE.getYcCuartoNumero() + "\nTotal " + YCControles.ycMoneda(ycE.getYcTotal())
                + " · ya pago " + YCControles.ycMoneda(ycE.ycGetPagado()));
        ycMensaje.getStyleClass().add("dialogo-mensaje");
        Label ycMonto = new Label(YCControles.ycMoneda(ycE.ycGetSaldo()));
        ycMonto.getStyleClass().add("estadia-total-monto");
        ComboBox<YCMetodoPago> ycMetodo = new ComboBox<>();
        ycMetodo.getItems().setAll(YCMetodoPago.values());
        ycMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodo.setMaxWidth(Double.MAX_VALUE);
        Label ycEtMetodo = new Label("Metodo de pago");
        ycEtMetodo.getStyleClass().add("campo-label");
        Button ycCancelarBtn = new Button("Cancelar");
        ycCancelarBtn.getStyleClass().add("button-outlined");
        ycCancelarBtn.setCancelButton(true);
        ycCancelarBtn.setOnAction(ycEv -> ycNotificador.ycCerrarDialogo());
        Button ycCobrar = new Button("Cobrar " + YCControles.ycMoneda(ycE.ycGetSaldo()), new FontIcon(Feather.CHECK));
        ycCobrar.getStyleClass().add("accent");
        ycCobrar.setDefaultButton(true);
        ycCobrar.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            try {
                YCEstadia ycActualizada = ycService.ycCobrarSaldo(ycEst.getYcId(), ycMetodo.getValue());
                ycRecargar.run();
                ycNotificador.ycExitoConAcciones("Saldo cobrado",
                        ycActualizada.getYcHuesped() + " ya pago todo: " + YCControles.ycMoneda(ycActualizada.getYcTotal()) + ".",
                        "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycActualizada));
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycAcciones = new HBox(10, ycEsp, ycCancelarBtn, ycCobrar);
        ycAcciones.getStyleClass().add("dialogo-acciones");
        VBox ycDialogo = new VBox(12, ycCirculo, ycTitulo, ycMensaje, ycMonto, new VBox(4, ycEtMetodo, ycMetodo), ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(420, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
        Platform.runLater(ycCobrar::requestFocus);
    }

    private void ycCobrarSaldoGrupo(YCEstadia ycE, List<YCEstadia> ycConSaldo, Runnable ycRecargar) {
        BigDecimal ycSaldo = ycConSaldo.stream().map(YCEstadia::ycGetSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.DOLLAR_SIGN));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Cobrar saldo de " + ycE.getYcHuesped());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        StringBuilder ycDetalle = new StringBuilder("Reserva de grupo: paga todas sus habitaciones juntas.");
        for (YCEstadia ycX : ycConSaldo) {
            ycDetalle.append("\n").append(ycX.getYcCuartoNumero()).append(" · debe ").append(YCControles.ycMoneda(ycX.ycGetSaldo()));
        }
        Label ycMensaje = new Label(ycDetalle.toString());
        ycMensaje.getStyleClass().add("dialogo-mensaje");
        ycMensaje.setWrapText(true);
        Label ycMonto = new Label(YCControles.ycMoneda(ycSaldo));
        ycMonto.getStyleClass().add("estadia-total-monto");
        ComboBox<YCMetodoPago> ycMetodo = new ComboBox<>();
        ycMetodo.getItems().setAll(YCMetodoPago.values());
        ycMetodo.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodo.setMaxWidth(Double.MAX_VALUE);
        Label ycEtMetodo = new Label("Metodo de pago");
        ycEtMetodo.getStyleClass().add("campo-label");
        Button ycCancelarBtn = new Button("Cancelar");
        ycCancelarBtn.getStyleClass().add("button-outlined");
        ycCancelarBtn.setCancelButton(true);
        ycCancelarBtn.setOnAction(ycEv -> ycNotificador.ycCerrarDialogo());
        Button ycCobrar = new Button("Cobrar " + YCControles.ycMoneda(ycSaldo), new FontIcon(Feather.CHECK));
        ycCobrar.getStyleClass().add("accent");
        ycCobrar.setDefaultButton(true);
        ycCobrar.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            try {
                List<YCEstadia> ycCobradas = ycService.ycCobrarSaldoGrupo(ycE, ycMetodo.getValue());
                ycRecargar.run();
                YCEstadia ycPrimera = ycCobradas.get(0);
                ycNotificador.ycExitoConAcciones("Saldo del grupo cobrado",
                        ycE.getYcHuesped() + " pago " + YCControles.ycMoneda(ycSaldo) + " por " + ycCobradas.size()
                                + " habitaciones. Ya no debe nada.",
                        "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycPrimera));
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycAcciones = new HBox(10, ycEsp, ycCancelarBtn, ycCobrar);
        ycAcciones.getStyleClass().add("dialogo-acciones");
        VBox ycDialogo = new VBox(12, ycCirculo, ycTitulo, ycMensaje, ycMonto, new VBox(4, ycEtMetodo, ycMetodo), ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(440, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
        Platform.runLater(ycCobrar::requestFocus);
    }

    void ycRegistrarSalida(YCEstadia ycE, Runnable ycRecargar) {
        if (ycE == null) {
            return;
        }
        BigDecimal ycDebeConsumos = ycConsumoService.ycDeEstadia(ycE).stream()
                .filter(ycC -> ycC.ycIsPendiente() && ycC.getYcEstadia().getYcId().equals(ycE.getYcId()))
                .map(com.alquilafacil.model.YCConsumoExtra::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ycDebeConsumos.signum() > 0) {
            ycNotificador.ycAvisoConAccion("Primero cobra los consumos",
                    ycE.getYcHuesped() + " tiene " + YCControles.ycMoneda(ycDebeConsumos) + " de consumos extra a la cuenta en "
                            + ycE.getYcCuartoNumero() + ". Cobralos y luego registra su salida.",
                    "Volver", "Cobrar consumos", () -> ycConsumos.ycCobrar(ycE, ycRecargar));
            return;
        }
        if (!ycE.getYcSalida().equals(LocalDate.now())) {
            ycMostrarSalidaAnticipada(ycE, ycRecargar);
            return;
        }
        if (ycE.ycTieneSaldo()) {
            ycNotificador.ycAvisoConAccion("Primero cobra el saldo",
                    ycE.getYcHuesped() + " aun te debe " + YCControles.ycMoneda(ycE.ycGetSaldo())
                            + ". Cuando pague todo, podras registrar su salida.",
                    "Volver", "Cobrar " + YCControles.ycMoneda(ycE.ycGetSaldo()), () -> ycCobrarSaldo(ycE, ycRecargar));
            return;
        }
        ycNotificador.ycConfirmar("¿" + ycE.getYcHuesped() + " se va hoy?",
                ycE.getYcCuartoNumero() + " quedara libre esta noche y marcada \"por limpiar\". El monto ("
                        + YCControles.ycMoneda(ycE.getYcTotal()) + ") no cambia.",
                "Registrar salida",
                () -> ycEjecutar(() -> ycService.ycRegistrarSalidaHoy(ycE.getYcId()), "Salida registrada. "
                        + ycE.getYcCuartoNumero() + " quedo por limpiar.", ycRecargar));
    }

    private void ycMostrarSalidaAnticipada(YCEstadia ycE, Runnable ycRecargar) {
        LocalDate ycHoy = LocalDate.now();
        LocalDate ycMinimo = ycE.getYcEntrada().plusDays(1);
        LocalDate ycMaximo = ycE.getYcSalida().minusDays(1);

        Label ycTitulo = new Label("Salida anticipada de " + ycE.getYcHuesped());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label(ycE.getYcCuartoNumero() + " · reservo del " + YCControles.ycFecha(ycE.getYcEntrada()) + " al "
                + YCControles.ycFecha(ycE.getYcSalida()) + " (" + ycE.getYcNoches() + (ycE.getYcNoches() == 1 ? " noche" : " noches")
                + ") a " + YCControles.ycMoneda(ycE.getYcPrecioNoche()) + " la noche. Ya pago " + YCControles.ycMoneda(ycE.ycGetRecibido()) + ".");
        ycSub.getStyleClass().add("dialogo-mensaje");
        ycSub.setWrapText(true);

        Label ycEFecha = new Label("¿Que dia se va?");
        ycEFecha.getStyleClass().add("campo-label");
        DatePicker ycFecha = new DatePicker(ycHoy.isBefore(ycMinimo) ? ycMinimo : ycHoy.isAfter(ycMaximo) ? ycMaximo : ycHoy);
        ycFecha.setConverter(YCControles.ycConvertidorFecha());
        ycFecha.setDayCellFactory(ycDp -> new DateCell() {
            @Override
            public void updateItem(LocalDate ycD, boolean ycVacio) {
                super.updateItem(ycD, ycVacio);
                setDisable(ycVacio || ycD.isBefore(ycMinimo) || ycD.isAfter(ycMaximo));
            }
        });
        ycFecha.setMaxWidth(Double.MAX_VALUE);
        CheckBox ycRecalcular = new CheckBox("Cobrar solo las noches que se quedo");
        ycRecalcular.setSelected(true);
        Label ycAyuda = new Label("Si lo desmarcas, se cobran igual todas las noches reservadas (no se devuelve nada).");
        ycAyuda.getStyleClass().add("campo-ayuda");
        ycAyuda.setWrapText(true);

        Label ycLNoches = new Label();
        Label ycLTotal = new Label();
        Label ycLPagado = new Label();
        for (Label ycL : List.of(ycLNoches, ycLTotal, ycLPagado)) {
            ycL.getStyleClass().add("estadia-total-detalle");
        }
        Label ycLTitulo = new Label();
        ycLTitulo.getStyleClass().add("estadia-total-etiqueta");
        Label ycLMonto = new Label();
        ycLMonto.getStyleClass().add("estadia-total-monto");
        VBox ycDetalle = new VBox(2, ycLTitulo, ycLNoches, ycLTotal, ycLPagado);
        HBox.setHgrow(ycDetalle, Priority.ALWAYS);
        HBox ycCaja = new HBox(12, ycDetalle, ycLMonto);
        ycCaja.setAlignment(Pos.CENTER_LEFT);
        ycCaja.getStyleClass().add("estadia-total");

        Button ycRegistrar = new Button("Registrar salida", new FontIcon(Feather.LOG_OUT));
        ycRegistrar.getStyleClass().add("accent");
        ycRegistrar.setDefaultButton(true);

        ComboBox<YCMetodoPago> ycMetodoCobro = new ComboBox<>();
        ycMetodoCobro.getItems().setAll(YCMetodoPago.values());
        ycMetodoCobro.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodoCobro.setMaxWidth(Double.MAX_VALUE);
        Label ycEtMetodo = new Label("¿Como paga lo que falta?");
        ycEtMetodo.getStyleClass().add("campo-label");
        VBox ycCajaMetodo = new VBox(4, ycEtMetodo, ycMetodoCobro);

        ComboBox<YCMetodoPago> ycMetodoDevolucion = new ComboBox<>();
        ycMetodoDevolucion.getItems().setAll(YCMetodoPago.values());
        ycMetodoDevolucion.setValue(YCMetodoPago.YC_EFECTIVO);
        ycMetodoDevolucion.setMaxWidth(Double.MAX_VALUE);
        Label ycEtDevolucion = new Label("¿Como le devuelves el dinero?");
        ycEtDevolucion.getStyleClass().add("campo-label");
        Label ycAyudaDevolucion = new Label("Queda anotado en la estadia, en la caja del dia y en el historial. "
                + "El comprobante trae una linea para que el huesped firme que lo recibio.");
        ycAyudaDevolucion.getStyleClass().add("campo-ayuda");
        ycAyudaDevolucion.setWrapText(true);
        VBox ycCajaDevolucion = new VBox(4, ycEtDevolucion, ycMetodoDevolucion, ycAyudaDevolucion);

        Runnable ycRecalculo = () -> {
            LocalDate ycF = ycFecha.getValue();
            if (ycF == null || ycF.isBefore(ycMinimo) || ycF.isAfter(ycMaximo)) {
                ycLTitulo.setText("ELIGE UN DIA ENTRE EL " + YCControles.ycFecha(ycMinimo) + " Y EL " + YCControles.ycFecha(ycMaximo));
                ycLMonto.setText("");
                ycRegistrar.setDisable(true);
                return;
            }
            ycRegistrar.setDisable(false);
            var ycR = YCEstadiaService.ycCalcularSalida(ycE, ycF, ycRecalcular.isSelected());
            ycLNoches.setText("Se queda " + ycR.ycNochesAhora() + " de " + ycR.ycNochesAntes() + " noches (sale el "
                    + YCControles.ycFecha(ycF) + " hasta las " + ycE.getYcCuarto().ycGetHoraSalidaODefecto() + ")");
            ycLTotal.setText("Nuevo total: " + YCControles.ycMoneda(ycR.ycTotalAhora())
                    + (ycR.ycTotalAhora().compareTo(ycR.ycTotalAntes()) != 0 ? "  (antes " + YCControles.ycMoneda(ycR.ycTotalAntes()) + ")" : ""));
            ycLPagado.setText("Ya pago: " + YCControles.ycMoneda(ycR.ycRecibido()));
            ycCaja.getStyleClass().removeAll("cobrar", "devolver", "a-mano");
            ycCajaMetodo.setVisible(ycR.ycACobrar().signum() > 0);
            ycCajaMetodo.setManaged(ycR.ycACobrar().signum() > 0);
            ycCajaDevolucion.setVisible(ycR.ycADevolver().signum() > 0);
            ycCajaDevolucion.setManaged(ycR.ycADevolver().signum() > 0);
            if (ycR.ycADevolver().signum() > 0) {
                ycLTitulo.setText("DEVUELVELE");
                ycLMonto.setText(YCControles.ycMoneda(ycR.ycADevolver()));
                ycCaja.getStyleClass().add("devolver");
                ycRegistrar.setText("Devolver " + YCControles.ycMoneda(ycR.ycADevolver()) + " y registrar salida");
            } else if (ycR.ycACobrar().signum() > 0) {
                ycLTitulo.setText("AUN TE DEBE");
                ycLMonto.setText(YCControles.ycMoneda(ycR.ycACobrar()));
                ycCaja.getStyleClass().add("cobrar");
                ycRegistrar.setText("Cobrar " + YCControles.ycMoneda(ycR.ycACobrar()) + " y registrar salida");
            } else {
                ycLTitulo.setText("QUEDAN A MANO");
                ycLMonto.setText(YCControles.ycMoneda(java.math.BigDecimal.ZERO));
                ycCaja.getStyleClass().add("a-mano");
                ycRegistrar.setText("Registrar salida");
            }
        };
        ycFecha.valueProperty().addListener((ycO, ycA, ycB) -> ycRecalculo.run());
        ycRecalcular.selectedProperty().addListener((ycO, ycA, ycB) -> ycRecalculo.run());
        ycRecalculo.run();

        ycRegistrar.setOnAction(ycEv -> {
            try {
                var ycR = ycService.ycRegistrarSalidaAnticipada(ycE.getYcId(), ycFecha.getValue(), ycRecalcular.isSelected(),
                        ycMetodoCobro.getValue(), ycMetodoDevolucion.getValue());
                ycNotificador.ycCerrarDialogo();
                ycRecargar.run();
                YCEstadia ycActualizada = ycService.ycObtener(ycE.getYcId());
                String ycMensaje = ycE.getYcHuesped() + " se queda " + ycR.ycNochesAhora() + " noches; total " + YCControles.ycMoneda(ycR.ycTotalAhora()) + ". ";
                if (ycR.ycADevolver().signum() > 0) {
                    ycNotificador.ycExitoConAcciones("Salida registrada · devolucion anotada", ycMensaje + "Devuelvele "
                                    + YCControles.ycMoneda(ycR.ycADevolver()) + " en " + ycMetodoDevolucion.getValue().ycGetDescripcion().toLowerCase()
                                    + ". Imprime el comprobante y pidele que firme \"Recibi conforme\". Desde el "
                                    + YCControles.ycFecha(ycFecha.getValue()) + " la habitacion queda libre.",
                            "Cerrar", null, "Imprimir comprobante", () -> ycMensajes.ycVerComprobante(ycActualizada));
                } else if (ycR.ycACobrar().signum() > 0) {
                    ycNotificador.ycExitoConAcciones("Salida registrada", ycMensaje + "Pago lo que faltaba: "
                                    + YCControles.ycMoneda(ycR.ycACobrar()) + " (" + ycMetodoCobro.getValue() + ").",
                            "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycActualizada));
                } else {
                    ycNotificador.ycExitoConAcciones("Salida registrada", ycMensaje + "Quedan a mano.",
                            "Cerrar", null, "Ver comprobante", () -> ycMensajes.ycVerComprobante(ycActualizada));
                }
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Button ycCancelarBtn = new Button("Cancelar");
        ycCancelarBtn.getStyleClass().add("button-outlined");
        ycCancelarBtn.setCancelButton(true);
        ycCancelarBtn.setOnAction(ycEv -> ycNotificador.ycCerrarDialogo());
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycEsp, ycCancelarBtn, ycRegistrar);
        ycPie.getStyleClass().add("dialogo-acciones");

        VBox ycDialogo = new VBox(12, ycTitulo, ycSub, new VBox(4, ycEFecha, ycFecha), ycRecalcular, ycAyuda, ycCaja, ycCajaMetodo,
                ycCajaDevolucion, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(520);
        ycDialogo.setMaxSize(520, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    void ycCancelar(YCEstadia ycE, Runnable ycRecargar) {
        if (ycE == null) {
            return;
        }
        if (YCEstadiaService.ycDebeAntesDeCancelar(ycE)) {
            ycNotificador.ycAvisoConAccion("Primero cobra el saldo",
                    ycE.getYcHuesped() + " ya se hospedo en " + ycE.getYcCuartoNumero() + " y aun te debe "
                            + YCControles.ycMoneda(ycE.ycGetSaldo()) + ". Cuando pague todo, podras cancelar la estadia.",
                    "Volver", "Cobrar " + YCControles.ycMoneda(ycE.ycGetSaldo()), () -> ycCobrarSaldo(ycE, ycRecargar));
            return;
        }
        ycNotificador.ycConfirmarConMotivo("¿Cancelar la estadia de " + ycE.getYcHuesped() + "?",
                "Queda en el historial como cancelada y " + ycE.getYcCuartoNumero()
                        + " se libera para esas fechas. Si ya pagaron, devuelve el dinero aparte.",
                "Ej. El huesped aviso que no viajara",
                "Cancelar estadia",
                ycMotivo -> ycEjecutar(() -> ycService.ycCancelar(ycE.getYcId(), ycMotivo), "Estadia cancelada.", ycRecargar));
    }

    private void ycEjecutar(Runnable ycAccion, String ycMensaje, Runnable ycRecargar) {
        try {
            ycAccion.run();
            ycNotificador.ycExito(ycMensaje);
            ycRecargar.run();
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        }
    }

    void ycRegistrarLlegada(YCEstadia ycE, Runnable ycRecargar) {
        try {
            if (ycE.ycIsDeGrupo()) {
                List<YCEstadia> ycLlegaron = ycService.ycRegistrarLlegadaGrupo(ycE);
                ycRecargar.run();
                BigDecimal ycDebe = ycService.ycDelGrupo(ycE).stream().map(YCEstadia::ycGetSaldo)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String ycDonde = String.join(", ", ycLlegaron.stream().map(YCEstadia::getYcCuartoNumero).toList());
                if (ycDebe.signum() > 0) {
                    ycNotificador.ycExitoConAcciones(ycE.getYcHuesped() + " llego con su grupo",
                            "Check-in registrado en " + ycDonde + ". Aun debe " + YCControles.ycMoneda(ycDebe) + ".",
                            "Despues", null, "Cobrar saldo ahora", () -> ycCobrarSaldo(ycLlegaron.get(0), ycRecargar));
                } else {
                    ycNotificador.ycExito("Check-in de " + ycE.getYcHuesped() + " registrado en " + ycDonde + ".");
                }
                return;
            }
            YCEstadia ycActualizada = ycService.ycRegistrarLlegada(ycE.getYcId());
            ycRecargar.run();
            if (ycActualizada.ycTieneSaldo()) {
                ycNotificador.ycExitoConAcciones(ycActualizada.getYcHuesped() + " llego",
                        "Check-in registrado en " + ycActualizada.getYcCuartoNumero() + ". Aun debe "
                                + YCControles.ycMoneda(ycActualizada.ycGetSaldo()) + ".",
                        "Despues", null, "Cobrar saldo ahora", () -> ycCobrarSaldo(ycActualizada, ycRecargar));
            } else {
                ycNotificador.ycExito("Check-in de " + ycActualizada.getYcHuesped() + " registrado en "
                        + ycActualizada.getYcCuartoNumero() + ".");
            }
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
        }
    }

    void ycRegistrarNoLlego(YCEstadia ycE, Runnable ycRecargar) {
        BigDecimal ycRecibido = ycE.ycGetRecibido();
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.USER_X));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "peligro");
        Label ycTitulo = new Label("¿" + ycE.getYcHuesped() + " no vino?");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycMensaje = new Label(ycE.ycGetNumero() + " · " + ycE.getYcCuartoNumero() + " del " + YCControles.ycFecha(ycE.getYcEntrada())
                + " al " + YCControles.ycFecha(ycE.getYcSalida()) + ".\nLa reserva queda en el historial como \"No vino\" y "
                + ycE.getYcCuartoNumero() + " se libera para alquilarla a otra persona.");
        ycMensaje.getStyleClass().add("dialogo-mensaje");
        ycMensaje.setWrapText(true);
        VBox ycCuerpo = new VBox(12, ycCirculo, ycTitulo, ycMensaje);
        ComboBox<YCMetodoPago> ycMetodoNoVino = new ComboBox<>();
        CheckBox ycDevolver = new CheckBox("Devolverle el adelanto (" + YCControles.ycMoneda(ycRecibido) + ")");
        if (ycRecibido.signum() > 0) {
            Label ycAyuda = new Label("Si no lo marcas, el adelanto se queda como penalidad por no presentarse "
                    + "y se sigue contando en lo cobrado del mes.");
            ycAyuda.getStyleClass().add("campo-ayuda");
            ycAyuda.setWrapText(true);
            ycCuerpo.getChildren().addAll(ycDevolver, ycAyuda);
            ycMetodoNoVino.setMaxWidth(Double.MAX_VALUE);
            ycMetodoNoVino.getItems().setAll(YCMetodoPago.values());
            ycMetodoNoVino.setValue(YCMetodoPago.YC_EFECTIVO);
            Label ycEtNoVino = new Label("¿Como le devuelves el adelanto?");
            ycEtNoVino.getStyleClass().add("campo-label");
            VBox ycCajaNoVino = new VBox(4, ycEtNoVino, ycMetodoNoVino);
            ycCajaNoVino.visibleProperty().bind(ycDevolver.selectedProperty());
            ycCajaNoVino.managedProperty().bind(ycDevolver.selectedProperty());
            ycCuerpo.getChildren().add(ycCajaNoVino);
        }
        Button ycVolver = new Button("Volver");
        ycVolver.getStyleClass().add("button-outlined");
        ycVolver.setCancelButton(true);
        ycVolver.setOnAction(ycEv -> ycNotificador.ycCerrarDialogo());
        Button ycConfirmar = new Button("Liberar habitacion", new FontIcon(Feather.UNLOCK));
        ycConfirmar.getStyleClass().add("danger");
        ycConfirmar.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            boolean ycDevuelve = ycDevolver.isSelected();
            YCMetodoPago ycComoDevuelve = ycMetodoNoVino.getValue();
            ycEjecutar(() -> ycService.ycRegistrarNoLlego(ycE.getYcId(), ycDevuelve, ycComoDevuelve), ycE.getYcCuartoNumero() + " quedo libre."
                    + (ycRecibido.signum() <= 0 ? ""
                    : ycDevuelve ? " Devuelvele " + YCControles.ycMoneda(ycRecibido) + " en "
                    + ycComoDevuelve.ycGetDescripcion().toLowerCase() + " (quedo anotado)."
                    : " El adelanto de " + YCControles.ycMoneda(ycRecibido) + " se queda."), ycRecargar);
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycAcciones = new HBox(10, ycEsp, ycVolver, ycConfirmar);
        ycAcciones.getStyleClass().add("dialogo-acciones");
        ycCuerpo.getChildren().add(ycAcciones);
        ycCuerpo.getStyleClass().add("dialogo");
        ycCuerpo.setMaxSize(460, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycCuerpo);
    }
}
