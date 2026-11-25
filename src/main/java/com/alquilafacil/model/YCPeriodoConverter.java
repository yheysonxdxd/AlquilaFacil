package com.alquilafacil.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

@Converter
public class YCPeriodoConverter implements AttributeConverter<YearMonth, String> {
    @Override
    public String convertToDatabaseColumn(YearMonth ycPeriodo) {
        return ycPeriodo == null ? null : ycPeriodo.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String ycTexto) {
        return ycTexto == null || ycTexto.isBlank() ? null : YearMonth.parse(ycTexto);
    }
}
