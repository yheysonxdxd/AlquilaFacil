package com.alquilafacil.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public final class YCTablas {
    public static final Locale YC_LOCALE = Locale.forLanguageTag("es-PE");
    public static final NumberFormat YC_MONEDA = NumberFormat.getCurrencyInstance(YC_LOCALE);

    private YCTablas() {
    }

    public static <T> void ycConfigurar(TableView<T> ycTabla, String ycTextoVacio, Consumer<T> ycAlDobleClic) {
        ycConfigurar(ycTabla, Feather.INBOX, "Sin resultados", ycTextoVacio, ycAlDobleClic);
    }

    public static <T> void ycConfigurar(TableView<T> ycTabla, Ikon ycIconoVacio, String ycTituloVacio, String ycTextoVacio,
                                      Consumer<T> ycAlDobleClic) {
        ycTabla.getStyleClass().add("tabla");
        ycTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        FontIcon ycIcono = new FontIcon(ycIconoVacio);
        ycIcono.getStyleClass().add("vacio-icono");
        Label ycTitulo = new Label(ycTituloVacio);
        ycTitulo.getStyleClass().add("vacio-titulo");
        Label ycDetalle = new Label(ycTextoVacio);
        ycDetalle.getStyleClass().add("vacio-detalle");
        VBox ycVacio = new VBox(8, ycIcono, ycTitulo, ycDetalle);
        ycVacio.setAlignment(Pos.CENTER);
        ycTabla.setPlaceholder(ycVacio);

        ycTabla.setRowFactory(ycTv -> {
            TableRow<T> ycFila = new TableRow<>();
            ycFila.setOnMouseClicked(ycE -> {
                if (ycE.getClickCount() == 2 && !ycFila.isEmpty()) {
                    ycAlDobleClic.accept(ycFila.getItem());
                }
            });
            return ycFila;
        });
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaId() {
        return ycCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-id");
            }

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                setText(ycVacio || ycValor == null ? null : "#" + ycValor);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaAvatar() {
        return ycCol -> new TableCell<>() {
            private final Label ycIniciales = new Label();
            private final StackPane ycAvatar = new StackPane(ycIniciales);
            private final Label ycNombre = new Label();
            private final HBox ycCaja = new HBox(10, ycAvatar, ycNombre);

            {
                ycAvatar.getStyleClass().add("avatar");
                ycCaja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                if (ycVacio || ycValor == null) {
                    setGraphic(null);
                    return;
                }
                ycIniciales.setText(ycIniciales(ycValor));
                ycNombre.setText(ycValor);
                setGraphic(ycCaja);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaChip(String ycTextoVacio) {
        return ycCol -> new TableCell<>() {
            private final Label ycChip = new Label();

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                if (ycVacio) {
                    setGraphic(null);
                    return;
                }
                boolean ycSinValor = ycValor == null || ycValor.isBlank();
                ycChip.setText(ycSinValor ? ycTextoVacio : ycValor);
                ycChip.getStyleClass().setAll("chip");
                if (ycSinValor) {
                    ycChip.getStyleClass().add("neutro");
                }
                setGraphic(ycChip);
            }
        };
    }

    public static <T> Callback<TableColumn<T, BigDecimal>, TableCell<T, BigDecimal>> ycCeldaMoneda() {
        return ycCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-importe");
            }

            @Override
            protected void updateItem(BigDecimal ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                setText(ycVacio || ycValor == null ? null : YC_MONEDA.format(ycValor));
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaOpcional() {
        return ycCol -> new TableCell<>() {
            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                boolean ycSinValor = !ycVacio && (ycValor == null || ycValor.isBlank());
                setText(ycVacio ? null : ycSinValor ? "—" : ycValor);
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("sin-valor"), ycSinValor);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaEstado() {
        return ycCol -> new TableCell<>() {
            private final Label ycChip = new Label();

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                if (ycVacio || ycValor == null) {
                    setGraphic(null);
                    return;
                }
                ycChip.setText(ycValor);
                ycChip.getStyleClass().setAll("chip", "ANULADA".equals(ycValor) ? "peligro" : "exito");
                setGraphic(ycChip);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> ycCeldaChip(
            java.util.function.Function<String, String> ycClaseSegunValor) {
        return ycCol -> new TableCell<>() {
            private final Label ycChip = new Label();

            @Override
            protected void updateItem(String ycValor, boolean ycVacio) {
                super.updateItem(ycValor, ycVacio);
                if (ycVacio || ycValor == null) {
                    setGraphic(null);
                    return;
                }
                ycChip.setText(ycValor);
                ycChip.getStyleClass().setAll("chip", ycClaseSegunValor.apply(ycValor));
                setGraphic(ycChip);
            }
        };
    }

    public static String ycIniciales(String ycTexto) {
        String[] ycPartes = ycTexto.trim().split("\\s+");
        StringBuilder ycSb = new StringBuilder();
        for (int ycI = 0; ycI < ycPartes.length && ycSb.length() < 2; ycI++) {
            if (!ycPartes[ycI].isEmpty()) {
                ycSb.append(Character.toUpperCase(ycPartes[ycI].charAt(0)));
            }
        }
        return ycSb.toString();
    }
}
