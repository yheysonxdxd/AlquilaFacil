package com.alquilafacil.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.alquilafacil.exception.YCNegocioException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Order(0)
public class YCRespaldoService implements ApplicationRunner {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCRespaldoService.class);
    private static final DateTimeFormatter YC_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final JdbcTemplate ycJdbc;
    private final YCSesion ycSesion;
    private final File ycCarpeta;
    private final int ycMaximo;

    public YCRespaldoService(JdbcTemplate ycJdbc, YCSesion ycSesion,
                           @Value("${app.respaldos.carpeta:respaldos}") String ycCarpeta,
                           @Value("${app.respaldos.maximo:30}") int ycMaximo) {
        this.ycJdbc = ycJdbc;
        this.ycSesion = ycSesion;
        this.ycCarpeta = new File(ycCarpeta).getAbsoluteFile();
        this.ycMaximo = ycMaximo;
    }

    @Override
    public void run(ApplicationArguments ycArgs) {
        try {
            String ycHoy = LocalDate.now().toString();
            boolean ycYaHay = ycListar().stream().anyMatch(ycF -> ycF.getName().startsWith("alquileres_" + ycHoy));
            if (!ycYaHay) {
                File ycF = ycRespaldarAhora();
                YC_LOG.info("Copia de seguridad automatica: {}", ycF.getName());
            } else {
                YCEstadoExterno ycE = ycEstadoExterno();
                if (ycE.ycCarpeta() != null && (ycE.ycUltima() == null || ycE.ycUltima().toLocalDate().isBefore(LocalDate.now()))) {
                    ycUltimo().ifPresent(this::ycCopiarAExterna);
                }
            }
        } catch (RuntimeException ycE) {
            YC_LOG.warn("No se pudo hacer la copia de seguridad automatica", ycE);
        }
    }

    static final String YC_SUBCARPETA_EXTERNA = "Hospedaje Supply - respaldos";

    public record YCEstadoExterno(String ycCarpeta, LocalDateTime ycUltima, String ycError) {}

    private com.alquilafacil.repository.YCAjusteRepository ycAjustes;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetAjustes(com.alquilafacil.repository.YCAjusteRepository ycAjustes) {
        this.ycAjustes = ycAjustes;
    }

    private String ycAjuste(String ycClave) {
        if (ycAjustes == null) {
            return null;
        }
        try {
            return ycAjustes.findById(ycClave).map(com.alquilafacil.model.YCAjuste::getYcValor)
                    .filter(ycV -> !ycV.isBlank()).orElse(null);
        } catch (RuntimeException ycE) {
            return null;
        }
    }

    private void ycGuardarAjuste(String ycClave, String ycValor) {
        if (ycAjustes != null) {
            ycAjustes.save(new com.alquilafacil.model.YCAjuste(ycClave, ycValor == null ? "" : ycValor));
        }
    }

    public YCEstadoExterno ycEstadoExterno() {
        String ycUltima = ycAjuste("respaldoExternoUltimo");
        LocalDateTime ycCuando = null;
        try {
            ycCuando = ycUltima == null ? null : LocalDateTime.parse(ycUltima);
        } catch (Exception ycIgnorada) {
        }
        return new YCEstadoExterno(ycAjuste("respaldoExternoCarpeta"), ycCuando, ycAjuste("respaldoExternoError"));
    }

    public YCEstadoExterno ycConfigurarCarpetaExterna(File ycCarpetaElegida) {
        ycSesion.ycExigirAdmin("elegir la carpeta de copias externas");
        if (ycCarpetaElegida == null || !ycCarpetaElegida.isDirectory()) {
            throw new YCNegocioException("Esa carpeta no existe o no esta conectada.");
        }
        if (ycCarpetaElegida.getAbsoluteFile().toPath().startsWith(ycCarpeta.toPath())) {
            throw new YCNegocioException("Elige una carpeta FUERA de la carpeta del programa (una USB o Google Drive).");
        }
        ycGuardarAjuste("respaldoExternoCarpeta", ycCarpetaElegida.getAbsolutePath());
        ycAuditar("Copia externa configurada", ycCarpetaElegida.getAbsolutePath());
        File ycUltima = ycUltimo().orElseGet(this::ycRespaldarAhora);
        ycCopiarAExterna(ycUltima);
        YCEstadoExterno ycE = ycEstadoExterno();
        if (ycE.ycError() != null) {
            throw new YCNegocioException("Se guardo la carpeta, pero no se pudo copiar: " + ycE.ycError());
        }
        return ycE;
    }

    public void ycQuitarCarpetaExterna() {
        ycSesion.ycExigirAdmin("quitar la carpeta de copias externas");
        ycGuardarAjuste("respaldoExternoCarpeta", null);
        ycGuardarAjuste("respaldoExternoError", null);
        ycAuditar("Copia externa quitada", "Ya no se copian los respaldos fuera de la PC");
    }

    void ycCopiarAExterna(File ycCopia) {
        String ycExterna = ycAjuste("respaldoExternoCarpeta");
        if (ycExterna == null || ycCopia == null) {
            return;
        }
        try {
            File ycBase = new File(ycExterna);
            if (!ycBase.isDirectory()) {
                throw new IOException("la carpeta " + ycExterna + " no esta conectada");
            }
            File ycDestino = new File(ycBase, YC_SUBCARPETA_EXTERNA);
            if (!ycDestino.isDirectory() && !ycDestino.mkdirs()) {
                throw new IOException("no se pudo crear " + ycDestino);
            }
            java.nio.file.Files.copy(ycCopia.toPath(), new File(ycDestino, ycCopia.getName()).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File[] ycViejas = ycDestino.listFiles((ycD, ycN) -> ycN.startsWith("alquileres_") && ycN.endsWith(".db"));
            if (ycViejas != null) {
                Arrays.stream(ycViejas).sorted(Comparator.comparing(File::getName).reversed()).skip(ycMaximo)
                        .forEach(File::delete);
            }
            ycGuardarAjuste("respaldoExternoUltimo", LocalDateTime.now().withNano(0).toString());
            ycGuardarAjuste("respaldoExternoError", null);
            YC_LOG.info("Copia externa: {} -> {}", ycCopia.getName(), ycDestino);
        } catch (Exception ycE) {
            YC_LOG.warn("No se pudo copiar el respaldo fuera de la PC", ycE);
            ycGuardarAjuste("respaldoExternoError", LocalDate.now() + ": " + ycE.getMessage());
        }
    }

    public File ycRespaldarAhora() {
        File ycDestino = ycCopiar("");
        ycLimpiarAntiguos(null);
        ycCopiarAExterna(ycDestino);
        return ycDestino;
    }

    public File ycRestaurar(File ycOrigen) {
        ycSesion.ycExigirAdmin("restaurar copias de seguridad");
        if (ycOrigen == null || !ycOrigen.isFile()) {
            throw new YCNegocioException("No se encontro el archivo de la copia.");
        }
        File ycArchivo = ycOrigen.getAbsoluteFile();
        if (ycArchivo.getPath().contains("\"")) {
            throw new YCNegocioException("El nombre del archivo no puede tener comillas (\"). Cambiale el nombre.");
        }
        ycValidarCopia(ycArchivo);
        File ycAntes = ycCopiar("_antes-de-restaurar");
        ycJdbc.execute("restore from \"" + ycArchivo.getPath().replace('\\', '/') + "\"");
        YC_LOG.warn("Base restaurada desde {} (copia previa: {}) por {}", ycArchivo, ycAntes.getName(), ycSesion.ycNombreUsuario());
        ycAuditar("Copia restaurada", "Se volvio a " + ycArchivo.getName() + " · lo anterior quedo en " + ycAntes.getName());
        ycLimpiarAntiguos(ycAntes);
        return ycAntes;
    }

    private static void ycValidarCopia(File ycArchivo) {
        byte[] ycCabecera = new byte[16];
        try (FileInputStream ycIn = new FileInputStream(ycArchivo)) {
            if (ycIn.read(ycCabecera) != 16 || !new String(ycCabecera, StandardCharsets.US_ASCII).startsWith("SQLite format 3")) {
                throw new YCNegocioException(ycArchivo.getName() + " no es una copia de seguridad del programa.");
            }
        } catch (IOException ycE) {
            throw new YCNegocioException("No se pudo leer " + ycArchivo.getName() + ".");
        }
        try (Connection ycC = DriverManager.getConnection("jdbc:sqlite:" + ycArchivo.getPath().replace('\\', '/'));
             ResultSet ycR = ycC.createStatement().executeQuery(
                     "select count(*) from sqlite_master where type='table' and name in ('cuarto','inquilino','pago')")) {
            if (!ycR.next() || ycR.getInt(1) < 3) {
                throw new YCNegocioException(ycArchivo.getName() + " no tiene los datos del programa (cuartos, inquilinos, pagos).");
            }
        } catch (SQLException ycE) {
            throw new YCNegocioException(ycArchivo.getName() + " esta danado o no es una copia de seguridad del programa.");
        }
    }

    private File ycCopiar(String ycSufijo) {
        if (!ycCarpeta.isDirectory() && !ycCarpeta.mkdirs()) {
            throw new IllegalStateException("No se pudo crear la carpeta " + ycCarpeta);
        }
        File ycDestino = new File(ycCarpeta, "alquileres_" + LocalDateTime.now().format(YC_ARCHIVO) + ycSufijo + ".db");
        if (ycDestino.exists() && !ycDestino.delete()) {
            throw new IllegalStateException("No se pudo reemplazar " + ycDestino.getName());
        }
        ycJdbc.execute("VACUUM INTO '" + ycDestino.getAbsolutePath().replace("'", "''") + "'");
        return ycDestino;
    }

    public File ycGetCarpeta() {
        return ycCarpeta;
    }

    public List<File> ycListar() {
        File[] ycArchivos = ycCarpeta.listFiles((ycD, ycN) -> ycN.startsWith("alquileres_") && ycN.endsWith(".db"));
        if (ycArchivos == null) {
            return List.of();
        }
        return Arrays.stream(ycArchivos).sorted(Comparator.comparing(File::getName).reversed()).toList();
    }

    public Optional<File> ycUltimo() {
        return ycListar().stream().findFirst();
    }

    private void ycLimpiarAntiguos(File ycConservar) {
        List<File> ycTodos = ycListar();
        for (int ycI = ycMaximo; ycI < ycTodos.size(); ycI++) {
            if (ycTodos.get(ycI).equals(ycConservar)) {
                continue;
            }
            if (!ycTodos.get(ycI).delete()) {
                YC_LOG.warn("No se pudo borrar el respaldo antiguo {}", ycTodos.get(ycI).getName());
            }
        }
    }

    private YCAuditoriaService ycAuditoria;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetAuditoria(YCAuditoriaService ycAuditoria) {
        this.ycAuditoria = ycAuditoria;
    }

    private void ycAuditar(String ycAccion, String ycDetalle) {
        if (ycAuditoria != null) {
            ycAuditoria.ycRegistrar(ycAccion, ycDetalle);
        }
    }
}
