package com.alquilafacil.ui;

import com.alquilafacil.service.YCFotoService;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class YCFotos {
    private static final double YC_ANCHO_CARGA = 720;

    private static final String[][] YC_DEGRADADOS = {
            {"#4f8ef7", "#2a5bd7"}, {"#f59e6b", "#e0613a"}, {"#34c79a", "#14916a"},
            {"#f2b84b", "#d48a12"}, {"#ef8fb6", "#d1517f"}, {"#8b8ff5", "#5b5fd6"},
            {"#5cc4dd", "#2a8fae"}, {"#b39ddb", "#7e57c2"},
    };

    private final YCFotoService ycFotoService;
    private final Map<String, Image> ycCache = new HashMap<>();

    public Optional<Image> ycCargar(String ycNombre) {
        return ycFotoService.ycRuta(ycNombre).map(ycRuta -> ycCache.computeIfAbsent(ycNombre, ycN -> ycDesdeArchivo(ycRuta)));
    }

    public void ycOlvidar(String ycNombre) {
        if (ycNombre != null) {
            ycCache.remove(ycNombre);
        }
    }

    public static Image ycDesdeArchivo(Path ycRuta) {
        return new Image(ycRuta.toUri().toString(), YC_ANCHO_CARGA, 0, true, true, true);
    }

    public static ImageView ycCubrir(Image ycImagen, double ycAncho, double ycAlto) {
        ImageView ycVista = new ImageView(ycImagen);
        ycVista.setFitWidth(ycAncho);
        ycVista.setFitHeight(ycAlto);
        ycVista.setSmooth(true);
        Runnable ycAjustar = () -> {
            double ycIw = ycImagen.getWidth();
            double ycIh = ycImagen.getHeight();
            if (ycIw <= 0 || ycIh <= 0) {
                return;
            }
            double ycEscala = Math.max(ycAncho / ycIw, ycAlto / ycIh);
            double ycVw = ycAncho / ycEscala;
            double ycVh = ycAlto / ycEscala;
            ycVista.setViewport(new Rectangle2D((ycIw - ycVw) / 2, (ycIh - ycVh) / 2, ycVw, ycVh));
        };
        if (ycImagen.getProgress() >= 1) {
            ycAjustar.run();
        } else {
            ycImagen.progressProperty().addListener((ycObs, ycA, ycP) -> {
                if (ycP.doubleValue() >= 1) {
                    ycAjustar.run();
                }
            });
        }
        return ycVista;
    }

    public static Node ycSinFoto(String ycSemilla, double ycAncho, double ycAlto, boolean ycConTexto) {
        String[] ycColores = YC_DEGRADADOS[Math.floorMod(ycSemilla == null ? 0 : ycSemilla.hashCode(), YC_DEGRADADOS.length)];
        FontIcon ycIcono = new FontIcon("fth-image");
        ycIcono.getStyleClass().add("sin-foto-icono");
        VBox ycContenido = new VBox(6, ycIcono);
        ycContenido.setAlignment(javafx.geometry.Pos.CENTER);
        if (ycConTexto) {
            Label ycTexto = new Label("Sin foto");
            ycTexto.getStyleClass().add("sin-foto-texto");
            ycContenido.getChildren().add(ycTexto);
        }
        StackPane ycFondo = new StackPane(ycContenido);
        ycFondo.getStyleClass().add("sin-foto");
        ycFondo.setStyle("-fx-background-color: linear-gradient(to bottom right, " + ycColores[0] + ", " + ycColores[1] + ");");
        ycFondo.setMinSize(ycAncho, ycAlto);
        ycFondo.setPrefSize(ycAncho, ycAlto);
        ycFondo.setMaxSize(ycAncho, ycAlto);
        return ycFondo;
    }

    public static void ycRedondearArriba(Node ycNodo, double ycAncho, double ycAlto, double ycRadio) {
        Rectangle ycRecorte = new Rectangle(ycAncho, ycAlto + ycRadio * 2);
        ycRecorte.setArcWidth(ycRadio * 2);
        ycRecorte.setArcHeight(ycRadio * 2);
        ycNodo.setClip(ycRecorte);
    }

    public static void ycRedondear(Node ycNodo, double ycAncho, double ycAlto, double ycRadio) {
        Rectangle ycRecorte = new Rectangle(ycAncho, ycAlto);
        ycRecorte.setArcWidth(ycRadio * 2);
        ycRecorte.setArcHeight(ycRadio * 2);
        ycNodo.setClip(ycRecorte);
    }
}
