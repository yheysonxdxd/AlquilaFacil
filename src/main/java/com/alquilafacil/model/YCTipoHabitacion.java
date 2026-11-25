package com.alquilafacil.model;

public enum YCTipoHabitacion {
    YC_SIMPLE("Simple", "Para una persona"),
    YC_DOBLE("Doble", "Dos camas separadas"),
    YC_MATRIMONIAL("Matrimonial", "Una cama grande para dos"),
    YC_TRIPLE("Triple", "Para tres personas"),
    YC_FAMILIAR("Familiar", "Para familias o grupos"),
    YC_COMPARTIDA("Compartida", "Camarotes, estilo hostal"),
    YC_SUITE("Suite", "Amplia, con ambientes separados");

    private final String ycTexto;
    private final String ycDetalle;

    YCTipoHabitacion(String ycTexto, String ycDetalle) {
        this.ycTexto = ycTexto;
        this.ycDetalle = ycDetalle;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetDetalle() {
        return ycDetalle;
    }

    @Override
    public String toString() {
        return ycTexto;
    }
}
