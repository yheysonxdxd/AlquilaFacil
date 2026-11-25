package com.alquilafacil.model;

public enum YCMetodoPago {
    YC_EFECTIVO("Efectivo"),
    YC_YAPE("Yape"),
    YC_PLIN("Plin"),
    YC_TRANSFERENCIA("Transferencia");

    private final String ycDescripcion;

    YCMetodoPago(String ycDescripcion) {
        this.ycDescripcion = ycDescripcion;
    }

    public String ycGetDescripcion() {
        return ycDescripcion;
    }

    @Override
    public String toString() {
        return ycDescripcion;
    }
}
