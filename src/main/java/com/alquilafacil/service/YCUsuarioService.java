package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.model.YCUsuario;
import com.alquilafacil.repository.YCUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCUsuarioService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCUsuarioService.class);
    private static final int YC_ITERACIONES = 120_000;
    private static final SecureRandom YC_AZAR = new SecureRandom();

    private final YCUsuarioRepository ycRepository;
    private final YCSesion ycSesion;

    public boolean ycHayUsuarios() {
        return ycRepository.count() > 0;
    }

    public List<YCUsuario> ycListar() {
        return ycRepository.ycFindAllByOrderByNombreAsc();
    }

    @Transactional
    public YCUsuario ycIngresar(String ycUsuario, String ycContrasena) {
        String ycLimpio = java.text.Normalizer.normalize(ycUsuario == null ? "" : ycUsuario.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        YCUsuario ycU = ycRepository.ycFindByUsuarioIgnoreCase(ycLimpio).orElse(null);
        if (ycU == null || !ycCoincide(ycContrasena, ycU.getYcSal(), ycU.getYcHash())) {
            throw new YCNegocioException("Usuario o contraseña incorrectos.");
        }
        if (!ycU.isYcActivo()) {
            throw new YCNegocioException("Este usuario esta desactivado. Habla con el administrador.");
        }
        ycU.setYcUltimoIngreso(LocalDateTime.now());
        ycRepository.save(ycU);
        ycSesion.ycIniciar(ycU);
        YC_LOG.info("Ingreso: {}", ycU.getYcUsuario());
        return ycU;
    }

    @Transactional
    public YCUsuario ycCrear(String ycUsuario, String ycNombre, YCRol ycRol, String ycContrasena) {
        String ycClave = ycNormalizar(ycUsuario);
        if (ycRepository.ycFindByUsuarioIgnoreCase(ycClave).isPresent()) {
            throw new YCNegocioException("Ya existe el usuario " + ycClave + ".");
        }
        if (ycNombre == null || ycNombre.isBlank()) {
            throw new YCNegocioException("Escribe el nombre de la persona.");
        }
        if (ycHayUsuarios()) {
            ycSesion.ycExigirAdmin("crear usuarios");
        } else {
            ycRol = YCRol.YC_ADMINISTRADOR;
        }
        ycValidarContrasena(ycContrasena);
        String ycSal = ycNuevaSal();
        YCUsuario ycU = ycRepository.save(YCUsuario.builder().ycUsuario(ycClave).ycNombre(ycNombre.trim())
                .ycRol(ycRol == null ? YCRol.YC_RECEPCION : ycRol)
                .ycSal(ycSal).ycHash(ycCifrar(ycContrasena, ycSal)).ycActivo(true).build());
        YC_LOG.info("Usuario creado: {} ({})", ycU.getYcUsuario(), ycU.getYcRol());
        ycAuditar("Usuario creado", ycU.getYcUsuario() + " · " + ycU.getYcNombre() + " · " + ycU.getYcRol().ycGetTexto());
        return ycU;
    }

    @Transactional
    public void ycCambiarContrasena(Long ycId, String ycActual, String ycNueva) {
        YCUsuario ycU = ycRepository.findById(ycId).orElseThrow(() -> new YCNegocioException("El usuario ya no existe."));
        boolean ycEsElMismo = ycSesion.ycGetUsuario() != null && ycSesion.ycGetUsuario().getYcId().equals(ycId);
        if (ycEsElMismo && !ycCoincide(ycActual, ycU.getYcSal(), ycU.getYcHash())) {
            throw new YCNegocioException("La contraseña actual no es correcta.");
        }
        if (!ycEsElMismo) {
            ycSesion.ycExigirAdmin("cambiar la contraseña de otros");
        }
        ycValidarContrasena(ycNueva);
        String ycSal = ycNuevaSal();
        ycU.setYcSal(ycSal);
        ycU.setYcHash(ycCifrar(ycNueva, ycSal));
        ycRepository.save(ycU);
        ycAuditar("Contraseña cambiada", ycEsElMismo ? ycU.getYcUsuario() + " cambio su propia contraseña"
                : "Nueva contraseña para " + ycU.getYcUsuario());
    }

    @Transactional
    public void ycCambiarActivo(Long ycId, boolean ycActivo) {
        ycSesion.ycExigirAdmin("activar o desactivar usuarios");
        YCUsuario ycU = ycRepository.findById(ycId).orElseThrow(() -> new YCNegocioException("El usuario ya no existe."));
        if (!ycActivo && ycSesion.ycGetUsuario() != null && ycSesion.ycGetUsuario().getYcId().equals(ycId)) {
            throw new YCNegocioException("No puedes desactivar tu propio usuario.");
        }
        if (!ycActivo && ycU.ycEsAdmin() && ycRepository.ycCountByRolAndActivoTrue(YCRol.YC_ADMINISTRADOR) <= 1) {
            throw new YCNegocioException("Debe quedar al menos un administrador activo.");
        }
        ycU.setYcActivo(ycActivo);
        ycRepository.save(ycU);
        ycAuditar(ycActivo ? "Usuario activado" : "Usuario desactivado", ycU.getYcUsuario() + " · " + ycU.getYcNombre());
    }

    private static final String YC_ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int YC_MAX_INTENTOS = 5;
    private static final java.time.Duration YC_BLOQUEO = java.time.Duration.ofMinutes(15);
    private final java.util.Map<String, YCIntentos> ycIntentos = new java.util.concurrent.ConcurrentHashMap<>();

    private record YCIntentos(int ycFallidos, LocalDateTime ycBloqueadoHasta) {}

    @Transactional
    public String ycNuevoCodigoRecuperacion() {
        YCUsuario ycActual = ycSesion.ycGetUsuario();
        if (ycActual == null || !ycActual.ycEsAdmin()) {
            throw new YCNegocioException("Solo el administrador tiene codigo de recuperacion.");
        }
        YCUsuario ycU = ycRepository.findById(ycActual.getYcId()).orElseThrow(() -> new YCNegocioException("El usuario ya no existe."));
        String ycCodigo = ycGenerarCodigo();
        ycGuardarCodigo(ycU, ycCodigo);
        ycRepository.save(ycU);
        ycActual.setYcCodigoRecuperacionHash(ycU.getYcCodigoRecuperacionHash());
        ycActual.setYcCodigoRecuperacionSal(ycU.getYcCodigoRecuperacionSal());
        YC_LOG.info("Codigo de recuperacion creado para {}", ycU.getYcUsuario());
        ycAuditar("Codigo de recuperacion", "Nuevo codigo para " + ycU.getYcUsuario());
        return ycCodigo;
    }

    public boolean ycTengoCodigoRecuperacion() {
        YCUsuario ycActual = ycSesion.ycGetUsuario();
        return ycActual != null && ycRepository.findById(ycActual.getYcId()).map(YCUsuario::ycTieneCodigoRecuperacion).orElse(false);
    }

    @Transactional
    public String ycRecuperar(String ycUsuario, String ycCodigo, String ycNueva) {
        String ycClave = java.text.Normalizer.normalize(ycUsuario == null ? "" : ycUsuario.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        YCIntentos ycPrevio = ycIntentos.get(ycClave);
        if (ycPrevio != null && ycPrevio.ycBloqueadoHasta() != null && LocalDateTime.now().isBefore(ycPrevio.ycBloqueadoHasta())) {
            throw new YCNegocioException("Demasiados intentos. Espera 15 minutos y vuelve a intentar.");
        }
        YCUsuario ycU = ycRepository.ycFindByUsuarioIgnoreCase(ycClave).orElse(null);
        String ycLimpio = ycCodigo == null ? "" : ycCodigo.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (ycU == null || !ycU.ycEsAdmin() || !ycU.isYcActivo() || !ycU.ycTieneCodigoRecuperacion()
                || !ycCoincide(ycLimpio, ycU.getYcCodigoRecuperacionSal(), ycU.getYcCodigoRecuperacionHash())) {
            int ycFallidos = (ycPrevio == null ? 0 : ycPrevio.ycFallidos()) + 1;
            ycIntentos.put(ycClave, new YCIntentos(ycFallidos,
                    ycFallidos >= YC_MAX_INTENTOS ? LocalDateTime.now().plus(YC_BLOQUEO) : null));
            YC_LOG.warn("Intento fallido de recuperacion para '{}' ({} de {})", ycClave, ycFallidos, YC_MAX_INTENTOS);
            throw new YCNegocioException("El usuario o el codigo de recuperacion no son correctos.");
        }
        ycValidarContrasena(ycNueva);
        ycIntentos.remove(ycClave);
        String ycSal = ycNuevaSal();
        ycU.setYcSal(ycSal);
        ycU.setYcHash(ycCifrar(ycNueva, ycSal));
        String ycOtro = ycGenerarCodigo();
        ycGuardarCodigo(ycU, ycOtro);
        ycRepository.save(ycU);
        YC_LOG.warn("Contrasena recuperada con codigo para {}", ycU.getYcUsuario());
        ycAuditar("Contraseña recuperada", ycU.getYcUsuario() + " puso una contraseña nueva con su codigo de recuperacion");
        return ycOtro;
    }

    static String ycGenerarCodigo() {
        StringBuilder ycSb = new StringBuilder();
        for (int ycI = 0; ycI < 12; ycI++) {
            if (ycI > 0 && ycI % 4 == 0) ycSb.append('-');
            ycSb.append(YC_ALFABETO.charAt(YC_AZAR.nextInt(YC_ALFABETO.length())));
        }
        return ycSb.toString();
    }

    private static void ycGuardarCodigo(YCUsuario ycU, String ycCodigo) {
        String ycSal = ycNuevaSal();
        ycU.setYcCodigoRecuperacionSal(ycSal);
        ycU.setYcCodigoRecuperacionHash(ycCifrar(ycCodigo.replace("-", ""), ycSal));
    }

    static String ycNormalizar(String ycUsuario) {
        String ycU = java.text.Normalizer.normalize(ycUsuario == null ? "" : ycUsuario.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        if (ycU.contains(" ")) {
            throw new YCNegocioException("El usuario es UNA sola palabra, sin espacios (ej. " + ycU.split(" ")[0]
                    + "). Tu nombre completo va en el campo \"Tu nombre\".");
        }
        if (ycU.length() < 3) {
            throw new YCNegocioException("El usuario debe tener al menos 3 letras (ej. juan).");
        }
        if (!ycU.matches("[a-z0-9._-]{3,40}")) {
            throw new YCNegocioException("El usuario solo puede tener letras y numeros (ej. juan2026), sin simbolos.");
        }
        return ycU;
    }

    private static void ycValidarContrasena(String ycC) {
        if (ycC == null || ycC.length() < 6) {
            throw new YCNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
    }

    private static String ycNuevaSal() {
        byte[] ycB = new byte[16];
        YC_AZAR.nextBytes(ycB);
        return Base64.getEncoder().encodeToString(ycB);
    }

    static String ycCifrar(String ycContrasena, String ycSal) {
        try {
            PBEKeySpec ycSpec = new PBEKeySpec(ycContrasena.toCharArray(), Base64.getDecoder().decode(ycSal), YC_ITERACIONES, 256);
            byte[] ycH = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(ycSpec).getEncoded();
            return Base64.getEncoder().encodeToString(ycH);
        } catch (Exception ycE) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", ycE);
        }
    }

    static boolean ycCoincide(String ycContrasena, String ycSal, String ycHash) {
        if (ycContrasena == null || ycContrasena.isEmpty() || ycSal == null || ycHash == null) {
            return false;
        }
        return MessageDigest.isEqual(ycCifrar(ycContrasena, ycSal).getBytes(StandardCharsets.US_ASCII),
                ycHash.getBytes(StandardCharsets.US_ASCII));
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
