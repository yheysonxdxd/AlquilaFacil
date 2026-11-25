package com.alquilafacil.model;

public enum YCComodidad {
    YC_BANO_PROPIO("Baño propio", "fth-droplet", true, true),
    YC_AGUA_CALIENTE("Agua caliente", "fth-thermometer", true, true),
    YC_WIFI("Wifi", "fth-wifi", true, true),
    YC_AMOBLADO("Amoblado", "fth-box", true, true),
    YC_CAMA("Cama", "fth-moon", true, false),
    YC_ROPERO("Ropero", "fth-archive", true, true),
    YC_ESCRITORIO("Escritorio", "fth-monitor", true, true),
    YC_TELEVISOR("Televisor", "fth-tv", true, true),
    YC_COCINA("Cocina", "fth-coffee", true, false),
    YC_BALCON("Balcón", "fth-sun", true, true),
    YC_VENTANA_CALLE("Ventana a la calle", "fth-maximize", true, true),
    YC_ENTRADA_INDEPENDIENTE("Entrada independiente", "fth-key", true, false),
    YC_TOALLAS("Toallas y sábanas", "fth-layers", false, true),
    YC_ARTICULOS_ASEO("Artículos de aseo", "fth-package", false, true),
    YC_DESAYUNO("Desayuno incluido", "fth-sunrise", false, true),
    YC_AIRE_ACONDICIONADO("Aire acondicionado", "fth-wind", false, true),
    YC_TV_CABLE("TV cable / Netflix", "fth-film", false, true),
    YC_FRIGOBAR("Frigobar", "fth-shopping-bag", false, true),
    YC_LIMPIEZA_DIARIA("Limpieza diaria", "fth-feather", false, true),
    YC_ESTACIONAMIENTO("Estacionamiento", "fth-truck", false, true),
    YC_RECEPCION_24H("Recepción 24 h", "fth-bell", false, true),
    YC_CAJA_FUERTE("Caja fuerte", "fth-lock", false, true);

    private final String ycTexto;
    private final String ycIcono;
    private final boolean ycMensual;
    private final boolean ycHospedaje;

    YCComodidad(String ycTexto, String ycIcono, boolean ycMensual, boolean ycHospedaje) {
        this.ycTexto = ycTexto;
        this.ycIcono = ycIcono;
        this.ycMensual = ycMensual;
        this.ycHospedaje = ycHospedaje;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetIcono() {
        return ycIcono;
    }

    public boolean ycIsMensual() {
        return ycMensual;
    }

    public boolean ycIsHospedaje() {
        return ycHospedaje;
    }
}
