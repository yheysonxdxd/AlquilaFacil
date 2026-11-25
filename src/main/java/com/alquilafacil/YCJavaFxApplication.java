package com.alquilafacil;

import atlantafx.base.theme.PrimerLight;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCNotificador;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class YCJavaFxApplication extends Application {
    private static final Logger ycLog = LoggerFactory.getLogger(YCJavaFxApplication.class);
    private static final File YC_CARPETA_LOGS = new File(System.getProperty("app.carpeta", "."), "logs");

    private ConfigurableApplicationContext ycContext;
    private Throwable ycFalloArranque;
    private final AtomicLong ycUltimoAviso = new AtomicLong();

    @Override
    public void init() {
        try {
            this.ycContext = new SpringApplicationBuilder(YCAlquilaFacilApplication.class)
                    .web(WebApplicationType.NONE)
                    .run();
        } catch (Throwable ycT) {
            ycFalloArranque = ycT;
            ycGuardarFalloArranque(ycT);
        }
    }

    @Override
    public void start(Stage ycStage) throws Exception {
        if (ycFalloArranque != null) {
            ycMostrarFalloArranque(ycFalloArranque);
            Platform.exit();
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((ycHilo, ycE) -> ycErrorInesperado(ycE));
        Thread.currentThread().setUncaughtExceptionHandler((ycHilo, ycE) -> ycErrorInesperado(ycE));

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        ycContext.getBean(YCExportador.class).ycSetHostServices(getHostServices());

        FXMLLoader ycLoader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        ycLoader.setControllerFactory(ycContext::getBean);

        Scene ycScene = new Scene(ycLoader.load());
        ycScene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        ycStage.setTitle(ycContext.getBean(com.alquilafacil.config.YCDatosArrendador.class).getYcNombreSistema()
                + " | Alquileres y hospedaje");
        ycStage.setScene(ycScene);
        ycStage.setMinWidth(1100);
        ycStage.setMinHeight(700);
        ycStage.setMaximized(true);
        ycStage.show();
        ycLog.info("Programa abierto");
    }

    @Override
    public void stop() {
        if (ycContext != null) {
            ycLog.info("Programa cerrado");
            ycContext.close();
        }
        Platform.exit();
    }

    void ycErrorInesperado(Throwable ycE) {
        YCNegocioException ycNegocio = ycBuscarNegocio(ycE);
        if (ycNegocio != null) {
            ycLog.warn("Aviso no atrapado: {}", ycNegocio.getMessage());
            ycAvisar(ycNegocio.getMessage());
            return;
        }
        ycLog.error("Error inesperado", ycE);
        long ycAhora = System.currentTimeMillis();
        if (ycAhora - ycUltimoAviso.getAndSet(ycAhora) < 3000) {
            return;
        }
        ycAvisar("Ocurrio un error inesperado. Quedo anotado en la carpeta \"logs\" del programa. "
                + "Si se repite, avisa a quien te da soporte.");
    }

    private void ycAvisar(String ycMensaje) {
        Runnable ycMostrar = () -> {
            try {
                ycContext.getBean(YCNotificador.class).ycError(ycMensaje);
            } catch (Throwable ycOtro) {
                ycLog.error("No se pudo mostrar el aviso de error", ycOtro);
            }
        };
        if (Platform.isFxApplicationThread()) {
            ycMostrar.run();
        } else {
            Platform.runLater(ycMostrar);
        }
    }

    static YCNegocioException ycBuscarNegocio(Throwable ycE) {
        for (Throwable ycT = ycE; ycT != null; ycT = ycT.getCause() == ycT ? null : ycT.getCause()) {
            if (ycT instanceof YCNegocioException ycN) {
                return ycN;
            }
        }
        return null;
    }

    private static void ycGuardarFalloArranque(Throwable ycT) {
        try {
            if (!YC_CARPETA_LOGS.isDirectory()) {
                YC_CARPETA_LOGS.mkdirs();
            }
            try (PrintWriter ycW = new PrintWriter(new FileWriter(new File(YC_CARPETA_LOGS, "error-arranque.txt"), true))) {
                ycW.println("==== " + LocalDateTime.now() + " ====");
                ycT.printStackTrace(ycW);
                ycW.println();
            }
        } catch (Exception ycIgnorada) {
            ycT.printStackTrace();
        }
    }

    private static void ycMostrarFalloArranque(Throwable ycT) {
        String ycTexto = ycTextoCompleto(ycT).toLowerCase();
        String ycMotivo;
        if (ycTexto.contains("sqlite_busy") || ycTexto.contains("database is locked")) {
            ycMotivo = "La base de datos esta en uso. Revisa que el programa no este abierto dos veces "
                    + "(tambien en IntelliJ) y vuelve a intentar.";
        } else if (ycTexto.contains("not a database") || ycTexto.contains("malformed")) {
            ycMotivo = "El archivo de la base de datos (alquileres.db) parece danado. No lo borres: "
                    + "pide ayuda para volver a una copia de la carpeta \"respaldos\".";
        } else {
            ycMotivo = "Ocurrio un problema al abrir el programa.";
        }
        Alert ycAlerta = new Alert(Alert.AlertType.ERROR);
        ycAlerta.setTitle("No se pudo abrir el programa");
        ycAlerta.setHeaderText("No se pudo abrir Hospedaje Supply");
        ycAlerta.setContentText(ycMotivo + "\n\nEl detalle quedo en: "
                + new File(YC_CARPETA_LOGS, "error-arranque.txt").getAbsolutePath());
        TextArea ycDetalle = new TextArea(ycTextoCompleto(ycT));
        ycDetalle.setEditable(false);
        ycDetalle.setWrapText(false);
        ycAlerta.getDialogPane().setExpandableContent(ycDetalle);
        ycAlerta.showAndWait();
    }

    private static String ycTextoCompleto(Throwable ycT) {
        StringWriter ycSw = new StringWriter();
        ycT.printStackTrace(new PrintWriter(ycSw));
        return ycSw.toString();
    }
}
