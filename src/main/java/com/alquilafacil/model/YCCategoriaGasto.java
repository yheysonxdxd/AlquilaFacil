package com.alquilafacil.model;

public enum YCCategoriaGasto {
    YC_LUZ_EDIFICIO("Luz del edificio", "fth-zap"),
    YC_AGUA_EDIFICIO("Agua del edificio", "fth-droplet"),
    YC_INTERNET("Internet y cable", "fth-wifi"),
    YC_LIMPIEZA("Limpieza y lavanderia", "fth-feather"),
    YC_INSUMOS("Sabanas, toallas y aseo", "fth-package"),
    YC_MANTENIMIENTO("Mantenimiento y arreglos", "fth-tool"),
    YC_PERSONAL("Personal y recepcion", "fth-users"),
    YC_IMPUESTOS("Impuestos y arbitrios", "fth-file-text"),
    YC_PUBLICIDAD("Publicidad", "fth-volume-2"),
    YC_OTROS("Otros", "fth-more-horizontal");

    private final String ycTexto;
    private final String ycIcono;

    YCCategoriaGasto(String ycTexto, String ycIcono) {
        this.ycTexto = ycTexto;
        this.ycIcono = ycIcono;
    }

    public String ycGetTexto() {
        return ycTexto;
    }

    public String ycGetIcono() {
        return ycIcono;
    }

    @Override
    public String toString() {
        return ycTexto;
    }
}
