package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCComodidad;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCModalidad;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.service.YCCuartoService;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.service.YCFotoService;
import com.alquilafacil.service.YCInquilinoService;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCFotos;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import com.alquilafacil.ui.YCTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class YCCuartoController implements YCRefrescable {
    private static final double YC_TARJETA_ANCHO = 290;
    private static final double YC_PORTADA_ALTO = 180;
    private static final double YC_PREVIEW_ANCHO = 404;
    private static final double YC_PREVIEW_ALTO = 220;
    private static final int YC_COMODIDADES_EN_TARJETA = 4;
    private static final int YC_MAX_FOTOS_EXTRA = 8;
    private static final double YC_MINI_ANCHO = 96;
    private static final double YC_MINI_ALTO = 70;
    private static final java.time.format.DateTimeFormatter YC_DIA_MES = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

    private final YCCuartoService ycService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCInquilinoService ycInquilinoService;
    private final YCFotoService ycFotoService;
    private final YCFotos ycFotos;
    private final YCNotificador ycNotificador;
    private final YCEstadiaService ycEstadiaService;
    private final YCMainController ycMainController;

    @FXML private VBox ycPanelFormulario;
    @FXML private Label ycLblTituloFormulario;
    @FXML private Label ycLblSubtituloFormulario;
    @FXML private Label ycLblResumen;
    @FXML private StackPane ycHero;
    @FXML private Label ycLblHeroDisponibles;
    @FXML private Label ycLblHeroOcupacion;
    @FXML private Label ycLblHeroDesde;
    @FXML private TextField ycTxtNumero;
    @FXML private TextField ycTxtPiso;
    @FXML private TextField ycTxtCapacidad;
    @FXML private TextArea ycTxtDescripcion;
    @FXML private TextField ycTxtPrecio;
    @FXML private TextField ycTxtBuscar;
    @FXML private StackPane ycPreviewFoto;
    @FXML private Button ycBtnQuitarFoto;
    @FXML private FlowPane ycPanelComodidades;
    @FXML private FlowPane ycPanelGaleria;

    @FXML private ToggleGroup ycGrupoVista;
    @FXML private ToggleGroup ycGrupoEstado;
    @FXML private ToggleButton ycVistaGaleria;
    @FXML private ToggleButton ycVistaLista;
    @FXML private ToggleButton ycFiltroTodos;
    @FXML private ToggleButton ycFiltroLibres;
    @FXML private ToggleButton ycFiltroOcupados;
    @FXML private Label ycLblPrecio;
    @FXML private Label ycLblAyudaPrecio;
    @FXML private ScrollPane ycScrollGaleria;
    @FXML private FlowPane ycGaleria;
    @FXML private VBox ycPanelLista;

    @FXML private TableView<YCCuarto> ycTabla;
    @FXML private TableColumn<YCCuarto, String> ycColNumero;
    @FXML private TableColumn<YCCuarto, String> ycColPiso;
    @FXML private TableColumn<YCCuarto, String> ycColDescripcion;
    @FXML private TableColumn<YCCuarto, BigDecimal> ycColPrecio;
    @FXML private TableColumn<YCCuarto, String> ycColEstado;
    @FXML private TableColumn<YCCuarto, String> ycColInquilino;

    @FXML private Button ycBtnEditar;
    @FXML private Button ycBtnNuevo;
    @FXML private Button ycBtnEliminar;

    private final ObservableList<YCCuarto> ycDatos = FXCollections.observableArrayList();
    private final FilteredList<YCCuarto> ycFiltrados = new FilteredList<>(ycDatos);
    private final Map<Long, YCInquilino> ycOcupantes = new HashMap<>();
    private final Map<Long, YCEstadia> ycHuespedes = new HashMap<>();
    private final Map<YCComodidad, ToggleButton> ycBotonesComodidad = new EnumMap<>(YCComodidad.class);

    private Long ycIdEditando;
    private String ycFotoOriginal;
    private String ycFoto360Original;
    private Path ycFotoNueva;
    private boolean ycFotoQuitada;
    private final List<Object> ycGaleriaForm = new java.util.ArrayList<>();
    private List<String> ycGaleriaOriginal = List.of();
    private Set<YCComodidad> ycComodidadesOcultas = EnumSet.noneOf(YCComodidad.class);

    @FXML
    public void initialize() {
        ycSesion.ycSoloAdmin(ycBtnNuevo);
        ycConfigurarTabla();
        ycCrearBotonesComodidad();

        ycTxtBuscar.textProperty().addListener((ycObs, ycA, ycB) -> ycAplicarFiltros());
        ycGrupoEstado.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual) -> {
            if (ycActual == null) {
                ycGrupoEstado.selectToggle(ycAnterior);
                return;
            }
            ycAplicarFiltros();
        });
        ycGrupoVista.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual) -> {
            if (ycActual == null) {
                ycGrupoVista.selectToggle(ycAnterior);
                return;
            }
            ycCambiarVista();
        });

        for (Button ycB : List.of(ycBtnEditar, ycBtnEliminar)) {
            ycB.visibleProperty().bind(ycVistaLista.selectedProperty().and(ycSesion.ycAdminProperty()));
            ycB.managedProperty().bind(ycVistaLista.selectedProperty().and(ycSesion.ycAdminProperty()));
        }

        for (ToggleGroup ycG : List.of(ycGrupoEstado, ycGrupoVista)) {
            ycG.getToggles().forEach(ycT -> ((Region) ycT).setMinWidth(Region.USE_PREF_SIZE));
        }
        ycTxtBuscar.setMinWidth(150);
        YCFormularios.ycSoloDigitos(ycTxtPiso, 2);
        YCFormularios.ycSoloDigitos(ycTxtCapacidad, 2);
        YCFormularios.ycSoloDecimal(ycTxtPrecio);
        YCFormularios.ycLimpiarAlEditar(ycTxtNumero, ycTxtPiso, ycTxtCapacidad, ycTxtDescripcion, ycTxtPrecio);
        YCFotos.ycRedondear(ycPreviewFoto, YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO, 14);
        Rectangle ycRecorteHero = new Rectangle();
        ycRecorteHero.setArcWidth(36);
        ycRecorteHero.setArcHeight(36);
        ycRecorteHero.widthProperty().bind(ycHero.widthProperty());
        ycRecorteHero.heightProperty().bind(ycHero.heightProperty());
        ycHero.setClip(ycRecorteHero);
        ycPreviewFoto.setMinSize(YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO);
        ycPreviewFoto.setMaxSize(YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO);
    }

    @Override
    public void ycRefrescar() {
        ycCargarDatos();
    }

    @FXML
    private void ycOnNuevo() {
        ycTabla.getSelectionModel().clearSelection();
        ycLimpiarFormulario();
        ycIdEditando = null;
        ycLblTituloFormulario.setText("Nuevo cuarto");
        ycLblSubtituloFormulario.setText("Completa los datos del cuarto");
        ycMostrarPreview();
        ycAbrirFormulario();
    }

    @FXML
    private void ycOnEditar() {
        ycEditar(ycTabla.getSelectionModel().getSelectedItem());
    }

    private void ycEditar(YCCuarto ycC) {
        if (ycC == null || !ycSesion.ycAdminProperty().get()) {
            return;
        }
        ycLimpiarFormulario();
        ycIdEditando = ycC.getYcId();
        ycFotoOriginal = ycC.getYcFoto();
        ycFoto360Original = ycC.getYcFoto360();
        ycTxtNumero.setText(ycC.getYcNumero());
        ycTxtPiso.setText(ycC.getYcPiso() == null ? "" : String.valueOf(ycC.getYcPiso()));
        ycTxtCapacidad.setText(ycC.getYcCapacidad() == null ? "" : String.valueOf(ycC.getYcCapacidad()));
        ycTxtDescripcion.setText(ycC.getYcDescripcion() == null ? "" : ycC.getYcDescripcion());
        ycTxtPrecio.setText(ycC.getYcPrecio().toPlainString());
        ycC.ycGetComodidades().stream().map(ycBotonesComodidad::get).filter(Objects::nonNull)
                .forEach(ycB -> ycB.setSelected(true));
        ycComodidadesOcultas = EnumSet.noneOf(YCComodidad.class);
        ycC.ycGetComodidades().stream().filter(ycCom -> !ycBotonesComodidad.containsKey(ycCom))
                .forEach(ycComodidadesOcultas::add);
        ycGaleriaOriginal = ycC.ycGetGaleria();
        ycGaleriaForm.addAll(ycGaleriaOriginal);
        ycConstruirMiniaturas();
        ycLblTituloFormulario.setText("Editar cuarto");
        ycLblSubtituloFormulario.setText("Registro #" + ycC.getYcId() + " · " + ycC.getYcNumero());
        ycMostrarPreview();
        ycAbrirFormulario();
    }

    @FXML
    private void ycOnElegirFoto() {
        FileChooser ycSelector = new FileChooser();
        ycSelector.setTitle("Elegir foto del cuarto");
        ycSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp",
                "*.JPG", "*.JPEG", "*.PNG"));
        File ycImagenes = new File(System.getProperty("user.home"), "Pictures");
        if (ycImagenes.isDirectory()) {
            ycSelector.setInitialDirectory(ycImagenes);
        }
        File ycElegido = ycSelector.showOpenDialog(ycPanelFormulario.getScene().getWindow());
        if (ycElegido == null) {
            return;
        }
        ycFotoNueva = ycElegido.toPath();
        ycFotoQuitada = false;
        ycMostrarPreview();
    }

    @FXML
    private void ycOnQuitarFoto() {
        ycFotoNueva = null;
        ycFotoQuitada = true;
        ycMostrarPreview();
    }

    @FXML
    private void ycOnCerrarFormulario() {
        ycNotificador.ycCerrarPanel();
    }

    @FXML
    private void ycOnGuardar() {
        YCFormularios.ycLimpiarErrores(ycTxtNumero, ycTxtPiso, ycTxtCapacidad, ycTxtDescripcion, ycTxtPrecio);

        String ycFotoCopiada = null;
        List<String> ycGaleriaCopiadas = new java.util.ArrayList<>();
        boolean ycGuardadoOk = false;
        try {
            List<String> ycGaleriaFinal = new java.util.ArrayList<>();
            for (Object ycItem : ycGaleriaForm) {
                if (ycItem instanceof Path ycRuta) {
                    String ycNombre = ycFotoService.ycGuardar(ycRuta);
                    ycGaleriaCopiadas.add(ycNombre);
                    ycGaleriaFinal.add(ycNombre);
                } else {
                    ycGaleriaFinal.add((String) ycItem);
                }
            }
            String ycFotoFinal = ycFotoQuitada ? null : ycFotoOriginal;
            if (ycFotoNueva != null) {
                ycFotoCopiada = ycFotoService.ycGuardar(ycFotoNueva);
                ycFotoFinal = ycFotoCopiada;
            }

            Set<YCComodidad> ycComodidades = EnumSet.noneOf(YCComodidad.class);
            ycComodidades.addAll(ycComodidadesOcultas);
            ycBotonesComodidad.forEach((ycCom, ycBoton) -> {
                if (ycBoton.isSelected()) {
                    ycComodidades.add(ycCom);
                }
            });

            YCCuarto ycCuarto = YCCuarto.builder()
                    .ycId(ycIdEditando)
                    .ycNumero(ycTxtNumero.getText().trim())
                    .ycPiso(ycEntero(ycTxtPiso))
                    .ycCapacidad(ycEntero(ycTxtCapacidad))
                    .ycDescripcion(ycTxtDescripcion.getText().isBlank() ? null : ycTxtDescripcion.getText().trim())
                    .ycPrecio(YCControles.ycImporte(ycTxtPrecio.getText()))
                    .ycModalidad(YCModalidad.YC_MENSUAL)
                    .ycFoto(ycFotoFinal)
                    .ycFoto360(ycIdEditando == null ? null : ycFoto360Original)
                    .ycComodidades(ycComodidades)
                    .ycFotosExtra(ycGaleriaFinal.isEmpty() ? null : String.join(",", ycGaleriaFinal))
                    .build();

            boolean ycEsNuevo = ycCuarto.getYcId() == null;
            YCCuarto ycGuardado = ycService.ycGuardar(ycCuarto);
            ycGuardadoOk = true;

            if (!Objects.equals(ycFotoOriginal, ycFotoFinal)) {
                ycFotos.ycOlvidar(ycFotoOriginal);
                ycFotoService.ycEliminar(ycFotoOriginal);
            }
            for (String ycQuitada : ycGaleriaOriginal) {
                if (!ycGaleriaFinal.contains(ycQuitada)) {
                    ycFotos.ycOlvidar(ycQuitada);
                    ycFotoService.ycEliminar(ycQuitada);
                }
            }
            ycNotificador.ycCerrarPanel();
            ycNotificador.ycExito(ycEsNuevo
                    ? ycGuardado.getYcNumero() + " se registro correctamente."
                    : "Los cambios de " + ycGuardado.getYcNumero() + " se guardaron.");
            ycCargarDatos();
        } catch (ConstraintViolationException ycE) {
            List<String> ycOtros = YCFormularios.ycMostrarViolaciones(ycE, Map.of(
                    "ycNumero", ycTxtNumero, "ycPiso", ycTxtPiso, "ycCapacidad", ycTxtCapacidad,
                    "ycDescripcion", ycTxtDescripcion, "ycPrecio", ycTxtPrecio));
            ycOtros.forEach(ycNotificador::ycAdvertencia);
        } catch (YCNegocioException ycE) {
            if (ycFotoNueva != null && ycFotoCopiada == null) {
                ycNotificador.ycError(ycE.getMessage());
            } else {
                YCFormularios.ycMarcarError(ycTxtNumero, ycE.getMessage());
            }
        } catch (Exception ycE) {
            ycNotificador.ycError("Error inesperado: " + ycE.getMessage());
        } finally {
            if (!ycGuardadoOk && ycFotoCopiada != null) {
                ycFotoService.ycEliminar(ycFotoCopiada);
            }
            if (!ycGuardadoOk) {
                ycGaleriaCopiadas.forEach(ycFotoService::ycEliminar);
            }
        }
    }

    @FXML
    private void ycOnEliminar() {
        ycEliminar(ycTabla.getSelectionModel().getSelectedItem());
    }

    private void ycEliminar(YCCuarto ycSeleccionado) {
        if (ycSeleccionado == null) {
            return;
        }
        ycNotificador.ycConfirmar("¿Eliminar el " + ycSeleccionado.getYcNumero() + "?",
                "Tambien se borraran su foto y sus registros de luz y agua. Esta accion no se puede deshacer.",
                "Eliminar",
                () -> {
                    try {
                        ycService.ycEliminar(ycSeleccionado.getYcId());
                        ycFotos.ycOlvidar(ycSeleccionado.getYcFoto());
                        ycNotificador.ycExito("Cuarto eliminado.");
                        ycCargarDatos();
                    } catch (YCNegocioException ycE) {
                        ycNotificador.ycError(ycE.getMessage());
                    }
                });
    }

    private void ycCambiarVista() {
        boolean ycLista = ycVistaLista.isSelected();
        ycPanelLista.setVisible(ycLista);
        ycPanelLista.setManaged(ycLista);
        ycScrollGaleria.setVisible(!ycLista);
        ycScrollGaleria.setManaged(!ycLista);
        Animations.fadeIn(ycLista ? ycPanelLista : ycScrollGaleria, Duration.millis(220)).play();
    }

    private void ycAplicarFiltros() {
        String ycT = ycTxtBuscar.getText() == null ? "" : ycTxtBuscar.getText().trim().toLowerCase();
        Toggle ycEstado = ycGrupoEstado.getSelectedToggle();
        ycFiltrados.setPredicate(ycC -> {
            boolean ycOcupado = ycOcupado(ycC);
            if (ycEstado == ycFiltroLibres && ycOcupado || ycEstado == ycFiltroOcupados && !ycOcupado) {
                return false;
            }
            return ycT.isEmpty()
                    || ycC.getYcNumero().toLowerCase().contains(ycT)
                    || (ycC.getYcDescripcion() != null && ycC.getYcDescripcion().toLowerCase().contains(ycT))
                    || ycC.ycGetComodidades().stream().anyMatch(ycCom -> ycCom.ycGetTexto().toLowerCase().contains(ycT));
        });
        ycConstruirGaleria();
    }

    private void ycConstruirGaleria() {
        ycGaleria.getChildren().clear();
        if (ycFiltrados.isEmpty()) {
            ycGaleria.getChildren().add(ycGaleriaVacia());
            return;
        }
        int ycI = 0;
        for (YCCuarto ycC : ycFiltrados) {
            Node ycTarjeta = ycTarjeta(ycC);
            ycGaleria.getChildren().add(ycTarjeta);
            var ycAnimacion = Animations.fadeInUp(ycTarjeta, Duration.millis(320));
            ycAnimacion.setDelay(Duration.millis(Math.min(ycI++, 10) * 35));
            ycAnimacion.play();
        }
    }

    private Node ycTarjeta(YCCuarto ycC) {
        YCInquilino ycOcupante = ycOcupantes.get(ycC.getYcId());
        YCEstadia ycHuesped = ycHuespedes.get(ycC.getYcId());
        boolean ycOcupado = ycOcupante != null || ycHuesped != null;

        StackPane ycPortada = new StackPane();
        ycPortada.getStyleClass().add("cuarto-portada");
        ycPortada.setMinSize(YC_TARJETA_ANCHO, YC_PORTADA_ALTO);
        ycPortada.setMaxSize(YC_TARJETA_ANCHO, YC_PORTADA_ALTO);
        Optional<Image> ycImagen = ycFotos.ycCargar(ycC.getYcFoto());
        Node ycFondoPortada = ycImagen
                .<Node>map(ycImg -> YCFotos.ycCubrir(ycImg, YC_TARJETA_ANCHO, YC_PORTADA_ALTO))
                .orElseGet(() -> YCFotos.ycSinFoto(ycC.getYcNumero(), YC_TARJETA_ANCHO, YC_PORTADA_ALTO, true));
        Region ycVeloPortada = new Region();
        ycVeloPortada.getStyleClass().add("cuarto-portada-velo");
        ycPortada.getChildren().addAll(ycFondoPortada, ycVeloPortada);
        YCFotos.ycRedondearArriba(ycPortada, YC_TARJETA_ANCHO, YC_PORTADA_ALTO, 16);

        Label ycEstado = ycEtiquetaEstado(ycC, ycOcupado);
        StackPane.setAlignment(ycEstado, Pos.TOP_LEFT);
        StackPane.setMargin(ycEstado, new javafx.geometry.Insets(12));

        Label ycTipo = ycEtiquetaModalidad(ycC);
        StackPane.setAlignment(ycTipo, Pos.TOP_RIGHT);
        StackPane.setMargin(ycTipo, new javafx.geometry.Insets(12));

        Label ycPrecio = new Label("S/ " + ycC.getYcPrecio().setScale(0, RoundingMode.HALF_UP).toPlainString());
        ycPrecio.getStyleClass().add("cuarto-precio");
        Label ycMes = new Label(ycC.ycGetModalidad().ycGetSufijo());
        ycMes.getStyleClass().add("cuarto-precio-mes");
        HBox ycEtiquetaPrecio = new HBox(2, ycPrecio, ycMes);
        ycEtiquetaPrecio.getStyleClass().add("cuarto-precio-caja");
        ycEtiquetaPrecio.setAlignment(Pos.BASELINE_LEFT);
        ycEtiquetaPrecio.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycEtiquetaPrecio, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(ycEtiquetaPrecio, new javafx.geometry.Insets(12));
        ycPortada.getChildren().addAll(ycEstado, ycTipo, ycEtiquetaPrecio);
        if (!ycC.ycGetGaleria().isEmpty()) {
            Label ycNFotos = new Label(String.valueOf(ycC.ycGetGaleria().size() + (ycC.getYcFoto() == null ? 0 : 1)),
                    ycIcono("fth-camera"));
            ycNFotos.getStyleClass().add("contador-fotos");
            ycNFotos.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(ycNFotos, Pos.BOTTOM_LEFT);
            StackPane.setMargin(ycNFotos, new javafx.geometry.Insets(12));
            ycPortada.getChildren().add(ycNFotos);
        }

        Label ycTitulo = new Label(ycC.getYcNumero());
        ycTitulo.getStyleClass().add("cuarto-titulo");
        Region ycEspacio = new Region();
        HBox.setHgrow(ycEspacio, Priority.ALWAYS);
        HBox ycCabecera = new HBox(8, ycTitulo, ycEspacio);
        ycCabecera.setAlignment(Pos.CENTER_LEFT);
        if (ycC.getYcPiso() != null) {
            Label ycPiso = new Label("Piso " + ycC.getYcPiso(), ycIcono("fth-layers"));
            ycPiso.getStyleClass().add("cuarto-meta");
            ycCabecera.getChildren().add(ycPiso);
        }
        if (ycC.getYcCapacidad() != null) {
            Label ycCapacidad = new Label(ycC.getYcCapacidad() + (ycC.getYcCapacidad() == 1 ? " persona" : " personas"),
                    ycIcono("fth-users"));
            ycCapacidad.getStyleClass().add("cuarto-meta");
            ycCabecera.getChildren().add(ycCapacidad);
        }

        Label ycDescripcion = new Label(ycC.getYcDescripcion() == null ? "Sin descripcion" : ycC.getYcDescripcion());
        ycDescripcion.getStyleClass().add("cuarto-descripcion");
        if (ycC.getYcDescripcion() == null) {
            ycDescripcion.getStyleClass().add("vacia");
        }
        ycDescripcion.setWrapText(true);
        ycDescripcion.setMinHeight(36);
        ycDescripcion.setMaxHeight(36);

        FlowPane ycListaComodidades = new FlowPane(6, 6);
        ycListaComodidades.getStyleClass().add("cuarto-comodidades");
        ycListaComodidades.setMinHeight(26);
        List<YCComodidad> ycTodas = List.copyOf(ycC.ycGetComodidades());
        if (ycTodas.isEmpty()) {
            Label ycNinguna = new Label("Sin comodidades registradas");
            ycNinguna.getStyleClass().add("cuarto-comodidad-vacia");
            ycListaComodidades.getChildren().add(ycNinguna);
        }
        ycTodas.stream().limit(YC_COMODIDADES_EN_TARJETA).forEach(ycCom -> {
            Label ycChip = new Label(ycCom.ycGetTexto(), ycIcono(ycCom.ycGetIcono()));
            ycChip.getStyleClass().add("cuarto-comodidad");
            ycListaComodidades.getChildren().add(ycChip);
        });
        if (ycTodas.size() > YC_COMODIDADES_EN_TARJETA) {
            List<YCComodidad> ycResto = ycTodas.subList(YC_COMODIDADES_EN_TARJETA, ycTodas.size());
            Label ycMas = new Label("+" + ycResto.size());
            ycMas.getStyleClass().addAll("cuarto-comodidad", "mas");
            Tooltip ycAyuda = new Tooltip(ycResto.stream().map(YCComodidad::ycGetTexto).collect(Collectors.joining("\n")));
            ycAyuda.setShowDelay(Duration.millis(150));
            ycMas.setTooltip(ycAyuda);
            ycListaComodidades.getChildren().add(ycMas);
        }

        Label ycQuien;
        if (ycHuesped != null) {
            ycQuien = new Label(ycHuesped.getYcHuesped() + " · sale " + ycHuesped.getYcSalida().format(YC_DIA_MES),
                    ycIcono("fth-moon"));
        } else if (ycOcupante != null) {
            ycQuien = new Label(ycOcupante.ycGetNombreCompleto(), ycIcono("fth-user"));
        } else {
            ycQuien = new Label(ycC.ycIsHospedaje() ? "Libre esta noche" : "Listo para alquilar",
                    ycIcono("fth-check-circle"));
        }
        ycQuien.getStyleClass().addAll("cuarto-ocupante", ycOcupado ? "ocupado" : "libre");
        ycQuien.setMaxWidth(170);
        Region ycEspacioPie = new Region();
        HBox.setHgrow(ycEspacioPie, Priority.ALWAYS);
        Button ycEditar = ycBotonIcono(Feather.EDIT_2, "Editar");
        ycSesion.ycSoloAdmin(ycEditar);
        ycEditar.setOnAction(ycE -> ycEditar(ycC));
        Button ycBorrar = ycBotonIcono(Feather.TRASH_2, "Eliminar");
        ycBorrar.getStyleClass().add("danger");
        ycSesion.ycSoloAdmin(ycBorrar);
        ycBorrar.setOnAction(ycE -> ycEliminar(ycC));
        HBox ycPie = new HBox(4, ycQuien, ycEspacioPie, ycEditar, ycBorrar);
        if (!ycOcupado) {
            Button ycAlquilar = new Button("Alquilar", ycIcono("fth-key"));
            ycAlquilar.getStyleClass().addAll("accent", "boton-alquilar");
            ycAlquilar.setTooltip(new Tooltip("Registrar un inquilino en " + ycC.getYcNumero()));
            ycAlquilar.setOnAction(ycE -> ycAlquilar(ycC));
            ycPie.getChildren().add(2, ycAlquilar);
            ycQuien.setMaxWidth(110);
        }
        ycPie.getStyleClass().add("cuarto-pie");
        ycPie.setAlignment(Pos.CENTER_LEFT);

        VBox ycCuerpo = new VBox(10, ycCabecera, ycDescripcion, ycListaComodidades, ycPie);
        ycCuerpo.getStyleClass().add("cuarto-cuerpo");

        VBox ycTarjeta = new VBox(ycPortada, ycCuerpo);
        ycTarjeta.getStyleClass().add("cuarto-card");
        ycTarjeta.setPrefWidth(YC_TARJETA_ANCHO);
        ycTarjeta.setMaxWidth(YC_TARJETA_ANCHO);
        ycTarjeta.setOnMouseClicked(ycE -> {
            for (Node ycN = (Node) ycE.getTarget(); ycN != null && ycN != ycTarjeta; ycN = ycN.getParent()) {
                if (ycN instanceof ButtonBase) {
                    return;
                }
            }
            ycMostrarFicha(ycC);
        });
        ScaleTransition ycZoom = new ScaleTransition(Duration.millis(380), ycFondoPortada);
        ycZoom.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        ycTarjeta.hoverProperty().addListener((ycObs, ycA, ycEncima) -> {
            ycZoom.stop();
            ycZoom.setToX(ycEncima ? 1.07 : 1.0);
            ycZoom.setToY(ycEncima ? 1.07 : 1.0);
            ycZoom.play();
        });
        Tooltip.install(ycPortada, new Tooltip("Ver detalles"));
        return ycTarjeta;
    }

    private Node ycGaleriaVacia() {
        FontIcon ycIcono = new FontIcon("fth-home");
        ycIcono.getStyleClass().add("vacio-icono");
        boolean ycSinCuartos = ycDatos.isEmpty();
        Label ycTitulo = new Label(ycSinCuartos ? "Aun no hay cuartos" : "Ningun cuarto coincide");
        ycTitulo.getStyleClass().add("vacio-titulo");
        Label ycTexto = new Label(ycSinCuartos
                ? "Registra el primer cuarto con el boton \"Nuevo cuarto\" y agregale una foto."
                : "Prueba con otra busqueda o cambia el filtro.");
        ycTexto.getStyleClass().add("vacio-texto");
        VBox ycCaja = new VBox(8, ycIcono, ycTitulo, ycTexto);
        ycCaja.setAlignment(Pos.CENTER);
        ycCaja.getStyleClass().add("galeria-vacia");
        ycCaja.prefWidthProperty().bind(ycScrollGaleria.widthProperty().subtract(60));
        return ycCaja;
    }

    private void ycMostrarFicha(YCCuarto ycC) {
        YCInquilino ycOcupante = ycOcupantes.get(ycC.getYcId());
        YCEstadia ycHuesped = ycHuespedes.get(ycC.getYcId());
        boolean ycOcupado = ycOcupante != null || ycHuesped != null;
        double ycAncho = 780;
        double ycAlto = 340;

        StackPane ycFoto = new StackPane();
        ycFoto.setMinSize(ycAncho, ycAlto);
        ycFoto.setMaxSize(ycAncho, ycAlto);
        com.alquilafacil.ui.YCFotoFicha ycCabecera = new com.alquilafacil.ui.YCFotoFicha(ycFoto, ycAncho, ycAlto,
                ycFotoService.ycRuta(ycC.getYcFoto360()).orElse(null), ycC.getYcNumero());
        ycFoto.getChildren().add(ycCabecera.ycNodo());
        YCFotos.ycRedondearArriba(ycFoto, ycAncho, ycAlto, 18);
        Region ycVelo = new Region();
        ycVelo.getStyleClass().add("ficha-velo");
        ycFoto.getChildren().add(ycVelo);

        Label ycEstado = ycEtiquetaEstado(ycC, ycOcupado);
        Label ycTipoFicha = ycEtiquetaModalidad(ycC);
        HBox ycEtiquetas = new HBox(8, ycEstado, ycTipoFicha);
        ycEtiquetas.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycEtiquetas, Pos.TOP_LEFT);
        StackPane.setMargin(ycEtiquetas, new Insets(18));

        Button ycCerrar = new Button(null, new FontIcon(Feather.X));
        ycCerrar.getStyleClass().addAll("button-circle", "ficha-cerrar");
        ycCerrar.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        StackPane.setAlignment(ycCerrar, Pos.TOP_RIGHT);
        StackPane.setMargin(ycCerrar, new Insets(14));

        Label ycTitulo = new Label(ycC.getYcNumero());
        ycTitulo.getStyleClass().add("ficha-titulo");
        StringBuilder ycMeta = new StringBuilder(ycC.ycGetModalidad().ycGetTexto());
        if (ycC.getYcPiso() != null) {
            ycMeta.append("  ·  Piso ").append(ycC.getYcPiso());
        }
        if (ycC.getYcCapacidad() != null) {
            ycMeta.append(ycMeta.isEmpty() ? "" : "  ·  ").append("Para ").append(ycC.getYcCapacidad())
                    .append(ycC.getYcCapacidad() == 1 ? " persona" : " personas");
        }
        int ycCantidad = ycC.ycGetComodidades().size();
        ycMeta.append(ycMeta.isEmpty() ? "" : "  ·  ").append(ycCantidad)
                .append(ycCantidad == 1 ? " comodidad" : " comodidades");
        Label ycSubtitulo = new Label(ycMeta.toString());
        ycSubtitulo.getStyleClass().add("ficha-subtitulo");
        VBox ycTextosFoto = new VBox(4, ycTitulo, ycSubtitulo);
        ycTextosFoto.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycTextosFoto, Pos.BOTTOM_LEFT);
        StackPane.setMargin(ycTextosFoto, new Insets(22));
        ycFoto.getChildren().addAll(ycEtiquetas, ycCerrar, ycTextosFoto);
        List<javafx.scene.image.Image> ycImagenes = new java.util.ArrayList<>();
        ycFotos.ycCargar(ycC.getYcFoto()).ifPresent(ycImagenes::add);
        ycC.ycGetGaleria().forEach(ycN -> ycFotos.ycCargar(ycN).ifPresent(ycImagenes::add));
        ycCabecera.ycEmpezar(ycImagenes.isEmpty() ? YCFotos.ycSinFoto(ycC.getYcNumero(), ycAncho, ycAlto, true)
                : YCFotos.ycCubrir(ycImagenes.get(0), ycAncho, ycAlto), ycImagenes);

        Label ycTituloDescripcion = new Label("Sobre este cuarto");
        ycTituloDescripcion.getStyleClass().add("ficha-seccion");
        Label ycDescripcion = new Label(ycC.getYcDescripcion() == null ? "Aun no tiene descripcion." : ycC.getYcDescripcion());
        ycDescripcion.getStyleClass().add("ficha-texto");
        ycDescripcion.setWrapText(true);

        Label ycTituloIncluye = new Label("Que incluye");
        ycTituloIncluye.getStyleClass().add("ficha-seccion");
        VBox.setMargin(ycTituloIncluye, new Insets(10, 0, 0, 0));
        VBox ycIzquierda = new VBox(10, ycTituloDescripcion, ycDescripcion, ycTituloIncluye);
        if (ycC.ycGetComodidades().isEmpty()) {
            Label ycNada = new Label("No se registraron comodidades. Puedes marcarlas al editar el cuarto.");
            ycNada.getStyleClass().add("ficha-texto");
            ycNada.setWrapText(true);
            ycIzquierda.getChildren().add(ycNada);
        } else {
            GridPane ycIncluye = new GridPane();
            ycIncluye.setHgap(18);
            ycIncluye.setVgap(12);
            int ycI = 0;
            for (YCComodidad ycCom : ycC.ycGetComodidades()) {
                StackPane ycCirculo = new StackPane(new FontIcon(ycCom.ycGetIcono()));
                ycCirculo.getStyleClass().add("ficha-comodidad-icono");
                Label ycTexto = new Label(ycCom.ycGetTexto());
                ycTexto.getStyleClass().add("ficha-comodidad-texto");
                HBox ycFila = new HBox(10, ycCirculo, ycTexto);
                ycFila.setAlignment(Pos.CENTER_LEFT);
                ycFila.setPrefWidth(210);
                ycIncluye.add(ycFila, ycI % 2, ycI / 2);
                ycI++;
            }
            ycIzquierda.getChildren().add(ycIncluye);
        }
        HBox.setHgrow(ycIzquierda, Priority.ALWAYS);

        Label ycEtiquetaPrecio = new Label(ycC.ycIsHospedaje() ? "PRECIO POR NOCHE" : "PRECIO MENSUAL");
        ycEtiquetaPrecio.getStyleClass().add("ficha-lateral-etiqueta");
        Label ycPrecio = new Label(YCControles.ycMoneda(ycC.getYcPrecio()));
        ycPrecio.getStyleClass().add("ficha-precio");
        Label ycNotaPrecio = new Label(ycC.ycIsHospedaje()
                ? "Se cobra al registrar la estadia: noches x precio."
                : "Luz y agua se cobran aparte cada mes.");
        ycNotaPrecio.getStyleClass().add("ficha-lateral-nota");
        ycNotaPrecio.setWrapText(true);

        VBox ycOcupacion = new VBox(5);
        ycOcupacion.getStyleClass().add("ficha-ocupante");
        if (ycHuesped != null) {
            Label ycVive = new Label("HOSPEDADO HOY");
            ycVive.getStyleClass().add("ficha-lateral-etiqueta");
            Label ycNombre = new Label(ycHuesped.getYcHuesped(), new FontIcon(Feather.MOON));
            ycNombre.getStyleClass().add("ficha-ocupante-nombre");
            ycNombre.setWrapText(true);
            Label ycFechas = new Label("Del " + ycHuesped.getYcEntrada().format(YC_DIA_MES) + " al "
                    + ycHuesped.getYcSalida().format(YC_DIA_MES) + " · " + ycHuesped.getYcNoches()
                    + (ycHuesped.getYcNoches() == 1 ? " noche" : " noches"), new FontIcon(Feather.CALENDAR));
            ycFechas.getStyleClass().add("ficha-lateral-nota");
            ycOcupacion.getChildren().addAll(ycVive, ycNombre, ycFechas);
            if (ycHuesped.getYcTelefono() != null) {
                Label ycTel = new Label(ycHuesped.getYcTelefono(), new FontIcon(Feather.PHONE));
                ycTel.getStyleClass().add("ficha-lateral-nota");
                ycOcupacion.getChildren().add(ycTel);
            }
        } else if (ycOcupante != null) {
            Label ycVive = new Label("VIVE AQUI");
            ycVive.getStyleClass().add("ficha-lateral-etiqueta");
            Label ycNombre = new Label(ycOcupante.ycGetNombreCompleto(), new FontIcon(Feather.USER));
            ycNombre.getStyleClass().add("ficha-ocupante-nombre");
            ycNombre.setWrapText(true);
            ycOcupacion.getChildren().addAll(ycVive, ycNombre);
            if (ycOcupante.getYcTelefono() != null && !ycOcupante.getYcTelefono().isBlank()) {
                Label ycTel = new Label(ycOcupante.getYcTelefono(), new FontIcon(Feather.PHONE));
                ycTel.getStyleClass().add("ficha-lateral-nota");
                ycOcupacion.getChildren().add(ycTel);
            }
            Label ycDesde = new Label("Desde el " + YCControles.ycFecha(ycOcupante.getYcFechaIngreso())
                    + " · paga el dia " + ycOcupante.getYcDiaPago(), new FontIcon(Feather.CALENDAR));
            ycDesde.getStyleClass().add("ficha-lateral-nota");
            ycDesde.setWrapText(true);
            ycOcupacion.getChildren().add(ycDesde);
        } else {
            ycOcupacion.getStyleClass().add("libre");
            Label ycLibre = new Label(ycC.ycIsHospedaje() ? "Libre esta noche" : "Disponible ahora",
                    new FontIcon(Feather.CHECK_CIRCLE));
            ycLibre.getStyleClass().add("ficha-ocupante-nombre");
            Label ycTexto = new Label(ycC.ycIsHospedaje()
                    ? "Puedes registrar un huesped desde hoy."
                    : "Listo para recibir a un nuevo inquilino. Usa \"Alquilar este cuarto\".");
            ycTexto.getStyleClass().add("ficha-lateral-nota");
            ycTexto.setWrapText(true);
            ycOcupacion.getChildren().addAll(ycLibre, ycTexto);
        }

        VBox ycReservas = null;
        if (ycC.ycIsHospedaje()) {
            List<YCEstadia> ycProximas = ycEstadiaService.ycVigentesDe(ycC.getYcId()).stream()
                    .filter(ycE -> ycHuesped == null || !ycE.getYcId().equals(ycHuesped.getYcId()))
                    .limit(3)
                    .toList();
            if (!ycProximas.isEmpty()) {
                ycReservas = new VBox(4);
                Label ycTituloReservas = new Label("PROXIMAS RESERVAS");
                ycTituloReservas.getStyleClass().add("ficha-lateral-etiqueta");
                ycReservas.getChildren().add(ycTituloReservas);
                for (YCEstadia ycE : ycProximas) {
                    Label ycR = new Label(ycE.getYcEntrada().format(YC_DIA_MES) + " → " + ycE.getYcSalida().format(YC_DIA_MES)
                            + "  " + ycE.getYcHuesped(), new FontIcon(Feather.BOOKMARK));
                    ycR.getStyleClass().add("ficha-lateral-nota");
                    ycReservas.getChildren().add(ycR);
                }
                VBox.setMargin(ycReservas, new Insets(6, 0, 0, 0));
            }
        }

        Button ycEditarCuarto = new Button("Editar cuarto", new FontIcon(Feather.EDIT_2));
        ycSesion.ycSoloAdmin(ycEditarCuarto);
        ycEditarCuarto.getStyleClass().add("accent");
        ycEditarCuarto.setMaxWidth(Double.MAX_VALUE);
        ycEditarCuarto.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            ycEditar(ycC);
        });
        Button ycCerrarFicha = new Button("Cerrar");
        ycCerrarFicha.getStyleClass().add("button-outlined");
        ycCerrarFicha.setMaxWidth(Double.MAX_VALUE);
        ycCerrarFicha.setCancelButton(true);
        ycCerrarFicha.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());

        Region ycEmpuje = new Region();
        VBox.setVgrow(ycEmpuje, Priority.ALWAYS);
        VBox.setMargin(ycOcupacion, new Insets(8, 0, 0, 0));
        VBox ycDerecha = new VBox(8, ycEtiquetaPrecio, ycPrecio, ycNotaPrecio, ycOcupacion);
        if (ycReservas != null) {
            ycDerecha.getChildren().add(ycReservas);
        }
        ycDerecha.getChildren().add(ycEmpuje);
        if (!ycOcupado) {
            Button ycAlquilar = new Button("Alquilar este cuarto", new FontIcon(Feather.KEY));
            ycAlquilar.getStyleClass().add("accent");
            ycAlquilar.setMaxWidth(Double.MAX_VALUE);
            ycAlquilar.setOnAction(ycE -> {
                ycNotificador.ycCerrarDialogo();
                ycAlquilar(ycC);
            });
            ycEditarCuarto.getStyleClass().setAll("button", "button-outlined");
            ycDerecha.getChildren().add(ycAlquilar);
            Platform.runLater(ycAlquilar::requestFocus);
        } else if (ycOcupante != null) {
            Button ycVerInquilino = new Button("Ver inquilino", new FontIcon(Feather.USER));
            ycVerInquilino.getStyleClass().add("button-outlined");
            ycVerInquilino.setMaxWidth(Double.MAX_VALUE);
            ycVerInquilino.setOnAction(ycE -> {
                ycNotificador.ycCerrarDialogo();
                ycMainController.ycIrA("inquilino-view");
            });
            ycDerecha.getChildren().add(ycVerInquilino);
        }
        ycDerecha.getChildren().addAll(ycEditarCuarto, ycCerrarFicha);
        ycDerecha.getStyleClass().add("ficha-lateral");
        ycDerecha.setMinWidth(250);
        ycDerecha.setPrefWidth(250);

        HBox ycCuerpo = new HBox(26, ycIzquierda, ycDerecha);
        ycCuerpo.getStyleClass().add("ficha-cuerpo");

        VBox ycFicha = new VBox(ycFoto, ycCuerpo);
        ycFicha.getStyleClass().add("ficha");
        ycFicha.setPrefWidth(ycAncho);
        ycFicha.setMaxSize(ycAncho, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycFicha);
        Animations.zoomIn(ycFicha, Duration.millis(260)).play();
        if (ycOcupado) {
            Platform.runLater(ycEditarCuarto::requestFocus);
        }
    }

    private void ycActualizarBanner() {
        long ycLibres = ycDatos.stream().filter(ycC -> !ycOcupado(ycC)).count();
        int ycOcupacion = ycDatos.isEmpty() ? 0 : (int) Math.round((ycDatos.size() - ycLibres) * 100.0 / ycDatos.size());
        ycLblHeroDisponibles.setText(ycLibres == 1 ? "1 disponible" : ycLibres + " disponibles");
        ycLblHeroOcupacion.setText(ycOcupacion + "% ocupado");
        ycLblHeroDesde.setText(ycDatos.stream().map(YCCuarto::getYcPrecio).min(BigDecimal::compareTo)
                .map(ycP -> "Desde S/ " + ycP.setScale(0, RoundingMode.HALF_UP).toPlainString() + " al mes")
                .orElse("Sin precios aun"));
    }

    private void ycAlquilar(YCCuarto ycC) {
        ycMainController.ycIrA("inquilino-view");
        ((YCInquilinoController) ycMainController.ycControladorDe("inquilino-view")).ycNuevoPara(ycC);
    }

    private void ycConstruirMiniaturas() {
        ycPanelGaleria.getChildren().clear();
        for (Object ycItem : List.copyOf(ycGaleriaForm)) {
            Image ycImg = ycItem instanceof Path ycRuta ? YCFotos.ycDesdeArchivo(ycRuta) : ycFotos.ycCargar((String) ycItem).orElse(null);
            Node ycFondo = ycImg == null ? YCFotos.ycSinFoto("x", YC_MINI_ANCHO, YC_MINI_ALTO, false) : YCFotos.ycCubrir(ycImg, YC_MINI_ANCHO, YC_MINI_ALTO);
            Button ycQuitar = new Button(null, new FontIcon(Feather.X));
            ycQuitar.getStyleClass().addAll("button-circle", "mini-quitar");
            ycQuitar.setOnAction(ycE -> {
                ycGaleriaForm.remove(ycItem);
                ycConstruirMiniaturas();
            });
            StackPane.setAlignment(ycQuitar, Pos.TOP_RIGHT);
            StackPane.setMargin(ycQuitar, new Insets(4));
            StackPane ycMini = new StackPane(ycFondo, ycQuitar);
            ycMini.setMinSize(YC_MINI_ANCHO, YC_MINI_ALTO);
            ycMini.setMaxSize(YC_MINI_ANCHO, YC_MINI_ALTO);
            YCFotos.ycRedondear(ycMini, YC_MINI_ANCHO, YC_MINI_ALTO, 10);
            ycPanelGaleria.getChildren().add(ycMini);
        }
        if (ycGaleriaForm.size() < YC_MAX_FOTOS_EXTRA) {
            Button ycAgregar = new Button("Agregar", new FontIcon(Feather.PLUS));
            ycAgregar.getStyleClass().add("mini-agregar");
            ycAgregar.setContentDisplay(ContentDisplay.TOP);
            ycAgregar.setMinSize(YC_MINI_ANCHO, YC_MINI_ALTO);
            ycAgregar.setMaxSize(YC_MINI_ANCHO, YC_MINI_ALTO);
            ycAgregar.setOnAction(ycE -> ycAgregarFotos());
            ycPanelGaleria.getChildren().add(ycAgregar);
        }
    }

    private void ycAgregarFotos() {
        FileChooser ycSelector = new FileChooser();
        ycSelector.setTitle("Elegir fotos para la galeria (puedes elegir varias)");
        ycSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.JPG", "*.JPEG", "*.PNG"));
        File ycImagenes = new File(System.getProperty("user.home"), "Pictures");
        if (ycImagenes.isDirectory()) {
            ycSelector.setInitialDirectory(ycImagenes);
        }
        List<File> ycElegidas = ycSelector.showOpenMultipleDialog(ycPanelFormulario.getScene().getWindow());
        if (ycElegidas == null) {
            return;
        }
        int ycLibres = YC_MAX_FOTOS_EXTRA - ycGaleriaForm.size();
        ycElegidas.stream().limit(ycLibres).forEach(ycF -> ycGaleriaForm.add(ycF.toPath()));
        if (ycElegidas.size() > ycLibres) {
            ycNotificador.ycAdvertencia("Solo se agregaron " + ycLibres + " fotos: el maximo es " + YC_MAX_FOTOS_EXTRA + ".");
        }
        ycConstruirMiniaturas();
    }

    private void ycCrearBotonesComodidad() {
        for (YCComodidad ycCom : YCComodidad.values()) {
            if (!ycCom.ycIsMensual()) {
                continue;
            }
            ToggleButton ycBoton = new ToggleButton(ycCom.ycGetTexto(), ycIcono(ycCom.ycGetIcono()));
            ycBoton.getStyleClass().add("comodidad-toggle");
            ycBotonesComodidad.put(ycCom, ycBoton);
            ycPanelComodidades.getChildren().add(ycBoton);
        }
    }

    private void ycMostrarPreview() {
        Image ycImagen = null;
        if (ycFotoNueva != null) {
            ycImagen = YCFotos.ycDesdeArchivo(ycFotoNueva);
        } else if (!ycFotoQuitada) {
            ycImagen = ycFotos.ycCargar(ycFotoOriginal).orElse(null);
        }
        Node ycContenido = ycImagen == null
                ? YCFotos.ycSinFoto(ycTxtNumero.getText(), YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO, true)
                : YCFotos.ycCubrir(ycImagen, YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO);
        ycPreviewFoto.getChildren().setAll(ycContenido);
        ycBtnQuitarFoto.setDisable(ycImagen == null);
        Animations.fadeIn(ycContenido, Duration.millis(200)).play();
    }

    private void ycAbrirFormulario() {
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(ycTxtNumero::requestFocus);
    }

    private void ycLimpiarFormulario() {
        YCFormularios.ycLimpiarErrores(ycTxtNumero, ycTxtPiso, ycTxtCapacidad, ycTxtDescripcion, ycTxtPrecio);
        ycTxtNumero.clear();
        ycTxtPiso.clear();
        ycTxtCapacidad.clear();
        ycTxtDescripcion.clear();
        ycTxtPrecio.clear();
        ycBotonesComodidad.values().forEach(ycB -> ycB.setSelected(false));
        ycComodidadesOcultas = EnumSet.noneOf(YCComodidad.class);
        ycGaleriaForm.clear();
        ycGaleriaOriginal = List.of();
        ycConstruirMiniaturas();
        ycFotoOriginal = null;
        ycFoto360Original = null;
        ycFotoNueva = null;
        ycFotoQuitada = false;
    }

    private void ycConfigurarTabla() {
        ycColNumero.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcNumero()));
        ycColPiso.setCellValueFactory(ycC -> new SimpleStringProperty(
                ycC.getValue().getYcPiso() == null ? "" : String.valueOf(ycC.getValue().getYcPiso())));
        ycColPiso.setCellFactory(YCTablas.ycCeldaOpcional());
        ycColDescripcion.setCellValueFactory(ycC -> new SimpleStringProperty(ycC.getValue().getYcDescripcion()));
        ycColDescripcion.setCellFactory(YCTablas.ycCeldaOpcional());
        ycColPrecio.setCellValueFactory(ycC -> new SimpleObjectProperty<>(ycC.getValue().getYcPrecio()));
        ycColPrecio.setCellFactory(YCTablas.ycCeldaMoneda());
        ycColEstado.setCellValueFactory(ycC -> new SimpleStringProperty(ycOcupado(ycC.getValue()) ? "Ocupado" : "Libre"));
        ycColEstado.setCellFactory(YCTablas.ycCeldaChip(ycV -> "Libre".equals(ycV) ? "exito" : "neutro"));
        ycColInquilino.setCellValueFactory(ycC -> {
            YCInquilino ycI = ycOcupantes.get(ycC.getValue().getYcId());
            YCEstadia ycH = ycHuespedes.get(ycC.getValue().getYcId());
            return new SimpleStringProperty(ycI != null ? ycI.ycGetNombreCompleto()
                    : ycH != null ? ycH.getYcHuesped() + " (huesped)" : "");
        });
        ycColInquilino.setCellFactory(YCTablas.ycCeldaOpcional());

        YCTablas.ycConfigurar(ycTabla, Feather.KEY, "Aun no hay cuartos",
                "Registra el primer cuarto con el boton \"Nuevo cuarto\".", this::ycEditar);
        ycTabla.setItems(ycFiltrados);

        var ycSeleccion = ycTabla.getSelectionModel().selectedItemProperty();
        ycBtnEditar.disableProperty().bind(ycSeleccion.isNull());
        ycBtnEliminar.disableProperty().bind(ycSeleccion.isNull());
    }

    private void ycCargarDatos() {
        ycOcupantes.clear();
        ycInquilinoService.ycActivos().forEach(ycI -> ycOcupantes.put(ycI.getYcCuarto().getYcId(), ycI));
        ycHuespedes.clear();
        ycHuespedes.putAll(ycEstadiaService.ycEnCursoPorCuarto());
        ycDatos.setAll(ycService.ycMensuales());
        ycTabla.refresh();
        long ycLibres = ycDatos.stream().filter(ycC -> !ycOcupado(ycC)).count();
        ycLblResumen.setText(ycDatos.size() + (ycDatos.size() == 1 ? " cuarto registrado" : " cuartos registrados")
                + " · " + (ycDatos.size() - ycLibres) + " ocupados · " + ycLibres + " disponibles");
        ycFiltroLibres.setText("Disponibles (" + ycLibres + ")");
        ycFiltroOcupados.setText("Ocupados (" + (ycDatos.size() - ycLibres) + ")");
        ycActualizarBanner();
        ycAplicarFiltros();
    }

    private boolean ycOcupado(YCCuarto ycC) {
        return ycOcupantes.containsKey(ycC.getYcId()) || ycHuespedes.containsKey(ycC.getYcId());
    }

    private static Label ycEtiquetaEstado(YCCuarto ycC, boolean ycOcupado) {
        String ycTexto = ycC.ycIsHospedaje()
                ? (ycOcupado ? "Ocupada hoy" : "Libre hoy")
                : (ycOcupado ? "Ocupado" : "Disponible");
        Label ycEstado = new Label(ycTexto);
        ycEstado.getStyleClass().addAll("cuarto-estado", ycOcupado ? "ocupado" : "libre");
        return ycEstado;
    }

    private static Label ycEtiquetaModalidad(YCCuarto ycC) {
        Label ycTipo = new Label(ycC.ycGetModalidad().ycGetCorto(), new FontIcon(ycC.ycIsHospedaje() ? "fth-moon" : "fth-calendar"));
        ycTipo.getStyleClass().addAll("cuarto-modalidad", ycC.ycIsHospedaje() ? "noche" : "mes");
        ycTipo.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return ycTipo;
    }

    private static Integer ycEntero(TextField ycCampo) {
        return ycCampo.getText().isBlank() ? null : Integer.valueOf(ycCampo.getText().trim());
    }

    private static FontIcon ycIcono(String ycLiteral) {
        return new FontIcon(ycLiteral);
    }

    private static Button ycBotonIcono(Feather ycIcono, String ycAyuda) {
        Button ycBoton = new Button(null, new FontIcon(ycIcono));
        ycBoton.getStyleClass().addAll("flat", "button-icon", "button-circle", "cuarto-accion");
        Tooltip ycT = new Tooltip(ycAyuda);
        ycT.setShowDelay(Duration.millis(300));
        ycBoton.setTooltip(ycT);
        return ycBoton;
    }
}
