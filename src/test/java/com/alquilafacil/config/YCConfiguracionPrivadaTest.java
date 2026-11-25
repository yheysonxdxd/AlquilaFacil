package com.alquilafacil.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YCConfiguracionPrivadaTest {
    @Configuration
    static class YCVacia {
    }

    @TempDir
    Path ycTmp;

    @Test
    void ycLeeElTokenDelArchivoPrivado() throws Exception {
        Files.writeString(ycTmp.resolve("configuracion.properties"), "app.dni.token=sk_prueba123\n");
        try (ConfigurableApplicationContext ycCtx = new SpringApplicationBuilder(YCVacia.class)
                .web(WebApplicationType.NONE)
                .run("--app.carpeta=" + ycTmp.toString().replace('\\', '/'), "--logging.file.name=")) {
            assertEquals("sk_prueba123", ycCtx.getEnvironment().getProperty("app.dni.token"));
        }
    }

    @Test
    void ycSinArchivoPrivadoNoHayToken() {
        try (ConfigurableApplicationContext ycCtx = new SpringApplicationBuilder(YCVacia.class)
                .web(WebApplicationType.NONE)
                .run("--app.carpeta=" + ycTmp.toString().replace('\\', '/'), "--logging.file.name=")) {
            assertEquals("",ycCtx.getEnvironment().getProperty("app.dni.token"));
        }
    }
}
