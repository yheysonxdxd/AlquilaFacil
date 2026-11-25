package com.alquilafacil.ui;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.service.YCDatosNegocioService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class YCDatosNegocioDialogo {
    private final YCDatosNegocioService ycService;
    private final YCNotificador ycNotificador;
    private final com.alquilafacil.service.YCPagoDigitalService ycPagoDigital;
    private final com.alquilafacil.config.YCDatosArrendador ycArrendador;

    public void ycMostrar() {
        YCDatosNegocioService.YCDatos ycD = ycService.ycActuales();
        TextField ycNombre = ycCampo(ycD.ycNombre(), "Ej. Rosa Quispe Mamani");
        TextField ycDni = ycCampo(ycD.ycDni(), "8 digitos");
        YCFormularios.ycSoloDigitos(ycDni, 8);
        TextField ycTelefono = ycCampo(ycD.ycTelefono(), "9 digitos");
        YCFormularios.ycSoloDigitos(ycTelefono, 9);
        TextField ycDireccion = ycCampo(ycD.ycDireccion(), "Ej. Av. Los Andes 123, Wanchaq, Cusco");
        TextField ycHospedaje = ycCampo(ycD.ycNombreHospedaje(), "Ej. Hospedaje Supply");
        TextField ycHora = ycCampo(ycD.ycHoraLimiteLlegada(), "Ej. 20:00");
        TextField ycTasa = ycCampo(ycD.ycTasaImpuestoAlquiler() == null ? "5"
                : ycD.ycTasaImpuestoAlquiler().stripTrailingZeros().toPlainString(), "Ej. 5");
        YCFormularios.ycSoloDecimal(ycTasa);
        TextField ycMinutos = ycCampo(String.valueOf(ycD.ycMinutosInactividad() == null ? 15 : ycD.ycMinutosInactividad()), "Ej. 15");
        YCFormularios.ycSoloDigitos(ycMinutos, 3);
        if (ycService.ycFaltanDatos()) {
            if ("Nombre del Propietario".equals(ycNombre.getText())) ycNombre.clear();
            if ("Av. Principal 123".equals(ycDireccion.getText())) ycDireccion.clear();
        }

        GridPane ycGrid = new GridPane();
        ycGrid.setHgap(12);
        ycGrid.setVgap(10);
        ColumnConstraints ycC1 = new ColumnConstraints();
        ycC1.setPercentWidth(50);
        ColumnConstraints ycC2 = new ColumnConstraints();
        ycC2.setPercentWidth(50);
        ycGrid.getColumnConstraints().addAll(ycC1, ycC2);
        ycGrid.add(ycCaja("Tu nombre completo *", ycNombre), 0, 0, 2, 1);
        ycGrid.add(ycCaja("DNI", ycDni), 0, 1);
        ycGrid.add(ycCaja("Celular", ycTelefono), 1, 1);
        ycGrid.add(ycCaja("Direccion del inmueble *", ycDireccion), 0, 2, 2, 1);
        ycGrid.add(ycCaja("Nombre del hospedaje *", ycHospedaje), 0, 3);
        ycGrid.add(ycCaja("Hora limite de llegada", ycHora), 1, 3);
        ycGrid.add(ycCaja("Tasa del impuesto del alquiler (%)", ycTasa), 0, 4);
        ycGrid.add(ycCaja("Cerrar sesion si no se usa (minutos)", ycMinutos), 1, 4);
        Label ycAyudaTasa = new Label("La tasa es solo para el estimado del reporte anual (confirmala con tu contador). "
                + "Minutos: si nadie usa el programa ese tiempo, se cierra la sesion; 0 = nunca.");
        ycAyudaTasa.getStyleClass().add("campo-ayuda");
        ycAyudaTasa.setWrapText(true);
        ycGrid.add(ycAyudaTasa, 0, 5, 2, 1);

        TextField ycYape = ycCampo(ycArrendador.getYcYapeNumero(), "9 digitos");
        YCFormularios.ycSoloDigitos(ycYape, 9);
        TextField ycTitular = ycCampo(ycArrendador.getYcYapeTitular(), "Ej. Rosa Quispe M.");
        javafx.scene.image.ImageView ycVistaQr = new javafx.scene.image.ImageView();
        ycVistaQr.setFitWidth(96);
        ycVistaQr.setFitHeight(96);
        ycVistaQr.setPreserveRatio(true);
        Label ycSinQr = new Label("Sin QR");
        ycSinQr.getStyleClass().add("campo-ayuda");
        StackPane ycMarcoQr = new StackPane(ycSinQr, ycVistaQr);
        ycMarcoQr.setMinSize(100, 100);
        ycMarcoQr.setMaxSize(100, 100);
        ycMarcoQr.getStyleClass().add("estadia-info");
        java.nio.file.Path[] ycQrElegido = {null};
        boolean[] ycQuitarQr = {false};
        ycPagoDigital.ycQr().ifPresent(ycP -> ycVistaQr.setImage(new javafx.scene.image.Image(ycP.toUri().toString())));
        ycSinQr.visibleProperty().bind(ycVistaQr.imageProperty().isNull());
        Button ycElegirQr = new Button("Elegir imagen del QR", new FontIcon(Feather.IMAGE));
        ycElegirQr.getStyleClass().add("button-outlined");
        Button ycBorrarQr = new Button("Quitar QR", new FontIcon(Feather.TRASH_2));
        ycBorrarQr.getStyleClass().addAll("flat", "danger");
        ycBorrarQr.disableProperty().bind(ycVistaQr.imageProperty().isNull());
        ycBorrarQr.setOnAction(ycEv -> {
            ycVistaQr.setImage(null);
            ycQrElegido[0] = null;
            ycQuitarQr[0] = true;
        });
        Label ycAyudaQr = new Label("En Yape: Menu → Mi QR → descargar o tomar captura. Sale en los comprobantes de "
                + "hospedaje; el celular va en los mensajes de WhatsApp.");
        ycAyudaQr.getStyleClass().add("campo-ayuda");
        ycAyudaQr.setWrapText(true);
        VBox ycBotonesQr = new VBox(8, ycElegirQr, ycBorrarQr, ycAyudaQr);
        HBox ycFilaQr = new HBox(14, ycMarcoQr, ycBotonesQr);
        HBox.setHgrow(ycBotonesQr, Priority.ALWAYS);
        Label ycSeccionYape = new Label("COBROS CON YAPE / PLIN");
        ycSeccionYape.getStyleClass().add("seccion-form");
        ycGrid.add(ycSeccionYape, 0, 6, 2, 1);
        ycGrid.add(ycCaja("Celular de Yape / Plin", ycYape), 0, 7);
        ycGrid.add(ycCaja("A nombre de", ycTitular), 1, 7);
        ycGrid.add(ycFilaQr, 0, 8, 2, 1);

        StackPane ycCirculo = new StackPane(new FontIcon(Feather.BRIEFCASE));
        ycCirculo.getStyleClass().addAll("dialogo-icono", "exito");
        Label ycTitulo = new Label("Datos del negocio");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycTexto = new Label("Salen en los contratos, recibos, comprobantes, reportes y mensajes de WhatsApp. "
                + "Se guardan en la base (tambien en las copias de seguridad).");
        ycTexto.getStyleClass().add("dialogo-mensaje");
        ycTexto.setWrapText(true);

        Button ycCancelar = new Button("Cancelar");
        ycCancelar.getStyleClass().add("button-outlined");
        ycCancelar.setCancelButton(true);
        ycCancelar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        Button ycGuardar = new Button("Guardar", new FontIcon(Feather.CHECK));
        ycGuardar.getStyleClass().add("accent");
        ycGuardar.setDefaultButton(true);
        ycGuardar.setOnAction(ycE -> {
            BigDecimal ycT;
            try {
                ycT = new BigDecimal(ycTasa.getText().trim().replace(',', '.'));
            } catch (NumberFormatException ycEx) {
                ycNotificador.ycError("La tasa debe ser un numero, por ejemplo 5.");
                return;
            }
            try {
                ycService.ycGuardar(new YCDatosNegocioService.YCDatos(ycNombre.getText(), ycDni.getText(), ycDireccion.getText(),
                        ycTelefono.getText(), ycHospedaje.getText(), ycHora.getText(), ycT,
                        ycMinutos.getText().isBlank() ? 0 : Integer.parseInt(ycMinutos.getText().trim())));
                ycPagoDigital.ycGuardar(ycYape.getText(), ycTitular.getText(), ycQrElegido[0], ycQuitarQr[0]);
                ycNotificador.ycCerrarDialogo();
                ycNotificador.ycExito("Datos del negocio guardados. Ya salen en los nuevos contratos y recibos.");
            } catch (YCNegocioException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
        });
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycPie = new HBox(10, ycEsp, ycCancelar, ycGuardar);
        ycPie.setAlignment(Pos.CENTER_RIGHT);
        ycPie.getStyleClass().add("dialogo-acciones");

        VBox ycDialogo = new VBox(14, ycCirculo, ycTitulo, ycTexto, ycGrid, ycPie);
        ycElegirQr.setOnAction(ycEv -> {
            javafx.stage.FileChooser ycFc = new javafx.stage.FileChooser();
            ycFc.setTitle("Elige la imagen de tu QR de Yape o Plin");
            ycFc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen (PNG o JPG)",
                    "*.png", "*.jpg", "*.jpeg"));
            java.io.File ycF = ycFc.showOpenDialog(ycDialogo.getScene().getWindow());
            if (ycF != null) {
                ycQrElegido[0] = ycF.toPath();
                ycQuitarQr[0] = false;
                ycVistaQr.setImage(new javafx.scene.image.Image(ycF.toURI().toString()));
            }
        });
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setPrefWidth(560);
        ycDialogo.setMaxSize(560, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
        Platform.runLater(ycNombre::requestFocus);
    }

    private static TextField ycCampo(String ycValor, String ycEjemplo) {
        TextField ycT = new TextField(ycValor == null ? "" : ycValor);
        ycT.setPromptText(ycEjemplo);
        ycT.setMaxWidth(Double.MAX_VALUE);
        return ycT;
    }

    private static VBox ycCaja(String ycEtiqueta, TextField ycCampo) {
        Label ycL = new Label(ycEtiqueta);
        ycL.getStyleClass().add("campo-label");
        return new VBox(4, ycL, ycCampo);
    }
}
