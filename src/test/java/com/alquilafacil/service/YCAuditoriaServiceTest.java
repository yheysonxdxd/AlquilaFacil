package com.alquilafacil.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class YCAuditoriaServiceTest {
    @Test
    void ycDescribeSoloLoQueCambio() {
        var ycC = new YCAuditoriaService.YCCambios()
                .ycCampo("precio", new BigDecimal("350"), new BigDecimal("380"))
                .ycCampo("piso", 1, 1)
                .ycCampo("nombre", null, "Cuarto 101");
        assertTrue(ycC.ycHay());
        assertEquals("precio S/ 350.00 → S/ 380.00 · nombre — → Cuarto 101", ycC.toString());
        assertFalse(new YCAuditoriaService.YCCambios().ycCampo("x", "a", "a").ycHay());
    }
}
