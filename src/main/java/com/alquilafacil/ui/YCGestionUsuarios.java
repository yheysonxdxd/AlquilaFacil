package com.alquilafacil.ui;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.model.YCUsuario;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.service.YCUsuarioService;
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

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class YCGestionUsuarios {
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final YCUsuarioService ycService;
    private final YCSesion ycSesion;
    private final YCNotificador ycNotificador;

    public void ycMostrarLista() {
        VBox ycLista = new VBox(8);
        for (YCUsuario ycU : ycService.ycListar()) {
            ycLista.getChildren().add(ycFila(ycU));
        }

        TextField ycNombre = new TextField();
        ycNombre.setPromptText("Nombre (ej. Rosa Huaman)");
        TextField ycUsuario = new TextField();
        ycUsuario.setPromptText("Usuario (ej. rosa)");
        PasswordField ycClave = new PasswordField();
        ycClave.setPromptText("Contraseña (min. 6)");
        ComboBox<YCRol> ycRol = new ComboBox<>();
        ycRol.getItems().setAll(YCRol.values());
        ycRol.setValue(YCRol.YC_RECEPCION);
        Label ycDetalleRol = new Label(YCRol.YC_RECEPCION.ycGetDetalle());
        ycDetalleRol.getStyleClass().add("campo-ayuda");
        ycDetalleRol.setWrapText(true);
        ycRol.valueProperty().addListener((ycO, ycA, ycR) -> ycDetalleRol.setText(ycR == null ? "" : ycR.ycGetDetalle()));
        for (Control ycC : new Control[]{ycNombre, ycUsuario, ycClave, ycRol}) {
            HBox.setHgrow(ycC, Priority.ALWAYS);
            ycC.setMaxWidth(Double.MAX_VALUE);
        }
        Button ycCrear = new Button("Crear usuario", new FontIcon(Feather.USER_PLUS));
        ycCrear.getStyleClass().add("accent");
        ycCrear.setOnAction(ycE -> {
            try {
                YCUsuario ycU = ycService.ycCrear(ycUsuario.getText(), ycNombre.getText(), ycRol.getValue(), ycClave.getText());
                ycNotificador.ycExito("Usuario " + ycU.getYcUsuario() + " creado. Ya puede ingresar con su contraseña.");
                ycNotificador.ycCerrarDialogo();
                ycMostrarLista();
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Label ycTituloNuevo = new Label("NUEVO USUARIO");
        ycTituloNuevo.getStyleClass().add("seccion-form");
        VBox ycNuevo = new VBox(8, ycTituloNuevo, new HBox(8, ycNombre, ycUsuario), new HBox(8, ycClave, ycRol), ycDetalleRol, ycCrear);
        ycNuevo.getStyleClass().add("usuarios-nuevo");

        Label ycTitulo = new Label("Usuarios del sistema");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label("Cada persona entra con su propio usuario. La recepcion no puede borrar, anular recibos ni ver gastos.");
        ycSub.getStyleClass().add("dialogo-mensaje");
        ycSub.setWrapText(true);
        Button ycCerrar = new Button("Cerrar");
        ycCerrar.getStyleClass().add("button-outlined");
        ycCerrar.setCancelButton(true);
        ycCerrar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        boolean ycTieneCodigo = ycService.ycTengoCodigoRecuperacion();
        Button ycCodigo = new Button(ycTieneCodigo ? "Cambiar mi codigo de recuperacion" : "Crear mi codigo de recuperacion",
                new FontIcon(Feather.SHIELD));
        ycCodigo.getStyleClass().add(ycTieneCodigo ? "flat" : "success");
        ycCodigo.setOnAction(ycE -> ycConfirmarNuevoCodigo(ycTieneCodigo));
        Region ycEspPie = new Region();
        HBox.setHgrow(ycEspPie, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycCodigo, ycEspPie, ycCerrar);
        ycPie.setAlignment(Pos.CENTER_LEFT);
        ScrollPane ycScroll = new ScrollPane(ycLista);
        ycScroll.setFitToWidth(true);
        ycScroll.setMaxHeight(260);
        ycScroll.getStyleClass().add("pagina-scroll");
        VBox ycDialogo = new VBox(14, ycTitulo, ycSub, ycScroll, ycNuevo, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(620);
        ycDialogo.setMaxSize(620, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    private HBox ycFila(YCUsuario ycU) {
        StackPane ycAvatar = new StackPane(new Label(YCTablas.ycIniciales(ycU.getYcNombre())));
        ycAvatar.getStyleClass().add("avatar");
        Label ycNombre = new Label(ycU.getYcNombre() + "  ·  " + ycU.getYcUsuario());
        ycNombre.getStyleClass().add("ficha-dato-valor");
        Label ycDetalle = new Label((ycU.getYcUltimoIngreso() == null ? "Aun no ingresa" : "Ultimo ingreso " + ycU.getYcUltimoIngreso().format(YC_FECHA))
                + (ycU.isYcActivo() ? "" : "  ·  DESACTIVADO"));
        ycDetalle.getStyleClass().add("ficha-lateral-nota");
        VBox ycTextos = new VBox(1, ycNombre, ycDetalle);
        HBox.setHgrow(ycTextos, Priority.ALWAYS);
        Label ycRol = new Label(ycU.getYcRol().ycGetTexto());
        ycRol.getStyleClass().addAll("chip", ycU.ycEsAdmin() ? "advertencia" : "neutro");
        Button ycClave = new Button(null, new FontIcon(Feather.KEY));
        ycClave.getStyleClass().addAll("flat", "button-circle");
        ycClave.setTooltip(new Tooltip("Poner una contraseña nueva"));
        ycClave.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            ycMostrarCambioClave(ycU, false);
        });
        Button ycActivo = new Button(null, new FontIcon(ycU.isYcActivo() ? Feather.USER_X : Feather.USER_CHECK));
        ycActivo.getStyleClass().addAll("flat", "button-circle", ycU.isYcActivo() ? "danger" : "success");
        ycActivo.setTooltip(new Tooltip(ycU.isYcActivo() ? "Desactivar (ya no podra entrar)" : "Activar"));
        ycActivo.setOnAction(ycE -> {
            try {
                ycService.ycCambiarActivo(ycU.getYcId(), !ycU.isYcActivo());
                ycNotificador.ycExito(ycU.getYcUsuario() + (ycU.isYcActivo() ? " fue desactivado." : " fue activado."));
                ycNotificador.ycCerrarDialogo();
                ycMostrarLista();
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        HBox ycFila = new HBox(12, ycAvatar, ycTextos, ycRol, ycClave, ycActivo);
        ycFila.setAlignment(Pos.CENTER_LEFT);
        ycFila.getStyleClass().add("recordatorio-fila");
        return ycFila;
    }

    public void ycConfirmarNuevoCodigo(boolean ycYaTenia) {
        if (!ycYaTenia) {
            ycMostrarNuevoCodigo();
            return;
        }
        ycNotificador.ycAvisoConAccion("¿Crear un codigo nuevo?",
                "El codigo que tenias anotado dejara de servir. Tendras que anotar el nuevo.",
                "Cancelar", "Crear codigo nuevo", this::ycMostrarNuevoCodigo);
    }

    public void ycMostrarNuevoCodigo() {
        String ycCodigo;
        try {
            ycCodigo = ycService.ycNuevoCodigoRecuperacion();
        } catch (YCNegocioException ycEx) {
            ycNotificador.ycError(ycEx.getMessage());
            return;
        }
        StackPane ycCirculo = new StackPane(new FontIcon(Feather.SHIELD));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Tu codigo de recuperacion");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycTexto = new Label("Si olvidas tu contraseña, en la pantalla de ingreso elige \"¿Olvidaste tu contraseña?\" "
                + "y escribe este codigo. Anotalo en papel y guardalo lejos de la computadora. No se vuelve a mostrar.");
        ycTexto.getStyleClass().add("dialogo-mensaje");
        ycTexto.setWrapText(true);
        Label ycValor = new Label(ycCodigo);
        ycValor.getStyleClass().add("codigo-recuperacion");
        ycValor.setMaxWidth(Double.MAX_VALUE);
        ycValor.setAlignment(Pos.CENTER);
        Button ycCopiar = new Button("Copiar", new FontIcon(Feather.COPY));
        ycCopiar.getStyleClass().add("flat");
        ycCopiar.setOnAction(ycE -> {
            javafx.scene.input.ClipboardContent ycCc = new javafx.scene.input.ClipboardContent();
            ycCc.putString(ycCodigo);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(ycCc);
            ycCopiar.setText("Copiado");
        });
        CheckBox ycAnotado = new CheckBox("Lo anote en un lugar seguro");
        Button ycListo = new Button("Listo", new FontIcon(Feather.CHECK));
        ycListo.getStyleClass().add("accent");
        ycListo.disableProperty().bind(ycAnotado.selectedProperty().not());
        ycListo.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            ycNotificador.ycExito("Codigo de recuperacion guardado.");
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycAnotado, ycEsp, ycListo);
        ycPie.setAlignment(Pos.CENTER_LEFT);
        VBox ycDialogo = new VBox(14, ycCirculo, ycTitulo, ycTexto, ycValor, ycCopiar, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(440, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    public void ycMostrarCambioClave(YCUsuario ycU, boolean ycPropia) {
        PasswordField ycActual = new PasswordField();
        ycActual.setPromptText("Contraseña actual");
        PasswordField ycNueva = new PasswordField();
        ycNueva.setPromptText("Contraseña nueva (min. 6)");
        PasswordField ycRepetir = new PasswordField();
        ycRepetir.setPromptText("Repite la nueva");
        Label ycTitulo = new Label(ycPropia ? "Cambiar mi contraseña" : "Nueva contraseña para " + ycU.getYcNombre());
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Button ycGuardar = new Button("Guardar", new FontIcon(Feather.CHECK));
        ycGuardar.getStyleClass().add("accent");
        ycGuardar.setDefaultButton(true);
        ycGuardar.setOnAction(ycE -> {
            if (!ycNueva.getText().equals(ycRepetir.getText())) {
                ycNotificador.ycAdvertencia("Las contraseñas nuevas no coinciden.");
                return;
            }
            try {
                ycService.ycCambiarContrasena(ycU.getYcId(), ycActual.getText(), ycNueva.getText());
                ycNotificador.ycCerrarDialogo();
                ycNotificador.ycExito("Contraseña actualizada.");
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Button ycCancelar = new Button("Cancelar");
        ycCancelar.getStyleClass().add("button-outlined");
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycEsp, ycCancelar, ycGuardar);
        boolean ycPedirActual = ycSesion.ycGetUsuario() != null && ycSesion.ycGetUsuario().getYcId().equals(ycU.getYcId());
        VBox ycDialogo = new VBox(12, ycTitulo);
        if (ycPedirActual) {
            ycDialogo.getChildren().add(ycActual);
        }
        ycDialogo.getChildren().addAll(ycNueva, ycRepetir, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(400, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }
}
