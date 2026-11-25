package com.alquilafacil.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public final class YCConvertidoresEnum {

    public static final String YC_PREFIJO = "YC_";

    private YCConvertidoresEnum() {
    }

    public static String ycNombreDb(Enum<?> ycValor) {
        String ycNombre = ycValor.name();
        return ycNombre.startsWith(YC_PREFIJO) ? ycNombre.substring(YC_PREFIJO.length()) : ycNombre;
    }

    public static <E extends Enum<E>> E ycDesdeDb(Class<E> ycTipo, String ycTexto) {
        String ycNombre = ycTexto.trim();
        return Enum.valueOf(ycTipo, ycNombre.startsWith(YC_PREFIJO) ? ycNombre : YC_PREFIJO + ycNombre);
    }

    public abstract static class YCConvertidorBase<E extends Enum<E>> implements AttributeConverter<E, String> {

        private final Class<E> ycTipo;

        protected YCConvertidorBase(Class<E> ycTipo) {
            this.ycTipo = ycTipo;
        }

        @Override
        public String convertToDatabaseColumn(E ycValor) {
            return ycValor == null ? null : ycNombreDb(ycValor);
        }

        @Override
        public E convertToEntityAttribute(String ycTexto) {
            return ycTexto == null || ycTexto.isBlank() ? null : ycDesdeDb(ycTipo, ycTexto);
        }
    }

    @Converter
    public static class YCConvertidorMetodoPago extends YCConvertidorBase<YCMetodoPago> {
        public YCConvertidorMetodoPago() {
            super(YCMetodoPago.class);
        }
    }

    @Converter
    public static class YCConvertidorCanalReserva extends YCConvertidorBase<YCCanalReserva> {
        public YCConvertidorCanalReserva() {
            super(YCCanalReserva.class);
        }
    }

    @Converter
    public static class YCConvertidorCategoriaGasto extends YCConvertidorBase<YCCategoriaGasto> {
        public YCConvertidorCategoriaGasto() {
            super(YCCategoriaGasto.class);
        }
    }

    @Converter
    public static class YCConvertidorEstadoLimpieza extends YCConvertidorBase<YCEstadoLimpieza> {
        public YCConvertidorEstadoLimpieza() {
            super(YCEstadoLimpieza.class);
        }
    }

    @Converter
    public static class YCConvertidorModalidad extends YCConvertidorBase<YCModalidad> {
        public YCConvertidorModalidad() {
            super(YCModalidad.class);
        }
    }

    @Converter
    public static class YCConvertidorNegocio extends YCConvertidorBase<YCNegocio> {
        public YCConvertidorNegocio() {
            super(YCNegocio.class);
        }
    }

    @Converter
    public static class YCConvertidorRol extends YCConvertidorBase<YCRol> {
        public YCConvertidorRol() {
            super(YCRol.class);
        }
    }

    @Converter
    public static class YCConvertidorTipoHabitacion extends YCConvertidorBase<YCTipoHabitacion> {
        public YCConvertidorTipoHabitacion() {
            super(YCTipoHabitacion.class);
        }
    }
}
