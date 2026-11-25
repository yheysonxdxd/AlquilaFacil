package com.alquilafacil.model;

public enum YCModalidad {
    YC_MENSUAL("Alquiler mensual", "Por mes", "/mes", "mes"),
    YC_POR_NOCHE("Hospedaje", "Por noche", "/noche", "noche");

    private final String ycTexto;
    private final String ycCorto;
    private final String ycSufijo;
    private final String ycUnidad;

    YCModalidad(String ycTexto, String ycCorto, String ycSufijo, String ycUnidad) {
        this.ycTexto = ycTexto;
        this.ycCorto = ycCorto;
        this.ycSufijo = ycSufijo;
        this.ycUnidad = ycUnidad;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetCorto() {
        return ycCorto;
    }

    public String ycGetSufijo() {
        return ycSufijo;
    }

    public String ycGetUnidad() {
        return ycUnidad;
    }

    @Override
    public String toString() {
        return ycTexto;
    }
}
