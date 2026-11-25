package com.alquilafacil.controller;

import com.alquilafacil.YCAlquilaFacilApplication;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.service.YCUsuarioService;
import com.alquilafacil.ui.YCDatosNegocioDialogo;
import com.alquilafacil.ui.YCGestionUsuarios;
import com.alquilafacil.ui.YCHistorialDialogo;
import com.alquilafacil.ui.YCNotificador;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class YCPantallasTest {
    private static final List<String> YC_VISTAS = List.of("dashboard-view", "caja-view", "gasto-view", "cuarto-view",
            "inquilino-view", "pago-view", "habitacion-view", "hospedaje-view");

    private static Path ycCarpeta;

    private static ConfigurableApplicationContext ycContext;

    @BeforeAll
    static void ycArrancar() throws Exception {
        CountDownLatch ycListo = new CountDownLatch(1);
        try {
            Platform.startup(ycListo::countDown);
        } catch (IllegalStateException ycYaIniciado) {
            ycListo.countDown();
        } catch (UnsupportedOperationException | Error ycSinPantalla) {
            Assumptions.abort("Sin pantalla: se salta la prueba de pantallas (" + ycSinPantalla + ")");
        }
        assertTrue(ycListo.await(30, TimeUnit.SECONDS), "JavaFX no arranco");
        ycCarpeta = java.nio.file.Files.createTempDirectory("pantallas-hospedaje");
        ycContext = new SpringApplicationBuilder(YCAlquilaFacilApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .run("--app.carpeta=" + ycCarpeta.toString().replace('\\', '/'),
                        "--app.seeder.enabled=true", "--logging.file.name=", "--app.dni.token=");
    }

    @AfterAll
    static void ycCerrar() {
        if (ycContext != null) {
            ycContext.close();
        }
        if (ycCarpeta != null) {
            try (var ycArchivos = java.nio.file.Files.walk(ycCarpeta)) {
                ycArchivos.sorted(java.util.Comparator.reverseOrder()).forEach(ycF -> ycF.toFile().delete());
            } catch (Exception ycIgnorada) {
            }
        }
    }

    private static List<String> ycEnFx(List<YCPaso> ycPasos) throws Exception {
        List<String> ycErrores = new ArrayList<>();
        CountDownLatch ycFin = new CountDownLatch(1);
        Platform.runLater(() -> {
            for (YCPaso ycP : ycPasos) {
                try {
                    ycP.ycAccion().run();
                } catch (Throwable ycT) {
                    ycErrores.add(ycP.ycNombre() + ": " + ycT);
                    ycT.printStackTrace();
                }
            }
            ycFin.countDown();
        });
        assertTrue(ycFin.await(120, TimeUnit.SECONDS), "Las pantallas tardaron demasiado");
        return ycErrores;
    }

    private record YCPaso(String ycNombre, Runnable ycAccion) {}

    private static void ycPrivado(Object ycObjeto, String ycMetodo) {
        try {
            Method ycM = ycObjeto.getClass().getDeclaredMethod(ycMetodo);
            ycM.setAccessible(true);
            ycM.invoke(ycObjeto);
        } catch (java.lang.reflect.InvocationTargetException ycE) {
            throw new RuntimeException(ycE.getCause());
        } catch (ReflectiveOperationException ycE) {
            throw new RuntimeException(ycE);
        }
    }

    @Test
    void ycTodasLasPantallasYVentanasAbrenSinErrores() throws Exception {
        YCUsuarioService ycUsuarios = ycContext.getBean(YCUsuarioService.class);
        YCMainController[] ycMain = new YCMainController[1];
        List<YCPaso> ycPasos = new ArrayList<>();

        ycPasos.add(new YCPaso("ventana principal", () -> {
            FXMLLoader ycLoader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            ycLoader.setControllerFactory(ycContext::getBean);
            try {
                Parent ycRaiz = ycLoader.load();
                Stage ycStage = new Stage();
                ycStage.setScene(new Scene(ycRaiz, 1280, 800));
                ycMain[0] = ycLoader.getController();
            } catch (Exception ycE) {
                throw new RuntimeException(ycE);
            }
            ycUsuarios.ycCrear("prueba", "Admin de Prueba", YCRol.YC_ADMINISTRADOR, "clave123");
            ycUsuarios.ycIngresar("prueba", "clave123");
        }));
        for (String ycVista : YC_VISTAS) {
            ycPasos.add(new YCPaso("pantalla " + ycVista, () -> {
                ycMain[0].ycIrA(ycVista);
                assertNotNull(ycMain[0].ycControladorDe(ycVista));
            }));
        }
        YCNotificador ycNotificador = ycContext.getBean(YCNotificador.class);
        ycPasos.add(new YCPaso("copias de seguridad", () -> ycPrivado(ycMain[0], "onRespaldo")));
        ycPasos.add(new YCPaso("diseño", () -> ycPrivado(ycMain[0], "onDiseno")));
        ycPasos.add(new YCPaso("datos del negocio", () -> ycContext.getBean(YCDatosNegocioDialogo.class).ycMostrar()));
        ycPasos.add(new YCPaso("historial", () -> ycContext.getBean(YCHistorialDialogo.class).ycMostrar()));
        ycPasos.add(new YCPaso("usuarios", () -> ycContext.getBean(YCGestionUsuarios.class).ycMostrarLista()));
        ycPasos.add(new YCPaso("reporte anual", () -> ycPrivado(ycMain[0].ycControladorDe("dashboard-view"), "onReporteAnual")));
        ycPasos.add(new YCPaso("detalle y edicion de estadia", () -> {
            List<YCEstadia> ycTodas = ycContext.getBean(YCEstadiaRepository.class).findAll();
            assertFalse(ycTodas.isEmpty(), "el ejemplo deberia tener estadias");
            YCEstadia ycE = ycTodas.stream().filter(ycX -> !ycX.isYcCancelada()).findFirst().orElseThrow();
            ycContext.getBean(YCDetalleEstadia.class).ycMostrar(ycE, () -> { }, ycX -> { });
            ((YCHospedajeController) ycMain[0].ycControladorDe("hospedaje-view")).ycEditarEstadia(ycE);
        }));
        ycPasos.add(new YCPaso("consumos extra de una estadia", () -> {
            YCEstadia ycE = ycContext.getBean(YCEstadiaRepository.class).findAll().stream()
                    .filter(ycX -> !ycX.isYcCancelada()).findFirst().orElseThrow();
            ycContext.getBean(com.alquilafacil.service.YCConsumoExtraService.class)
                    .ycAgregar(ycE.getYcId(), "Desayuno", 2, new java.math.BigDecimal("12"), null);
            YCConsumosEstadia ycConsumos = ycContext.getBean(YCConsumosEstadia.class);
            ycConsumos.ycAgregar(ycE, () -> { });
            ycConsumos.ycCobrar(ycE, () -> { });
            ycContext.getBean(YCDetalleEstadia.class).ycMostrar(ycE, () -> { }, ycX -> { });
        }));
        ycPasos.add(new YCPaso("reserva de grupo en el formulario", () -> {
            YCHospedajeController ycH = (YCHospedajeController) ycMain[0].ycControladorDe("hospedaje-view");
            ycH.ycNuevaEstadia(null);
            ycPrivado(ycH, "onAgregarHabitacion");
            ycPrivado(ycH, "onAgregarHabitacion");
        }));
        ycPasos.add(new YCPaso("ficha de habitacion", () -> {
            YCCuarto ycC = ycContext.getBean(YCCuartoRepository.class).findAll().stream()
                    .filter(YCCuarto::ycIsHospedaje).findFirst().orElseThrow();
            ycContext.getBean(YCFichaHabitacion.class).ycMostrar(ycC, null, 3, () -> { }, () -> { }, () -> { });
        }));
        ycPasos.add(new YCPaso("cerrar dialogos", () -> {
            ycNotificador.ycCerrarDialogo();
            ycNotificador.ycCerrarPanel();
        }));

        List<String> ycErrores = ycEnFx(ycPasos);
        assertTrue(ycErrores.isEmpty(), "Errores en pantallas:\n" + String.join("\n", ycErrores));
    }

    @Test
    void ycLosReportesEnExcelSeGeneranConLosDatosDeEjemplo() throws Exception {
        java.io.File ycMensual = ycCarpeta.resolve("reporte-mensual.xlsx").toFile();
        java.io.File ycAnual = ycCarpeta.resolve("reporte-anual.xlsx").toFile();
        ycContext.getBean(com.alquilafacil.service.YCReporteMensualService.class).ycGenerarExcel(java.time.YearMonth.now(), ycMensual);
        ycContext.getBean(com.alquilafacil.service.YCReporteAnualService.class).ycGenerarExcel(java.time.LocalDate.now().getYear(), ycAnual);
        for (java.io.File ycF : List.of(ycMensual, ycAnual)) {
            try (java.util.zip.ZipFile ycZip = new java.util.zip.ZipFile(ycF)) {
                assertNotNull(ycZip.getEntry("xl/worksheets/sheet1.xml"), ycF.getName());
            }
        }
        String ycCopia = System.getProperty("excel.muestra");
        if (ycCopia != null) {
            java.nio.file.Files.copy(ycMensual.toPath(), java.nio.file.Path.of(ycCopia, "reporte-mensual.xlsx"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.nio.file.Files.copy(ycAnual.toPath(), java.nio.file.Path.of(ycCopia, "reporte-anual.xlsx"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
