package com.alquilafacil.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
public class YCNotificador {
    private static final Duration YC_DURACION_TOAST = Duration.seconds(3.5);

    private ModalPane ycModalPanel;
    private ModalPane ycModalDialogo;
    private VBox ycZonaToasts;

    public void ycRegistrar(ModalPane ycModalPanel, ModalPane ycModalDialogo, VBox ycZonaToasts) {
        this.ycModalPanel = ycModalPanel;
        this.ycModalDialogo = ycModalDialogo;
        this.ycZonaToasts = ycZonaToasts;
    }

    public void ycExito(String ycMensaje) {
        ycToast(ycMensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS);
    }

    public void ycAdvertencia(String ycMensaje) {
        ycToast(ycMensaje, Feather.ALERT_TRIANGLE, Styles.WARNING);
    }

    public void ycError(String ycMensaje) {
        ycToast(ycMensaje, Feather.X_CIRCLE, Styles.DANGER);
    }

    public void ycInfo(String ycMensaje) {
        ycToast(ycMensaje, Feather.INFO, Styles.ACCENT);
    }

    private void ycToast(String ycMensaje, Ikon ycIcono, String ycEstilo) {
        Notification ycN = new Notification(ycMensaje, new FontIcon(ycIcono));
        ycN.getStyleClass().addAll(ycEstilo, Styles.ELEVATED_2, "toast");
        ycN.setPrefWidth(360);
        ycN.setMaxWidth(360);
        ycN.setOnClose(ycE -> ycCerrarToast(ycN));

        ycZonaToasts.getChildren().add(ycN);
        Animations.fadeInUp(ycN, Duration.millis(280)).play();

        PauseTransition ycEspera = new PauseTransition(YC_DURACION_TOAST);
        ycEspera.setOnFinished(ycE -> ycCerrarToast(ycN));
        ycEspera.play();
    }

    private void ycCerrarToast(Notification ycN) {
        if (!ycZonaToasts.getChildren().contains(ycN) || ycN.getProperties().containsKey("cerrando")) {
            return;
        }
        ycN.getProperties().put("cerrando", true);
        Timeline ycSalida = Animations.fadeOutRight(ycN, Duration.millis(250));
        ycSalida.setOnFinished(ycE -> ycZonaToasts.getChildren().remove(ycN));
        ycSalida.play();
    }

    public void ycConfirmar(String ycTitulo, String ycMensaje, String ycTextoAccion, Runnable ycAlConfirmar) {
        ycDialogo(Feather.TRASH_2, "peligro", ycTitulo, ycMensaje,
                "Cancelar", null, ycTextoAccion, Styles.DANGER, ycAlConfirmar);
    }

    public void ycConfirmarConMotivo(String ycTitulo, String ycMensaje, String ycEjemplo, String ycTextoAccion,
                                   java.util.function.Consumer<String> ycAlConfirmar) {
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.ALERT_TRIANGLE));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "peligro");
        Label ycLblTitulo = new Label(ycTitulo);
        ycLblTitulo.getStyleClass().add("dialogo-titulo");
        ycLblTitulo.setWrapText(true);
        Label ycLblMensaje = new Label(ycMensaje);
        ycLblMensaje.getStyleClass().add("dialogo-mensaje");
        ycLblMensaje.setWrapText(true);
        Label ycEtiqueta = new Label("Motivo *");
        ycEtiqueta.getStyleClass().add("campo-label");
        javafx.scene.control.TextField ycMotivo = new javafx.scene.control.TextField();
        ycMotivo.setPromptText(ycEjemplo);
        ycMotivo.setTextFormatter(new javafx.scene.control.TextFormatter<String>(
                ycC -> ycC.getControlNewText().length() <= 200 ? ycC : null));
        Label ycAyuda = new Label("Queda guardado junto con tu usuario y la hora.");
        ycAyuda.getStyleClass().add("campo-ayuda");

        Button ycCancelar = new Button("Volver");
        ycCancelar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycModalDialogo.hide());
        Button ycAceptar = new Button(ycTextoAccion);
        ycAceptar.getStyleClass().add(Styles.DANGER);
        ycAceptar.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> ycMotivo.getText().trim().length() < 5, ycMotivo.textProperty()));
        ycAceptar.setDefaultButton(true);
        ycAceptar.setOnAction(ycE -> {
            ycModalDialogo.hide();
            ycAlConfirmar.accept(ycMotivo.getText().trim());
        });
        Region ycEspacio = new Region();
        HBox.setHgrow(ycEspacio, javafx.scene.layout.Priority.ALWAYS);
        HBox ycAcciones = new HBox(10, ycEspacio, ycCancelar, ycAceptar);
        ycAcciones.setAlignment(Pos.CENTER_RIGHT);
        ycAcciones.getStyleClass().add("dialogo-acciones");

        VBox ycCampo = new VBox(4, ycEtiqueta, ycMotivo, ycAyuda);
        VBox ycDialogo = new VBox(14, ycCirculo, ycLblTitulo, ycLblMensaje, ycCampo, ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(460, Region.USE_PREF_SIZE);
        ycModalDialogo.usePredefinedTransitionFactories(null);
        ycModalDialogo.show(ycDialogo);
        javafx.application.Platform.runLater(ycMotivo::requestFocus);
    }

    public void ycExitoConAcciones(String ycTitulo, String ycMensaje,
                                 String ycTextoSecundario, Runnable ycSecundaria,
                                 String ycTextoPrincipal, Runnable ycPrincipal) {
        ycDialogo(Feather.CHECK_CIRCLE, "exito", ycTitulo, ycMensaje,
                ycTextoSecundario, ycSecundaria, ycTextoPrincipal, Styles.ACCENT, ycPrincipal);
    }

    public void ycAvisoConAccion(String ycTitulo, String ycMensaje, String ycTextoSecundario,
                               String ycTextoPrincipal, Runnable ycPrincipal) {
        ycDialogo(Feather.ALERT_TRIANGLE, "peligro", ycTitulo, ycMensaje,
                ycTextoSecundario, null, ycTextoPrincipal, Styles.ACCENT, ycPrincipal);
    }

    private void ycDialogo(Ikon ycIconoDialogo, String ycClaseIcono, String ycTitulo, String ycMensaje,
                         String ycTextoSecundario, Runnable ycSecundaria,
                         String ycTextoPrincipal, String ycEstiloPrincipal, Runnable ycPrincipal) {
        FontIcon ycIcono = new FontIcon(ycIconoDialogo);
        StackPane ycCirculo = new StackPane(ycIcono);
        ycCirculo.getStyleClass().addAll("dialogo-icono", ycClaseIcono);

        Label ycLblTitulo = new Label(ycTitulo);
        ycLblTitulo.getStyleClass().add("dialogo-titulo");
        Label ycLblMensaje = new Label(ycMensaje);
        ycLblMensaje.getStyleClass().add("dialogo-mensaje");
        ycLblMensaje.setWrapText(true);

        Button ycCancelar = new Button(ycTextoSecundario);
        ycCancelar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> {
            ycModalDialogo.hide();
            if (ycSecundaria != null) {
                ycSecundaria.run();
            }
        });

        Button ycAceptar = new Button(ycTextoPrincipal);
        ycAceptar.getStyleClass().add(ycEstiloPrincipal);
        ycAceptar.setOnAction(ycE -> {
            ycModalDialogo.hide();
            ycPrincipal.run();
        });

        Region ycEspacio = new Region();
        HBox.setHgrow(ycEspacio, javafx.scene.layout.Priority.ALWAYS);
        HBox ycAcciones = new HBox(10, ycEspacio, ycCancelar, ycAceptar);
        ycAcciones.setAlignment(Pos.CENTER_RIGHT);
        ycAcciones.getStyleClass().add("dialogo-acciones");

        VBox ycDialogo = new VBox(14, ycCirculo, ycLblTitulo, ycLblMensaje, ycAcciones);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(440, Region.USE_PREF_SIZE);

        ycModalDialogo.usePredefinedTransitionFactories(null);
        ycModalDialogo.show(ycDialogo);
        ycAceptar.requestFocus();
    }

    public void ycMostrarDialogo(Node ycContenido) {
        ycModalDialogo.usePredefinedTransitionFactories(null);
        ycModalDialogo.show(ycContenido);
    }

    public void ycCerrarDialogo() {
        ycModalDialogo.hide();
    }

    public void ycAbrirPanel(Node ycContenido) {
        ycModalPanel.usePredefinedTransitionFactories(Side.RIGHT,
                Duration.millis(320), Duration.millis(220));
        ycModalPanel.show(ycContenido);
    }

    public void ycCerrarPanel() {
        ycModalPanel.hide();
    }
}
