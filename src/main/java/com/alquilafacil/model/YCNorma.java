package com.alquilafacil.model;

public enum YCNorma {
    YC_NO_FUMAR("No fumar", "fth-slash"),
    YC_NO_MASCOTAS("No se aceptan mascotas", "fth-x-circle"),
    YC_MASCOTAS("Mascotas permitidas", "fth-heart"),
    YC_SIN_FIESTAS("Sin fiestas ni eventos", "fth-music"),
    YC_SILENCIO("Silencio desde las 22:00", "fth-volume-x"),
    YC_SOLO_REGISTRADOS("Solo huéspedes registrados", "fth-user-check"),
    YC_DOCUMENTO("Presentar DNI o pasaporte", "fth-credit-card"),
    YC_PAGO_ADELANTADO("Pago al registrarse", "fth-dollar-sign");

    private final String ycTexto;
    private final String ycIcono;

    YCNorma(String ycTexto, String ycIcono) {
        this.ycTexto = ycTexto;
        this.ycIcono = ycIcono;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetIcono() {
        return ycIcono;
    }
}
