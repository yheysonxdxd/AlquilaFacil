package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;

final class YCMotivos {
    static final int YC_MINIMO = 5;
    static final int YC_MAXIMO = 200;

    private YCMotivos() {
    }

    static String ycExigir(String ycMotivo, String ycAccion) {
        String ycM = ycMotivo == null ? "" : ycMotivo.trim().replaceAll("\\s+", " ");
        if (ycM.length() < YC_MINIMO) {
            throw new YCNegocioException("Escribe el motivo para " + ycAccion + " (minimo " + YC_MINIMO + " letras).");
        }
        return ycM.length() > YC_MAXIMO ? ycM.substring(0, YC_MAXIMO) : ycM;
    }
}
