package com.alquilafacil.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class YCFoto3D {
    private static final double YC_GIRO_MAX = 10, YC_INCLINACION_MAX = 6, YC_DESPLAZAMIENTO = 18;
    private static final double YC_ZOOM = 1.36;

    private final Group ycContenido = new Group();
    private final SubScene ycVista;
    private final Rotate ycGiroY;
    private final Rotate ycGiroX;
    private final Translate ycDesplazamiento = new Translate();
    private final javafx.scene.layout.Region ycBrillo = new javafx.scene.layout.Region();
    private final double ycAncho, ycAlto;

    private double ycObjetivoX, ycObjetivoY, ycActualX, ycActualY;
    private boolean ycMouseEncima;
    private long ycInicio;

    public YCFoto3D(double ycAncho, double ycAlto) {
        this.ycAncho = ycAncho;
        this.ycAlto = ycAlto;
        double ycLado = Math.max(ycAncho, ycAlto) * 1.1;
        ycBrillo.setMinSize(ycLado, ycLado);
        ycBrillo.setPrefSize(ycLado, ycLado);
        ycBrillo.setMaxSize(ycLado, ycLado);
        ycBrillo.resize(ycLado, ycLado);
        ycBrillo.setMouseTransparent(true);
        ycBrillo.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 50%, "
                + "rgba(255,255,255,0.28), rgba(255,255,255,0.08) 45%, transparent);");
        Group ycMundo = new Group(ycContenido);
        ycGiroY = new Rotate(0, ycAncho / 2, ycAlto / 2, 0, Rotate.Y_AXIS);
        ycGiroX = new Rotate(0, ycAncho / 2, ycAlto / 2, 0, Rotate.X_AXIS);
        Scale ycZoom = new Scale(YC_ZOOM, YC_ZOOM, ycAncho / 2, ycAlto / 2);
        ycContenido.getTransforms().addAll(ycDesplazamiento, ycGiroY, ycGiroX, ycZoom);
        ycVista = new SubScene(ycMundo, ycAncho, ycAlto);
        ycVista.setFill(Color.TRANSPARENT);
        PerspectiveCamera ycCamara = new PerspectiveCamera(false);
        ycCamara.setFieldOfView(55);
        ycVista.setCamera(ycCamara);

        AnimationTimer ycAnimacion = new AnimationTimer() {
            @Override
            public void handle(long ycAhora) {
                if (ycInicio == 0) {
                    ycInicio = ycAhora;
                }
                if (!ycMouseEncima) {
                    double ycT = (ycAhora - ycInicio) / 1e9;
                    ycObjetivoX = Math.sin(ycT * 0.7) * 0.45;
                    ycObjetivoY = Math.sin(ycT * 0.5) * 0.25;
                }
                ycActualX += (ycObjetivoX - ycActualX) * 0.08;
                ycActualY += (ycObjetivoY - ycActualY) * 0.08;
                ycGiroY.setAngle(ycActualX * YC_GIRO_MAX);
                ycGiroX.setAngle(-ycActualY * YC_INCLINACION_MAX);
                ycDesplazamiento.setX(-ycActualX * YC_DESPLAZAMIENTO);
                ycDesplazamiento.setY(-ycActualY * YC_DESPLAZAMIENTO * 0.6);
                double ycLado = ycBrillo.getWidth();
                ycBrillo.setTranslateX(YCFoto3D.this.ycAncho / 2 + ycActualX * YCFoto3D.this.ycAncho * 0.45 - ycLado / 2);
                ycBrillo.setTranslateY(YCFoto3D.this.ycAlto / 2 + ycActualY * YCFoto3D.this.ycAlto * 0.45 - ycLado / 2);
                ycBrillo.setOpacity(ycMouseEncima ? 1 : 0.55);
            }
        };
        ycVista.sceneProperty().addListener((ycO, ycAntes, ycEscena) -> {
            if (ycEscena == null) {
                ycAnimacion.stop();
            } else {
                ycAnimacion.start();
            }
        });
    }

    public Node ycNodo() {
        return ycVista;
    }

    public void ycMostrar(Node ycFoto) {
        ycContenido.getChildren().setAll(ycFoto, ycBrillo);
    }

    public void ycConectar(Node ycZona) {
        ycZona.setOnMouseMoved(ycEv -> {
            ycMouseEncima = true;
            var ycB = ycZona.getLayoutBounds();
            ycObjetivoX = ycLimitar((ycEv.getX() - ycB.getWidth() / 2) / (ycB.getWidth() / 2));
            ycObjetivoY = ycLimitar((ycEv.getY() - ycB.getHeight() / 2) / (ycB.getHeight() / 2));
        });
        ycZona.setOnMouseExited(ycEv -> {
            ycMouseEncima = false;
            ycObjetivoX = 0;
            ycObjetivoY = 0;
        });
    }

    private static double ycLimitar(double ycV) {
        return Math.max(-1, Math.min(1, ycV));
    }
}
