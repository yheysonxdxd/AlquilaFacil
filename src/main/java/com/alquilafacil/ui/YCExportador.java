package com.alquilafacil.ui;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class YCExportador {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCExportador.class);
    @FunctionalInterface
    public interface YCGenerador {
        void ycGenerar(File ycDestino) throws Exception;
    }

    public enum YCFormato {
        YC_PDF("Documento PDF", "*.pdf", ".pdf"),
        YC_EXCEL("Libro de Excel", "*.xlsx", ".xlsx"),
        YC_CSV("Excel (CSV)", "*.csv", ".csv");

        final String ycDescripcion;
        final String ycPatron;
        final String ycExtension;

        YCFormato(String ycDescripcion, String ycPatron, String ycExtension) {
            this.ycDescripcion = ycDescripcion;
            this.ycPatron = ycPatron;
            this.ycExtension = ycExtension;
        }
    }

    private final YCNotificador ycNotificador;
    private HostServices ycHostServices;
    private File ycUltimaCarpeta = new File(System.getProperty("user.home"));

    public void ycSetHostServices(HostServices ycHostServices) {
        this.ycHostServices = ycHostServices;
    }

    public void ycGuardar(Window ycVentana, String ycTitulo, String ycNombreSugerido, YCFormato ycFormato, YCGenerador ycGenerador) {
        FileChooser ycSelector = new FileChooser();
        ycSelector.setTitle(ycTitulo);
        ycSelector.setInitialFileName(ycNombreSugerido + ycFormato.ycExtension);
        ycSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(ycFormato.ycDescripcion, ycFormato.ycPatron));
        if (ycUltimaCarpeta.isDirectory()) {
            ycSelector.setInitialDirectory(ycUltimaCarpeta);
        }
        File ycElegido = ycSelector.showSaveDialog(ycVentana);
        if (ycElegido == null) {
            return;
        }
        File ycDestino = ycElegido.getName().toLowerCase().endsWith(ycFormato.ycExtension)
                ? ycElegido
                : new File(ycElegido.getParentFile(), ycElegido.getName() + ycFormato.ycExtension);
        ycUltimaCarpeta = ycDestino.getParentFile();

        ycEjecutar(ycDestino, ycGenerador, () -> {
            ycNotificador.ycExito("Archivo guardado: " + ycDestino.getName());
            ycAbrir(ycDestino);
        });
    }

    public void ycAbrirTemporal(String ycNombre, YCFormato ycFormato, YCGenerador ycGenerador) {
        File ycDestino;
        try {
            File ycCarpeta = Files.createTempDirectory("comprobantes").toFile();
            ycCarpeta.deleteOnExit();
            ycDestino = new File(ycCarpeta, ycNombre + ycFormato.ycExtension);
            ycDestino.deleteOnExit();
        } catch (IOException ycE) {
            ycNotificador.ycError("No se pudo crear el archivo temporal: " + ycE.getMessage());
            return;
        }
        ycEjecutar(ycDestino, ycGenerador, () -> {
            ycNotificador.ycInfo("Abriendo " + ycDestino.getName() + "…");
            ycAbrir(ycDestino);
        });
    }

    public void ycGenerar(File ycDestino, YCGenerador ycGenerador, Runnable ycAlTerminar) {
        File ycCarpeta = ycDestino.getParentFile();
        if (ycCarpeta != null && !ycCarpeta.isDirectory() && !ycCarpeta.mkdirs()) {
            ycNotificador.ycError("No se pudo crear la carpeta " + ycCarpeta.getAbsolutePath());
            return;
        }
        ycEjecutar(ycDestino, ycGenerador, ycAlTerminar);
    }

    public void ycAbrirArchivo(File ycArchivo) {
        ycAbrir(ycArchivo);
    }

    private void ycEjecutar(File ycDestino, YCGenerador ycGenerador, Runnable ycAlTerminar) {
        Task<Void> ycTarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ycGenerador.ycGenerar(ycDestino);
                return null;
            }
        };
        ycTarea.setOnSucceeded(ycE -> ycAlTerminar.run());
        ycTarea.setOnFailed(ycE -> {
            Throwable ycCausa = ycTarea.getException();
            YC_LOG.error("No se pudo generar {}", ycDestino, ycCausa);
            ycNotificador.ycError("No se pudo generar el archivo: " + ycCausa.getMessage());
        });
        Thread ycHilo = new Thread(ycTarea, "exportador");
        ycHilo.setDaemon(true);
        ycHilo.start();
    }

    public void ycAbrirEnlace(String ycUrl) {
        if (ycHostServices != null) {
            ycHostServices.showDocument(ycUrl);
        }
    }

    private void ycAbrir(File ycArchivo) {
        if (ycHostServices != null) {
            ycHostServices.showDocument(ycArchivo.toURI().toString());
        }
    }
}
