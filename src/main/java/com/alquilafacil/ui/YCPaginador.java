package com.alquilafacil.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;

public class YCPaginador extends HBox {
    private static final int YC_BOTONES_VISIBLES = 5;

    private final Label ycLblResumen = new Label();
    private final ComboBox<Integer> ycCmbTamanio =
            new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 25, 50));
    private final HBox ycBotones = new HBox(4);

    private int ycPagina = 0;
    private int ycTotalPaginas = 1;
    private Runnable ycOnCambio = () -> { };

    public YCPaginador() {
        getStyleClass().add("paginador");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        ycLblResumen.getStyleClass().add("paginador-resumen");

        Label ycLblFilas = new Label("Filas");
        ycLblFilas.getStyleClass().add("paginador-resumen");
        ycCmbTamanio.setValue(10);
        ycCmbTamanio.getStyleClass().add("paginador-tamanio");
        ycCmbTamanio.valueProperty().addListener((ycObs, ycA, ycB) -> ycIrAPrimera());

        ycBotones.setAlignment(Pos.CENTER);

        Region ycEspacio = new Region();
        HBox.setHgrow(ycEspacio, Priority.ALWAYS);

        getChildren().addAll(ycLblResumen, ycEspacio, ycLblFilas, ycCmbTamanio, ycBotones);
    }

    public void ycSetOnCambio(Runnable ycOnCambio) {
        this.ycOnCambio = ycOnCambio;
    }

    public int ycGetPagina() {
        return ycPagina;
    }

    public int ycGetTamanio() {
        return ycCmbTamanio.getValue();
    }

    public void ycIrAPrimera() {
        ycIrA(0);
    }

    public void ycActualizar(Page<?> ycResultado) {
        ycTotalPaginas = Math.max(ycResultado.getTotalPages(), 1);
        if (ycPagina >= ycTotalPaginas) {
            ycIrA(ycTotalPaginas - 1);
            return;
        }

        long ycTotal = ycResultado.getTotalElements();
        long ycDesde = ycTotal == 0 ? 0 : (long) ycPagina * ycGetTamanio() + 1;
        long ycHasta = (long) ycPagina * ycGetTamanio() + ycResultado.getNumberOfElements();
        ycLblResumen.setText(ycTotal == 0
                ? "Sin registros"
                : "Mostrando " + ycDesde + "–" + ycHasta + " de " + ycTotal);

        ycConstruirBotones();
    }

    private void ycIrA(int ycNuevaPagina) {
        ycPagina = Math.max(0, Math.min(ycNuevaPagina, ycTotalPaginas - 1));
        ycOnCambio.run();
    }

    private void ycConstruirBotones() {
        ycBotones.getChildren().clear();
        ycBotones.getChildren().add(ycBotonIcono(Feather.CHEVRONS_LEFT, 0, ycPagina == 0));
        ycBotones.getChildren().add(ycBotonIcono(Feather.CHEVRON_LEFT, ycPagina - 1, ycPagina == 0));

        int ycInicio = Math.max(0, Math.min(ycPagina - YC_BOTONES_VISIBLES / 2, ycTotalPaginas - YC_BOTONES_VISIBLES));
        int ycFin = Math.min(ycTotalPaginas, ycInicio + YC_BOTONES_VISIBLES);

        if (ycInicio > 0) {
            ycBotones.getChildren().add(ycBotonNumero(0));
            if (ycInicio > 1) {
                ycBotones.getChildren().add(ycElipsis());
            }
        }
        for (int ycI = ycInicio; ycI < ycFin; ycI++) {
            ycBotones.getChildren().add(ycBotonNumero(ycI));
        }
        if (ycFin < ycTotalPaginas) {
            if (ycFin < ycTotalPaginas - 1) {
                ycBotones.getChildren().add(ycElipsis());
            }
            ycBotones.getChildren().add(ycBotonNumero(ycTotalPaginas - 1));
        }

        boolean ycEsUltima = ycPagina >= ycTotalPaginas - 1;
        ycBotones.getChildren().add(ycBotonIcono(Feather.CHEVRON_RIGHT, ycPagina + 1, ycEsUltima));
        ycBotones.getChildren().add(ycBotonIcono(Feather.CHEVRONS_RIGHT, ycTotalPaginas - 1, ycEsUltima));
    }

    private Button ycBotonNumero(int ycIndice) {
        Button ycB = new Button(String.valueOf(ycIndice + 1));
        ycB.getStyleClass().add("pagina-btn");
        if (ycIndice == ycPagina) {
            ycB.getStyleClass().add("activa");
        }
        ycB.setOnAction(ycE -> {
            if (ycIndice != ycPagina) {
                ycIrA(ycIndice);
            }
        });
        return ycB;
    }

    private Button ycBotonIcono(Feather ycIcono, int ycDestino, boolean ycDeshabilitado) {
        Button ycB = new Button(null, new FontIcon(ycIcono));
        ycB.getStyleClass().add("pagina-btn");
        ycB.setDisable(ycDeshabilitado);
        ycB.setOnAction(ycE -> ycIrA(ycDestino));
        return ycB;
    }

    private Label ycElipsis() {
        Label ycL = new Label("…");
        ycL.getStyleClass().add("paginador-resumen");
        return ycL;
    }
}
