package com.alquilafacil.controller;

import com.alquilafacil.service.YCPagoDigitalService;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class YCCobroYape {
    private final YCPagoDigitalService ycPagoDigital;

    String ycFrase() {
        String ycTexto = ycPagoDigital.ycTexto();
        return ycTexto.isEmpty() ? "" : "Puede pagar por " + ycTexto + " y enviarnos la captura. ";
    }

    boolean ycCopiarQr() {
        return ycPagoDigital.ycQr().map(ycRuta -> {
            ClipboardContent ycCc = new ClipboardContent();
            ycCc.putFiles(List.of(ycRuta.toFile()));
            Clipboard.getSystemClipboard().setContent(ycCc);
            return true;
        }).orElse(false);
    }
}
