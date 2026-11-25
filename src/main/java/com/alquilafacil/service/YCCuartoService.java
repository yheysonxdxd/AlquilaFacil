package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCCuartoService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCCuartoService.class);
    private final YCCuartoRepository ycRepository;
    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCConsumoServicioRepository ycConsumoRepository;
    private final YCFotoService ycFotoService;
    private final YCEstadiaRepository ycEstadiaRepository;
    private final YCSesion ycSesion;

    public List<YCCuarto> ycDeHospedaje() {
        return ycRepository.ycFindAllByOrderByNumeroAsc().stream().filter(YCCuarto::ycIsHospedaje).toList();
    }

    public List<YCCuarto> ycMensuales() {
        return ycRepository.ycFindAllByOrderByNumeroAsc().stream().filter(ycC -> !ycC.ycIsHospedaje()).toList();
    }

    public List<YCCuarto> ycListar() {
        return ycRepository.ycFindAllByOrderByNumeroAsc();
    }

    public List<YCCuarto> ycLibres() {
        return ycRepository.ycLibres();
    }

    public List<YCCuarto> ycDisponiblesPara(YCCuarto ycActual) {
        List<YCCuarto> ycLista = new ArrayList<>(ycRepository.ycLibres());
        if (ycActual != null && !ycLista.contains(ycActual)) {
            ycLista.add(0, ycActual);
        }
        return ycLista;
    }

    public Optional<YCInquilino> ycOcupante(Long ycCuartoId) {
        return ycInquilinoRepository.ycFindByCuartoIdAndActivoTrue(ycCuartoId);
    }

    @Transactional
    public YCCuarto ycGuardar(@Valid YCCuarto ycCuarto) {
        ycSesion.ycExigirAdmin("crear o editar cuartos y sus precios");
        ycRepository.ycFindByNumeroIgnoreCase(ycCuarto.getYcNumero()).ifPresent(ycExistente -> {
            if (ycCuarto.getYcId() == null || !ycExistente.getYcId().equals(ycCuarto.getYcId())) {
                throw new YCNegocioException("Ya existe un cuarto con el nombre " + ycCuarto.getYcNumero());
            }
        });
        if (ycCuarto.getYcId() != null) {
            if (ycCuarto.ycIsHospedaje() && ycInquilinoRepository.ycFindByCuartoIdAndActivoTrue(ycCuarto.getYcId()).isPresent()) {
                throw new YCNegocioException("No se puede pasar a hospedaje: el cuarto tiene un inquilino mensual. "
                        + "Primero dalo de baja.");
            }
            if (!ycCuarto.ycIsHospedaje() && ycEstadiaRepository.ycTieneVigentes(ycCuarto.getYcId(), java.time.LocalDate.now())) {
                throw new YCNegocioException("No se puede pasar a alquiler mensual: tiene huespedes o reservas "
                        + "pendientes en Hospedaje.");
            }
        }
        if (ycCuarto.getYcId() != null && ycCuarto.ycGetLimpieza() == com.alquilafacil.model.YCEstadoLimpieza.YC_LIMPIA) {
            ycRepository.findById(ycCuarto.getYcId()).ifPresent(ycAntes -> ycCuarto.setYcLimpieza(ycAntes.ycGetLimpieza()));
        }
        String ycCambios = ycCuarto.getYcId() == null ? null : ycRepository.findById(ycCuarto.getYcId())
                .map(ycAntes -> new YCAuditoriaService.YCCambios()
                        .ycCampo("nombre", ycAntes.getYcNumero(), ycCuarto.getYcNumero())
                        .ycCampo("precio", ycAntes.getYcPrecio(), ycCuarto.getYcPrecio())
                        .ycCampo("modalidad", ycAntes.ycGetModalidad().toString(), ycCuarto.ycGetModalidad().toString())
                        .toString())
                .orElse(null);
        YCCuarto ycGuardado = ycRepository.save(ycCuarto);
        YC_LOG.info("Cuarto guardado: id={} numero={}", ycGuardado.getYcId(), ycGuardado.getYcNumero());
        if (ycCambios == null) {
            ycAuditar("Cuarto creado", ycGuardado.getYcNumero() + " · S/ " + ycGuardado.getYcPrecio());
        } else if (!ycCambios.isEmpty()) {
            ycAuditar("Cuarto editado", ycGuardado.getYcNumero() + ": " + ycCambios);
        }
        return ycGuardado;
    }

    @Transactional
    public void ycEliminar(Long ycId) {
        ycSesion.ycExigirAdmin("eliminar cuartos");
        if (!ycRepository.existsById(ycId)) {
            throw new YCNegocioException("El cuarto ya no existe en la base de datos.");
        }
        if (ycInquilinoRepository.ycExistsByCuartoId(ycId)) {
            throw new YCNegocioException("No se puede eliminar: el cuarto tiene inquilinos registrados (actuales o anteriores).");
        }
        if (ycEstadiaRepository.ycExistsByCuartoId(ycId)) {
            throw new YCNegocioException("No se puede eliminar: la habitacion tiene estadias de hospedaje registradas.");
        }
        YCCuarto ycCuarto = ycRepository.findById(ycId).orElseThrow();
        ycConsumoRepository.ycDeleteByCuartoId(ycId);
        ycRepository.deleteById(ycId);
        ycAuditar("Cuarto eliminado", ycCuarto.getYcNumero() + " · S/ " + ycCuarto.getYcPrecio()
                + (ycCuarto.ycIsHospedaje() ? " por noche" : " al mes"));
        ycFotoService.ycEliminar(ycCuarto.getYcFoto());
        ycCuarto.ycGetGaleria().forEach(ycFotoService::ycEliminar);
        ycFotoService.ycEliminar(ycCuarto.getYcFoto360());
    }

    public String ycFotoActual(Long ycId) {
        return ycId == null ? null : ycRepository.findById(ycId).map(YCCuarto::getYcFoto).orElse(null);
    }

    @Transactional
    public void ycCambiarLimpieza(Long ycCuartoId, com.alquilafacil.model.YCEstadoLimpieza ycEstado) {
        YCCuarto ycC = ycRepository.findById(ycCuartoId)
                .orElseThrow(() -> new YCNegocioException("La habitacion ya no existe."));
        ycC.setYcLimpieza(ycEstado);
        ycRepository.save(ycC);
    }

    public long ycContar() {
        return ycRepository.count();
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
