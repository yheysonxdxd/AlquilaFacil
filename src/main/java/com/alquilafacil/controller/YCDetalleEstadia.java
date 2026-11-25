package com.alquilafacil.controller;

import atlantafx.base.util.Animations;
import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.service.YCSesion;
import com.alquilafacil.ui.YCNotificador;
import com.alquilafacil.ui.YCTablas;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
class YCDetalleEstadia {
    private final YCEstadiaService ycService;
    private final YCSesion ycSesion;
    private final YCNotificador ycNotificador;
    private final YCDatosArrendador ycArrendador;
    private final YCAccionesEstadia ycAcciones;
    private final YCMensajesHuesped ycMensajes;
    private final YCConsumosEstadia ycConsumos;

    private static String ycHistorialUsuarios(YCEstadia ycE) {
        java.util.List<String> ycPartes = new java.util.ArrayList<>();
        if (ycE.getYcRegistradoPor() != null) ycPartes.add("Registro: " + ycE.getYcRegistradoPor());
        if (ycE.getYcSaldoCobradoPor() != null) ycPartes.add("Cobro el saldo: " + ycE.getYcSaldoCobradoPor());
        if (ycE.getYcDevuelto() != null && ycE.getYcDevuelto().signum() > 0) {
            ycPartes.add("Se le devolvio " + YCControles.ycMoneda(ycE.getYcDevuelto()) + " en "
                    + ycE.ycGetMetodoDevolucionODefecto().ycGetDescripcion().toLowerCase()
                    + (ycE.getYcFechaDevolucion() == null ? "" : " el "
                    + ycE.getYcFechaDevolucion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    + (ycE.getYcDevueltoPor() == null ? "" : " (por " + ycE.getYcDevueltoPor() + ")"));
        }
        return String.join("  ·  ", ycPartes);
    }

    void ycMostrar(YCEstadia ycE, Runnable ycRecargar, java.util.function.Consumer<YCEstadia> ycAlEditar) {
        YCEstadia.YCEstado ycEstado = ycE.ycEstado(LocalDate.now());
        YCCuarto ycC = ycE.getYcCuarto();

        StackPane ycAvatar = new StackPane(new Label(YCTablas.ycIniciales(ycE.getYcHuesped())));
        ycAvatar.getStyleClass().addAll("avatar", "avatar-grande");
        Label ycNombre = new Label(ycE.getYcHuesped());
        ycNombre.getStyleClass().add("dialogo-titulo");
        Label ycSub = new Label(ycE.ycGetNumero() + " · " + ycE.getYcCuartoNumero()
                + (ycC.getYcTipoHabitacion() == null ? "" : " (" + ycC.getYcTipoHabitacion().ycGetTexto() + ")"));
        ycSub.getStyleClass().add("dialogo-mensaje");
        String ycTextoEstado = YCEstadoEstadia.ycTexto(ycE, ycArrendador.ycGetHoraLimiteLlegadaHora());
        Label ycChip = new Label(ycTextoEstado);
        ycChip.getStyleClass().addAll("chip", YCEstadoEstadia.ycClase(ycTextoEstado));
        Region ycEsp = new Region();
        HBox.setHgrow(ycEsp, Priority.ALWAYS);
        VBox ycTitulos = new VBox(3, ycNombre, ycSub);
        ycService.ycHuespedPorDocumento(ycE.getYcDocumento()).filter(ycH -> ycH.ycVisitas() > 1).ifPresent(ycH -> {
            Label ycFrecuente = new Label("Cliente frecuente · " + ycH.ycVisitas() + " estadias", new FontIcon(Feather.STAR));
            ycFrecuente.getStyleClass().add("chip-frecuente");
            ycTitulos.getChildren().add(ycFrecuente);
        });
        HBox ycCabecera = new HBox(14, ycAvatar, ycTitulos, ycEsp, ycChip);
        ycCabecera.setAlignment(Pos.CENTER_LEFT);

        HBox ycFechas = new HBox(12,
                ycBloqueFecha("ENTRADA", ycE.getYcEntrada(), "desde las " + ycC.ycGetHoraEntradaODefecto(), Feather.LOG_IN),
                ycFlecha(ycE.getYcNoches()),
                ycBloqueFecha("SALIDA", ycE.getYcSalida(), "hasta las " + ycC.ycGetHoraSalidaODefecto(), Feather.LOG_OUT));
        ycFechas.setAlignment(Pos.CENTER);
        ycFechas.getStyleClass().add("detalle-fechas");

        GridPane ycInfo = new GridPane();
        ycInfo.setHgap(24);
        ycInfo.setVgap(10);
        String[][] ycFilas = {
                {"Documento", ycE.getYcDocumento() == null ? "—" : ycE.getYcDocumento()},
                {"Celular", ycE.getYcTelefono() == null ? "—" : ycE.getYcTelefono()},
                {"Personas", String.valueOf(ycE.getYcPersonas())},
                {"Precio por noche", YCControles.ycMoneda(ycE.getYcPrecioNoche())},
                {"Metodo de pago", ycE.getYcMetodo().toString()
                        + (ycE.getYcMetodoSaldo() != null ? " + " + ycE.getYcMetodoSaldo() : "")},
                {"Registrado", ycE.getYcFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))},
                {"Reservo por", ycE.getYcCanal() == null ? "—" : ycE.getYcCanal().ycGetTexto()},
                {"Llegada (check-in)", ycE.ycIsNoLlego() ? "No se presento"
                        : ycE.getYcFechaLlegada() != null
                        ? ycE.getYcFechaLlegada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ycE.ycIsLlego() ? "—" : "Aun no llega"},
        };
        for (int ycI = 0; ycI < ycFilas.length; ycI++) {
            Label ycEt = new Label(ycFilas[ycI][0].toUpperCase());
            ycEt.getStyleClass().add("ficha-lateral-etiqueta");
            Label ycV = new Label(ycFilas[ycI][1]);
            ycV.getStyleClass().add("ficha-dato-valor");
            ycInfo.add(new VBox(2, ycEt, ycV), ycI % 3, ycI / 3);
        }

        VBox ycContenido = new VBox(18, ycCabecera, ycFechas, ycInfo);
        java.util.List<YCEstadia> ycGrupo = ycService.ycDelGrupo(ycE);
        if (ycGrupo.size() > 1) {
            java.math.BigDecimal ycTotalGrupo = ycGrupo.stream().filter(ycX -> !ycX.isYcCancelada()).map(YCEstadia::getYcTotal)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal ycDebeGrupo = ycGrupo.stream().map(YCEstadia::ycGetSaldo)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            Label ycEtGrupo = new Label("Reserva de grupo: " + ycE.getYcHuesped() + " alquila " + ycGrupo.size()
                    + " habitaciones y paga todo. " + String.join(" · ", ycGrupo.stream()
                    .map(ycX -> ycX.getYcCuartoNumero() + " (" + ycX.getYcPersonas() + " pers."
                            + (ycX.isYcCancelada() ? ", cancelada" : "") + ")").toList())
                    + ". Total del grupo " + YCControles.ycMoneda(ycTotalGrupo)
                    + (ycDebeGrupo.signum() > 0 ? " · debe " + YCControles.ycMoneda(ycDebeGrupo) : " · pagado") + ".",
                    new FontIcon(Feather.USERS));
            ycEtGrupo.getStyleClass().add("estadia-info");
            ycEtGrupo.setWrapText(true);
            ycEtGrupo.setMaxWidth(Double.MAX_VALUE);
            ycContenido.getChildren().add(ycEtGrupo);
        }
        if (ycE.getYcNota() != null) {
            Label ycNota = new Label(ycE.getYcNota(), new FontIcon(Feather.MESSAGE_SQUARE));
            ycNota.getStyleClass().add("estadia-info");
            ycNota.setWrapText(true);
            ycNota.setMaxWidth(Double.MAX_VALUE);
            ycContenido.getChildren().add(ycNota);
        }
        String ycQuien = ycHistorialUsuarios(ycE);
        if (!ycQuien.isEmpty()) {
            Label ycHistorial = new Label(ycQuien, new FontIcon(Feather.USER_CHECK));
            ycHistorial.getStyleClass().add("campo-ayuda");
            ycHistorial.setWrapText(true);
            ycContenido.getChildren().add(ycHistorial);
        }
        if (ycE.isYcCancelada() && ycE.getYcMotivoCancelacion() != null) {
            Label ycMotivo = new Label((ycE.ycIsNoLlego() ? "No vino" : "Cancelada")
                    + (ycE.getYcCanceladaPor() == null ? "" : " por " + ycE.getYcCanceladaPor())
                    + (ycE.getYcFechaCancelacion() == null ? ""
                    : " el " + ycE.getYcFechaCancelacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    + ". Motivo: " + ycE.getYcMotivoCancelacion(), new FontIcon(Feather.X_CIRCLE));
            ycMotivo.getStyleClass().add("estadia-info");
            ycMotivo.setWrapText(true);
            ycMotivo.setMaxWidth(Double.MAX_VALUE);
            ycContenido.getChildren().add(ycMotivo);
        }
        ycContenido.getChildren().add(ycConsumos.ycSeccion(ycE, () -> {
            ycRecargar.run();
            ycMostrar(ycService.ycObtener(ycE.getYcId()), ycRecargar, ycAlEditar);
        }));
        Label ycTotal = new Label(YCControles.ycMoneda(ycE.getYcTotal()));
        ycTotal.getStyleClass().add("estadia-total-monto");
        Label ycDetalleTotal = new Label(ycE.getYcNoches() + (ycE.getYcNoches() == 1 ? " noche" : " noches") + " × "
                + YCControles.ycMoneda(ycE.getYcPrecioNoche()) + (ycE.isYcCancelada() ? " · cancelada"
                : ycE.ycTieneSaldo() ? " · pago " + YCControles.ycMoneda(ycE.ycGetPagado()) : " · pagado completo"));
        ycDetalleTotal.getStyleClass().add("estadia-total-detalle");
        Label ycEtTotal = new Label("TOTAL");
        ycEtTotal.getStyleClass().add("estadia-total-etiqueta");
        VBox ycTextosTotal = new VBox(2, ycEtTotal, ycDetalleTotal);
        HBox.setHgrow(ycTextosTotal, Priority.ALWAYS);
        if (ycE.ycTieneSaldo()) {
            Label ycSaldo = new Label("Debe " + YCControles.ycMoneda(ycE.ycGetSaldo()));
            ycSaldo.getStyleClass().add("estadia-saldo");
            ycTextosTotal.getChildren().add(ycSaldo);
        }
        HBox ycCajaTotal = new HBox(12, ycTextosTotal, ycTotal);
        ycCajaTotal.setAlignment(Pos.CENTER_LEFT);
        ycCajaTotal.getStyleClass().add("estadia-total");
        if (ycE.ycTieneSaldo()) {
            Button ycCobrarBtn = new Button("Cobrar saldo", new FontIcon(Feather.DOLLAR_SIGN));
            ycCobrarBtn.getStyleClass().add("accent");
            ycCobrarBtn.setOnAction(ycEv -> {
                ycNotificador.ycCerrarDialogo();
                ycAcciones.ycCobrarSaldo(ycE, ycRecargar);
            });
            ycCajaTotal.getChildren().add(ycCobrarBtn);
        }
        ycContenido.getChildren().add(ycCajaTotal);

        HBox ycBotones = new HBox(8);
        ycBotones.setAlignment(Pos.CENTER_RIGHT);
        if (!ycE.isYcCancelada() && ycEstado != YCEstadia.YCEstado.YC_TERMINADA) {
            Button ycCancelarBtn = new Button("Cancelar estadia", new FontIcon(Feather.X_CIRCLE));
            ycCancelarBtn.getStyleClass().addAll("flat", "danger");
            ycSesion.ycSoloAdmin(ycCancelarBtn);
            ycCancelarBtn.setOnAction(ycEv -> {
                ycNotificador.ycCerrarDialogo();
                ycAcciones.ycCancelar(ycE, ycRecargar);
            });
            ycBotones.getChildren().add(ycCancelarBtn);
        }
        Region ycEspAcc = new Region();
        HBox.setHgrow(ycEspAcc, Priority.ALWAYS);
        ycBotones.getChildren().add(ycEspAcc);
        if (ycE.ycEsperandoLlegada(LocalDate.now())) {
            Button ycLlego = new Button("Llego", new FontIcon(Feather.CHECK));
            ycLlego.getStyleClass().add("accent");
            ycLlego.setOnAction(ycEv -> {
                ycNotificador.ycCerrarDialogo();
                ycAcciones.ycRegistrarLlegada(ycE, ycRecargar);
            });
            ycBotones.getChildren().add(ycLlego);
        } else if (ycEstado == YCEstadia.YCEstado.YC_EN_CURSO || (!ycE.isYcCancelada() && ycE.getYcSalida().equals(LocalDate.now()))) {
            Button ycSalida = new Button("Registrar salida", new FontIcon(Feather.LOG_OUT));
            ycSalida.getStyleClass().add("button-outlined");
            ycSalida.setOnAction(ycEv -> {
                ycNotificador.ycCerrarDialogo();
                ycAcciones.ycRegistrarSalida(ycE, ycRecargar);
            });
            ycBotones.getChildren().add(ycSalida);
        }
        if (!ycE.isYcCancelada()) {
            Button ycEditar = new Button("Editar", new FontIcon(Feather.EDIT_2));
            ycEditar.getStyleClass().add("flat");
            ycEditar.setOnAction(ycEv -> {
                ycNotificador.ycCerrarDialogo();
                ycAlEditar.accept(ycE);
            });
            ycBotones.getChildren().add(ycEditar);
        }
        Button ycPdf = new Button("Comprobante", new FontIcon(Feather.FILE_TEXT));
        ycPdf.getStyleClass().add("button-outlined");
        ycPdf.setOnAction(ycEv -> ycMensajes.ycVerComprobante(ycE));
        Button ycCopiar = new Button("Copiar mensaje", new FontIcon(Feather.COPY));
        ycCopiar.getStyleClass().add("button-outlined");
        ycCopiar.setOnAction(ycEv -> ycMensajes.ycCopiarConfirmacion(ycE));
        Button ycWhatsapp = new Button("WhatsApp + PDF", new FontIcon(Feather.MESSAGE_CIRCLE));
        ycWhatsapp.getStyleClass().add("success");
        ycWhatsapp.setOnAction(ycEv -> {
            ycNotificador.ycCerrarDialogo();
            ycMensajes.ycEnviarWhatsapp(ycE);
        });
        Button ycCerrar = new Button("Cerrar");
        ycCerrar.getStyleClass().add("flat");
        ycCerrar.setCancelButton(true);
        ycCerrar.setOnAction(ycEv -> ycNotificador.ycCerrarDialogo());
        ycBotones.getChildren().addAll(ycPdf, ycCopiar, ycWhatsapp, ycCerrar);
        ycContenido.getChildren().add(ycBotones);

        ycContenido.getStyleClass().addAll("dialogo", "detalle-estadia");
        ycContenido.setPrefWidth(760);
        ycContenido.setMaxSize(760, Region.USE_PREF_SIZE);
        ycNotificador.ycMostrarDialogo(ycContenido);
        Animations.zoomIn(ycContenido, Duration.millis(240)).play();
    }

    private static Node ycBloqueFecha(String ycEtiqueta, LocalDate ycFecha, String ycHora, Feather ycIcono) {
        Label ycEt = new Label(ycEtiqueta, new FontIcon(ycIcono));
        ycEt.getStyleClass().add("ficha-lateral-etiqueta");
        Label ycDia = new Label(YCControles.ycCapitalizar(ycFecha.format(YCControles.YC_FECHA_LARGA)));
        ycDia.getStyleClass().add("detalle-fecha");
        Label ycH = new Label(ycHora);
        ycH.getStyleClass().add("ficha-lateral-nota");
        VBox ycCaja = new VBox(3, ycEt, ycDia, ycH);
        ycCaja.getStyleClass().add("detalle-fecha-caja");
        HBox.setHgrow(ycCaja, Priority.ALWAYS);
        ycCaja.setMaxWidth(Double.MAX_VALUE);
        return ycCaja;
    }

    private static Node ycFlecha(int ycNoches) {
        Label ycN = new Label(ycNoches + (ycNoches == 1 ? " noche" : " noches"), new FontIcon(Feather.MOON));
        ycN.getStyleClass().add("detalle-noches");
        FontIcon ycF = new FontIcon(Feather.ARROW_RIGHT);
        ycF.getStyleClass().add("detalle-flecha");
        VBox ycCaja = new VBox(4, ycN, ycF);
        ycCaja.setAlignment(Pos.CENTER);
        return ycCaja;
    }
}
