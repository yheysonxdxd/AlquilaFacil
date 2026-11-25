package com.alquilafacil.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class YCEstrategiaNombres extends CamelCaseToUnderscoresNamingStrategy {

    private static final String YC_PREFIJO = "yc_";

    @Override
    public Identifier toPhysicalColumnName(Identifier ycNombre, JdbcEnvironment ycEntorno) {
        Identifier ycFisico = super.toPhysicalColumnName(ycNombre, ycEntorno);
        if (ycFisico == null) {
            return null;
        }
        String ycTexto = ycFisico.getText();
        if (ycTexto.toLowerCase().startsWith(YC_PREFIJO)) {
            return Identifier.toIdentifier(ycTexto.substring(YC_PREFIJO.length()), ycFisico.isQuoted());
        }
        return ycFisico;
    }
}
