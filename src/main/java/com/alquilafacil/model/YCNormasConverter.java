package com.alquilafacil.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class YCNormasConverter implements AttributeConverter<Set<YCNorma>, String> {
    @Override
    public String convertToDatabaseColumn(Set<YCNorma> ycNormas) {
        if (ycNormas == null || ycNormas.isEmpty()) {
            return null;
        }
        return ycNormas.stream().map(YCConvertidoresEnum::ycNombreDb).collect(Collectors.joining(","));
    }

    @Override
    public Set<YCNorma> convertToEntityAttribute(String ycTexto) {
        Set<YCNorma> ycResultado = EnumSet.noneOf(YCNorma.class);
        if (ycTexto == null || ycTexto.isBlank()) {
            return ycResultado;
        }
        Arrays.stream(ycTexto.split(",")).map(String::trim).forEach(ycNombre -> {
            try {
                ycResultado.add(YCConvertidoresEnum.ycDesdeDb(YCNorma.class, ycNombre));
            } catch (IllegalArgumentException ycIgnorada) {
            }
        });
        return ycResultado;
    }
}
