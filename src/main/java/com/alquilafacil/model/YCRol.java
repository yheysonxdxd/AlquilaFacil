package com.alquilafacil.model;

public enum YCRol {
    YC_ADMINISTRADOR("Administrador", "Todo: incluye borrar, anular recibos, gastos, reportes y usuarios"),
    YC_RECEPCION("Recepcion", "Registra inquilinos, cobros, estadias y salidas. No ve cuanto gana el negocio, no cambia precios ni cuartos, no borra ni anula, y no ve gastos ni reportes");

    private final String ycTexto;
    private final String ycDetalle;

    YCRol(String ycTexto, String ycDetalle) {
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
