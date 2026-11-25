package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCComodidad;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCEstadoLimpieza;
import com.alquilafacil.model.YCModalidad;
import com.alquilafacil.model.YCNorma;
import com.alquilafacil.model.YCTipoHabitacion;
import com.alquilafacil.service.YCCuartoService;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.service.YCFotoService;
import com.alquilafacil.ui.YCFormularios;
import com.alquilafacil.ui.YCFotos;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCRefrescable;
import jakarta.validation.ConstraintViolationException;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class YCHabitacionController implements YCRefrescable {
    private static final double YC_TARJETA_ANCHO = 300;
    private static final double YC_PORTADA_ALTO = 180;
    private static final double YC_PREVIEW_ANCHO = 424;
    private static final double YC_PREVIEW_ALTO = 220;
    private static final int YC_COMODIDADES_EN_TARJETA = 3;
    private static final int YC_MAX_FOTOS_EXTRA = 8;
    private static final double YC_MINI_ANCHO = 96;
    private static final double YC_MINI_ALTO = 70;
    private static final DateTimeFormatter YC_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final YCCuartoService ycService;
    private final com.alquilafacil.service.YCSesion ycSesion;
    private final YCEstadiaService ycEstadiaService;
    private final YCFotoService ycFotoService;
    private final YCFotos ycFotos;
    private final YCNotificador ycNotificador;
    private final YCMainController ycMainController;
    private final YCFichaHabitacion ycFicha;

    @FXML private VBox ycPanelFormulario;
    @FXML private Label ycLblTituloFormulario;
    @FXML private Label ycLblSubtituloFormulario;
    @FXML private StackPane ycPreviewFoto;
    @FXML private Button ycBtnQuitarFoto;
    @FXML private TextField ycTxtNumero;
    @FXML private TextField ycTxtPiso;
    @FXML private TextField ycTxtMetros;
    @FXML private ComboBox<YCTipoHabitacion> ycCmbTipo;
    @FXML private Label ycLblTipoAyuda;
    @FXML private TextField ycTxtCapacidad;
    @FXML private TextField ycTxtCamasDobles;
    @FXML private TextField ycTxtCamasSimples;
    @FXML private TextArea ycTxtDescripcion;
    @FXML private TextField ycTxtPrecio;
    @FXML private TextField ycTxtHoraEntrada;
    @FXML private TextField ycTxtHoraSalida;
    @FXML private FlowPane ycPanelComodidades;
    @FXML private FlowPane ycPanelNormas;
    @FXML private FlowPane ycPanelGaleria;

    @FXML private StackPane ycHero;
    @FXML private Label ycLblResumen;
    @FXML private Label ycLblHeroLibres;
    @FXML private Label ycLblHeroOcupacion;
    @FXML private Label ycLblHeroDesde;
    @FXML private TextField ycTxtBuscar;
    @FXML private ToggleGroup ycGrupoEstado;
    @FXML private ToggleButton ycFiltroTodas;
    @FXML private ToggleButton ycFiltroLibres;
    @FXML private ToggleButton ycFiltroOcupadas;
    @FXML private ComboBox<YCTipoHabitacion> ycCmbFiltroTipo;
    @FXML private ScrollPane ycScrollGaleria;
    @FXML private Button ycBtnNuevo;
    @FXML private FlowPane ycGaleria;

    private final List<YCCuarto> ycDatos = new ArrayList<>();
    private final Map<Long, YCEstadia> ycHuespedes = new HashMap<>();
    private final Map<Long, YCEstadia> ycProximas = new HashMap<>();
    private final Map<Long, Integer> ycNochesMes = new HashMap<>();
    private final Map<YCComodidad, ToggleButton> ycBotonesComodidad = new EnumMap<>(YCComodidad.class);
    private final Map<YCNorma, ToggleButton> ycBotonesNorma = new EnumMap<>(YCNorma.class);

    private Long ycIdEditando;
    private String ycFotoOriginal;
    private Path ycFotoNueva;
    private boolean ycFotoQuitada;
    private String ycFoto360Original;
    private Path ycFoto360Nueva;
    private boolean ycFoto360Quitada;
    @FXML private Label ycLblFoto360;
    @FXML private Button ycBtnVerFoto360;
    @FXML private Button ycBtnQuitarFoto360;
    private Set<YCComodidad> ycComodidadesOcultas = EnumSet.noneOf(YCComodidad.class);
    private final List<Object> ycGaleriaForm = new ArrayList<>();
    private List<String> ycGaleriaOriginal = List.of();

    @FXML
    public void initialize() {
        ycSesion.ycSoloAdmin(ycBtnNuevo);
        for (YCComodidad ycCom : YCComodidad.values()) {
            if (ycCom.ycIsHospedaje()) {
                ToggleButton ycB = new ToggleButton(ycCom.ycGetTexto(), new FontIcon(ycCom.ycGetIcono()));
                ycB.getStyleClass().add("comodidad-toggle");
                ycBotonesComodidad.put(ycCom, ycB);
                ycPanelComodidades.getChildren().add(ycB);
            }
        }
        for (YCNorma ycN : YCNorma.values()) {
            ToggleButton ycB = new ToggleButton(ycN.ycGetTexto(), new FontIcon(ycN.ycGetIcono()));
            ycB.getStyleClass().addAll("comodidad-toggle", "norma-toggle");
            ycBotonesNorma.put(ycN, ycB);
            ycPanelNormas.getChildren().add(ycB);
        }
        ycBotonesNorma.get(YCNorma.YC_NO_MASCOTAS).selectedProperty().addListener((ycO, ycA, ycS) -> {
            if (ycS) ycBotonesNorma.get(YCNorma.YC_MASCOTAS).setSelected(false);
        });
        ycBotonesNorma.get(YCNorma.YC_MASCOTAS).selectedProperty().addListener((ycO, ycA, ycS) -> {
            if (ycS) ycBotonesNorma.get(YCNorma.YC_NO_MASCOTAS).setSelected(false);
        });

        ycCmbTipo.getItems().setAll(YCTipoHabitacion.values());
        ycCmbTipo.valueProperty().addListener((ycO, ycA, ycT) -> {
            YCFormularios.ycLimpiarError(ycCmbTipo);
            ycLblTipoAyuda.setText(ycT == null ? " " : ycT.ycGetDetalle());
            ycSugerirCamas(ycT);
        });

        ycCmbFiltroTipo.getItems().add(null);
        ycCmbFiltroTipo.getItems().addAll(YCTipoHabitacion.values());
        ycCmbFiltroTipo.setConverter(new StringConverter<>() {
            @Override
            public String toString(YCTipoHabitacion ycT) {
                return ycT == null ? "Todos los tipos" : ycT.ycGetTexto();
            }

            @Override
            public YCTipoHabitacion fromString(String ycS) {
                return null;
            }
        });
        ycCmbFiltroTipo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(YCTipoHabitacion ycT, boolean ycVacio) {
                super.updateItem(ycT, ycVacio);
                setText(ycT == null ? "Todos los tipos" : ycT.ycGetTexto());
            }
        });
        ycCmbFiltroTipo.valueProperty().addListener((ycO, ycA, ycB) -> ycConstruirGaleria());

        ycTxtBuscar.textProperty().addListener((ycO, ycA, ycB) -> ycConstruirGaleria());
        ycGrupoEstado.selectedToggleProperty().addListener((ycObs, ycAnterior, ycActual) -> {
            if (ycActual == null) {
                ycGrupoEstado.selectToggle(ycAnterior);
                return;
            }
            ycConstruirGaleria();
        });
        ycGrupoEstado.getToggles().forEach(ycT -> ((Region) ycT).setMinWidth(Region.USE_PREF_SIZE));
        ycTxtBuscar.setMinWidth(150);

        YCFormularios.ycSoloDigitos(ycTxtPiso, 2);
        YCFormularios.ycSoloDigitos(ycTxtMetros, 3);
        YCFormularios.ycSoloDigitos(ycTxtCapacidad, 2);
        YCFormularios.ycSoloDigitos(ycTxtCamasDobles, 2);
        YCFormularios.ycSoloDigitos(ycTxtCamasSimples, 2);
        YCFormularios.ycSoloDecimal(ycTxtPrecio);
        for (TextField ycHora : List.of(ycTxtHoraEntrada, ycTxtHoraSalida)) {
            ycHora.setTextFormatter(new TextFormatter<>(ycC -> ycC.getControlNewText().matches("[0-9:]{0,5}") ? ycC : null));
        }
        YCFormularios.ycLimpiarAlEditar(ycTxtNumero, ycTxtPiso, ycTxtMetros, ycTxtCapacidad, ycTxtCamasDobles, ycTxtCamasSimples,
                ycTxtDescripcion, ycTxtPrecio, ycTxtHoraEntrada, ycTxtHoraSalida);

        YCFotos.ycRedondear(ycPreviewFoto, YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO, 14);
        ycPreviewFoto.setMinSize(YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO);
        ycPreviewFoto.setMaxSize(YC_PREVIEW_ANCHO, YC_PREVIEW_ALTO);
        Rectangle ycRecorteHero = new Rectangle();
        ycRecorteHero.setArcWidth(36);
        ycRecorteHero.setArcHeight(36);
        ycRecorteHero.widthProperty().bind(ycHero.widthProperty());
        ycRecorteHero.heightProperty().bind(ycHero.heightProperty());
        ycHero.setClip(ycRecorteHero);
    }

    @Override
    public void ycRefrescar() {
        ycCargar();
    }

    @FXML
    private void ycOnNuevo() {
        ycLimpiarFormulario();
        ycIdEditando = null;
        ycLblTituloFormulario.setText("Nueva habitacion");
        ycLblSubtituloFormulario.setText("Como es la habitacion que ofreces por noche");
        ycTxtHoraEntrada.setText("14:00");
        ycTxtHoraSalida.setText("12:00");
        ycBotonesNorma.get(YCNorma.YC_NO_FUMAR).setSelected(true);
        ycBotonesNorma.get(YCNorma.YC_DOCUMENTO).setSelected(true);
        ycBotonesComodidad.get(YCComodidad.YC_TOALLAS).setSelected(true);
        ycMostrarPreview();
        ycAbrirFormulario();
    }

    @FXML
    private void ycOnVerEstadias() {
        ycMainController.ycIrA("hospedaje-view");
    }

    private void ycEditar(YCCuarto ycC) {
        ycLimpiarFormulario();
        ycIdEditando = ycC.getYcId();
        ycFotoOriginal = ycC.getYcFoto();
        ycFoto360Original = ycC.getYcFoto360();
        ycTxtNumero.setText(ycC.getYcNumero());
        ycTxtPiso.setText(ycTexto(ycC.getYcPiso()));
        ycTxtMetros.setText(ycTexto(ycC.getYcMetros()));
        ycTxtCapacidad.setText(ycTexto(ycC.getYcCapacidad()));
        ycTxtCamasDobles.setText(ycTexto(ycC.getYcCamasDobles()));
        ycTxtCamasSimples.setText(ycTexto(ycC.getYcCamasSimples()));
        ycCmbTipo.setValue(ycC.getYcTipoHabitacion());
        ycTxtDescripcion.setText(ycC.getYcDescripcion() == null ? "" : ycC.getYcDescripcion());
        ycTxtPrecio.setText(ycC.getYcPrecio().toPlainString());
        ycTxtHoraEntrada.setText(ycC.ycGetHoraEntradaODefecto());
        ycTxtHoraSalida.setText(ycC.ycGetHoraSalidaODefecto());
        for (YCComodidad ycCom : ycC.ycGetComodidades()) {
            ToggleButton ycB = ycBotonesComodidad.get(ycCom);
            if (ycB != null) {
                ycB.setSelected(true);
            } else {
                ycComodidadesOcultas.add(ycCom);
            }
        }
        ycC.ycGetNormas().forEach(ycN -> ycBotonesNorma.get(ycN).setSelected(true));
        ycGaleriaOriginal = ycC.ycGetGaleria();
        ycGaleriaForm.addAll(ycGaleriaOriginal);
        ycConstruirMiniaturas();
        ycMostrarFoto360();
        ycLblTituloFormulario.setText("Editar habitacion");
        ycLblSubtituloFormulario.setText("Registro #" + ycC.getYcId() + " · " + ycC.getYcNumero());
        ycMostrarPreview();
        ycAbrirFormulario();
    }

    @FXML
    private void ycOnElegirFoto() {
        FileChooser ycSelector = new FileChooser();
        ycSelector.setTitle("Elegir foto de la habitacion");
        ycSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.JPG", "*.JPEG", "*.PNG"));
        File ycImagenes = new File(System.getProperty("user.home"), "Pictures");
        if (ycImagenes.isDirectory()) {
            ycSelector.setInitialDirectory(ycImagenes);
        }
        File ycElegido = ycSelector.showOpenDialog(ycPanelFormulario.getScene().getWindow());
        if (ycElegido != null) {
            ycFotoNueva = ycElegido.toPath();
            ycFotoQuitada = false;
            ycMostrarPreview();
        }
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
        List<Control> ycCampos = List.of(ycTxtNumero, ycTxtPiso, ycTxtMetros, ycCmbTipo, ycTxtCapacidad, ycTxtCamasDobles,
                ycTxtCamasSimples, ycTxtDescripcion, ycTxtPrecio, ycTxtHoraEntrada, ycTxtHoraSalida);
        YCFormularios.ycLimpiarErrores(ycCampos.toArray(Control[]::new));
        if (ycCmbTipo.getValue() == null) {
            YCFormularios.ycMarcarError(ycCmbTipo, "Elige el tipo de habitacion.");
            return;
        }
        if (ycTxtCapacidad.getText().isBlank()) {
            YCFormularios.ycMarcarError(ycTxtCapacidad, "¿Cuantos huespedes?");
            return;
        }
        int ycCamas = ycValor(ycTxtCamasDobles) * 2 + ycValor(ycTxtCamasSimples);
        if (ycCamas > 0 && ycCamas < ycValor(ycTxtCapacidad)) {
            YCFormularios.ycMarcarError(ycTxtCapacidad, "Las camas son para " + ycCamas + (ycCamas == 1 ? " persona." : " personas."));
            return;
        }

        String ycFotoCopiada = null;
        String ycFoto360Copiada = null;
        List<String> ycGaleriaCopiadas = new ArrayList<>();
        boolean ycGuardadoOk = false;
        try {
            List<String> ycGaleriaFinal = new ArrayList<>();
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
            String ycFoto360Final = ycFoto360Quitada ? null : ycFoto360Original;
            if (ycFoto360Nueva != null) {
                ycFoto360Copiada = ycFotoService.ycGuardar(ycFoto360Nueva);
                ycFoto360Final = ycFoto360Copiada;
            }
            Set<YCComodidad> ycComodidades = EnumSet.noneOf(YCComodidad.class);
            ycComodidades.addAll(ycComodidadesOcultas);
            ycBotonesComodidad.forEach((ycCom, ycB) -> {
                if (ycB.isSelected()) ycComodidades.add(ycCom);
            });
            Set<YCNorma> ycNormas = EnumSet.noneOf(YCNorma.class);
            ycBotonesNorma.forEach((ycN, ycB) -> {
                if (ycB.isSelected()) ycNormas.add(ycN);
            });

            YCCuarto ycCuarto = YCCuarto.builder()
                    .ycId(ycIdEditando)
                    .ycNumero(ycTxtNumero.getText().trim())
                    .ycPiso(ycEntero(ycTxtPiso))
                    .ycMetros(ycEntero(ycTxtMetros))
                    .ycCapacidad(ycEntero(ycTxtCapacidad))
                    .ycTipoHabitacion(ycCmbTipo.getValue())
                    .ycCamasDobles(ycEntero(ycTxtCamasDobles))
                    .ycCamasSimples(ycEntero(ycTxtCamasSimples))
                    .ycDescripcion(ycTxtDescripcion.getText().isBlank() ? null : ycTxtDescripcion.getText().trim())
                    .ycPrecio(YCControles.ycImporte(ycTxtPrecio.getText()))
                    .ycHoraEntrada(ycTxtHoraEntrada.getText().trim())
                    .ycHoraSalida(ycTxtHoraSalida.getText().trim())
                    .ycModalidad(YCModalidad.YC_POR_NOCHE)
                    .ycFoto(ycFotoFinal)
                    .ycFoto360(ycFoto360Final)
                    .ycComodidades(ycComodidades)
                    .ycNormas(ycNormas)
                    .ycFotosExtra(ycGaleriaFinal.isEmpty() ? null : String.join(",", ycGaleriaFinal))
                    .build();
            boolean ycEsNuevo = ycCuarto.getYcId() == null;
            YCCuarto ycGuardado = ycService.ycGuardar(ycCuarto);
            ycGuardadoOk = true;
            if (!Objects.equals(ycFotoOriginal, ycFotoFinal)) {
                ycFotos.ycOlvidar(ycFotoOriginal);
                ycFotoService.ycEliminar(ycFotoOriginal);
            }
            if (!Objects.equals(ycFoto360Original, ycFoto360Final)) {
                ycFotoService.ycEliminar(ycFoto360Original);
            }
            for (String ycQuitada : ycGaleriaOriginal) {
                if (!ycGaleriaFinal.contains(ycQuitada)) {
                    ycFotos.ycOlvidar(ycQuitada);
                    ycFotoService.ycEliminar(ycQuitada);
                }
            }
            ycNotificador.ycCerrarPanel();
            ycNotificador.ycExito(ycEsNuevo
                    ? ycGuardado.getYcNumero() + " ya esta lista para recibir huespedes."
                    : "Los cambios de " + ycGuardado.getYcNumero() + " se guardaron.");
            ycCargar();
        } catch (ConstraintViolationException ycE) {
            YCFormularios.ycMostrarViolaciones(ycE, Map.of(
                    "ycNumero", ycTxtNumero, "ycPiso", ycTxtPiso, "ycCapacidad", ycTxtCapacidad, "ycMetros", ycTxtMetros,
                    "ycCamasDobles", ycTxtCamasDobles, "ycCamasSimples", ycTxtCamasSimples,
                    "ycDescripcion", ycTxtDescripcion, "ycPrecio", ycTxtPrecio,
                    "ycHoraEntrada", ycTxtHoraEntrada, "ycHoraSalida", ycTxtHoraSalida)).forEach(ycNotificador::ycAdvertencia);
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
            if (!ycGuardadoOk && ycFoto360Copiada != null) {
                ycFotoService.ycEliminar(ycFoto360Copiada);
            }
            if (!ycGuardadoOk) {
                ycGaleriaCopiadas.forEach(ycFotoService::ycEliminar);
            }
        }
    }

    private void ycEliminar(YCCuarto ycC) {
        ycNotificador.ycConfirmar("¿Eliminar la " + ycC.getYcNumero() + "?",
                "Tambien se borrara su foto. Esta accion no se puede deshacer.", "Eliminar", () -> {
                    try {
                        ycService.ycEliminar(ycC.getYcId());
                        ycFotos.ycOlvidar(ycC.getYcFoto());
                        ycNotificador.ycExito("Habitacion eliminada.");
                        ycCargar();
                    } catch (YCNegocioException ycE) {
                        ycNotificador.ycError(ycE.getMessage());
                    }
                });
    }

    private void ycNuevaEstadia(YCCuarto ycC) {
        ycMainController.ycIrA("hospedaje-view");
        ((YCHospedajeController) ycMainController.ycControladorDe("hospedaje-view")).ycNuevaEstadia(ycC);
    }

    private void ycCargar() {
        LocalDate ycHoy = LocalDate.now();
        ycDatos.clear();
        ycDatos.addAll(ycService.ycDeHospedaje());
        ycHuespedes.clear();
        ycHuespedes.putAll(ycEstadiaService.ycEnCursoPorCuarto());
        ycProximas.clear();
        for (YCEstadia ycE : ycEstadiaService.ycEntre(ycHoy.plusDays(1), ycHoy.plusYears(1))) {
            if (ycE.getYcEntrada().isAfter(ycHoy)) {
                ycProximas.putIfAbsent(ycE.getYcCuarto().getYcId(), ycE);
            }
        }
        YearMonth ycMes = YearMonth.from(ycHoy);
        LocalDate ycInicio = ycMes.atDay(1);
        LocalDate ycFin = ycMes.plusMonths(1).atDay(1);
        ycNochesMes.clear();
        for (YCEstadia ycE : ycEstadiaService.ycEntre(ycInicio, ycFin)) {
            LocalDate ycDesde = ycE.getYcEntrada().isBefore(ycInicio) ? ycInicio : ycE.getYcEntrada();
            LocalDate ycHasta = ycE.getYcSalida().isAfter(ycFin) ? ycFin : ycE.getYcSalida();
            ycNochesMes.merge(ycE.getYcCuarto().getYcId(), (int) ChronoUnit.DAYS.between(ycDesde, ycHasta), Integer::sum);
        }

        long ycLibres = ycDatos.stream().filter(ycC -> !ycHuespedes.containsKey(ycC.getYcId())).count();
        int ycNochesPosibles = ycDatos.size() * ycMes.lengthOfMonth();
        int ycOcupadas = ycNochesMes.values().stream().mapToInt(Integer::intValue).sum();
        int ycOcupacion = ycNochesPosibles == 0 ? 0 : (int) Math.round(ycOcupadas * 100.0 / ycNochesPosibles);
        ycLblResumen.setText(ycDatos.size() + (ycDatos.size() == 1 ? " habitacion" : " habitaciones") + " · "
                + ycHuespedes.size() + " ocupadas esta noche · " + ycProximas.size() + " con reserva proxima"
                + ycPorLimpiarTexto());
        ycLblHeroLibres.setText(ycLibres + (ycLibres == 1 ? " libre hoy" : " libres hoy"));
        ycLblHeroOcupacion.setText(ycOcupacion + "% ocupacion en " + YCControles.ycNombreMes(ycMes));
        ycLblHeroDesde.setText(ycDatos.stream().map(YCCuarto::getYcPrecio).min(BigDecimal::compareTo)
                .map(ycP -> "Desde S/ " + ycP.setScale(0, RoundingMode.HALF_UP).toPlainString() + " la noche")
                .orElse("Sin precios aun"));
        ycFiltroLibres.setText("Libres hoy (" + ycLibres + ")");
        ycFiltroOcupadas.setText("Ocupadas (" + (ycDatos.size() - ycLibres) + ")");
        ycConstruirGaleria();
    }

    private void ycConstruirGaleria() {
        String ycT = ycTxtBuscar.getText() == null ? "" : ycTxtBuscar.getText().trim().toLowerCase();
        Toggle ycEstado = ycGrupoEstado.getSelectedToggle();
        YCTipoHabitacion ycTipo = ycCmbFiltroTipo.getValue();
        List<YCCuarto> ycVisibles = ycDatos.stream().filter(ycC -> {
            boolean ycOcupada = ycHuespedes.containsKey(ycC.getYcId());
            if (ycEstado == ycFiltroLibres && ycOcupada || ycEstado == ycFiltroOcupadas && !ycOcupada) {
                return false;
            }
            if (ycTipo != null && ycC.getYcTipoHabitacion() != ycTipo) {
                return false;
            }
            return ycT.isEmpty()
                    || ycC.getYcNumero().toLowerCase().contains(ycT)
                    || (ycC.getYcDescripcion() != null && ycC.getYcDescripcion().toLowerCase().contains(ycT))
                    || (ycC.getYcTipoHabitacion() != null && ycC.getYcTipoHabitacion().ycGetTexto().toLowerCase().contains(ycT))
                    || ycC.ycGetComodidades().stream().anyMatch(ycCom -> ycCom.ycGetTexto().toLowerCase().contains(ycT));
        }).toList();

        ycGaleria.getChildren().clear();
        if (ycVisibles.isEmpty()) {
            ycGaleria.getChildren().add(ycVacio());
            return;
        }
        int ycI = 0;
        for (YCCuarto ycC : ycVisibles) {
            Node ycTarjeta = ycTarjeta(ycC);
            ycGaleria.getChildren().add(ycTarjeta);
            var ycAnim = Animations.fadeInUp(ycTarjeta, Duration.millis(320));
            ycAnim.setDelay(Duration.millis(Math.min(ycI++, 10) * 35));
            ycAnim.play();
        }
    }

    private Node ycVacio() {
        FontIcon ycIcono = new FontIcon("fth-moon");
        ycIcono.getStyleClass().add("vacio-icono");
        boolean ycSinNada = ycDatos.isEmpty();
        Label ycTitulo = new Label(ycSinNada ? "Aun no hay habitaciones de hospedaje" : "Ninguna habitacion coincide");
        ycTitulo.getStyleClass().add("vacio-titulo");
        Label ycTexto = new Label(ycSinNada
                ? "Registra la primera con \"Nueva habitacion\": tipo, camas, horarios y lo que incluye."
                : "Prueba con otra busqueda o cambia el filtro.");
        ycTexto.getStyleClass().add("vacio-texto");
        VBox ycCaja = new VBox(8, ycIcono, ycTitulo, ycTexto);
        ycCaja.setAlignment(Pos.CENTER);
        ycCaja.getStyleClass().add("galeria-vacia");
        ycCaja.prefWidthProperty().bind(ycScrollGaleria.widthProperty().subtract(60));
        return ycCaja;
    }

    private Node ycTarjeta(YCCuarto ycC) {
        YCEstadia ycHuesped = ycHuespedes.get(ycC.getYcId());
        YCEstadia ycProxima = ycProximas.get(ycC.getYcId());

        StackPane ycPortada = new StackPane();
        ycPortada.getStyleClass().add("cuarto-portada");
        ycPortada.setMinSize(YC_TARJETA_ANCHO, YC_PORTADA_ALTO);
        ycPortada.setMaxSize(YC_TARJETA_ANCHO, YC_PORTADA_ALTO);
        Node ycFondo = ycFotos.ycCargar(ycC.getYcFoto())
                .<Node>map(ycImg -> YCFotos.ycCubrir(ycImg, YC_TARJETA_ANCHO, YC_PORTADA_ALTO))
                .orElseGet(() -> YCFotos.ycSinFoto(ycC.getYcNumero(), YC_TARJETA_ANCHO, YC_PORTADA_ALTO, true));
        Region ycVelo = new Region();
        ycVelo.getStyleClass().add("cuarto-portada-velo");
        ycPortada.getChildren().addAll(ycFondo, ycVelo);
        YCFotos.ycRedondearArriba(ycPortada, YC_TARJETA_ANCHO, YC_PORTADA_ALTO, 16);

        Label ycEstado = new Label(ycHuesped == null ? "Libre hoy" : "Ocupada hoy");
        ycEstado.getStyleClass().addAll("cuarto-estado", ycHuesped == null ? "libre" : "ocupado");
        StackPane.setAlignment(ycEstado, Pos.TOP_LEFT);
        StackPane.setMargin(ycEstado, new Insets(12));

        Label ycTipo = new Label(ycC.getYcTipoHabitacion() == null ? "Hospedaje" : ycC.getYcTipoHabitacion().ycGetTexto(),
                new FontIcon("fth-moon"));
        ycTipo.getStyleClass().addAll("cuarto-modalidad", "noche");
        ycTipo.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycTipo, Pos.TOP_RIGHT);
        StackPane.setMargin(ycTipo, new Insets(12));
        if (!ycC.ycGetGaleria().isEmpty()) {
            Label ycNFotos = new Label(String.valueOf(ycC.ycGetGaleria().size() + (ycC.getYcFoto() == null ? 0 : 1)),
                    new FontIcon("fth-camera"));
            ycNFotos.getStyleClass().add("contador-fotos");
            ycNFotos.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(ycNFotos, Pos.BOTTOM_LEFT);
            StackPane.setMargin(ycNFotos, new Insets(12));
            ycPortada.getChildren().add(ycNFotos);
        }
        if (ycC.getYcFoto360() != null) {
            Label ycEn360 = new Label("360°", new FontIcon("fth-compass"));
            ycEn360.getStyleClass().add("contador-fotos");
            ycEn360.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            ycEn360.setTooltip(new Tooltip("Tiene foto 360°: abrela y presiona \"Ver en 360°\""));
            StackPane.setAlignment(ycEn360, Pos.BOTTOM_LEFT);
            StackPane.setMargin(ycEn360, new Insets(12, 12, 12, ycC.ycGetGaleria().isEmpty() ? 12 : 62));
            ycPortada.getChildren().add(ycEn360);
        }

        Label ycPrecio = new Label("S/ " + ycC.getYcPrecio().setScale(0, RoundingMode.HALF_UP).toPlainString());
        ycPrecio.getStyleClass().add("cuarto-precio");
        Label ycNoche = new Label("/noche");
        ycNoche.getStyleClass().add("cuarto-precio-mes");
        HBox ycCajaPrecio = new HBox(2, ycPrecio, ycNoche);
        ycCajaPrecio.getStyleClass().add("cuarto-precio-caja");
        ycCajaPrecio.setAlignment(Pos.BASELINE_LEFT);
        ycCajaPrecio.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycCajaPrecio, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(ycCajaPrecio, new Insets(12));
        ycPortada.getChildren().addAll(ycEstado, ycTipo, ycCajaPrecio);

        Label ycTitulo = new Label(ycC.getYcNumero());
        ycTitulo.getStyleClass().add("cuarto-titulo");
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        HBox ycCabecera = new HBox(8, ycTitulo, ycEsp, ycFicha.ycMenuLimpieza(ycC, this::ycCargar));
        ycCabecera.setAlignment(Pos.CENTER_LEFT);

        HBox ycSpecs = new HBox(6);
        ycSpecs.getStyleClass().add("hab-specs");
        if (ycC.getYcCapacidad() != null) {
            ycSpecs.getChildren().add(ycSpec("fth-users", ycC.getYcCapacidad() + (ycC.getYcCapacidad() == 1 ? " huesped" : " huespedes")));
        }
        if (ycC.ycGetCamasTexto() != null) {
            ycSpecs.getChildren().add(ycSpec("fth-moon", ycCamasCorto(ycC)));
        }
        if (ycC.getYcMetros() != null) {
            ycSpecs.getChildren().add(ycSpec("fth-maximize", ycC.getYcMetros() + " m²"));
        }
        if (ycC.getYcPiso() != null) {
            ycSpecs.getChildren().add(ycSpec("fth-layers", "Piso " + ycC.getYcPiso()));
        }

        Label ycHorario = new Label("Check-in " + ycC.ycGetHoraEntradaODefecto() + "  ·  Check-out " + ycC.ycGetHoraSalidaODefecto(),
                new FontIcon("fth-clock"));
        ycHorario.getStyleClass().add("hab-horario");

        FlowPane ycChips = new FlowPane(6, 6);
        ycChips.setMinHeight(26);
        List<YCComodidad> ycTodas = List.copyOf(ycC.ycGetComodidades());
        ycTodas.stream().limit(YC_COMODIDADES_EN_TARJETA).forEach(ycCom -> {
            Label ycChip = new Label(ycCom.ycGetTexto(), new FontIcon(ycCom.ycGetIcono()));
            ycChip.getStyleClass().addAll("cuarto-comodidad", "hosp");
            ycChips.getChildren().add(ycChip);
        });
        if (ycTodas.size() > YC_COMODIDADES_EN_TARJETA) {
            List<YCComodidad> ycResto = ycTodas.subList(YC_COMODIDADES_EN_TARJETA, ycTodas.size());
            Label ycMas = new Label("+" + ycResto.size());
            ycMas.getStyleClass().addAll("cuarto-comodidad", "mas");
            Tooltip ycAyuda = new Tooltip(ycResto.stream().map(YCComodidad::ycGetTexto).collect(Collectors.joining("\n")));
            ycAyuda.setShowDelay(Duration.millis(150));
            ycMas.setTooltip(ycAyuda);
            ycChips.getChildren().add(ycMas);
        }
        if (ycTodas.isEmpty()) {
            Label ycNada = new Label("Sin comodidades registradas");
            ycNada.getStyleClass().add("cuarto-comodidad-vacia");
            ycChips.getChildren().add(ycNada);
        }

        Label ycQuien;
        if (ycHuesped != null) {
            ycQuien = new Label(ycHuesped.getYcHuesped() + " · sale " + ycHuesped.getYcSalida().format(YC_DIA_MES), new FontIcon("fth-user"));
            ycQuien.getStyleClass().addAll("cuarto-ocupante", "ocupado");
        } else if (ycProxima != null) {
            ycQuien = new Label("Libre · reserva el " + ycProxima.getYcEntrada().format(YC_DIA_MES), new FontIcon("fth-bookmark"));
            ycQuien.getStyleClass().addAll("cuarto-ocupante", "libre");
        } else {
            ycQuien = new Label("Libre esta noche", new FontIcon("fth-check-circle"));
            ycQuien.getStyleClass().addAll("cuarto-ocupante", "libre");
        }
        ycQuien.setMaxWidth(170);
        Region ycEspPie = new Region();
        HBox.setHgrow(ycEspPie, Priority.ALWAYS);
        Button ycReservar = ycBotonIcono(Feather.PLUS, "Nueva estadia");
        ycReservar.setOnAction(ycE -> ycNuevaEstadia(ycC));
        Button ycEditar = ycBotonIcono(Feather.EDIT_2, "Editar");
        ycSesion.ycSoloAdmin(ycEditar);
        ycEditar.setOnAction(ycE -> ycEditar(ycC));
        Button ycBorrar = ycBotonIcono(Feather.TRASH_2, "Eliminar");
        ycBorrar.getStyleClass().add("danger");
        ycSesion.ycSoloAdmin(ycBorrar);
        ycBorrar.setOnAction(ycE -> ycEliminar(ycC));
        HBox ycPie = new HBox(2, ycQuien, ycEspPie, ycReservar, ycEditar, ycBorrar);
        ycPie.getStyleClass().add("cuarto-pie");
        ycPie.setAlignment(Pos.CENTER_LEFT);

        VBox ycCuerpo = new VBox(10, ycCabecera, ycSpecs, ycHorario, ycChips, ycPie);
        ycCuerpo.getStyleClass().add("cuarto-cuerpo");

        VBox ycTarjeta = new VBox(ycPortada, ycCuerpo);
        ycTarjeta.getStyleClass().addAll("cuarto-card", "hab-card");
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
        ScaleTransition ycZoom = new ScaleTransition(Duration.millis(380), ycFondo);
        ycZoom.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        ycTarjeta.hoverProperty().addListener((ycObs, ycA, ycEncima) -> {
            ycZoom.stop();
            ycZoom.setToX(ycEncima ? 1.07 : 1.0);
            ycZoom.setToY(ycEncima ? 1.07 : 1.0);
            ycZoom.play();
        });
        Tooltip.install(ycPortada, new Tooltip("Ver todos los detalles"));
        return ycTarjeta;
    }

    private void ycMostrarFicha(YCCuarto ycC) {
        ycFicha.ycMostrar(ycC, ycHuespedes.get(ycC.getYcId()), ycNochesMes.getOrDefault(ycC.getYcId(), 0),
                () -> ycNuevaEstadia(ycC), () -> ycEditar(ycC), this::ycCargar);
    }

    private String ycPorLimpiarTexto() {
        long ycN = ycDatos.stream().filter(ycC -> ycC.ycGetLimpieza() != YCEstadoLimpieza.YC_LIMPIA).count();
        return ycN == 0 ? "" : " · " + ycN + (ycN == 1 ? " por preparar" : " por preparar");
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
    }

    private void ycAbrirFormulario() {
        ycNotificador.ycAbrirPanel(ycPanelFormulario);
        Platform.runLater(ycTxtNumero::requestFocus);
    }

    private void ycLimpiarFormulario() {
        YCFormularios.ycLimpiarErrores(ycTxtNumero, ycTxtPiso, ycTxtMetros, ycCmbTipo, ycTxtCapacidad, ycTxtCamasDobles,
                ycTxtCamasSimples, ycTxtDescripcion, ycTxtPrecio, ycTxtHoraEntrada, ycTxtHoraSalida);
        for (TextInputControl ycT : List.of(ycTxtNumero, ycTxtPiso, ycTxtMetros, ycTxtCapacidad, ycTxtCamasDobles,
                ycTxtCamasSimples, ycTxtDescripcion, ycTxtPrecio, ycTxtHoraEntrada, ycTxtHoraSalida)) {
            ycT.clear();
        }
        ycCmbTipo.setValue(null);
        ycBotonesComodidad.values().forEach(ycB -> ycB.setSelected(false));
        ycBotonesNorma.values().forEach(ycB -> ycB.setSelected(false));
        ycComodidadesOcultas = EnumSet.noneOf(YCComodidad.class);
        ycGaleriaForm.clear();
        ycGaleriaOriginal = List.of();
        ycConstruirMiniaturas();
        ycFotoOriginal = null;
        ycFotoNueva = null;
        ycFotoQuitada = false;
        ycFoto360Original = null;
        ycFoto360Nueva = null;
        ycFoto360Quitada = false;
        ycMostrarFoto360();
    }

    private java.util.Optional<Path> ycFoto360Actual() {
        if (ycFoto360Nueva != null) {
            return java.util.Optional.of(ycFoto360Nueva);
        }
        return ycFoto360Quitada ? java.util.Optional.empty() : ycFotoService.ycRuta(ycFoto360Original);
    }

    private void ycMostrarFoto360() {
        java.util.Optional<Path> ycActual = ycFoto360Actual();
        ycBtnVerFoto360.setDisable(ycActual.isEmpty());
        ycBtnQuitarFoto360.setDisable(ycActual.isEmpty());
        ycLblFoto360.getStyleClass().remove("advertencia-360");
        if (ycActual.isEmpty()) {
            ycLblFoto360.setText("Sin foto 360°. Opcional: deja que el huesped recorra el cuarto con el mouse.");
            ycLblFoto360.setGraphic(new FontIcon(Feather.COMPASS));
        } else if (com.alquilafacil.ui.YCVisor360.ycPareceCompleta(ycActual.get())) {
            ycLblFoto360.setText((ycFoto360Nueva != null ? "Foto 360° nueva lista: " : "Tiene foto 360°: ")
                    + ycActual.get().getFileName() + ". Presiona \"Ver\" para probarla.");
            ycLblFoto360.setGraphic(new FontIcon(Feather.CHECK_CIRCLE));
        } else {
            ycLblFoto360.setText("Esta foto no parece panoramica (es casi tan alta como ancha): en el visor se vera "
                    + "estirada. Usa el modo Panorama del celular o una app 360.");
            ycLblFoto360.setGraphic(new FontIcon(Feather.ALERT_TRIANGLE));
        }
    }

    @FXML
    private void ycOnElegirFoto360() {
        FileChooser ycSelector = new FileChooser();
        ycSelector.setTitle("Elegir foto 360° o panoramica de la habitacion");
        ycSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
        File ycImagenes = new File(System.getProperty("user.home"), "Pictures");
        if (ycImagenes.isDirectory()) {
            ycSelector.setInitialDirectory(ycImagenes);
        }
        File ycElegido = ycSelector.showOpenDialog(ycPanelFormulario.getScene().getWindow());
        if (ycElegido != null) {
            ycFoto360Nueva = ycElegido.toPath();
            ycFoto360Quitada = false;
            ycMostrarFoto360();
        }
    }

    @FXML
    private void ycOnVerFoto360() {
        ycFoto360Actual().ifPresent(ycRuta -> com.alquilafacil.ui.YCVisor360.ycMostrar(ycPanelFormulario.getScene().getWindow(),
                ycRuta, ycTxtNumero.getText().isBlank() ? "Habitacion" : ycTxtNumero.getText().trim()));
    }

    @FXML
    private void ycOnQuitarFoto360() {
        ycFoto360Nueva = null;
        ycFoto360Quitada = true;
        ycMostrarFoto360();
    }

    private void ycSugerirCamas(YCTipoHabitacion ycT) {
        if (ycT == null || !ycTxtCamasDobles.getText().isBlank() || !ycTxtCamasSimples.getText().isBlank()) {
            return;
        }
        int[] ycSugerencia = switch (ycT) {
            case YC_SIMPLE -> new int[]{0, 1, 1};
            case YC_DOBLE -> new int[]{0, 2, 2};
            case YC_MATRIMONIAL, YC_SUITE -> new int[]{1, 0, 2};
            case YC_TRIPLE -> new int[]{1, 1, 3};
            case YC_FAMILIAR -> new int[]{1, 2, 4};
            case YC_COMPARTIDA -> new int[]{0, 4, 4};
        };
        ycTxtCamasDobles.setText(String.valueOf(ycSugerencia[0]));
        ycTxtCamasSimples.setText(String.valueOf(ycSugerencia[1]));
        if (ycTxtCapacidad.getText().isBlank()) {
            ycTxtCapacidad.setText(String.valueOf(ycSugerencia[2]));
        }
    }

    private static String ycCamasCorto(YCCuarto ycC) {
        int ycTotal = (ycC.getYcCamasDobles() == null ? 0 : ycC.getYcCamasDobles()) + (ycC.getYcCamasSimples() == null ? 0 : ycC.getYcCamasSimples());
        return ycTotal + (ycTotal == 1 ? " cama" : " camas");
    }

    private static Label ycSpec(String ycIcono, String ycTexto) {
        Label ycL = new Label(ycTexto, new FontIcon(ycIcono));
        ycL.getStyleClass().add("hab-spec");
        return ycL;
    }

    private static String ycTexto(Integer ycN) {
        return ycN == null ? "" : String.valueOf(ycN);
    }

    private static Integer ycEntero(TextField ycCampo) {
        return ycCampo.getText().isBlank() ? null : Integer.valueOf(ycCampo.getText().trim());
    }

    private static int ycValor(TextField ycCampo) {
        return ycCampo.getText().isBlank() ? 0 : Integer.parseInt(ycCampo.getText().trim());
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
