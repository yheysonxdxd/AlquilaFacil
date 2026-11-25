package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCEstadoLimpieza;
import com.alquilafacil.service.YCCuartoService;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.ui.YCFotos;
import com.alquilafacil.ui.YCNotificador;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class YCFichaHabitacion {
    private static final DateTimeFormatter YC_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final YCCuartoService ycCuartoService;
    private final YCEstadiaService ycEstadiaService;
    private final YCSesion ycSesion;
    private final YCFotos ycFotos;
    private final YCNotificador ycNotificador;
    private final com.alquilafacil.service.YCFotoService ycFotoService;

    void ycMostrar(YCCuarto ycC, YCEstadia ycHuesped, int ycNoches, Runnable ycNuevaEstadia, Runnable ycAlEditar, Runnable ycRecargar) {
        double ycAncho = 860;
        double ycAlto = 320;

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

        Label ycEstado = new Label(ycHuesped == null ? "Libre hoy" : "Ocupada hoy");
        ycEstado.getStyleClass().addAll("cuarto-estado", ycHuesped == null ? "libre" : "ocupado");
        Label ycTipo = new Label(ycC.getYcTipoHabitacion() == null ? "Hospedaje" : ycC.getYcTipoHabitacion().ycGetTexto(),
                new FontIcon("fth-moon"));
        ycTipo.getStyleClass().addAll("cuarto-modalidad", "noche");
        HBox ycEtiquetas = new HBox(8, ycEstado, ycTipo);
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
        List<String> ycMeta = new ArrayList<>();
        if (ycC.getYcTipoHabitacion() != null) ycMeta.add("Habitacion " + ycC.getYcTipoHabitacion().ycGetTexto().toLowerCase());
        if (ycC.getYcCapacidad() != null) ycMeta.add("Hasta " + ycC.getYcCapacidad() + (ycC.getYcCapacidad() == 1 ? " huesped" : " huespedes"));
        if (ycC.ycGetCamasTexto() != null) ycMeta.add(ycC.ycGetCamasTexto());
        if (ycC.getYcMetros() != null) ycMeta.add(ycC.getYcMetros() + " m²");
        Label ycSubtitulo = new Label(String.join("  ·  ", ycMeta));
        ycSubtitulo.getStyleClass().add("ficha-subtitulo");
        VBox ycTextosFoto = new VBox(4, ycTitulo, ycSubtitulo);
        ycTextosFoto.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ycTextosFoto, Pos.BOTTOM_LEFT);
        StackPane.setMargin(ycTextosFoto, new Insets(22));
        ycFoto.getChildren().addAll(ycEtiquetas, ycCerrar, ycTextosFoto);
        List<javafx.scene.image.Image> ycImagenes = new ArrayList<>();
        ycFotos.ycCargar(ycC.getYcFoto()).ifPresent(ycImagenes::add);
        ycC.ycGetGaleria().forEach(ycN -> ycFotos.ycCargar(ycN).ifPresent(ycImagenes::add));
        ycCabecera.ycEmpezar(ycImagenes.isEmpty() ? YCFotos.ycSinFoto(ycC.getYcNumero(), ycAncho, ycAlto, true)
                : YCFotos.ycCubrir(ycImagenes.get(0), ycAncho, ycAlto), ycImagenes);

        VBox ycIzquierda = new VBox(10);
        HBox.setHgrow(ycIzquierda, Priority.ALWAYS);
        ycIzquierda.getChildren().add(ycSeccion("Sobre esta habitacion"));
        Label ycDescripcion = new Label(ycC.getYcDescripcion() == null ? "Aun no tiene descripcion." : ycC.getYcDescripcion());
        ycDescripcion.getStyleClass().add("ficha-texto");
        ycDescripcion.setWrapText(true);
        ycIzquierda.getChildren().add(ycDescripcion);

        GridPane ycDatosHab = new GridPane();
        ycDatosHab.setHgap(10);
        ycDatosHab.setVgap(10);
        String[][] ycItems = {
                {"fth-home", "Tipo", ycC.getYcTipoHabitacion() == null ? "—" : ycC.getYcTipoHabitacion().ycGetTexto()},
                {"fth-users", "Capacidad", ycC.getYcCapacidad() == null ? "—" : ycC.getYcCapacidad() + (ycC.getYcCapacidad() == 1 ? " persona" : " personas")},
                {"fth-moon", "Camas", ycC.ycGetCamasTexto() == null ? "—" : ycC.ycGetCamasTexto()},
                {"fth-maximize", "Tamaño", ycC.getYcMetros() == null ? "—" : ycC.getYcMetros() + " m²"},
                {"fth-log-in", "Check-in", "Desde las " + ycC.ycGetHoraEntradaODefecto()},
                {"fth-log-out", "Check-out", "Hasta las " + ycC.ycGetHoraSalidaODefecto()},
        };
        for (int ycI = 0; ycI < ycItems.length; ycI++) {
            ycDatosHab.add(ycDato(ycItems[ycI][0], ycItems[ycI][1], ycItems[ycI][2]), ycI % 3, ycI / 3);
        }
        VBox.setMargin(ycDatosHab, new Insets(6, 0, 4, 0));
        ycIzquierda.getChildren().add(ycDatosHab);

        ycIzquierda.getChildren().add(ycSeccion("Que incluye"));
        if (ycC.ycGetComodidades().isEmpty()) {
            Label ycNada = new Label("No se registraron comodidades.");
            ycNada.getStyleClass().add("ficha-texto");
            ycIzquierda.getChildren().add(ycNada);
        } else {
            ycIzquierda.getChildren().add(ycGrillaIconos(ycC.ycGetComodidades().stream()
                    .map(ycCom -> new String[]{ycCom.ycGetIcono(), ycCom.ycGetTexto()}).toList(), false));
        }

        ycIzquierda.getChildren().add(ycSeccion("Normas de la casa"));
        if (ycC.ycGetNormas().isEmpty()) {
            Label ycNada = new Label("Sin normas registradas.");
            ycNada.getStyleClass().add("ficha-texto");
            ycIzquierda.getChildren().add(ycNada);
        } else {
            ycIzquierda.getChildren().add(ycGrillaIconos(ycC.ycGetNormas().stream()
                    .map(ycN -> new String[]{ycN.ycGetIcono(), ycN.ycGetTexto()}).toList(), true));
        }

        Label ycEtiquetaPrecio = new Label("PRECIO POR NOCHE");
        ycEtiquetaPrecio.getStyleClass().add("ficha-lateral-etiqueta");
        Label ycPrecio = new Label(YCControles.ycMoneda(ycC.getYcPrecio()));
        ycPrecio.getStyleClass().add("ficha-precio");
        Label ycEjemplo = new Label("3 noches: " + YCControles.ycMoneda(ycC.getYcPrecio().multiply(BigDecimal.valueOf(3))));
        ycEjemplo.getStyleClass().add("ficha-lateral-nota");

        YearMonth ycMes = YearMonth.now();
        double ycPorcentaje = ycNoches / (double) ycMes.lengthOfMonth();
        Label ycEtiquetaOcup = new Label("OCUPACION EN " + YCControles.ycNombreMes(ycMes).toUpperCase());
        ycEtiquetaOcup.getStyleClass().add("ficha-lateral-etiqueta");
        ProgressBar ycBarra = new ProgressBar(ycPorcentaje);
        ycBarra.getStyleClass().add("barra-fina");
        ycBarra.setMaxWidth(Double.MAX_VALUE);
        Label ycDetalleOcup = new Label(ycNoches + " de " + ycMes.lengthOfMonth() + " noches · "
                + Math.round(ycPorcentaje * 100) + "%");
        ycDetalleOcup.getStyleClass().add("ficha-lateral-nota");
        VBox ycOcupacionMes = new VBox(6, ycEtiquetaOcup, ycBarra, ycDetalleOcup);
        VBox.setMargin(ycOcupacionMes, new Insets(8, 0, 0, 0));

        VBox ycHoy = new VBox(5);
        ycHoy.getStyleClass().add("ficha-ocupante");
        if (ycHuesped != null) {
            Label ycEt = new Label("HOSPEDADO HOY");
            ycEt.getStyleClass().add("ficha-lateral-etiqueta");
            Label ycNombre = new Label(ycHuesped.getYcHuesped(), new FontIcon(Feather.USER));
            ycNombre.getStyleClass().add("ficha-ocupante-nombre");
            ycNombre.setWrapText(true);
            Label ycFechas = new Label("Del " + ycHuesped.getYcEntrada().format(YC_DIA_MES) + " al "
                    + ycHuesped.getYcSalida().format(YC_DIA_MES) + " · " + ycHuesped.getYcPersonas()
                    + (ycHuesped.getYcPersonas() == 1 ? " persona" : " personas"), new FontIcon(Feather.CALENDAR));
            ycFechas.getStyleClass().add("ficha-lateral-nota");
            ycHoy.getChildren().addAll(ycEt, ycNombre, ycFechas);
        } else {
            ycHoy.getStyleClass().add("libre");
            Label ycLibre = new Label("Libre esta noche", new FontIcon(Feather.CHECK_CIRCLE));
            ycLibre.getStyleClass().add("ficha-ocupante-nombre");
            Label ycTexto = new Label("Puedes registrar un huesped desde hoy.");
            ycTexto.getStyleClass().add("ficha-lateral-nota");
            ycTexto.setWrapText(true);
            ycHoy.getChildren().addAll(ycLibre, ycTexto);
        }
        VBox.setMargin(ycHoy, new Insets(8, 0, 0, 0));

        Label ycEtLimpieza = new Label("LIMPIEZA");
        ycEtLimpieza.getStyleClass().add("ficha-lateral-etiqueta");
        MenuButton ycLimpiezaFicha = ycMenuLimpieza(ycC, ycRecargar);
        HBox ycFilaLimpieza = new HBox(10, ycEtLimpieza, ycLimpiezaFicha);
        ycFilaLimpieza.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(ycFilaLimpieza, new Insets(6, 0, 0, 0));
        VBox ycDerecha = new VBox(8, ycEtiquetaPrecio, ycPrecio, ycEjemplo, ycOcupacionMes, ycHoy, ycFilaLimpieza);
        List<YCEstadia> ycSiguientes = ycEstadiaService.ycVigentesDe(ycC.getYcId()).stream()
                .filter(ycE -> ycHuesped == null || !ycE.getYcId().equals(ycHuesped.getYcId()))
                .limit(3).toList();
        if (!ycSiguientes.isEmpty()) {
            Label ycEt = new Label("PROXIMAS RESERVAS");
            ycEt.getStyleClass().add("ficha-lateral-etiqueta");
            VBox ycReservas = new VBox(4, ycEt);
            for (YCEstadia ycE : ycSiguientes) {
                Label ycR = new Label(ycE.getYcEntrada().format(YC_DIA_MES) + " → " + ycE.getYcSalida().format(YC_DIA_MES)
                        + "  " + ycE.getYcHuesped(), new FontIcon(Feather.BOOKMARK));
                ycR.getStyleClass().add("ficha-lateral-nota");
                ycReservas.getChildren().add(ycR);
            }
            VBox.setMargin(ycReservas, new Insets(6, 0, 0, 0));
            ycDerecha.getChildren().add(ycReservas);
        }
        Region ycEmpuje = new Region();
        VBox.setVgrow(ycEmpuje, Priority.ALWAYS);
        Button ycNueva = new Button("Nueva estadia", new FontIcon(Feather.PLUS));
        ycNueva.getStyleClass().add("accent");
        ycNueva.setMaxWidth(Double.MAX_VALUE);
        ycNueva.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            ycNuevaEstadia.run();
        });
        Button ycEditar = new Button("Editar habitacion", new FontIcon(Feather.EDIT_2));
        ycSesion.ycSoloAdmin(ycEditar);
        ycEditar.getStyleClass().add("button-outlined");
        ycEditar.setMaxWidth(Double.MAX_VALUE);
        ycEditar.setOnAction(ycE -> {
            ycNotificador.ycCerrarDialogo();
            ycAlEditar.run();
        });
        Button ycCerrarFicha = new Button("Cerrar");
        ycCerrarFicha.getStyleClass().add("flat");
        ycCerrarFicha.setMaxWidth(Double.MAX_VALUE);
        ycCerrarFicha.setCancelButton(true);
        ycCerrarFicha.setOnAction(ycE -> ycNotificador.ycCerrarDialogo());
        ycDerecha.getChildren().add(ycEmpuje);
        ycFotoService.ycRuta(ycC.getYcFoto360()).ifPresent(ycRuta -> {
            Button ycVer360 = new Button("Ver en 360°", new FontIcon(Feather.COMPASS));
            ycVer360.getStyleClass().addAll("success", "button-outlined");
            ycVer360.setMaxWidth(Double.MAX_VALUE);
            ycVer360.setTooltip(new Tooltip("Recorre la habitacion con el mouse, como si estuvieras adentro"));
            ycVer360.setOnAction(ycE -> com.alquilafacil.ui.YCVisor360.ycMostrar(ycVer360.getScene().getWindow(), ycRuta, ycC.getYcNumero()));
            ycDerecha.getChildren().add(ycVer360);
        });
        ycDerecha.getChildren().addAll(ycNueva, ycEditar, ycCerrarFicha);
        ycDerecha.getStyleClass().add("ficha-lateral");
        ycDerecha.setMinWidth(260);
        ycDerecha.setPrefWidth(260);

        HBox ycCuerpo = new HBox(26, ycIzquierda, ycDerecha);
        ycCuerpo.getStyleClass().add("ficha-cuerpo");
        ScrollPane ycScroll = new ScrollPane(ycCuerpo);
        ycScroll.setFitToWidth(true);
        ycScroll.getStyleClass().add("pagina-scroll");
        ycScroll.setMaxHeight(420);
        ycScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox ycFicha = new VBox(ycFoto, ycScroll);
        ycFicha.getStyleClass().add("ficha");
        ycFicha.setPrefWidth(ycAncho);
        ycFicha.setMaxSize(ycAncho, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycFicha);
        Animations.zoomIn(ycFicha, Duration.millis(260)).play();
        Platform.runLater(ycNueva::requestFocus);
    }

    MenuButton ycMenuLimpieza(YCCuarto ycC, Runnable ycRecargar) {
        YCEstadoLimpieza ycActual = ycC.ycGetLimpieza();
        MenuButton ycMenu = new MenuButton(ycActual.ycGetTexto(), new FontIcon(ycActual.ycGetIcono()));
        ycMenu.getStyleClass().addAll("limpieza-chip", ycActual.ycGetClase());
        ycMenu.setTooltip(new Tooltip("Cambiar el estado de limpieza"));
        for (YCEstadoLimpieza ycE : YCEstadoLimpieza.values()) {
            MenuItem ycItem = new MenuItem(ycE.ycGetTexto(), new FontIcon(ycE.ycGetIcono()));
            ycItem.setDisable(ycE == ycActual);
            ycItem.setOnAction(ycEv -> {
                try {
                    ycCuartoService.ycCambiarLimpieza(ycC.getYcId(), ycE);
                    ycNotificador.ycCerrarDialogo();
                    ycNotificador.ycExito(ycC.getYcNumero() + ": " + ycE.ycGetTexto().toLowerCase() + ".");
                    ycRecargar.run();
                } catch (YCNegocioException ycEx) {
                    ycNotificador.ycError(ycEx.getMessage());
                }
            });
            ycMenu.getItems().add(ycItem);
        }
        return ycMenu;
    }

    private static Label ycSeccion(String ycTexto) {
        Label ycL = new Label(ycTexto);
        ycL.getStyleClass().add("ficha-seccion");
        VBox.setMargin(ycL, new Insets(8, 0, 0, 0));
        return ycL;
    }

    private static Node ycDato(String ycIcono, String ycEtiqueta, String ycValor) {
        StackPane ycCirculo = new StackPane(new FontIcon(ycIcono));
        ycCirculo.getStyleClass().add("ficha-comodidad-icono");
        Label ycEt = new Label(ycEtiqueta.toUpperCase());
        ycEt.getStyleClass().add("ficha-lateral-etiqueta");
        Label ycV = new Label(ycValor);
        ycV.getStyleClass().add("ficha-dato-valor");
        VBox ycTextos = new VBox(1, ycEt, ycV);
        HBox ycCaja = new HBox(10, ycCirculo, ycTextos);
        ycCaja.setAlignment(Pos.CENTER_LEFT);
        ycCaja.getStyleClass().add("ficha-dato");
        ycCaja.setPrefWidth(178);
        return ycCaja;
    }

    private static GridPane ycGrillaIconos(List<String[]> ycFilas, boolean ycNorma) {
        GridPane ycGrid = new GridPane();
        ycGrid.setHgap(18);
        ycGrid.setVgap(10);
        int ycI = 0;
        for (String[] ycF : ycFilas) {
            StackPane ycCirculo = new StackPane(new FontIcon(ycF[0]));
            ycCirculo.getStyleClass().add(ycNorma ? "ficha-norma-icono" : "ficha-comodidad-icono");
            Label ycTexto = new Label(ycF[1]);
            ycTexto.getStyleClass().add("ficha-comodidad-texto");
            HBox ycFila = new HBox(10, ycCirculo, ycTexto);
            ycFila.setAlignment(Pos.CENTER_LEFT);
            ycFila.setPrefWidth(250);
            ycGrid.add(ycFila, ycI % 2, ycI / 2);
            ycI++;
        }
        return ycGrid;
    }
}
