package com.alquilafacil.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class YCNumeroALetras {
    private static final String[] YC_UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE",
            "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO",
            "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] YC_DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA",
            "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] YC_CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
            "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private YCNumeroALetras() {
    }

    public static String ycImporte(BigDecimal ycValor) {
        BigDecimal ycRedondeado = ycValor.setScale(2, RoundingMode.HALF_UP);
        long ycEntero = ycRedondeado.longValue();
        int ycCentimos = ycRedondeado.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String ycLetras = ycEntero == 0 ? "CERO" : ycConvertir(ycEntero);
        return ycLetras + " CON " + String.format("%02d", ycCentimos) + "/100 SOLES";
    }

    static String ycConvertir(long ycN) {
        if (ycN >= 1_000_000) {
            long ycMillones = ycN / 1_000_000;
            String ycPrefijo = ycMillones == 1 ? "UN MILLON" : ycApocopar(ycConvertir(ycMillones)) + " MILLONES";
            long ycResto = ycN % 1_000_000;
            return ycResto == 0 ? ycPrefijo : ycPrefijo + " " + ycConvertir(ycResto);
        }
        if (ycN >= 1000) {
            long ycMiles = ycN / 1000;
            String ycPrefijo = ycMiles == 1 ? "MIL" : ycApocopar(ycConvertir(ycMiles)) + " MIL";
            long ycResto = ycN % 1000;
            return ycResto == 0 ? ycPrefijo : ycPrefijo + " " + ycConvertir(ycResto);
        }
        return ycMenorQueMil((int) ycN);
    }

    private static String ycMenorQueMil(int ycN) {
        if (ycN == 100) {
            return "CIEN";
        }
        int ycC = ycN / 100;
        int ycResto = ycN % 100;
        String ycCentena = YC_CENTENAS[ycC];
        String ycDecena;
        if (ycResto < 30) {
            ycDecena = YC_UNIDADES[ycResto];
        } else {
            ycDecena = YC_DECENAS[ycResto / 10] + (ycResto % 10 == 0 ? "" : " Y " + YC_UNIDADES[ycResto % 10]);
        }
        return (ycCentena + " " + ycDecena).trim();
    }

    private static String ycApocopar(String ycTexto) {
        return ycTexto.endsWith("UNO") ? ycTexto.substring(0, ycTexto.length() - 1) : ycTexto;
    }
}
