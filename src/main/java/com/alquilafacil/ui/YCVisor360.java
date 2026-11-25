package com.alquilafacil.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;

public final class YCVisor360 {
    private static final int YC_ANCHO_MAXIMO = 4096;
    private static final double YC_INICIO = 180;
    private static final Color YC_RELLENO = Color.rgb(40, 42, 46);

    private YCVisor360() {
    }

    public static double ycProporcion(Path ycArchivo) {
        Image ycImg = new Image(ycArchivo.toUri().toString(), 600, 0, true, false, false);
        return ycImg.getHeight() <= 0 ? 0 : ycImg.getWidth() / ycImg.getHeight();
    }

    public static boolean ycPareceCompleta(Path ycArchivo) {
        return ycProporcion(ycArchivo) >= 1.8;
    }

    public static final class YCPanorama {
        private final SubScene ycVista;
        private final PerspectiveCamera ycCamara = new PerspectiveCamera(true);
        private final Rotate ycGiro = new Rotate(YC_INICIO, Rotate.Y_AXIS);
        private final Rotate ycInclinacion = new Rotate(0, Rotate.X_AXIS);
        private boolean ycTocado;

        public YCPanorama(Path ycArchivo, double ycAncho, double ycAlto) {
            int ycAnchoReal = ycAnchoDe(ycArchivo);
            Image ycOriginal = ycAnchoReal > YC_ANCHO_MAXIMO || ycAnchoReal <= 0
                    ? new Image(ycArchivo.toUri().toString(), YC_ANCHO_MAXIMO, 0, true, true, false)
                    : new Image(ycArchivo.toUri().toString(), false);
            Sphere ycEsfera = new Sphere(1000, 120);
            PhongMaterial ycMaterial = new PhongMaterial(Color.WHITE);
            ycMaterial.setDiffuseMap(ycEquirectangular(ycOriginal));
            ycEsfera.setMaterial(ycMaterial);
            ycEsfera.setCullFace(CullFace.NONE);
            ycEsfera.setScaleX(-1);

            ycCamara.setNearClip(0.1);
            ycCamara.setFarClip(5000);
            ycCamara.setFieldOfView(75);
            ycCamara.getTransforms().addAll(ycGiro, ycInclinacion);
            Group ycMundo = new Group(ycEsfera, new AmbientLight(Color.WHITE), ycCamara);
            ycVista = new SubScene(ycMundo, ycAncho, ycAlto, true, SceneAntialiasing.BALANCED);
            ycVista.setCamera(ycCamara);
            ycVista.setFill(Color.BLACK);

            AnimationTimer ycAutoGiro = new AnimationTimer() {
                private long ycAntes = 0;

                @Override
                public void handle(long ycAhora) {
                    if (ycAntes != 0 && !ycTocado) {
                        ycGiro.setAngle(ycGiro.getAngle() + (ycAhora - ycAntes) / 1e9 * 6);
                    }
                    ycAntes = ycAhora;
                }
            };
            ycVista.sceneProperty().addListener((ycO, ycA, ycEscena) -> {
                if (ycEscena == null) {
                    ycAutoGiro.stop();
                } else {
                    ycAutoGiro.start();
                }
            });
        }

        public SubScene ycNodo() {
            return ycVista;
        }

        public void ycConectar(Node ycZona) {
            double[] ycUltimo = new double[2];
            ycZona.setOnMousePressed(ycEv -> {
                ycTocado = true;
                ycUltimo[0] = ycEv.getSceneX();
                ycUltimo[1] = ycEv.getSceneY();
                ycZona.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            });
            ycZona.setOnMouseReleased(ycEv -> ycZona.setCursor(javafx.scene.Cursor.OPEN_HAND));
            ycZona.setCursor(javafx.scene.Cursor.OPEN_HAND);
            ycZona.setOnMouseDragged(ycEv -> {
                double ycFactor = ycCamara.getFieldOfView() / Math.max(300, ycVista.getHeight());
                ycGiro.setAngle(ycGiro.getAngle() - (ycEv.getSceneX() - ycUltimo[0]) * ycFactor);
                ycInclinacion.setAngle(ycLimitar(ycInclinacion.getAngle() + (ycEv.getSceneY() - ycUltimo[1]) * ycFactor, -80, 80));
                ycUltimo[0] = ycEv.getSceneX();
                ycUltimo[1] = ycEv.getSceneY();
            });
            ycZona.setOnScroll(ycEv -> {
                ycTocado = true;
                ycCamara.setFieldOfView(ycLimitar(ycCamara.getFieldOfView() - ycEv.getDeltaY() * 0.05, 30, 100));
                ycEv.consume();
            });
        }

        public void ycCentrar() {
            ycTocado = true;
            ycGiro.setAngle(YC_INICIO);
            ycInclinacion.setAngle(0);
            ycCamara.setFieldOfView(75);
        }
    }

    public static void ycMostrar(Window ycDuenio, Path ycArchivo, String ycTitulo) {
        YCPanorama ycPanorama = new YCPanorama(ycArchivo, 960, 600);
        SubScene ycVista = ycPanorama.ycNodo();
        StackPane ycRaiz = new StackPane(ycVista);
        ycRaiz.setStyle("-fx-background-color: black;");
        ycVista.widthProperty().bind(ycRaiz.widthProperty());
        ycVista.heightProperty().bind(ycRaiz.heightProperty());
        ycPanorama.ycConectar(ycRaiz);

        Label ycNombre = new Label(ycTitulo, new FontIcon(Feather.COMPASS));
        ycNombre.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        ((FontIcon) ycNombre.getGraphic()).setIconColor(Color.WHITE);
        Label ycAyuda = new Label("Arrastra para mirar alrededor · rueda del mouse para acercar");
        ycAyuda.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        Button ycCentrar = ycBoton("Centrar", Feather.REFRESH_CW);
        Button ycCerrar = ycBoton("Cerrar", Feather.X);
        HBox ycBarra = new HBox(14, ycNombre, ycAyuda, ycEsp, ycCentrar, ycCerrar);
        ycBarra.setAlignment(Pos.CENTER_LEFT);
        ycBarra.setPadding(new Insets(10, 14, 10, 14));
        ycBarra.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.65), transparent);");
        ycBarra.setMaxHeight(Region.USE_PREF_SIZE);
        ycBarra.setPickOnBounds(false);
        StackPane.setAlignment(ycBarra, Pos.TOP_CENTER);
        ycRaiz.getChildren().add(ycBarra);

        Stage ycVentana = new Stage();
        ycVentana.initOwner(ycDuenio);
        ycVentana.initModality(Modality.WINDOW_MODAL);
        ycVentana.setTitle(ycTitulo + " · 360°");
        Scene ycEscena = new Scene(ycRaiz, 1000, 640, false, SceneAntialiasing.DISABLED);
        if (ycDuenio != null && ycDuenio.getScene() != null) {
            ycEscena.getStylesheets().addAll(ycDuenio.getScene().getStylesheets());
        }
        ycEscena.setOnKeyPressed(ycEv -> {
            if (ycEv.getCode() == KeyCode.ESCAPE) {
                ycVentana.close();
            }
        });
        ycCentrar.setOnAction(ycEv -> ycPanorama.ycCentrar());
        ycCerrar.setOnAction(ycEv -> ycVentana.close());
        ycVentana.setScene(ycEscena);
        if (ycDuenio instanceof Stage ycS) {
            ycVentana.getIcons().setAll(ycS.getIcons());
        }
        ycVentana.show();
    }

    static Image ycEquirectangular(Image ycImg) {
        int ycAncho = (int) ycImg.getWidth();
        int ycAlto = (int) ycImg.getHeight();
        if (ycAncho <= 0 || ycAlto <= 0) {
            return ycImg;
        }
        int ycAltoTotal = Math.max(ycAlto, ycAncho / 2);
        if (ycAltoTotal == ycAlto) {
            return ycImg;
        }
        WritableImage ycSalida = new WritableImage(ycAncho, ycAltoTotal);
        PixelWriter ycPw = ycSalida.getPixelWriter();
        PixelReader ycPr = ycImg.getPixelReader();
        int ycArriba = (ycAltoTotal - ycAlto) / 2;
        int ycGris = ycArgb(YC_RELLENO);
        int[] ycFila = new int[ycAncho];
        java.util.Arrays.fill(ycFila, ycGris);
        javafx.scene.image.WritablePixelFormat<java.nio.IntBuffer> ycFormato =
                javafx.scene.image.PixelFormat.getIntArgbInstance();
        for (int ycY = 0; ycY < ycAltoTotal; ycY++) {
            if (ycY < ycArriba || ycY >= ycArriba + ycAlto) {
                ycPw.setPixels(0, ycY, ycAncho, 1, ycFormato, ycFila, 0, ycAncho);
            }
        }
        ycPw.setPixels(0, ycArriba, ycAncho, ycAlto, ycPr, 0, 0);
        return ycSalida;
    }

    private static int ycAnchoDe(Path ycArchivo) {
        try (javax.imageio.stream.ImageInputStream ycIn = javax.imageio.ImageIO.createImageInputStream(ycArchivo.toFile())) {
            var ycLectores = javax.imageio.ImageIO.getImageReaders(ycIn);
            if (!ycLectores.hasNext()) {
                return 0;
            }
            javax.imageio.ImageReader ycR = ycLectores.next();
            try {
                ycR.setInput(ycIn);
                return ycR.getWidth(0);
            } finally {
                ycR.dispose();
            }
        } catch (Exception ycE) {
            return 0;
        }
    }

    private static int ycArgb(Color ycC) {
        return 0xFF << 24 | (int) Math.round(ycC.getRed() * 255) << 16
                | (int) Math.round(ycC.getGreen() * 255) << 8 | (int) Math.round(ycC.getBlue() * 255);
    }

    private static Button ycBoton(String ycTexto, Feather ycIcono) {
        FontIcon ycI = new FontIcon(ycIcono);
        ycI.setIconColor(Color.WHITE);
        Button ycB = new Button(ycTexto, ycI);
        ycB.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8;");
        ycB.setFocusTraversable(false);
        return ycB;
    }

    private static double ycLimitar(double ycV, double ycMin, double ycMax) {
        return Math.max(ycMin, Math.min(ycMax, ycV));
    }
}
