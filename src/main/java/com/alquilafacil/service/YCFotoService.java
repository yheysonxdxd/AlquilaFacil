package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class YCFotoService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCFotoService.class);
    private static final Set<String> YC_EXTENSIONES = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final long YC_TAMANIO_MAXIMO = 15L * 1024 * 1024;

    private final Path ycCarpeta;

    public YCFotoService(@Value("${app.fotos.carpeta:fotos}") String ycCarpeta) {
        this.ycCarpeta = Paths.get(ycCarpeta).toAbsolutePath().normalize();
    }

    public String ycGuardar(Path ycOrigen) {
        String ycExtension = ycExtension(ycOrigen.getFileName().toString());
        if (!YC_EXTENSIONES.contains(ycExtension)) {
            throw new YCNegocioException("Formato no soportado. Usa una foto JPG, PNG, GIF o BMP.");
        }
        try {
            if (Files.size(ycOrigen) > YC_TAMANIO_MAXIMO) {
                throw new YCNegocioException("La foto pesa mas de 15 MB. Elige una mas liviana.");
            }
            Files.createDirectories(ycCarpeta);
            String ycNombre = "cuarto-" + UUID.randomUUID().toString().substring(0, 8) + "." + ycExtension;
            Files.copy(ycOrigen, ycCarpeta.resolve(ycNombre), StandardCopyOption.REPLACE_EXISTING);
            YC_LOG.info("Foto guardada: {}", ycNombre);
            return ycNombre;
        } catch (IOException ycE) {
            throw new YCNegocioException("No se pudo guardar la foto: " + ycE.getMessage());
        }
    }

    public String ycGuardar(java.io.InputStream ycOrigen, String ycExtension) {
        try (ycOrigen) {
            Files.createDirectories(ycCarpeta);
            String ycNombre = "cuarto-" + UUID.randomUUID().toString().substring(0, 8) + "."
                    + ycExtension.toLowerCase(Locale.ROOT);
            Files.copy(ycOrigen, ycCarpeta.resolve(ycNombre), StandardCopyOption.REPLACE_EXISTING);
            return ycNombre;
        } catch (IOException ycE) {
            throw new YCNegocioException("No se pudo guardar la foto: " + ycE.getMessage());
        }
    }

    public Optional<Path> ycRuta(String ycNombre) {
        if (ycNombre == null || ycNombre.isBlank()) {
            return Optional.empty();
        }
        Path ycRuta = ycCarpeta.resolve(ycNombre).normalize();
        return ycRuta.startsWith(ycCarpeta) && Files.isRegularFile(ycRuta) ? Optional.of(ycRuta) : Optional.empty();
    }

    public void ycEliminar(String ycNombre) {
        ycRuta(ycNombre).ifPresent(ycRuta -> {
            try {
                Files.deleteIfExists(ycRuta);
            } catch (IOException ycE) {
                YC_LOG.warn("No se pudo borrar la foto {}", ycRuta, ycE);
            }
        });
    }

    private static String ycExtension(String ycArchivo) {
        int ycPunto = ycArchivo.lastIndexOf('.');
        return ycPunto < 0 ? "" : ycArchivo.substring(ycPunto + 1).toLowerCase(Locale.ROOT);
    }
}
