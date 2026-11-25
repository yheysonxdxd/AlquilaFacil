package com.alquilafacil.ui;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCAuditoria;
import com.alquilafacil.service.YCAuditoriaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YCHistorialDialogo {
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final YCAuditoriaService ycService;
    private final YCNotificador ycNotificador;
    private final YCExportador ycExportador;

    private enum YCPeriodo {
        YC_SEMANA("Ultimos 7 dias", 7), YC_MES("Ultimos 30 dias", 30), YC_ANIO("Ultimo año", 365), YC_TODO("Todo", 0);
        final String ycTexto;
        final int ycDias;

        YCPeriodo(String ycTexto, int ycDias) {
            this.ycTexto = ycTexto;
            this.ycDias = ycDias;
        }

        @Override
        public String toString() {
            return ycTexto;
        }
    }

    public void ycMostrar() {
        TableView<YCAuditoria> ycTabla = new TableView<>();
        TableColumn<YCAuditoria, String> ycColFecha = new TableColumn<>("FECHA");
        ycColFecha.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcFecha().format(YC_FECHA)));
        ycColFecha.setPrefWidth(120);
        TableColumn<YCAuditoria, String> ycColUsuario = new TableColumn<>("USUARIO");
        ycColUsuario.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcUsuario()));
        ycColUsuario.setPrefWidth(90);
        TableColumn<YCAuditoria, String> ycColAccion = new TableColumn<>("QUE HIZO");
        ycColAccion.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcAccion()));
        ycColAccion.setCellFactory(YCTablas.ycCeldaChip(ycV -> ycV.contains("eliminad") || ycV.contains("anulad")
                || ycV.contains("cancelad") || ycV.contains("restaurad") || ycV.contains("desactivad") ? "peligro"
                : ycV.contains("editad") || ycV.contains("Contraseña") ? "advertencia" : "neutro"));
        ycColAccion.setPrefWidth(160);
        TableColumn<YCAuditoria, String> ycColDetalle = new TableColumn<>("DETALLE");
        ycColDetalle.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcDetalle()));
        ycColDetalle.setPrefWidth(460);
        ycTabla.getColumns().setAll(List.of(ycColFecha, ycColUsuario, ycColAccion, ycColDetalle));
        YCTablas.ycConfigurar(ycTabla, Feather.CLOCK, "Sin cambios",
                "Aqui aparecen los borrados, ediciones, anulaciones y cancelaciones.", ycA -> ycVerDetalle(ycA));
        ycTabla.setPrefHeight(420);

        TextField ycBuscar = new TextField();
        ycBuscar.setPromptText("Buscar (usuario, gasto, nombre, recibo...)");
        HBox.setHgrow(ycBuscar, Priority.ALWAYS);
        ComboBox<YCPeriodo> ycPeriodo = new ComboBox<>();
        ycPeriodo.getItems().setAll(YCPeriodo.values());
        ycPeriodo.setValue(YCPeriodo.YC_MES);
        Label ycTotal = new Label();
        ycTotal.getStyleClass().add("campo-ayuda");
        Runnable ycCargar = () -> {
            try {
                YCPeriodo ycP = ycPeriodo.getValue();
                List<YCAuditoria> ycLista = ycService.ycBuscar(ycP.ycDias == 0 ? null : LocalDateTime.now().minusDays(ycP.ycDias),
                        ycBuscar.getText());
                ycTabla.getItems().setAll(ycLista);
                ycTotal.setText(ycLista.size() + (ycLista.size() == 1 ? " cambio" : " cambios")
                        + (ycLista.size() >= 500 ? " (se muestran los ultimos 500)" : ""));
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        };
        ycBuscar.textProperty().addListener((ycO, ycA, ycB) -> ycCargar.run());
        ycPeriodo.valueProperty().addListener((ycO, ycA, ycB) -> ycCargar.run());
        ycCargar.run();

        Label ycTitulo = new Label("Historial de cambios");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label("Quien borro, edito, anulo o cancelo algo, y cuando. No se puede modificar. "
                + "Doble clic en una fila para leer el detalle completo.");
        ycSub.getStyleClass().add("dialogo-mensaje");
        ycSub.setWrapText(true);
        Button ycCerrar = new Button("Cerrar");
        ycCerrar.getStyleClass().add("button-outlined");
        ycCerrar.setCancelButton(true);
        ycCerrar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        Button ycExcel = new Button("Excel", new org.kordamp.ikonli.javafx.FontIcon(Feather.DOWNLOAD));
        ycExcel.getStyleClass().add("button-outlined");
        ycExcel.setOnAction(ycE -> {
            com.alquilafacil.service.YCExcel.YCHoja ycH = new com.alquilafacil.service.YCExcel.YCHoja("Historial")
                    .ycTitulo("Historial de cambios · " + ycPeriodo.getValue()
                            + (ycBuscar.getText().isBlank() ? "" : " · busqueda: " + ycBuscar.getText().trim()))
                    .ycColumna("Fecha", com.alquilafacil.service.YCExcel.YCTipo.YC_FECHA_HORA, 17)
                    .ycColumna("Usuario", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 12)
                    .ycColumna("Que hizo", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 24)
                    .ycColumna("Detalle", com.alquilafacil.service.YCExcel.YCTipo.YC_TEXTO, 90);
            for (YCAuditoria ycA : ycTabla.getItems()) {
                ycH.ycFila(ycA.getYcFecha(), ycA.getYcUsuario(), ycA.getYcAccion(), ycA.getYcDetalle());
            }
            ycExportador.ycGuardar(ycTabla.getScene().getWindow(), "Guardar historial en Excel",
                    "Historial de cambios " + java.time.LocalDate.now(), YCExportador.YCFormato.YC_EXCEL,
                    ycF -> com.alquilafacil.service.YCExcel.ycEscribir(ycF, List.of(ycH)));
        });
        HBox ycPie = new HBox(10, ycTotal, ycEsp, ycExcel, ycCerrar);
        ycPie.setAlignment(Pos.CENTER_LEFT);
        HBox ycFiltros = new HBox(10, ycBuscar, ycPeriodo);
        VBox ycDialogo = new VBox(12, ycTitulo, ycSub, ycFiltros, ycTabla, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(900);
        ycDialogo.setMaxSize(900, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    private void ycVerDetalle(YCAuditoria ycA) {
        Alert ycAlerta = new Alert(Alert.AlertType.INFORMATION);
        ycAlerta.setTitle("Detalle del cambio");
        ycAlerta.setHeaderText(ycA.getYcAccion() + " · " + ycA.getYcUsuario() + " · " + ycA.getYcFecha().format(YC_FECHA));
        TextArea ycTexto = new TextArea(ycA.getYcDetalle());
        ycTexto.setWrapText(true);
        ycTexto.setEditable(false);
        ycTexto.setPrefRowCount(6);
        ycAlerta.getDialogPane().setContent(ycTexto);
        ycAlerta.show();
    }
}
