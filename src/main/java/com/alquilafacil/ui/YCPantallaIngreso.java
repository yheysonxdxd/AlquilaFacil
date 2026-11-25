package com.alquilafacil.ui;

import atlantafx.base.util.Animations;
import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.model.YCUsuario;
import com.alquilafacil.service.YCUsuarioService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class YCPantallaIngreso {
    private final YCUsuarioService ycUsuarioService;
    private final YCDatosArrendador ycArrendador;

    public Node ycCrear(Consumer<YCUsuario> ycAlIngresar) {
        StackPane ycCapa = new StackPane();
        ycCapa.getStyleClass().add("ingreso-capa");
        Region ycFondo = new Region();
        ycFondo.getStyleClass().add("ingreso-fondo");
        Region ycVelo = new Region();
        ycVelo.getStyleClass().add("ingreso-velo");
        ycCapa.getChildren().addAll(ycFondo, ycVelo);
        ycMostrar(ycCapa, ycUsuarioService.ycHayUsuarios() ? ycFormIngreso(ycCapa, ycAlIngresar) : ycFormPrimerAdmin(ycCapa, ycAlIngresar));
        return ycCapa;
    }

    private static void ycMostrar(StackPane ycCapa, VBox ycTarjeta) {
        if (ycCapa.getChildren().size() > 2) {
            ycCapa.getChildren().remove(2, ycCapa.getChildren().size());
        }
        ycCapa.getChildren().add(ycTarjeta);
        Animations.fadeInUp(ycTarjeta, Duration.millis(420)).play();
    }

    private VBox ycFormIngreso(StackPane ycCapa, Consumer<YCUsuario> ycAlIngresar) {
        TextField ycUsuario = ycCampoTexto("Usuario", Feather.USER);
        PasswordField ycClave = ycCampoClave("Contraseña");
        Label ycError = ycError();
        Button ycEntrar = new Button("Ingresar", new FontIcon(Feather.LOG_IN));
        ycEntrar.getStyleClass().addAll("accent", "ingreso-boton");
        ycEntrar.setDefaultButton(true);
        ycEntrar.setMaxWidth(Double.MAX_VALUE);
        ycEntrar.setOnAction(ycE -> {
            try {
                YCUsuario ycU = ycUsuarioService.ycIngresar(ycUsuario.getText(), ycClave.getText());
                ycAlIngresar.accept(ycU);
            } catch (YCNegocioException ycEx) {
                ycMostrarError(ycError, ycEx.getMessage());
                ycClave.clear();
                ycClave.requestFocus();
            }
        });
        Hyperlink ycOlvide = new Hyperlink("¿Olvidaste tu contraseña?");
        ycOlvide.getStyleClass().add("ingreso-ayuda");
        ycOlvide.setOnAction(ycE -> ycMostrar(ycCapa, ycFormRecuperar(ycCapa, ycAlIngresar, ycUsuario.getText())));
        VBox ycT = ycTarjeta("Bienvenido", "Ingresa para continuar",
                ycEtiqueta("Usuario"), ycUsuario, ycEtiqueta("Contraseña"), ycClave, ycError, ycEntrar, ycOlvide);
        Platform.runLater(ycUsuario::requestFocus);
        return ycT;
    }

    private VBox ycFormRecuperar(StackPane ycCapa, Consumer<YCUsuario> ycAlIngresar, String ycUsuarioEscrito) {
        TextField ycUsuario = ycCampoTexto("Usuario del administrador", Feather.USER);
        ycUsuario.setText(ycUsuarioEscrito == null ? "" : ycUsuarioEscrito.trim());
        TextField ycCodigo = ycCampoTexto("Ej. K7QM-3XRP-W9TD", Feather.KEY);
        PasswordField ycNueva = ycCampoClave("Contraseña nueva (minimo 6)");
        PasswordField ycRepetir = ycCampoClave("Repite la contraseña nueva");
        Label ycError = ycError();
        Button ycCambiar = new Button("Poner contraseña nueva", new FontIcon(Feather.CHECK));
        ycCambiar.getStyleClass().addAll("accent", "ingreso-boton");
        ycCambiar.setDefaultButton(true);
        ycCambiar.setMaxWidth(Double.MAX_VALUE);
        ycCambiar.setOnAction(ycE -> {
            if (!ycNueva.getText().equals(ycRepetir.getText())) {
                ycMostrarError(ycError, "Las contraseñas no coinciden.");
                return;
            }
            try {
                String ycOtro = ycUsuarioService.ycRecuperar(ycUsuario.getText(), ycCodigo.getText(), ycNueva.getText());
                ycMostrar(ycCapa, ycTarjetaCodigo(ycOtro, "Contraseña cambiada",
                        "Tu codigo anterior ya no sirve. Este es tu NUEVO codigo de recuperacion:",
                        "Ya lo anote, ir a ingresar",
                        () -> ycMostrar(ycCapa, ycFormIngreso(ycCapa, ycAlIngresar))));
            } catch (YCNegocioException ycEx) {
                ycMostrarError(ycError, ycEx.getMessage());
            }
        });
        Hyperlink ycVolver = new Hyperlink("Volver a ingresar");
        ycVolver.getStyleClass().add("ingreso-ayuda");
        ycVolver.setOnAction(ycE -> ycMostrar(ycCapa, ycFormIngreso(ycCapa, ycAlIngresar)));
        Label ycAyuda = new Label("Solo para el administrador, con el codigo que anoto al crear su cuenta. "
                + "Si eres de recepcion, pide al administrador que te ponga una contraseña nueva.");
        ycAyuda.getStyleClass().add("ingreso-ayuda");
        ycAyuda.setWrapText(true);
        VBox ycT = ycTarjeta("Recuperar contraseña", "Usa tu codigo de recuperacion",
                ycEtiqueta("Usuario"), ycUsuario, ycEtiqueta("Codigo de recuperacion"), ycCodigo,
                ycEtiqueta("Contraseña nueva"), ycNueva, ycRepetir, ycError, ycCambiar, ycAyuda, ycVolver);
        Platform.runLater(() -> (ycUsuario.getText().isBlank() ? ycUsuario : ycCodigo).requestFocus());
        return ycT;
    }

    private VBox ycFormPrimerAdmin(StackPane ycCapa, Consumer<YCUsuario> ycAlIngresar) {
        TextField ycNombre = ycCampoTexto("Ej. Juan Quispe", Feather.USER);
        TextField ycUsuario = ycCampoTexto("Ej. juan", Feather.AT_SIGN);
        PasswordField ycClave = ycCampoClave("Minimo 6 caracteres");
        PasswordField ycRepetir = ycCampoClave("Escribela otra vez");
        Label ycError = ycError();
        Button ycCrear = new Button("Crear mi cuenta y entrar", new FontIcon(Feather.CHECK));
        ycCrear.getStyleClass().addAll("accent", "ingreso-boton");
        ycCrear.setDefaultButton(true);
        ycCrear.setMaxWidth(Double.MAX_VALUE);
        ycCrear.setOnAction(ycE -> {
            if (!ycClave.getText().equals(ycRepetir.getText())) {
                ycMostrarError(ycError, "Las contraseñas no coinciden.");
                return;
            }
            try {
                ycUsuarioService.ycCrear(ycUsuario.getText(), ycNombre.getText(), YCRol.YC_ADMINISTRADOR, ycClave.getText());
                YCUsuario ycU = ycUsuarioService.ycIngresar(ycUsuario.getText(), ycClave.getText());
                String ycCodigo = ycUsuarioService.ycNuevoCodigoRecuperacion();
                ycMostrar(ycCapa, ycTarjetaCodigo(ycCodigo, "Tu codigo de recuperacion",
                        "Si algun dia olvidas tu contraseña, con este codigo pones una nueva. "
                                + "Anotalo en papel y guardalo lejos de la computadora:",
                        "Ya lo anote, entrar", () -> ycAlIngresar.accept(ycU)));
            } catch (YCNegocioException ycEx) {
                ycMostrarError(ycError, ycEx.getMessage());
            }
        });
        Label ycAyuda = new Label("Esta es la cuenta del dueño (administrador). Despues podras crear usuarios "
                + "para la recepcion desde \"Usuarios\". Anota tu contraseña en un lugar seguro.");
        ycAyuda.getStyleClass().add("ingreso-ayuda");
        ycAyuda.setWrapText(true);
        Label ycAyudaUsuario = new Label("Una sola palabra, sin espacios. Es lo que escribiras para entrar.");
        ycAyudaUsuario.getStyleClass().add("campo-ayuda");
        VBox ycT = ycTarjeta("Configura tu cuenta", "Es la primera vez: crea el usuario administrador",
                ycEtiqueta("Tu nombre"), ycNombre, ycEtiqueta("Usuario para entrar"), ycUsuario, ycAyudaUsuario,
                ycEtiqueta("Contraseña"), ycClave, ycEtiqueta("Repite la contraseña"), ycRepetir, ycError, ycCrear, ycAyuda);
        Platform.runLater(ycNombre::requestFocus);
        return ycT;
    }

    private VBox ycTarjetaCodigo(String ycCodigo, String ycTitulo, String ycExplicacion, String ycTextoBoton, Runnable ycAlContinuar) {
        Label ycTexto = new Label(ycExplicacion);
        ycTexto.getStyleClass().add("ingreso-ayuda");
        ycTexto.setWrapText(true);
        Label ycValor = new Label(ycCodigo);
        ycValor.getStyleClass().add("codigo-recuperacion");
        ycValor.setMaxWidth(Double.MAX_VALUE);
        ycValor.setAlignment(Pos.CENTER);
        Button ycCopiar = new Button("Copiar", new FontIcon(Feather.COPY));
        ycCopiar.getStyleClass().add("flat");
        ycCopiar.setOnAction(ycE -> {
            ClipboardContent ycCc = new ClipboardContent();
            ycCc.putString(ycCodigo);
            Clipboard.getSystemClipboard().setContent(ycCc);
            ycCopiar.setText("Copiado");
        });
        CheckBox ycAnotado = new CheckBox("Lo anote en un lugar seguro");
        Button ycSeguir = new Button(ycTextoBoton, new FontIcon(Feather.ARROW_RIGHT));
        ycSeguir.getStyleClass().addAll("accent", "ingreso-boton");
        ycSeguir.setMaxWidth(Double.MAX_VALUE);
        ycSeguir.disableProperty().bind(ycAnotado.selectedProperty().not());
        ycSeguir.setDefaultButton(true);
        ycSeguir.setOnAction(ycE -> ycAlContinuar.run());
        Label ycAviso = new Label("No se vuelve a mostrar. Puedes crear otro desde \"Usuarios\".");
        ycAviso.getStyleClass().add("campo-ayuda");
        ycAviso.setWrapText(true);
        return ycTarjeta(ycTitulo, "Guardalo bien", ycTexto, ycValor, ycCopiar, ycAnotado, ycSeguir, ycAviso);
    }

    private VBox ycTarjeta(String ycTitulo, String ycSubtitulo, Node... ycContenido) {
        StackPane ycLogo = new StackPane(new FontIcon(Feather.HOME));
        ycLogo.getStyleClass().add("ingreso-logo");
        Label ycMarca = new Label(ycArrendador.getYcNombreSistema());
        ycMarca.getStyleClass().add("ingreso-marca");
        Label ycT = new Label(ycTitulo);
        ycT.getStyleClass().add("ingreso-titulo");
        Label ycS = new Label(ycSubtitulo);
        ycS.getStyleClass().add("ingreso-subtitulo");
        ycS.setWrapText(true);
        VBox ycCabecera = new VBox(6, ycLogo, ycMarca, ycT, ycS);
        ycCabecera.setAlignment(Pos.CENTER);
        VBox ycCaja = new VBox(8);
        ycCaja.getChildren().add(ycCabecera);
        Region ycEsp = new Region();
        ycEsp.setMinHeight(10);
        ycCaja.getChildren().add(ycEsp);
        ycCaja.getChildren().addAll(ycContenido);
        ycCaja.getStyleClass().add("ingreso-tarjeta");
        ycCaja.setMaxSize(400, Region.USE_PREF_SIZE);
        ycCaja.setPrefWidth(400);
        return ycCaja;
    }

    private static Label ycEtiqueta(String ycTexto) {
        Label ycL = new Label(ycTexto);
        ycL.getStyleClass().add("campo-label");
        return ycL;
    }

    private static TextField ycCampoTexto(String ycPrompt, Feather ycIcono) {
        atlantafx.base.controls.CustomTextField ycT = new atlantafx.base.controls.CustomTextField();
        ycT.setPromptText(ycPrompt);
        ycT.setLeft(new FontIcon(ycIcono));
        ycT.getStyleClass().add("ingreso-campo");
        return ycT;
    }

    private static PasswordField ycCampoClave(String ycPrompt) {
        PasswordField ycP = new PasswordField();
        ycP.setPromptText(ycPrompt);
        ycP.getStyleClass().add("ingreso-campo");
        return ycP;
    }

    private static Label ycError() {
        Label ycL = new Label();
        ycL.getStyleClass().add("ingreso-error");
        ycL.setWrapText(true);
        ycL.setVisible(false);
        ycL.setManaged(false);
        return ycL;
    }

    private static void ycMostrarError(Label ycL, String ycMsg) {
        ycL.setText(ycMsg);
        ycL.setGraphic(new FontIcon(Feather.ALERT_CIRCLE));
        ycL.setVisible(true);
        ycL.setManaged(true);
        Animations.shakeX(ycL, 4).play();
    }
}
