package com.alquilafacil.controller;

import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCEstadoLimpieza;
import com.alquilafacil.ui.YCTablas;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class YCCalendarioOcupacion {
    static final int YC_DIAS_CALENDARIO = 14;
    private static final double YC_ALTO_FILA = 40;

    private final GridPane ycGrid;
    private final Label ycLblRango;
    private final BiConsumer<YCCuarto, LocalDate> ycAlElegirLibre;
    private final Consumer<YCEstadia> ycAlElegirEstadia;

    YCCalendarioOcupacion(GridPane ycGrid, Label ycLblRango, BiConsumer<YCCuarto, LocalDate> ycAlElegirLibre,
                        Consumer<YCEstadia> ycAlElegirEstadia) {
        this.ycGrid = ycGrid;
        this.ycLblRango = ycLblRango;
        this.ycAlElegirLibre = ycAlElegirLibre;
        this.ycAlElegirEstadia = ycAlElegirEstadia;
    }

    void ycDibujar(LocalDate ycInicioCalendario, List<YCCuarto> ycCuartos, List<YCEstadia> ycEstadias) {
        ycGrid.getChildren().clear();
        ycGrid.getColumnConstraints().clear();
        ycGrid.getRowConstraints().clear();

        LocalDate ycHoy = LocalDate.now();
        LocalDate ycFin = ycInicioCalendario.plusDays(YC_DIAS_CALENDARIO);
        ycLblRango.setText("Del " + YCControles.ycFecha(ycInicioCalendario) + " al "
                + YCControles.ycFecha(ycFin.minusDays(1)));

        ColumnConstraints ycNombres = new ColumnConstraints(150);
        ycGrid.getColumnConstraints().add(ycNombres);
        for (int ycD = 0; ycD < YC_DIAS_CALENDARIO; ycD++) {
            ColumnConstraints ycCol = new ColumnConstraints();
            ycCol.setHgrow(Priority.ALWAYS);
            ycCol.setMinWidth(44);
            ycCol.setPercentWidth(-1);
            ycGrid.getColumnConstraints().add(ycCol);
        }

        Label ycVacio = new Label("HABITACION");
        ycVacio.getStyleClass().add("cal-cabecera-hab");
        ycGrid.add(ycVacio, 0, 0);
        for (int ycD = 0; ycD < YC_DIAS_CALENDARIO; ycD++) {
            LocalDate ycDia = ycInicioCalendario.plusDays(ycD);
            Label ycNombreDia = new Label(ycDia.getDayOfWeek().getDisplayName(TextStyle.SHORT, YCTablas.YC_LOCALE)
                    .replace(".", "").toUpperCase());
            ycNombreDia.getStyleClass().add("cal-dia-nombre");
            Label ycNumero = new Label(String.valueOf(ycDia.getDayOfMonth()));
            ycNumero.getStyleClass().add("cal-dia-numero");
            VBox ycCab = new VBox(0, ycNombreDia, ycNumero);
            ycCab.setAlignment(Pos.CENTER);
            ycCab.getStyleClass().add("cal-dia");
            if (ycDia.equals(ycHoy)) ycCab.getStyleClass().add("hoy");
            if (ycFinDeSemana(ycDia)) ycCab.getStyleClass().add("finde");
            ycCab.setMaxWidth(Double.MAX_VALUE);
            ycGrid.add(ycCab, ycD + 1, 0);
        }
        ycGrid.getRowConstraints().add(new RowConstraints(48));

        if (ycCuartos.isEmpty()) {
            Label ycNada = new Label("Aun no hay habitaciones. Registralas en Hospedaje → Habitaciones.");
            ycNada.getStyleClass().add("vacio-texto");
            ycNada.setPadding(new Insets(16, 0, 8, 0));
            ycGrid.add(ycNada, 0, 1, YC_DIAS_CALENDARIO + 1, 1);
            return;
        }
        int ycFila = 1;
        for (YCCuarto ycC : ycCuartos) {
            ycGrid.getRowConstraints().add(new RowConstraints(YC_ALTO_FILA));
            Label ycNombre = new Label(ycC.getYcNumero());
            ycNombre.getStyleClass().add("cal-hab");
            Label ycTipo = new Label((ycC.getYcTipoHabitacion() == null ? "" : ycC.getYcTipoHabitacion().ycGetTexto() + " · ")
                    + "S/ " + ycC.getYcPrecio().setScale(0, RoundingMode.HALF_UP).toPlainString());
            ycTipo.getStyleClass().add("cal-hab-detalle");
            YCEstadoLimpieza ycLimpieza = ycC.ycGetLimpieza();
            Region ycPunto = new Region();
            ycPunto.getStyleClass().addAll("limpieza-punto", ycLimpieza.ycGetClase());
            Tooltip.install(ycPunto, new Tooltip(ycLimpieza.ycGetTexto()));
            ycNombre.setGraphic(ycPunto);
            ycNombre.setContentDisplay(ContentDisplay.RIGHT);
            VBox ycCeldaNombre = new VBox(0, ycNombre, ycTipo);
            ycCeldaNombre.setAlignment(Pos.CENTER_LEFT);
            ycCeldaNombre.getStyleClass().add("cal-hab-celda");
            ycGrid.add(ycCeldaNombre, 0, ycFila);

            for (int ycD = 0; ycD < YC_DIAS_CALENDARIO; ycD++) {
                LocalDate ycDia = ycInicioCalendario.plusDays(ycD);
                Region ycCelda = new Region();
                ycCelda.getStyleClass().add("cal-celda");
                if (ycDia.equals(ycHoy)) ycCelda.getStyleClass().add("hoy");
                if (ycFinDeSemana(ycDia)) ycCelda.getStyleClass().add("finde");
                ycCelda.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                if (!ycDia.isBefore(ycHoy)) {
                    ycCelda.getStyleClass().add("libre");
                    Tooltip.install(ycCelda, new Tooltip("Registrar en " + ycC.getYcNumero() + " desde el "
                            + YCControles.ycFecha(ycDia)));
                    ycCelda.setOnMouseClicked(ycE -> ycAlElegirLibre.accept(ycC, ycDia));
                }
                ycGrid.add(ycCelda, ycD + 1, ycFila);
            }
            for (YCEstadia ycE : ycEstadias) {
                if (!ycE.getYcCuarto().getYcId().equals(ycC.getYcId()) || ycE.ycEstado(ycHoy) == YCEstadia.YCEstado.YC_TERMINADA) {
                    continue;
                }
                LocalDate ycDesde = ycE.getYcEntrada().isBefore(ycInicioCalendario) ? ycInicioCalendario : ycE.getYcEntrada();
                LocalDate ycHasta = ycE.getYcSalida().isAfter(ycFin) ? ycFin : ycE.getYcSalida();
                int ycCol = (int) ChronoUnit.DAYS.between(ycInicioCalendario, ycDesde) + 1;
                int ycSpan = (int) ChronoUnit.DAYS.between(ycDesde, ycHasta);
                if (ycSpan <= 0) {
                    continue;
                }
                ycGrid.add(ycBarra(ycE, ycSpan, !ycE.getYcEntrada().isBefore(ycInicioCalendario),
                        !ycE.getYcSalida().isAfter(ycFin)), ycCol, ycFila, ycSpan, 1);
            }
            ycFila++;
        }
    }

    private Node ycBarra(YCEstadia ycE, int ycSpan, boolean ycEmpiezaAqui, boolean ycTerminaAqui) {
        YCEstadia.YCEstado ycEstado = ycE.ycEstado(LocalDate.now());
        Label ycTexto = new Label(ycSpan >= 2 ? ycE.getYcHuesped() : YCTablas.ycIniciales(ycE.getYcHuesped()),
                ycSpan >= 3 ? new FontIcon(Feather.USER) : null);
        ycTexto.getStyleClass().add("cal-barra-texto");
        ycTexto.setMinWidth(0);
        StackPane ycBarra = new StackPane(ycTexto);
        StackPane.setAlignment(ycTexto, Pos.CENTER_LEFT);
        ycBarra.getStyleClass().addAll("cal-barra", switch (ycEstado) {
            case YC_EN_CURSO -> ycE.ycIsLlego() ? "en-curso" : "proxima";
            case YC_PROXIMA -> "proxima";
            default -> "terminada";
        });
        if (!ycEmpiezaAqui) ycBarra.getStyleClass().add("sigue-antes");
        if (!ycTerminaAqui) ycBarra.getStyleClass().add("sigue-despues");
        ycBarra.setMaxSize(Double.MAX_VALUE, 28);
        GridPane.setMargin(ycBarra, new Insets(0, 3, 0, 3));
        Tooltip ycAyuda = new Tooltip(ycE.getYcHuesped() + " · " + ycE.getYcCuartoNumero() + "\n"
                + YCControles.ycFecha(ycE.getYcEntrada()) + " → " + YCControles.ycFecha(ycE.getYcSalida()) + " · "
                + ycE.getYcNoches() + (ycE.getYcNoches() == 1 ? " noche" : " noches") + " · " + YCControles.ycMoneda(ycE.getYcTotal()));
        ycAyuda.setShowDelay(Duration.millis(150));
        Tooltip.install(ycBarra, ycAyuda);
        ycBarra.setOnMouseClicked(ycEv -> ycAlElegirEstadia.accept(ycE));
        return ycBarra;
    }

    private static boolean ycFinDeSemana(LocalDate ycDia) {
        return ycDia.getDayOfWeek() == DayOfWeek.SATURDAY || ycDia.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
