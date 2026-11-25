package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCConsumoServicio;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCPagoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCConsumoServicioService {
    private final YCConsumoServicioRepository ycRepository;
    private final YCPagoRepository ycPagoRepository;
    private final YCSesion ycSesion;

    public Page<YCConsumoServicio> ycListar(String ycFiltro, int ycPagina, int ycTamanio) {
        String ycTexto = ycFiltro == null ? "" : ycFiltro.trim();
        Sort ycOrden = Sort.by(Sort.Order.desc("ycPeriodo"), Sort.Order.asc("ycCuarto.ycNumero"));
        return ycRepository.ycBuscar(ycTexto, PageRequest.of(ycPagina, ycTamanio, ycOrden));
    }

    public boolean ycEstaCobrado(YCConsumoServicio ycConsumo) {
        return ycPagoRepository.ycExistsByCuartoIdAndPeriodoAndAnuladoFalse(
                ycConsumo.getYcCuarto().getYcId(), ycConsumo.getYcPeriodo());
    }

    @Transactional
    public YCConsumoServicio ycGuardar(@Valid YCConsumoServicio ycConsumo) {
        ycRepository.ycFindByCuartoIdAndPeriodo(ycConsumo.getYcCuarto().getYcId(), ycConsumo.getYcPeriodo())
                .ifPresent(ycExistente -> {
                    if (ycConsumo.getYcId() == null) {
                        ycConsumo.setYcId(ycExistente.getYcId());
                    } else if (!ycExistente.getYcId().equals(ycConsumo.getYcId())) {
                        throw new YCNegocioException("Ese cuarto ya tiene luz y agua registrados para "
                                + YCCalendarioPagos.ycTexto(ycConsumo.getYcPeriodo()) + ".");
                    }
                });
        return ycRepository.save(ycConsumo);
    }

    @Transactional
    public void ycEliminar(Long ycId) {
        ycSesion.ycExigirAdmin("borrar registros de luz y agua");
        if (!ycRepository.existsById(ycId)) {
            throw new YCNegocioException("El registro ya no existe.");
        }
        ycRepository.findById(ycId).ifPresent(ycC -> ycAuditar("Luz y agua eliminado", ycC.getYcCuarto().getYcNumero() + " · "
                + YCCalendarioPagos.ycTexto(ycC.getYcPeriodo()) + " · luz S/ " + ycC.getYcLuz() + " · agua S/ " + ycC.getYcAgua()));
        ycRepository.deleteById(ycId);
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
