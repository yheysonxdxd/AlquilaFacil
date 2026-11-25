package com.alquilafacil.model;

public enum YCEstadoLimpieza {
    YC_LIMPIA("Limpia", "fth-check-circle", "limpia"),
    YC_POR_LIMPIAR("Por limpiar", "fth-wind", "por-limpiar"),
    YC_MANTENIMIENTO("En mantenimiento", "fth-tool", "mantenimiento");

    private final String ycTexto;
    private final String ycIcono;
    private final String ycClase;

    YCEstadoLimpieza(String ycTexto, String ycIcono, String ycClase) {
        this.ycTexto = ycTexto;
        this.ycIcono = ycIcono;
        this.ycClase = ycClase;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetIcono() {
        return ycIcono;
    }

    public String ycGetClase() {
        return ycClase;
    }
}
