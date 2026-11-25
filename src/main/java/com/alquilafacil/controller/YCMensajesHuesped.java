package com.alquilafacil.controller;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.service.YCComprobanteHospedajeService;
import com.alquilafacil.service.YCEstadiaService;
import com.alquilafacil.ui.YCExportador;
import com.alquilafacil.ui.YCNotificador;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class YCMensajesHuesped {
    private static final File YC_CARPETA_COMPROBANTES = new File(System.getProperty("app.carpeta", "."), "comprobantes");

    private final YCEstadiaService ycService;
    private final YCNotificador ycNotificador;
    private final YCExportador ycExportador;
    private final YCDatosArrendador ycArrendador;
    private final YCComprobanteHospedajeService ycComprobanteService;
    private final YCCobroYape ycCobroYape;

    void ycPreguntarLlegada(YCEstadia ycE) {
        StringBuilder ycSb = new StringBuilder("Hola ").append(ycE.getYcHuesped().split(" ")[0]).append(", le saluda ")
                .append(ycArrendador.getYcNombreHospedaje()).append(". Tenemos su reserva ").append(ycE.ycGetNumero())
                .append(" en ").append(ycE.getYcCuartoNumero()).append(" desde el ").append(YCControles.ycFecha(ycE.getYcEntrada()))
                .append(". ¿Sigue en pie su llegada? Por favor indiquenos a que hora llegaria para tener lista su habitacion. ");
        if (ycArrendador.getYcDireccion() != null && !ycArrendador.getYcDireccion().isBlank()) {
            ycSb.append("Direccion: ").append(ycArrendador.getYcDireccion()).append(". ");
        }
        ycSb.append("¡Gracias!");
        String ycCelular = ycE.getYcTelefono().replaceAll("\\D", "");
        ycExportador.ycAbrirEnlace("https://wa.me/51" + ycCelular + "?text="
                + URLEncoder.encode(ycSb.toString(), StandardCharsets.UTF_8).replace("+", "%20"));
        ycNotificador.ycExito("Se abrio WhatsApp con " + ycE.getYcHuesped().split(" ")[0] + ". Solo dale Enviar.");
    }

    void ycEnviarRecordatorio(YCEstadia ycE, Runnable ycRecargar) {
        if (!ycTieneCelular(ycE)) {
            ycNotificador.ycAdvertencia(ycE.getYcHuesped() + " no tiene celular registrado.");
            return;
        }
        YCCuarto ycC = ycE.getYcCuarto();
        StringBuilder ycSb = new StringBuilder("Hola ").append(ycE.getYcHuesped().split(" ")[0]).append(", le saluda ")
                .append(ycArrendador.getYcNombreHospedaje()).append(". Le recordamos su reserva ").append(ycE.ycGetNumero())
                .append(" para manana ").append(YCControles.ycFecha(ycE.getYcEntrada())).append(" en ").append(ycE.getYcCuartoNumero())
                .append(". Puede llegar desde las ").append(ycC.ycGetHoraEntradaODefecto()).append(" y la salida es el ")
                .append(YCControles.ycFecha(ycE.getYcSalida())).append(" hasta las ").append(ycC.ycGetHoraSalidaODefecto()).append(". ");
        if (ycE.ycTieneSaldo()) {
            ycSb.append("Queda un saldo de ").append(YCControles.ycMoneda(ycE.ycGetSaldo())).append(" a pagar al llegar. ")
                    .append(ycCobroYape.ycFrase());
        }
        if (ycArrendador.getYcDireccion() != null && !ycArrendador.getYcDireccion().isBlank()) {
            ycSb.append("Direccion: ").append(ycArrendador.getYcDireccion()).append(". ");
        }
        ycSb.append("Si hay algun cambio, por favor avisenos. ¡Le esperamos!");
        String ycCelular = ycE.getYcTelefono().replaceAll("\\D", "");
        ycExportador.ycAbrirEnlace("https://wa.me/51" + ycCelular + "?text="
                + URLEncoder.encode(ycSb.toString(), StandardCharsets.UTF_8).replace("+", "%20"));
        ycService.ycMarcarRecordatorio(ycE.getYcId());
        ycNotificador.ycExito("Se abrio WhatsApp con el recordatorio para " + ycE.getYcHuesped() + ". Solo dale Enviar.");
        ycRecargar.run();
    }

    void ycCopiarConfirmacion(YCEstadia ycE) {
        ClipboardContent ycCc = new ClipboardContent();
        ycCc.putString(ycMensajeConfirmacion(ycE));
        Clipboard.getSystemClipboard().setContent(ycCc);
        ycNotificador.ycExito("Confirmacion copiada. Pegala en WhatsApp o SMS con Ctrl+V.");
    }

    void ycVerComprobante(YCEstadia ycE) {
        ycExportador.ycAbrirTemporal(YCComprobanteHospedajeService.ycNombreArchivo(ycE), YCExportador.YCFormato.YC_PDF,
                ycDestino -> ycComprobanteService.ycGenerar(ycE.getYcId(), ycDestino));
    }

    static boolean ycTieneCelular(YCEstadia ycE) {
        return ycE.getYcTelefono() != null && ycE.getYcTelefono().replaceAll("\\D", "").length() == 9;
    }

    void ycEnviarWhatsapp(YCEstadia ycE) {
        if (!ycTieneCelular(ycE)) {
            ycNotificador.ycAdvertencia(ycE.getYcHuesped() + " no tiene un celular registrado. Usa \"Copiar mensaje\" en el detalle.");
            return;
        }
        String ycCelular = ycE.getYcTelefono().replaceAll("\\D", "");
        File ycDestino = new File(YC_CARPETA_COMPROBANTES, YCComprobanteHospedajeService.ycNombreArchivo(ycE) + ".pdf");
        ycExportador.ycGenerar(ycDestino, ycF -> ycComprobanteService.ycGenerar(ycE.getYcId(), ycF), () -> {
            ClipboardContent ycCc = new ClipboardContent();
            ycCc.putFiles(List.of(ycDestino.getAbsoluteFile()));
            Clipboard.getSystemClipboard().setContent(ycCc);
            String ycTexto = URLEncoder.encode(ycMensajeConfirmacion(ycE), StandardCharsets.UTF_8).replace("+", "%20");
            ycExportador.ycAbrirEnlace("https://wa.me/51" + ycCelular + "?text=" + ycTexto);
            ycNotificador.ycExitoConAcciones("Se abrio WhatsApp con " + ycE.getYcHuesped().split(" ")[0],
                    "El mensaje ya esta escrito y el comprobante PDF quedo copiado. En el chat presiona "
                            + "Ctrl+V para adjuntarlo y luego Enviar.\n\nTambien quedo guardado en la carpeta "
                            + "\"comprobantes\" del programa.",
                    "Abrir carpeta", () -> ycExportador.ycAbrirArchivo(YC_CARPETA_COMPROBANTES.getAbsoluteFile()),
                    "Entendido", () -> { });
        });
    }

    String ycMensajeConfirmacion(YCEstadia ycE) {
        YCCuarto ycC = ycE.getYcCuarto();
        String ycNombre = ycE.getYcHuesped().split(" ")[0];
        StringBuilder ycSb = new StringBuilder("Hola ").append(ycNombre).append(", le saluda ")
                .append(ycArrendador.getYcNombreHospedaje()).append(". ");
        List<YCEstadia> ycGrupo = ycService.ycDelGrupo(ycE).stream().filter(ycX -> !ycX.isYcCancelada()).toList();
        if (ycGrupo.size() > 1) {
            ycMensajeGrupo(ycSb, ycE, ycGrupo);
            return ycSb.toString();
        }
        ycSb.append(ycE.ycEstado(LocalDate.now()) == YCEstadia.YCEstado.YC_PROXIMA ? "Le confirmamos su reserva " : "Detalle de su estadia ")
                .append(ycE.ycGetNumero()).append(" en ").append(ycE.getYcCuartoNumero()).append(": ");
        ycSb.append("entrada el ").append(YCControles.ycFecha(ycE.getYcEntrada())).append(" desde las ").append(ycC.ycGetHoraEntradaODefecto())
                .append(", salida el ").append(YCControles.ycFecha(ycE.getYcSalida())).append(" hasta las ").append(ycC.ycGetHoraSalidaODefecto())
                .append(" (").append(ycE.getYcNoches()).append(ycE.getYcNoches() == 1 ? " noche" : " noches").append(", ")
                .append(ycE.getYcPersonas()).append(ycE.getYcPersonas() == 1 ? " persona" : " personas").append("). ");
        if (ycE.ycTieneSaldo()) {
            ycSb.append("Total: ").append(YCControles.ycMoneda(ycE.getYcTotal())).append(". Adelanto recibido: ")
                    .append(YCControles.ycMoneda(ycE.ycGetPagado())).append(" (").append(ycE.getYcMetodo()).append("). Saldo a pagar al llegar: ")
                    .append(YCControles.ycMoneda(ycE.ycGetSaldo())).append(". ").append(ycCobroYape.ycFrase());
        } else {
            ycSb.append("Total: ").append(YCControles.ycMoneda(ycE.getYcTotal())).append(", pagado con ").append(ycE.getYcMetodo()).append(". ");
        }
        if (ycArrendador.getYcDireccion() != null && !ycArrendador.getYcDireccion().isBlank()) {
            ycSb.append("Direccion: ").append(ycArrendador.getYcDireccion()).append(". ");
        }
        if (!ycC.ycGetNormas().isEmpty()) {
            ycSb.append("Recuerde: ").append(String.join(", ", ycC.ycGetNormas().stream()
                    .map(ycN -> ycN.ycGetTexto().toLowerCase()).toList())).append(". ");
        }
        ycSb.append("Le adjunto su comprobante en PDF. ");
        ycSb.append(ycE.ycEstado(LocalDate.now()) == YCEstadia.YCEstado.YC_PROXIMA ? "¡Le esperamos!" : "¡Gracias por su preferencia!");
        return ycSb.toString();
    }

    private void ycMensajeGrupo(StringBuilder ycSb, YCEstadia ycE, List<YCEstadia> ycGrupo) {
        YCCuarto ycC = ycE.getYcCuarto();
        BigDecimal ycTotal = ycGrupo.stream().map(YCEstadia::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ycPagado = ycGrupo.stream().map(YCEstadia::ycGetPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ycSaldo = ycGrupo.stream().map(YCEstadia::ycGetSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        int ycPersonas = ycGrupo.stream().mapToInt(YCEstadia::getYcPersonas).sum();
        boolean ycProxima = ycE.ycEstado(LocalDate.now()) == YCEstadia.YCEstado.YC_PROXIMA;
        ycSb.append(ycProxima ? "Le confirmamos su reserva " : "Detalle de su reserva ").append(ycE.ycGetNumero())
                .append(" de ").append(ycGrupo.size()).append(" habitaciones (").append(ycPersonas)
                .append(ycPersonas == 1 ? " persona): " : " personas): ")
                .append(String.join(", ", ycGrupo.stream().map(YCEstadia::getYcCuartoNumero).toList())).append(". ");
        ycSb.append("Entrada el ").append(YCControles.ycFecha(ycE.getYcEntrada())).append(" desde las ").append(ycC.ycGetHoraEntradaODefecto())
                .append(", salida el ").append(YCControles.ycFecha(ycE.getYcSalida())).append(" hasta las ").append(ycC.ycGetHoraSalidaODefecto())
                .append(" (").append(ycE.getYcNoches()).append(ycE.getYcNoches() == 1 ? " noche). " : " noches). ");
        ycSb.append("Total: ").append(YCControles.ycMoneda(ycTotal)).append(". ");
        if (ycSaldo.signum() > 0) {
            ycSb.append("Pagado: ").append(YCControles.ycMoneda(ycPagado)).append(". Saldo a pagar al llegar: ")
                    .append(YCControles.ycMoneda(ycSaldo)).append(". ").append(ycCobroYape.ycFrase());
        }
        if (ycArrendador.getYcDireccion() != null && !ycArrendador.getYcDireccion().isBlank()) {
            ycSb.append("Direccion: ").append(ycArrendador.getYcDireccion()).append(". ");
        }
        ycSb.append("Le adjunto su comprobante en PDF. ").append(ycProxima ? "¡Le esperamos!" : "¡Gracias por su preferencia!");
    }
}
