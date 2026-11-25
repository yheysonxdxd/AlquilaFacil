package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.model.YCUsuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YCSesionTest {
    private static YCUsuario ycUsuario(YCRol ycRol) {
        return YCUsuario.builder().ycId(1L).ycUsuario("prueba").ycRol(ycRol).build();
    }

    @Test
    void ycSinSesionNoSePermiteNadaDeAdministrador() {
        YCSesion ycS = new YCSesion();
        assertThrows(YCNegocioException.class, () -> ycS.ycExigirAdmin("anular recibos"));
        assertFalse(ycS.ycAdminProperty().get());
    }

    @Test
    void ycRecepcionNoPuedeAccionesDeAdministrador() {
        YCSesion ycS = new YCSesion();
        ycS.ycIniciar(ycUsuario(YCRol.YC_RECEPCION));
        assertThrows(YCNegocioException.class, () -> ycS.ycExigirAdmin("anular recibos"));
    }

    @Test
    void ycAdministradorSiPuedeYAlCerrarSesionPierdeElPermiso() {
        YCSesion ycS = new YCSesion();
        ycS.ycIniciar(ycUsuario(YCRol.YC_ADMINISTRADOR));
        assertDoesNotThrow(() -> ycS.ycExigirAdmin("anular recibos"));
        assertTrue(ycS.ycAdminProperty().get());
        ycS.ycCerrar();
        assertThrows(YCNegocioException.class, () -> ycS.ycExigirAdmin("anular recibos"));
        assertFalse(ycS.ycAdminProperty().get());
    }
}
