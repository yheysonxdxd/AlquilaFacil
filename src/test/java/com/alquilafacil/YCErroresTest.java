package com.alquilafacil;

import com.alquilafacil.exception.YCNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YCErroresTest {
    @Test
    void ycEncuentraElAvisoDelNegocioAunqueVengaEnvuelto() {
        YCNegocioException ycAviso = new YCNegocioException("Escribe el motivo");
        assertSame(ycAviso, YCJavaFxApplication.ycBuscarNegocio(new RuntimeException("envoltura", ycAviso)));
        assertNull(YCJavaFxApplication.ycBuscarNegocio(new IllegalStateException("falla real")));
        assertNull(YCJavaFxApplication.ycBuscarNegocio(null));
    }
}
