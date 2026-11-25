package com.alquilafacil.controller;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class YCMainController {
    private static final org.slf4j.Logger ycLog = org.slf4j.LoggerFactory.getLogger(YCMainController.class);

    private final ApplicationContext ycContext;
    private final YCNotificador ycNotificador;
    private final com.alquilafacil.service.YCRespaldoService ycRespaldoService;
    private final com.alquilafacil.ui.YCExportador ycExportador;
    private final com.alquilafacil.config.YCDatosArrendador ycArrendador;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final com.alquilafacil.ui.YCPantallaIngreso ycPantallaIngreso;
    private final com.alquilafacil.ui.YCGestionUsuarios ycGestionUsuarios;
    private final com.alquilafacil.ui.YCDatosNegocioDialogo ycDatosNegocioDialogo;
    private final com.alquilafacil.ui.YCHistorialDialogo ycHistorialDialogo;
    private final com.alquilafacil.service.YCDatosNegocioService ycDatosNegocio;
    private final com.alquilafacil.service.YCUsuarioService ycUsuarioService;

    @FXML private StackPane ycRaiz;
    @FXML private VBox ycBarra;
    @FXML private StackPane ycContenido;
    @FXML private ModalPane ycModalPanel;
    @FXML private ModalPane ycModalDialogo;
    @FXML private VBox ycZonaNotificaciones;

    @FXML private ToggleGroup ycMenu;
    @FXML private ToggleButton ycNavInicio;
    @FXML private ToggleButton ycNavCuartos;
    @FXML private ToggleButton ycNavInquilinos;
    @FXML private ToggleButton ycNavPagos;
    @FXML private ToggleButton ycNavHospedaje;
    @FXML private ToggleButton ycNavHabitaciones;
    @FXML private ToggleButton ycNavGastos;
    @FXML private ToggleButton ycNavCaja;
    @FXML private Label ycLblIniciales;
    @FXML private Label ycLblUsuario;
    @FXML private Label ycLblRol;
    @FXML private javafx.scene.control.MenuButton ycMenuOpciones;
    private final javafx.scene.control.MenuItem ycItemUsuarios = new javafx.scene.control.MenuItem("Usuarios", new FontIcon(Feather.USERS));
    private final javafx.scene.control.MenuItem ycItemRespaldo = new javafx.scene.control.MenuItem("Copia de seguridad", new FontIcon(Feather.HARD_DRIVE));
    private final javafx.scene.control.MenuItem ycItemDatosNegocio = new javafx.scene.control.MenuItem("Datos del negocio", new FontIcon(Feather.BRIEFCASE));
    private final javafx.scene.control.MenuItem ycItemHistorial = new javafx.scene.control.MenuItem("Historial de cambios", new FontIcon(Feather.CLOCK));
    private final javafx.scene.control.MenuItem ycItemDiseno = new javafx.scene.control.MenuItem("Diseño y colores", new FontIcon(Feather.DROPLET));
    private final javafx.scene.control.MenuItem ycItemTema = new javafx.scene.control.MenuItem("Modo oscuro", new FontIcon(Feather.MOON));
    @FXML private Button ycBtnSalir;
    private Node ycCapaIngreso;

    @FXML private Label ycLblMarca;

    private record YCTema(String ycClave, String ycNombre, String ycPrincipal, String ycHospedaje, String ycFondo) {}

    private static final List<YCTema> YC_TEMAS = List.of(
            new YCTema("coral", "Coral reservas", "#ff385c", "#222222", "#f7f7f7"),
            new YCTema("bosque", "Bosque y dorado", "#047857", "#a16207", "#f4f7f5"),
            new YCTema("oceano", "Oceano", "#2563eb", "#0891b2", "#f4f7fb"),
            new YCTema("vino", "Vino y crema", "#9f1239", "#a16207", "#faf7f2"),
            new YCTema("dorado", "Negro y dorado", "#b8860b", "#111111", "#f6f5f2"),
            new YCTema("lavanda", "Lavanda pastel", "#8b5cf6", "#0d9488", "#faf8ff"),
            new YCTema("terracota", "Terracota andino", "#c2410c", "#0f766e", "#faf6f2"),
            new YCTema("turquesa", "Verde azulado", "#0d9488", "#7c3aed", "#f3f6f9"),
            new YCTema("lima", "Grafito y lima", "#65a30d", "#0284c7", "#f4f5f6"));
    private static final java.io.File YC_PREFERENCIAS = new java.io.File(System.getProperty("app.carpeta", "."), "preferencias.properties");
    private String ycTemaActual = "coral";

    private final Map<String, YCVista> ycVistas = new HashMap<>();
    private boolean ycOscuro = false;

    private record YCVista(Parent ycNodo, Object ycControlador) {}

    @FXML
    public void initialize() {
        ycNotificador.ycRegistrar(ycModalPanel, ycModalDialogo, ycZonaNotificaciones);
        ycLblMarca.setText(ycArrendador.getYcNombreSistema());
        ycAplicarTema(ycLeerPreferencia("tema", "coral"), false);
        ycModalPanel.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        ycModalDialogo.setAlignment(javafx.geometry.Pos.CENTER);

        ycNavInicio.setUserData("dashboard-view");
        ycNavCuartos.setUserData("cuarto-view");
        ycNavInquilinos.setUserData("inquilino-view");
        ycNavPagos.setUserData("pago-view");
        ycNavHospedaje.setUserData("hospedaje-view");
        ycNavHabitaciones.setUserData("habitacion-view");
        ycNavGastos.setUserData("gasto-view");
        ycNavCaja.setUserData("caja-view");
        ycSesion.ycSoloAdmin(ycNavGastos);
        ycArmarMenuOpciones();
        for (ToggleButton ycB : ycBotonesMenu()) {
            Tooltip ycT = new Tooltip(ycB.getText());
            ycT.setShowDelay(Duration.millis(300));
            ycB.setTooltip(ycT);
        }

        ycMenu.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual) -> {
            if (ycActual == null) {
                ycMenu.selectToggle(ycAnterior);
                return;
            }
            ycMostrar((String) ycActual.getUserData());
        });

        ycMenu.selectToggle(ycNavInicio);
        ycMostrarIngreso();
        ycVigilarInactividad();
    }

    private long ycUltimaActividad = System.currentTimeMillis();

    private void ycVigilarInactividad() {
        javafx.event.EventHandler<javafx.event.Event> ycActividad = ycE -> ycUltimaActividad = System.currentTimeMillis();
        ycRaiz.sceneProperty().addListener((ycO, ycAntes, ycEscena) -> {
            if (ycEscena != null) {
                ycEscena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, ycActividad);
                ycEscena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, ycActividad);
                ycEscena.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ycActividad);
                ycEscena.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, ycActividad);
            }
        });
        Timeline ycRevisar = new Timeline(new KeyFrame(Duration.seconds(20), ycE -> {
            int ycMinutos = ycArrendador.getYcMinutosInactividad();
            if (ycMinutos > 0 && ycSesion.ycIniciada()
                    && System.currentTimeMillis() - ycUltimaActividad > ycMinutos * 60_000L) {
                String ycQuien = ycSesion.ycNombreUsuario();
                ycOnCerrarSesion();
                ycLog.info("Sesion de {} cerrada por {} minutos sin uso", ycQuien, ycMinutos);
            }
        }));
        ycRevisar.setCycleCount(javafx.animation.Animation.INDEFINITE);
        ycRevisar.play();
    }

    private void ycArmarMenuOpciones() {
        ycItemUsuarios.setOnAction(ycE -> ycOnUsuarios());
        ycItemRespaldo.setOnAction(ycE -> ycOnRespaldo());
        ycItemDatosNegocio.setOnAction(ycE -> ycOnDatosNegocio());
        ycItemHistorial.setOnAction(ycE -> ycOnHistorial());
        ycItemDiseno.setOnAction(ycE -> ycOnDiseno());
        ycItemTema.setOnAction(ycE -> ycOnCambiarTema());
        for (javafx.scene.control.MenuItem ycSoloAdmin : List.of(ycItemDatosNegocio, ycItemHistorial, ycItemDiseno)) {
            ycSoloAdmin.visibleProperty().bind(ycSesion.ycAdminProperty());
        }
        javafx.scene.control.SeparatorMenuItem ycSeparador = new javafx.scene.control.SeparatorMenuItem();
        ycSeparador.visibleProperty().bind(ycSesion.ycAdminProperty());
        ycMenuOpciones.getItems().setAll(ycItemUsuarios, ycItemRespaldo, ycSeparador, ycItemDatosNegocio, ycItemHistorial,
                ycItemDiseno, new javafx.scene.control.SeparatorMenuItem(), ycItemTema);
        ycMenuOpciones.setOnShowing(ycE -> ycItemRespaldo.setText(ycRespaldoService.ycUltimo()
                .map(ycF -> "Copia de seguridad (ultima: " + ycF.getName().replace("alquileres_", "").replace(".db", "")
                        .replace("_antes-de-restaurar", "").replaceFirst("^\\d{4}-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2}).*$", "$2/$1 $3:$4") + ")")
                .orElse("Copia de seguridad (aun no hay)")));
    }

    private void ycMostrarIngreso() {
        ycCapaIngreso = ycPantallaIngreso.ycCrear(ycU -> {
            ycUltimaActividad = System.currentTimeMillis();
            ycRaiz.getChildren().remove(ycCapaIngreso);
            ycCapaIngreso = null;
            ycLblUsuario.setText(ycU.getYcNombre());
            ycLblRol.setText(ycU.getYcRol().ycGetTexto());
            ycLblIniciales.setText(com.alquilafacil.ui.YCTablas.ycIniciales(ycU.getYcNombre()));
            ycItemUsuarios.setText(ycU.ycEsAdmin() ? "Usuarios" : "Mi contraseña");
            if (ycMenu.getSelectedToggle() == ycNavInicio) {
                ycMostrar("dashboard-view");
            } else {
                ycMenu.selectToggle(ycNavInicio);
            }
            ycNotificador.ycExito("Hola, " + ycU.getYcNombre().split(" ")[0] + ".");
            if (ycU.ycEsAdmin()) {
                Platform.runLater(this::ycRecordatoriosAdmin);
            }
        });
        ycRaiz.getChildren().add(ycCapaIngreso);
    }

    private void ycRecordatoriosAdmin() {
        if (ycDatosNegocio.ycFaltanDatos()) {
            ycNotificador.ycAvisoConAccion("Completa los datos de tu negocio",
                    "Tu nombre y la direccion todavia son los de ejemplo, y asi salen en los contratos, "
                            + "recibos y reportes.", "Despues", "Completar ahora", ycDatosNegocioDialogo::ycMostrar);
        } else if (!ycUsuarioService.ycTengoCodigoRecuperacion()) {
            ycNotificador.ycAvisoConAccion("Crea tu codigo de recuperacion",
                    "Si olvidas tu contraseña de administrador, con este codigo pones una nueva. "
                            + "Sin el, nadie podria volver a anular, cancelar ni ver las ganancias.",
                    "Despues", "Crear ahora", ycGestionUsuarios::ycMostrarNuevoCodigo);
        } else {
            var ycExterna = ycRespaldoService.ycEstadoExterno();
            boolean ycVieja = ycExterna.ycUltima() == null
                    || ycExterna.ycUltima().isBefore(java.time.LocalDateTime.now().minusDays(7));
            if (ycExterna.ycCarpeta() == null) {
                ycNotificador.ycAvisoConAccion("Guarda tus copias fuera de esta PC",
                        "Hoy todas las copias de seguridad estan en el mismo disco que el programa. Si se daña "
                                + "o roban la PC, se pierde todo. Elige una carpeta de Google Drive o una USB.",
                        "Despues", "Configurar", this::ycOnRespaldo);
            } else if (ycVieja) {
                ycNotificador.ycAvisoConAccion("Hace mas de 7 dias que no llega una copia externa",
                        "No se pudo copiar a " + ycExterna.ycCarpeta() + (ycExterna.ycError() == null ? "" : " (" + ycExterna.ycError() + ")")
                                + ". Conecta la USB o abre Google Drive y haz una copia.",
                        "Despues", "Abrir copias", this::ycOnRespaldo);
            }
        }
    }

    @FXML
    private void ycOnHistorial() {
        ycHistorialDialogo.ycMostrar();
    }

    @FXML
    private void ycOnDatosNegocio() {
        ycDatosNegocioDialogo.ycMostrar();
    }

    @FXML
    private void ycOnCerrarSesion() {
        ycNotificador.ycCerrarDialogo();
        ycNotificador.ycCerrarPanel();
        ycSesion.ycCerrar();
        ycLblUsuario.setText("Sin sesion");
        ycLblRol.setText("");
        ycMostrarIngreso();
    }

    @FXML
    private void ycOnUsuarios() {
        if (ycSesion.ycEsAdmin()) {
            ycGestionUsuarios.ycMostrarLista();
        } else if (ycSesion.ycGetUsuario() != null) {
            ycGestionUsuarios.ycMostrarCambioClave(ycSesion.ycGetUsuario(), true);
        }
    }

    public void ycIrA(String ycVista) {
        ycBotonesMenu().stream()
                .filter(ycB -> ycVista.equals(ycB.getUserData()))
                .findFirst()
                .ifPresent(ycMenu::selectToggle);
    }

    public Object ycControladorDe(String ycVista) {
        return ycVistas.computeIfAbsent(ycVista, this::ycCargar).ycControlador();
    }

    private void ycMostrar(String ycNombre) {
        YCVista ycVista = ycVistas.computeIfAbsent(ycNombre, this::ycCargar);
        ycContenido.getChildren().setAll(ycVista.ycNodo());
        if (ycVista.ycControlador() instanceof YCRefrescable ycR) {
            ycR.ycRefrescar();
        }
        Animations.fadeInUp(ycVista.ycNodo(), Duration.millis(380)).play();
    }

    private YCVista ycCargar(String ycNombre) {
        FXMLLoader ycLoader = new FXMLLoader(getClass().getResource("/view/" + ycNombre + ".fxml"));
        ycLoader.setControllerFactory(ycContext::getBean);
        ycLoader.setClassLoader(getClass().getClassLoader());
        try {
            Parent ycNodo = ycLoader.load();
            return new YCVista(ycNodo, ycLoader.getController());
        } catch (IOException ycE) {
            throw new UncheckedIOException("No se pudo cargar la vista " + ycNombre, ycE);
        }
    }

    @FXML
    private void ycOnRespaldo() {
        Label ycTitulo = new Label("Copias de seguridad");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycAyuda = new Label("Se hace una copia automatica cada dia al abrir el programa y se guardan las ultimas 30. "
                + "Para mas seguridad, copia la carpeta \"respaldos\" a una USB o a Google Drive de vez en cuando.");
        ycAyuda.getStyleClass().add("dialogo-mensaje");
        ycAyuda.setWrapText(true);

        Button ycCopiarAhora = new Button("Hacer copia ahora", new FontIcon(Feather.SAVE));
        ycCopiarAhora.getStyleClass().add("accent");
        ycCopiarAhora.setOnAction(ycE -> {
            try {
                java.io.File ycCopia = ycRespaldoService.ycRespaldarAhora();
                ycNotificador.ycExito("Copia guardada: " + ycFechaCopia(ycCopia));
                ycOnRespaldo();
            } catch (RuntimeException ycEx) {
                ycNotificador.ycError("No se pudo hacer la copia: " + ycEx.getMessage());
            }
        });
        Button ycAbrirCarpeta = new Button("Abrir carpeta", new FontIcon(Feather.FOLDER));
        ycAbrirCarpeta.getStyleClass().add(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
        ycAbrirCarpeta.setOnAction(ycE -> ycExportador.ycAbrirArchivo(ycRespaldoService.ycGetCarpeta()));
        javafx.scene.layout.HBox ycBotones = new javafx.scene.layout.HBox(10, ycCopiarAhora, ycAbrirCarpeta);

        VBox ycLista = new VBox(6);
        List<java.io.File> ycCopias = ycRespaldoService.ycListar();
        if (ycCopias.isEmpty()) {
            ycLista.getChildren().add(new Label("Aun no hay copias."));
        }
        for (java.io.File ycF : ycCopias.stream().limit(10).toList()) {
            Label ycNombre = new Label(ycFechaCopia(ycF));
            Region ycEspacio = new Region();
            javafx.scene.layout.HBox.setHgrow(ycEspacio, javafx.scene.layout.Priority.ALWAYS);
            Button ycRestaurar = new Button("Volver a esta copia", new FontIcon(Feather.ROTATE_CCW));
            ycRestaurar.getStyleClass().addAll(atlantafx.base.theme.Styles.SMALL, atlantafx.base.theme.Styles.BUTTON_OUTLINED);
            ycRestaurar.setOnAction(ycE -> ycConfirmarRestaurar(ycF));
            ycSesion.ycSoloAdmin(ycRestaurar);
            javafx.scene.layout.HBox ycFila = new javafx.scene.layout.HBox(10, ycNombre, ycEspacio, ycRestaurar);
            ycFila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ycLista.getChildren().add(ycFila);
        }
        Label ycTituloLista = new Label(ycCopias.size() > 10 ? "Ultimas 10 copias" : "Copias guardadas");
        ycTituloLista.getStyleClass().add("tema-nombre");

        Button ycOtroArchivo = new Button("Restaurar desde otro archivo (USB, Drive)...", new FontIcon(Feather.UPLOAD));
        ycOtroArchivo.getStyleClass().add(atlantafx.base.theme.Styles.FLAT);
        ycOtroArchivo.setOnAction(ycE -> {
            javafx.stage.FileChooser ycFc = new javafx.stage.FileChooser();
            ycFc.setTitle("Elige la copia de seguridad (.db)");
            ycFc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Copia de seguridad (*.db)", "*.db"));
            java.io.File ycElegido = ycFc.showOpenDialog(ycRaiz.getScene().getWindow());
            if (ycElegido != null) {
                ycConfirmarRestaurar(ycElegido);
            }
        });
        ycSesion.ycSoloAdmin(ycOtroArchivo);

        Button ycCerrar = new Button("Cerrar");
        ycCerrar.setCancelButton(true);
        ycCerrar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        Region ycEspacioPie = new Region();
        javafx.scene.layout.HBox.setHgrow(ycEspacioPie, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox ycPie = new javafx.scene.layout.HBox(10, ycOtroArchivo, ycEspacioPie, ycCerrar);
        ycPie.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox ycDialogo = new VBox(14, ycTitulo, ycAyuda, ycBotones, ycSeccionExterna(), ycTituloLista, ycLista, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(560, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    private VBox ycSeccionExterna() {
        var ycEstado = ycRespaldoService.ycEstadoExterno();
        java.time.format.DateTimeFormatter ycF = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Label ycTituloExt = new Label("COPIA FUERA DE ESTA PC (USB o Google Drive)");
        ycTituloExt.getStyleClass().add("tema-nombre");
        Label ycInfo;
        if (ycEstado.ycCarpeta() == null) {
            ycInfo = new Label("Aun no configurada. Si el disco se daña o roban la PC, se pierden tambien las copias. "
                    + "Elige una carpeta de Google Drive (ej. G:\\Mi unidad) o una USB y cada copia se guardara tambien alli.",
                    new FontIcon(Feather.ALERT_TRIANGLE));
            ycInfo.getStyleClass().add("estadia-info");
        } else {
            String ycTexto = "Carpeta: " + ycEstado.ycCarpeta() + "\nUltima copia que llego: "
                    + (ycEstado.ycUltima() == null ? "ninguna todavia" : ycEstado.ycUltima().format(ycF));
            if (ycEstado.ycError() != null) {
                ycTexto += "\nUltimo problema: " + ycEstado.ycError() + ". Conecta la USB o abre Google Drive y haz una copia.";
            }
            ycInfo = new Label(ycTexto, new FontIcon(ycEstado.ycError() == null ? Feather.CHECK_CIRCLE : Feather.ALERT_TRIANGLE));
            ycInfo.getStyleClass().add(ycEstado.ycError() == null ? "campo-ayuda" : "estadia-info");
        }
        ycInfo.setWrapText(true);
        ycInfo.setMaxWidth(Double.MAX_VALUE);
        Button ycElegir = new Button(ycEstado.ycCarpeta() == null ? "Elegir carpeta..." : "Cambiar carpeta...",
                new FontIcon(Feather.FOLDER_PLUS));
        ycElegir.getStyleClass().add(ycEstado.ycCarpeta() == null ? "success" : atlantafx.base.theme.Styles.FLAT);
        ycElegir.setOnAction(ycE -> {
            javafx.stage.DirectoryChooser ycDc = new javafx.stage.DirectoryChooser();
            ycDc.setTitle("Elige la carpeta de la USB o de Google Drive");
            java.io.File ycElegida = ycDc.showDialog(ycRaiz.getScene().getWindow());
            if (ycElegida == null) {
                return;
            }
            try {
                ycRespaldoService.ycConfigurarCarpetaExterna(ycElegida);
                ycNotificador.ycExito("Listo: las copias tambien se guardan en " + ycElegida.getAbsolutePath() + ".");
            } catch (RuntimeException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
            ycOnRespaldo();
        });
        Button ycQuitar = new Button("Quitar", new FontIcon(Feather.X));
        ycQuitar.getStyleClass().add(atlantafx.base.theme.Styles.FLAT);
        ycQuitar.setVisible(ycEstado.ycCarpeta() != null);
        ycQuitar.setManaged(ycEstado.ycCarpeta() != null);
        ycQuitar.setOnAction(ycE -> {
            try {
                ycRespaldoService.ycQuitarCarpetaExterna();
            } catch (RuntimeException ycEx) {
                ycNotificador.ycError(ycEx.getMessage());
            }
            ycOnRespaldo();
        });
        ycSesion.ycSoloAdmin(ycElegir);
        javafx.scene.layout.HBox ycAcciones = new javafx.scene.layout.HBox(8, ycElegir, ycQuitar);
        VBox ycCaja = new VBox(6, ycTituloExt, ycInfo, ycAcciones);
        return ycCaja;
    }

    private static String ycFechaCopia(java.io.File ycF) {
        String ycN = ycF.getName().replace("alquileres_", "").replace(".db", "");
        boolean ycAntes = ycN.endsWith("_antes-de-restaurar");
        ycN = ycN.replace("_antes-de-restaurar", "");
        String[] ycP = ycN.split("_");
        String ycTexto = ycP.length == 2 ? ycP[0] + "  " + ycP[1].replace('-', ':') : ycN;
        return ycAntes ? ycTexto + "  (antes de restaurar)" : ycTexto;
    }

    private void ycConfirmarRestaurar(java.io.File ycCopia) {
        ycNotificador.ycConfirmar("¿Volver a la copia del " + ycFechaCopia(ycCopia) + "?",
                "Los datos actuales se reemplazan por los de esa copia: se pierde lo registrado despues de esa fecha. "
                        + "Antes se guarda una copia de lo que hay ahora, por si te equivocas de copia. "
                        + "Al terminar, el programa se cierra y se vuelve a abrir.",
                "Restaurar", () -> {
                    try {
                        ycRespaldoService.ycRestaurar(ycCopia);
                        ycNotificador.ycExitoConAcciones("Copia restaurada",
                                "Los datos ya son los de la copia. El programa se cerrara para volver a cargarlos.",
                                "Cerrar", this::ycReiniciar, "Reiniciar ahora", this::ycReiniciar);
                    } catch (RuntimeException ycEx) {
                        ycNotificador.ycError("No se pudo restaurar: " + ycEx.getMessage());
                    }
                });
    }

    private void ycReiniciar() {
        ProcessHandle.current().info().command().ifPresent(ycCmd -> {
            String ycExe = new java.io.File(ycCmd).getName().toLowerCase();
            if (ycExe.endsWith(".exe") && !ycExe.equals("java.exe") && !ycExe.equals("javaw.exe")) {
                try {
                    new ProcessBuilder(ycCmd).directory(new java.io.File(System.getProperty("user.dir"))).start();
                } catch (IOException ycE) {
                }
            }
        });
        Platform.exit();
    }

    @FXML
    private void ycOnDiseno() {
        javafx.scene.layout.TilePane ycOpciones = new javafx.scene.layout.TilePane(12, 12);
        ycOpciones.setPrefColumns(3);
        for (YCTema ycT : YC_TEMAS) {
            javafx.scene.layout.HBox ycMuestras = new javafx.scene.layout.HBox(6,
                    ycMuestra(ycT.ycFondo(), 34), ycMuestra(ycT.ycPrincipal(), 34), ycMuestra(ycT.ycHospedaje(), 34));
            Label ycNombre = new Label(ycT.ycNombre());
            ycNombre.getStyleClass().add("tema-nombre");
            VBox ycTarjeta = new VBox(10, ycMuestras, ycNombre);
            ycTarjeta.getStyleClass().add("tema-opcion");
            ycTarjeta.setPrefWidth(170);
            if (ycT.ycClave().equals(ycTemaActual)) {
                ycTarjeta.getStyleClass().add("activa");
            }
            ycTarjeta.setOnMouseClicked(ycE -> {
                ycAplicarTema(ycT.ycClave(), true);
                ycOpciones.getChildren().forEach(ycN -> ycN.getStyleClass().remove("activa"));
                ycTarjeta.getStyleClass().add("activa");
            });
            ycOpciones.getChildren().add(ycTarjeta);
        }
        Label ycTitulo = new Label("Elige el diseño");
        ycTitulo.getStyleClass().add("dialogo-titulo");
        Label ycAyuda = new Label("Haz clic en uno y se aplica al instante. Prueba todos los que quieras; se recuerda el ultimo.");
        ycAyuda.getStyleClass().add("dialogo-mensaje");
        ycAyuda.setWrapText(true);
        Button ycListo = new Button("Listo");
        ycListo.getStyleClass().add("accent");
        ycListo.setDefaultButton(true);
        ycListo.setCancelButton(true);
        ycListo.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        javafx.scene.layout.HBox ycPie = new javafx.scene.layout.HBox(ycListo);
        ycPie.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        VBox ycDialogo = new VBox(14, ycTitulo, ycAyuda, ycOpciones, ycPie);
        ycDialogo.getStyleClass().add("dialogo");
        ycDialogo.setMaxSize(600, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycDialogo);
    }

    private static Region ycMuestra(String ycColor, double ycTam) {
        Region ycR = new Region();
        ycR.setMinSize(ycTam, ycTam);
        ycR.setMaxSize(ycTam, ycTam);
        ycR.setStyle("-fx-background-color: " + ycColor + "; -fx-background-radius: 9; "
                + "-fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 9;");
        return ycR;
    }

    private void ycAplicarTema(String ycClave, boolean ycGuardar) {
        if (YC_TEMAS.stream().noneMatch(ycT -> ycT.ycClave().equals(ycClave))) {
            return;
        }
        ycRaiz.getStyleClass().removeIf(ycC -> ycC.startsWith("tema-"));
        ycRaiz.getStyleClass().add("tema-" + ycClave);
        ycTemaActual = ycClave;
        if (ycGuardar) {
            ycGuardarPreferencia("tema", ycClave);
        }
    }

    private static String ycLeerPreferencia(String ycClave, String ycPorDefecto) {
        java.util.Properties ycP = new java.util.Properties();
        try (var ycIn = new java.io.FileInputStream(YC_PREFERENCIAS)) {
            ycP.load(ycIn);
        } catch (IOException ycE) {
            return ycPorDefecto;
        }
        return ycP.getProperty(ycClave, ycPorDefecto);
    }

    private static void ycGuardarPreferencia(String ycClave, String ycValor) {
        java.util.Properties ycP = new java.util.Properties();
        try (var ycIn = new java.io.FileInputStream(YC_PREFERENCIAS)) {
            ycP.load(ycIn);
        } catch (IOException ycIgnorada) {
        }
        ycP.setProperty(ycClave, ycValor);
        try (var ycOut = new java.io.FileOutputStream(YC_PREFERENCIAS)) {
            ycP.store(ycOut, "Preferencias de Hospedaje Supply");
        } catch (IOException ycE) {
        }
    }

    @FXML
    private void ycOnCambiarTema() {
        ycOscuro = !ycOscuro;
        Application.setUserAgentStylesheet(ycOscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (ycOscuro) {
            ycRaiz.getStyleClass().add("dark");
        } else {
            ycRaiz.getStyleClass().remove("dark");
        }
        ((FontIcon) ycItemTema.getGraphic()).setIconCode(ycOscuro ? Feather.SUN : Feather.MOON);
        ycItemTema.setText(ycOscuro ? "Modo claro" : "Modo oscuro");
        Animations.fadeIn(ycContenido, Duration.millis(300)).play();
    }

    private List<ToggleButton> ycBotonesMenu() {
        return List.of(ycNavInicio, ycNavCaja, ycNavGastos, ycNavHabitaciones, ycNavHospedaje, ycNavCuartos, ycNavInquilinos, ycNavPagos);
    }
}
