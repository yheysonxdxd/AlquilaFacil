package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YCRespaldoServiceTest {
    @TempDir
    Path ycTmp;

    private final java.util.List<SingleConnectionDataSource> ycAbiertas = new java.util.ArrayList<>();

    @org.junit.jupiter.api.AfterEach
    void ycCerrar() {
        ycAbiertas.forEach(SingleConnectionDataSource::destroy);
    }

    private static YCSesion ycAdmin() {
        YCSesion ycS = new YCSesion();
        ycS.ycIniciar(com.alquilafacil.model.YCUsuario.builder().ycId(1L).ycUsuario("admin")
                .ycRol(com.alquilafacil.model.YCRol.YC_ADMINISTRADOR).build());
        return ycS;
    }

    private JdbcTemplate ycBase(String ycNombre, int ycValor) {
        SingleConnectionDataSource ycDs = new SingleConnectionDataSource(
                "jdbc:sqlite:" + ycTmp.resolve(ycNombre).toString().replace('\\', '/'), true);
        ycAbiertas.add(ycDs);
        JdbcTemplate ycJdbc = new JdbcTemplate(ycDs);
        ycJdbc.execute("create table cuarto(id integer)");
        ycJdbc.execute("create table inquilino(id integer)");
        ycJdbc.execute("create table pago(id integer)");
        ycJdbc.update("insert into cuarto values (?)", ycValor);
        return ycJdbc;
    }

    @Test
    void ycRestaurarReemplazaLosDatosYGuardaLosAnteriores() {
        JdbcTemplate ycActual = ycBase("actual.db", 1);
        JdbcTemplate ycVieja = ycBase("vieja.db", 99);
        ((SingleConnectionDataSource) ycVieja.getDataSource()).destroy();

        YCRespaldoService ycServicio = new YCRespaldoService(ycActual, ycAdmin(), ycTmp.resolve("respaldos").toString(), 30);
        File ycAntes = ycServicio.ycRestaurar(ycTmp.resolve("vieja.db").toFile());

        assertEquals(99, ycActual.queryForObject("select id from cuarto", Integer.class));
        assertTrue(ycAntes.isFile());
        assertTrue(ycAntes.getName().endsWith("_antes-de-restaurar.db"));
    }

    @Test
    void ycRechazaArchivosQueNoSonCopias() throws Exception {
        JdbcTemplate ycActual = ycBase("actual.db", 1);
        YCRespaldoService ycServicio = new YCRespaldoService(ycActual, ycAdmin(), ycTmp.resolve("respaldos").toString(), 30);
        File ycTexto = Files.writeString(ycTmp.resolve("otro.db"), "hola, no soy una base de datos").toFile();

        assertThrows(YCNegocioException.class, () -> ycServicio.ycRestaurar(ycTexto));
        assertEquals(1, ycActual.queryForObject("select id from cuarto", Integer.class));
    }

    private static com.alquilafacil.repository.YCAjusteRepository ycAjustesEnMemoria() {
        java.util.Map<String, String> ycMapa = new java.util.HashMap<>();
        var ycRepo = org.mockito.Mockito.mock(com.alquilafacil.repository.YCAjusteRepository.class);
        org.mockito.Mockito.when(ycRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(ycI -> {
            com.alquilafacil.model.YCAjuste ycA = ycI.getArgument(0);
            ycMapa.put(ycA.getYcClave(), ycA.getYcValor());
            return ycA;
        });
        org.mockito.Mockito.when(ycRepo.findById(org.mockito.ArgumentMatchers.anyString())).thenAnswer(ycI -> {
            String ycClave = ycI.getArgument(0);
            return java.util.Optional.ofNullable(ycMapa.get(ycClave)).map(ycV -> new com.alquilafacil.model.YCAjuste(ycClave, ycV));
        });
        return ycRepo;
    }

    @Test
    void ycCadaCopiaTambienSeGuardaEnLaCarpetaExterna() throws Exception {
        JdbcTemplate ycActual = ycBase("actual.db", 1);
        YCRespaldoService ycServicio = new YCRespaldoService(ycActual, ycAdmin(), ycTmp.resolve("respaldos").toString(), 30);
        ycServicio.ycSetAjustes(ycAjustesEnMemoria());
        File ycUsb = Files.createDirectories(ycTmp.resolve("usb")).toFile();

        ycServicio.ycConfigurarCarpetaExterna(ycUsb);
        File ycOtra = ycServicio.ycRespaldarAhora();

        File ycSub = new File(ycUsb, YCRespaldoService.YC_SUBCARPETA_EXTERNA);
        assertTrue(new File(ycSub, ycOtra.getName()).isFile(), "la copia nueva llego a la USB");
        var ycEstado = ycServicio.ycEstadoExterno();
        assertEquals(ycUsb.getAbsolutePath(), ycEstado.ycCarpeta());
        assertNotNull(ycEstado.ycUltima());
        assertNull(ycEstado.ycError());
    }

    @Test
    void ycSiLaUsbNoEstaConectadaSeAnotaElProblemaSinFallar() throws Exception {
        JdbcTemplate ycActual = ycBase("actual.db", 1);
        YCRespaldoService ycServicio = new YCRespaldoService(ycActual, ycAdmin(), ycTmp.resolve("respaldos").toString(), 30);
        var ycAjustes = ycAjustesEnMemoria();
        ycServicio.ycSetAjustes(ycAjustes);
        File ycUsb = Files.createDirectories(ycTmp.resolve("usb")).toFile();
        ycServicio.ycConfigurarCarpetaExterna(ycUsb);
        ycAjustes.save(new com.alquilafacil.model.YCAjuste("respaldoExternoCarpeta", ycTmp.resolve("no-existe").toString()));

        assertDoesNotThrow(ycServicio::ycRespaldarAhora);
        assertNotNull(ycServicio.ycEstadoExterno().ycError());
        assertThrows(YCNegocioException.class, () -> ycServicio.ycConfigurarCarpetaExterna(ycTmp.resolve("respaldos").toFile()),
                "no se acepta la carpeta del propio programa");
    }
}
