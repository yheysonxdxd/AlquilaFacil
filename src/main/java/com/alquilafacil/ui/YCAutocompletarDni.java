package com.alquilafacil.ui;

import com.alquilafacil.service.YCConsultaDniService;
import com.alquilafacil.service.YCConsultaDniService.YCPersona;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class YCAutocompletarDni {
    private final YCConsultaDniService ycConsulta;

    public void ycConectar(TextField ycDni, Label ycEstado, Consumer<YCPersona> ycAlEncontrar) {
        ycDni.textProperty().addListener((ycObs, ycAnterior, ycTexto) -> {
            String ycD = ycTexto == null ? "" : ycTexto.trim();
            if (!ycD.matches("\\d{8}")) {
                ycOcultar(ycEstado);
                return;
            }
            Optional<YCPersona> ycLocal = ycConsulta.ycBuscarLocal(ycD);
            if (ycLocal.isPresent()) {
                ycMostrar(ycEstado, "ok", Feather.CHECK_CIRCLE, "Encontrado: " + ycLocal.get().ycNombreCompleto()
                        + " (" + ycLocal.get().ycFuente().ycGetTexto().toLowerCase() + ")");
                ycAlEncontrar.accept(ycLocal.get());
                return;
            }
            if (!ycConsulta.ycReniecConfigurado()) {
                ycMostrar(ycEstado, "info", Feather.INFO, "No esta registrado aqui. Para buscar cualquier DNI en RENIEC, "
                        + "configura app.dni.token en application.properties.");
                return;
            }
            ycMostrar(ycEstado, "buscando", Feather.LOADER, "Buscando en RENIEC…");
            Thread ycHilo = new Thread(() -> {
                try {
                    Optional<YCPersona> ycP = ycConsulta.ycBuscarReniec(ycD);
                    Platform.runLater(() -> {
                        if (!ycD.equals(ycDni.getText().trim())) {
                            return;
                        }
                        if (ycP.isPresent()) {
                            ycMostrar(ycEstado, "ok", Feather.CHECK_CIRCLE, "RENIEC: " + ycP.get().ycNombreCompleto());
                            ycAlEncontrar.accept(ycP.get());
                        } else {
                            ycMostrar(ycEstado, "aviso", Feather.ALERT_CIRCLE, "RENIEC no encontro ese DNI. Revisa el numero.");
                        }
                    });
                } catch (YCConsultaDniService.YCConsultaException ycE) {
                    Platform.runLater(() -> {
                        if (ycD.equals(ycDni.getText().trim())) {
                            ycMostrar(ycEstado, "aviso", Feather.WIFI_OFF, ycE.getMessage() + " Escribe el nombre a mano.");
                        }
                    });
                }
            }, "consulta-dni");
            ycHilo.setDaemon(true);
            ycHilo.start();
        });
    }

    public static void ycOcultar(Label ycEstado) {
        ycEstado.setVisible(false);
        ycEstado.setManaged(false);
    }

    private static void ycMostrar(Label ycEstado, String ycTipo, Feather ycIcono, String ycTexto) {
        ycEstado.getStyleClass().removeAll("ok", "info", "buscando", "aviso");
        ycEstado.getStyleClass().addAll("dni-estado", ycTipo);
        ycEstado.setText(ycTexto);
        ycEstado.setGraphic(new FontIcon(ycIcono));
        ycEstado.setWrapText(true);
        ycEstado.setVisible(true);
        ycEstado.setManaged(true);
    }
}
