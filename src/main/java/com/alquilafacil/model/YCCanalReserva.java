package com.alquilafacil.model;

public enum YCCanalReserva {
    YC_EN_PERSONA("En persona"),
    YC_LLAMADA("Llamada"),
    YC_WHATSAPP("WhatsApp"),
    YC_REDES("Facebook / redes"),
    YC_RECOMENDACION("Recomendacion"),
    YC_PLATAFORMA("Booking / Airbnb"),
    YC_OTRO("Otro");

    private final String ycTexto;

    YCCanalReserva(String ycTexto) {
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
