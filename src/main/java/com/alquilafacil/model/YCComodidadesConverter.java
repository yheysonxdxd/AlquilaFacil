package com.alquilafacil.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class YCComodidadesConverter implements AttributeConverter<Set<YCComodidad>, String> {
    @Override
    public String convertToDatabaseColumn(Set<YCComodidad> ycComodidades) {
        if (ycComodidades == null || ycComodidades.isEmpty()) {
            return null;
        }
        return ycComodidades.stream().map(YCConvertidoresEnum::ycNombreDb).collect(Collectors.joining(","));
    }

    @Override
    public Set<YCComodidad> convertToEntityAttribute(String ycTexto) {
        Set<YCComodidad> ycResultado = EnumSet.noneOf(YCComodidad.class);
        if (ycTexto == null || ycTexto.isBlank()) {
            return ycResultado;
        }
        Arrays.stream(ycTexto.split(","))
                .map(String::trim)
                .forEach(ycNombre -> {
                    try {
                        ycResultado.add(YCConvertidoresEnum.ycDesdeDb(YCComodidad.class, ycNombre));
                    } catch (IllegalArgumentException ycIgnorada) {
                    }
                });
        return ycResultado;
    }
}
