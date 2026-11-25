package com.alquilafacil.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;

public final class YCFotoFicha {
    private final StackPane ycZona;
    private final double ycAncho, ycAlto;
    private final StackPane ycCapa = new StackPane();
    private final Path ycRuta360;
    private final String ycTitulo;
    private YCVisor360.YCPanorama ycPanorama;
    private final HBox ycControles360;

    public YCFotoFicha(StackPane ycZona, double ycAncho, double ycAlto, Path ycRuta360, String ycTitulo) {
        this.ycZona = ycZona;
        this.ycAncho = ycAncho;
        this.ycAlto = ycAlto;
        this.ycRuta360 = ycRuta360;
        this.ycTitulo = ycTitulo;
        ycCapa.setMinSize(ycAncho, ycAlto);
        ycCapa.setMaxSize(ycAncho, ycAlto);

        Label ycAviso = new Label("360° · arrastra para mirar el cuarto", new FontIcon(Feather.COMPASS));
        ycAviso.getStyleClass().add("contador-fotos");
        ycAviso.setMouseTransparent(true);
        Button ycGrande = new Button(null, new FontIcon(Feather.MAXIMIZE_2));
        ycGrande.getStyleClass().addAll("button-circle", "ficha-cerrar");
        ycGrande.setTooltip(new Tooltip("Ver el 360° en pantalla completa"));
        ycGrande.setOnAction(ycE -> YCVisor360.ycMostrar(ycZona.getScene().getWindow(), ycRuta360, ycTitulo));
        ycControles360 = new HBox(8, ycAviso, ycGrande);
        ycControles360.setAlignment(Pos.CENTER_RIGHT);
        ycControles360.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        ycControles360.setPickOnBounds(false);
        StackPane.setAlignment(ycControles360, Pos.TOP_RIGHT);
        StackPane.setMargin(ycControles360, new Insets(14, 64, 14, 14));
    }

    public Node ycNodo() {
        return ycCapa;
    }

    public Node ycControles() {
        return ycControles360;
    }

    public boolean ycTiene360() {
        return ycRuta360 != null;
    }

    public void ycMostrar360() {
        if (ycRuta360 == null) {
            return;
        }
        if (ycPanorama == null) {
            ycPanorama = new YCVisor360.YCPanorama(ycRuta360, ycAncho, ycAlto);
        }
        ycLimpiarMouse();
        ycCapa.getChildren().setAll(ycPanorama.ycNodo());
        ycPanorama.ycConectar(ycZona);
        ycControles360.setVisible(true);
    }

    public void ycMostrarFoto(Node ycFoto) {
        ycLimpiarMouse();
        YCFoto3D ycF = new YCFoto3D(ycAncho, ycAlto);
        ycF.ycMostrar(ycFoto);
        ycCapa.getChildren().setAll(ycF.ycNodo());
        ycF.ycConectar(ycZona);
        ycControles360.setVisible(false);
    }

    private void ycLimpiarMouse() {
        ycZona.setOnMousePressed(null);
        ycZona.setOnMouseReleased(null);
        ycZona.setOnMouseDragged(null);
        ycZona.setOnScroll(null);
        ycZona.setOnMouseMoved(null);
        ycZona.setOnMouseExited(null);
        ycZona.setCursor(null);
    }

    public void ycEmpezar(Node ycPrincipal, java.util.List<javafx.scene.image.Image> ycImagenes) {
        if (ycTiene360()) {
            ycMostrar360();
        } else {
            ycMostrarFoto(ycPrincipal);
        }
        int ycCuantas = ycImagenes.size() + (ycTiene360() ? 1 : 0);
        if (ycCuantas > 1) {
            HBox ycTira = new HBox(8);
            ycTira.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            ycTira.getStyleClass().add("tira-fotos");
            java.util.List<Node> ycMinis = new java.util.ArrayList<>();
            java.util.function.BiConsumer<StackPane, Runnable> ycAgregar = (ycMini, ycAccion) -> {
                YCFotos.ycRedondear(ycMini, 64, 46, 8);
                ycMini.getStyleClass().add("mini-foto");
                ycMini.setMaxSize(64, 46);
                if (ycMinis.isEmpty()) {
                    ycMini.getStyleClass().add("activa");
                }
                ycMini.setOnMouseClicked(ycEv -> {
                    ycAccion.run();
                    ycMinis.forEach(ycM -> ycM.getStyleClass().remove("activa"));
                    ycMini.getStyleClass().add("activa");
                    ycEv.consume();
                });
                ycMini.setOnMousePressed(javafx.event.Event::consume);
                ycMinis.add(ycMini);
                ycTira.getChildren().add(ycMini);
            };
            if (ycTiene360()) {
                StackPane ycMini = ycMiniatura360(64, 46);
                Tooltip.install(ycMini, new Tooltip("Volver al 360°"));
                ycAgregar.accept(ycMini, this::ycMostrar360);
            }
            for (javafx.scene.image.Image ycImg : ycImagenes) {
                ycAgregar.accept(new StackPane(YCFotos.ycCubrir(ycImg, 64, 46)), () -> ycMostrarFoto(YCFotos.ycCubrir(ycImg, ycAncho, ycAlto)));
            }
            StackPane.setAlignment(ycTira, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(ycTira, new Insets(18));
            ycZona.getChildren().add(ycTira);
        }
        ycZona.getChildren().add(ycControles360);
    }

    public static StackPane ycMiniatura360(double ycAncho, double ycAlto) {
        Label ycL = new Label("360°", new FontIcon(Feather.COMPASS));
        ycL.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        ((FontIcon) ycL.getGraphic()).setIconColor(javafx.scene.paint.Color.WHITE);
        StackPane ycMini = new StackPane(ycL);
        ycMini.setStyle("-fx-background-color: linear-gradient(to bottom right, #3b6fd8, #7a3bd8);");
        ycMini.setMinSize(ycAncho, ycAlto);
        ycMini.setMaxSize(ycAncho, ycAlto);
        return ycMini;
    }
}
