package com.alquilafacil.ui;

import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class YCFormularios {
    private static final Pattern YC_DECIMAL = Pattern.compile("\\d{0,8}([.,]\\d{0,2})?");

    private YCFormularios() {
    }

    public static void ycMarcarError(Control ycCampo, String ycMensaje) {
        ycCampo.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        Label ycError = ycEtiquetaError(ycCampo);
        if (ycError != null) {
            ycError.setText(ycMensaje);
            if (!ycError.isVisible()) {
                ycError.setVisible(true);
                ycError.setManaged(true);
                Animations.fadeInDown(ycError, Duration.millis(220)).play();
            }
        }
        Animations.shakeX(ycCampo, 4).play();
    }

    public static void ycLimpiarError(Control ycCampo) {
        ycCampo.pseudoClassStateChanged(Styles.STATE_DANGER, false);
        Label ycError = ycEtiquetaError(ycCampo);
        if (ycError != null) {
            ycError.setVisible(false);
            ycError.setManaged(false);
        }
    }

    public static void ycLimpiarErrores(Control... ycCampos) {
        for (Control ycCampo : ycCampos) {
            ycLimpiarError(ycCampo);
        }
    }

    public static void ycLimpiarAlEditar(TextInputControl... ycCampos) {
        for (TextInputControl ycCampo : ycCampos) {
            ycCampo.textProperty().addListener((ycObs, ycA, ycB) -> ycLimpiarError(ycCampo));
        }
    }

    public static List<String> ycMostrarViolaciones(ConstraintViolationException ycE,
                                                  Map<String, ? extends Control> ycCampos) {
        List<String> ycSinCampo = new ArrayList<>();
        for (ConstraintViolation<?> ycV : ycE.getConstraintViolations()) {
            Control ycCampo = ycCampos.get(ycUltimoNodo(ycV.getPropertyPath()));
            if (ycCampo != null) {
                ycMarcarError(ycCampo, ycV.getMessage());
            } else {
                ycSinCampo.add(ycV.getMessage());
            }
        }
        return ycSinCampo;
    }

    public static void ycSoloDigitos(TextInputControl ycCampo, int ycMaximo) {
        ycCampo.setTextFormatter(new TextFormatter<>(ycCambio ->
                ycCambio.getControlNewText().matches("\\d{0," + ycMaximo + "}") ? ycCambio : null));
    }

    public static void ycSoloDecimal(TextInputControl ycCampo) {
        ycCampo.setTextFormatter(new TextFormatter<>(ycCambio ->
                YC_DECIMAL.matcher(ycCambio.getControlNewText()).matches() ? ycCambio : null));
    }

    private static Label ycEtiquetaError(Control ycCampo) {
        Parent ycContenedor = ycCampo.getParent();
        while (ycContenedor != null && !ycContenedor.getStyleClass().contains("campo")) {
            ycContenedor = ycContenedor.getParent();
        }
        if (ycContenedor == null) {
            return null;
        }
        Node ycNodo = ycContenedor.lookup(".campo-error");
        return ycNodo instanceof Label ycL ? ycL : null;
    }

    private static String ycUltimoNodo(Path ycRuta) {
        String ycNombre = null;
        for (Path.Node ycNodo : ycRuta) {
            ycNombre = ycNodo.getName();
        }
        return ycNombre;
    }
}
