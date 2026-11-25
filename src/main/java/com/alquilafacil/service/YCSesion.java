package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCUsuario;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class YCSesion {
    private YCUsuario ycUsuario;
    private final List<Runnable> ycAlCambiar = new ArrayList<>();

    private final SimpleBooleanProperty ycAdmin = new SimpleBooleanProperty(false);

    public YCUsuario ycGetUsuario() {
        return ycUsuario;
    }

    public boolean ycIniciada() {
        return ycUsuario != null;
    }

    public boolean ycEsAdmin() {
        return ycUsuario != null && ycUsuario.ycEsAdmin();
    }

    public ReadOnlyBooleanProperty ycAdminProperty() {
        return ycAdmin;
    }

    public String ycNombreUsuario() {
        return ycUsuario == null ? "sistema" : ycUsuario.getYcUsuario();
    }

    void ycIniciar(YCUsuario ycU) {
        ycUsuario = ycU;
        ycAdmin.set(ycU.ycEsAdmin());
        ycAlCambiar.forEach(Runnable::run);
    }

    public void ycCerrar() {
        ycUsuario = null;
        ycAdmin.set(false);
        ycAlCambiar.forEach(Runnable::run);
    }

    public void ycAlCambiar(Runnable ycR) {
        ycAlCambiar.add(ycR);
    }

    public void ycSoloAdmin(Node... ycNodos) {
        for (Node ycN : ycNodos) {
            ycN.visibleProperty().bind(ycAdmin);
            ycN.managedProperty().bind(ycAdmin);
        }
    }

    public void ycExigirAdmin(String ycAccion) {
        if (ycUsuario == null) {
            throw new YCNegocioException("Inicia sesion como administrador para " + ycAccion + ".");
        }
        if (!ycUsuario.ycEsAdmin()) {
            throw new YCNegocioException("Solo el administrador puede " + ycAccion + ".");
        }
    }
}
