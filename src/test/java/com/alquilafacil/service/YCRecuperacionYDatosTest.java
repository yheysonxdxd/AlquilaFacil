package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCAjuste;
import com.alquilafacil.model.YCRol;
import com.alquilafacil.model.YCUsuario;
import com.alquilafacil.repository.YCAjusteRepository;
import com.alquilafacil.repository.YCUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YCRecuperacionYDatosTest {
    private final YCUsuarioRepository ycUsuarios = mock(YCUsuarioRepository.class);
    private final YCSesion ycSesion = new YCSesion();
    private final YCUsuarioService ycService = new YCUsuarioService(ycUsuarios, ycSesion);
    private YCUsuario ycAdmin;

    @BeforeEach
    void ycDatos() {
        String ycSal = "c2FsLWRlLXBydWViYS0xMjM=";
        ycAdmin = YCUsuario.builder().ycId(1L).ycUsuario("rosa").ycNombre("Rosa").ycRol(YCRol.YC_ADMINISTRADOR).ycActivo(true)
                .ycSal(ycSal).ycHash(YCUsuarioService.ycCifrar("vieja123", ycSal)).build();
        when(ycUsuarios.findById(1L)).thenReturn(Optional.of(ycAdmin));
        when(ycUsuarios.ycFindByUsuarioIgnoreCase("rosa")).thenReturn(Optional.of(ycAdmin));
        when(ycUsuarios.save(any())).thenAnswer(ycI -> ycI.getArgument(0));
    }

    private String ycCrearCodigo() {
        ycSesion.ycIniciar(ycAdmin);
        String ycCodigo = ycService.ycNuevoCodigoRecuperacion();
        ycSesion.ycCerrar();
        return ycCodigo;
    }

    @Test
    void ycElCodigoTieneFormatoFacilDeAnotarYNoSeGuardaEnTexto() {
        String ycCodigo = ycCrearCodigo();
        assertTrue(ycCodigo.matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}"), ycCodigo);
        assertTrue(ycAdmin.ycTieneCodigoRecuperacion());
        assertFalse(ycAdmin.getYcCodigoRecuperacionHash().contains(ycCodigo.replace("-", "")));
    }

    @Test
    void ycConElCodigoSePoneUnaContrasenaNuevaYElCodigoSeGasta() {
        String ycCodigo = ycCrearCodigo();
        String ycOtro = ycService.ycRecuperar("Rosa", ycCodigo.toLowerCase().replace("-", " "), "nueva456");

        ycSesion.ycCerrar();
        assertDoesNotThrow(() -> ycService.ycIngresar("rosa", "nueva456"));
        assertThrows(YCNegocioException.class, () -> ycService.ycIngresar("rosa", "vieja123"));
        assertNotEquals(ycCodigo, ycOtro);
        assertThrows(YCNegocioException.class, () -> ycService.ycRecuperar("rosa", ycCodigo, "otra789"),
                "el codigo usado ya no sirve");
        assertDoesNotThrow(() -> ycService.ycRecuperar("rosa", ycOtro, "otra789"), "el nuevo si sirve");
    }

    @Test
    void ycCodigoIncorrectoSeBloqueaTras5Intentos() {
        String ycCodigo = ycCrearCodigo();
        for (int ycI = 0; ycI < 5; ycI++) {
            assertThrows(YCNegocioException.class, () -> ycService.ycRecuperar("rosa", "AAAA-BBBB-CCCC", "nueva456"));
        }
        YCNegocioException ycE = assertThrows(YCNegocioException.class, () -> ycService.ycRecuperar("rosa", ycCodigo, "nueva456"));
        assertTrue(ycE.getMessage().contains("15 minutos"), "aun con el codigo correcto queda bloqueado");
    }

    @Test
    void ycRecepcionNoTieneCodigoDeRecuperacion() {
        ycSesion.ycIniciar(YCUsuario.builder().ycId(2L).ycUsuario("brandon").ycRol(YCRol.YC_RECEPCION).build());
        assertThrows(YCNegocioException.class, ycService::ycNuevoCodigoRecuperacion);
    }

    @Test
    void ycUnaContrasenaVaciaNoRevientaSoloEsIncorrecta() {
        assertFalse(YCUsuarioService.ycCoincide("", ycAdmin.getYcSal(), ycAdmin.getYcHash()));
        assertThrows(YCNegocioException.class, () -> ycService.ycIngresar("rosa", ""));
        assertThrows(YCNegocioException.class, () -> ycService.ycRecuperar("rosa", "", "nueva456"));
    }

    private YCDatosNegocioService.YCDatos ycDatos(String ycNombre, String ycDni, String ycDireccion, String ycHora, String ycTasa) {
        return new YCDatosNegocioService.YCDatos(ycNombre, ycDni, ycDireccion, "987654321", "Hospedaje Supply", ycHora,
                ycTasa == null ? null : new BigDecimal(ycTasa), 15);
    }

    @Test
    void ycValidaLosDatosDelNegocio() {
        assertThrows(YCNegocioException.class, () -> YCDatosNegocioService.ycValidar(ycDatos("Nombre del Propietario", "", "Av. Sol 1", "20:00", "5")));
        assertThrows(YCNegocioException.class, () -> YCDatosNegocioService.ycValidar(ycDatos("Rosa Quispe", "123", "Av. Sol 1", "20:00", "5")));
        assertThrows(YCNegocioException.class, () -> YCDatosNegocioService.ycValidar(ycDatos("Rosa Quispe", "", "Av. Principal 123", "20:00", "5")));
        assertThrows(YCNegocioException.class, () -> YCDatosNegocioService.ycValidar(ycDatos("Rosa Quispe", "", "Av. Sol 1", "25:99", "5")));
        assertThrows(YCNegocioException.class, () -> YCDatosNegocioService.ycValidar(ycDatos("Rosa Quispe", "", "Av. Sol 1", "20:00", "80")));

        var ycV = YCDatosNegocioService.ycValidar(ycDatos("  Rosa   Quispe ", "40404040", "Av. Sol 1, Cusco", "9:30", "6.50"));
        assertEquals("Rosa Quispe", ycV.get("nombre"));
        assertEquals("09:30", ycV.get("horaLimiteLlegada"));
        assertEquals("6.5", ycV.get("tasaImpuestoAlquiler"));
    }

    @Test
    void ycGuardarAplicaLosDatosAlInstanteYSoloElAdministrador() {
        YCAjusteRepository ycAjustes = mock(YCAjusteRepository.class);
        YCDatosArrendador ycArrendador = new YCDatosArrendador();
        ycArrendador.setYcNombre("Nombre del Propietario");
        ycArrendador.setYcDireccion("Av. Principal 123");
        YCDatosNegocioService ycNegocio = new YCDatosNegocioService(ycAjustes, ycArrendador, ycSesion);
        assertTrue(ycNegocio.ycFaltanDatos());

        ycSesion.ycIniciar(YCUsuario.builder().ycId(2L).ycUsuario("brandon").ycRol(YCRol.YC_RECEPCION).build());
        assertThrows(YCNegocioException.class, () -> ycNegocio.ycGuardar(ycDatos("Rosa Quispe", "", "Av. Sol 1", "20:00", "5")));

        ycSesion.ycIniciar(ycAdmin);
        ycNegocio.ycGuardar(ycDatos("Rosa Quispe", "40404040", "Av. Sol 1, Cusco", "21:00", "5"));
        assertEquals("Rosa Quispe", ycArrendador.getYcNombre());
        assertEquals("21:00", ycArrendador.getYcHoraLimiteLlegada());
        assertFalse(ycNegocio.ycFaltanDatos());
        verify(ycAjustes, times(8)).save(any());
    }

    @Test
    void ycAlAbrirElProgramaSeUsanLosDatosGuardados() {
        YCAjusteRepository ycAjustes = mock(YCAjusteRepository.class);
        when(ycAjustes.findAll()).thenReturn(List.of(new YCAjuste("nombre", "Rosa Quispe"),
                new YCAjuste("tasaImpuestoAlquiler", "6"), new YCAjuste("direccion", "Av. Sol 1")));
        YCDatosArrendador ycArrendador = new YCDatosArrendador();
        new YCDatosNegocioService(ycAjustes, ycArrendador, ycSesion).run(null);
        assertEquals("Rosa Quispe", ycArrendador.getYcNombre());
        assertEquals(new BigDecimal("6"), ycArrendador.getYcTasaImpuestoAlquiler());
    }
}
