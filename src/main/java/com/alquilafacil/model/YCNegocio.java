package com.alquilafacil.model;

public enum YCNegocio {
    YC_MENSUAL("Alquiler mensual"),
    YC_HOSPEDAJE("Hospedaje"),
    YC_GENERAL("General (los dos)");

    private final String ycTexto;

    YCNegocio(String ycTexto) {
        this.ycTexto = ycTexto;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    @Override
    public String toString() {
        return ycTexto;
    }
}
