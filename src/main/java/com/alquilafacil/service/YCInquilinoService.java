package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCInquilinoService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCInquilinoService.class);
    private final YCInquilinoRepository ycRepository;
    private final YCPagoRepository ycPagoRepository;
    private final YCSesion ycSesion;

    public Page<YCInquilino> ycListar(String ycFiltro, boolean ycIncluirRetirados, int ycPagina, int ycTamanio) {
        String ycTexto = ycFiltro == null ? "" : ycFiltro.trim();
        Sort ycOrden = Sort.by(Sort.Order.desc("ycActivo"), Sort.Order.asc("ycApellido"), Sort.Order.asc("ycNombre"));
        return ycRepository.ycBuscar(ycTexto, ycIncluirRetirados, PageRequest.of(ycPagina, ycTamanio, ycOrden));
    }

    public List<YCInquilino> ycActivos() {
        return ycRepository.ycFindByActivoTrueOrderByApellidoAscNombreAsc();
    }

    public YCInquilino ycObtener(Long ycId) {
        return ycRepository.findById(ycId)
                .orElseThrow(() -> new YCNegocioException("El inquilino ya no existe en la base de datos."));
    }

    @Transactional
    public YCInquilino ycGuardar(@Valid YCInquilino ycInquilino) {
        if (ycInquilino.getYcId() != null && !ycObtener(ycInquilino.getYcId()).isYcActivo()) {
            throw new YCNegocioException("No se puede editar a un inquilino que ya se retiro.");
        }
        ycRepository.ycFindByDniAndActivoTrue(ycInquilino.getYcDni()).ifPresent(ycOtro -> {
            if (ycInquilino.getYcId() == null || !ycOtro.getYcId().equals(ycInquilino.getYcId())) {
                throw new YCNegocioException("Ya hay un inquilino activo con el DNI " + ycInquilino.getYcDni());
            }
        });
        if (ycInquilino.getYcCuarto().ycIsHospedaje()) {
            throw new YCNegocioException(ycInquilino.getYcCuarto().getYcNumero()
                    + " es de hospedaje (por noche). Registra al huesped en la seccion Hospedaje.");
        }
        ycRepository.ycFindByCuartoIdAndActivoTrue(ycInquilino.getYcCuarto().getYcId()).ifPresent(ycOtro -> {
            if (ycInquilino.getYcId() == null || !ycOtro.getYcId().equals(ycInquilino.getYcId())) {
                throw new YCNegocioException("El " + ycInquilino.getYcCuarto().getYcNumero()
                        + " ya esta ocupado por " + ycOtro.ycGetNombreCompleto());
            }
        });
        if (ycInquilino.getYcFechaIngreso().isAfter(LocalDate.now().plusMonths(2))) {
            throw new YCNegocioException("La fecha de ingreso es demasiado lejana.");
        }
        if (ycInquilino.getYcRenta() == null) {
            ycInquilino.setYcRenta(ycInquilino.getYcCuarto().getYcPrecio());
        }
        if (ycInquilino.getYcRenta().signum() <= 0) {
            throw new YCNegocioException("La renta mensual debe ser mayor a 0.");
        }
        if (ycInquilino.getYcFechaFinContrato() != null
                && !ycInquilino.getYcFechaFinContrato().isAfter(ycInquilino.getYcFechaIngreso())) {
            throw new YCNegocioException("El fin del contrato debe ser despues de la fecha de ingreso.");
        }
        ycInquilino.setYcActivo(true);
        String ycCambios = ycInquilino.getYcId() == null ? null : ycRepository.findById(ycInquilino.getYcId())
                .map(ycAntes -> new YCAuditoriaService.YCCambios()
                        .ycCampo("cuarto", ycAntes.getYcCuarto().getYcNumero(), ycInquilino.getYcCuarto().getYcNumero())
                        .ycCampo("renta", ycAntes.ycGetRentaMensual(), ycInquilino.getYcRenta())
                        .ycCampo("ingreso", ycAntes.getYcFechaIngreso(), ycInquilino.getYcFechaIngreso())
                        .ycCampo("dia de pago", ycAntes.getYcDiaPago(), ycInquilino.getYcDiaPago())
                        .ycCampo("garantia", ycAntes.getYcGarantia(), ycInquilino.getYcGarantia())
                        .ycCampo("fin de contrato", ycAntes.getYcFechaFinContrato(), ycInquilino.getYcFechaFinContrato())
                        .ycCampo("DNI", ycAntes.getYcDni(), ycInquilino.getYcDni())
                        .toString())
                .orElse(null);
        YCInquilino ycGuardado = ycRepository.save(ycInquilino);
        if (ycCambios != null && !ycCambios.isEmpty()) {
            ycAuditar("Inquilino editado", ycGuardado.ycGetNombreCompleto() + ": " + ycCambios);
        }
        YC_LOG.info("Inquilino guardado: id={} dni={}", ycGuardado.getYcId(), ycGuardado.getYcDni());
        return ycGuardado;
    }

    @Transactional
    public void ycDarDeBaja(Long ycId) {
        YCInquilino ycInquilino = ycObtener(ycId);
        if (!ycInquilino.isYcActivo()) {
            throw new YCNegocioException("Este inquilino ya fue dado de baja.");
        }
        ycInquilino.setYcActivo(false);
        ycInquilino.setYcFechaSalida(LocalDate.now());
        ycRepository.save(ycInquilino);
    }

    @Transactional
    public void ycEliminar(Long ycId) {
        ycSesion.ycExigirAdmin("eliminar inquilinos");
        ycObtener(ycId);
        if (ycPagoRepository.ycExistsByInquilinoId(ycId)) {
            throw new YCNegocioException("No se puede eliminar: tiene pagos registrados. Usa \"Dar de baja\".");
        }
        YCInquilino ycBorrado = ycObtener(ycId);
        ycAuditar("Inquilino eliminado", ycBorrado.ycGetNombreCompleto() + " · DNI " + ycBorrado.getYcDni() + " · "
                + ycBorrado.getYcCuarto().getYcNumero());
        ycRepository.deleteById(ycId);
    }

    public List<YCInquilino> ycContratosPorVencer(int ycDias) {
        LocalDate ycHoy = LocalDate.now();
        return ycRepository.ycFindByActivoTrueOrderByApellidoAscNombreAsc().stream()
                .filter(ycI -> ycI.ycContratoPorVencer(ycHoy, ycDias))
                .sorted(java.util.Comparator.comparing(YCInquilino::getYcFechaFinContrato))
                .toList();
    }

    @Transactional
    public YCInquilino ycRenovarContrato(Long ycId, int ycMeses) {
        if (ycMeses < 1 || ycMeses > 60) {
            throw new YCNegocioException("La renovacion va de 1 a 60 meses.");
        }
        YCInquilino ycI = ycObtener(ycId);
        if (!ycI.isYcActivo()) {
            throw new YCNegocioException(ycI.ycGetNombreCompleto() + " ya se retiro.");
        }
        LocalDate ycHoy = LocalDate.now();
        LocalDate ycBase = ycI.getYcFechaFinContrato() == null || ycI.getYcFechaFinContrato().isBefore(ycHoy.minusMonths(1))
                ? ycHoy : ycI.getYcFechaFinContrato();
        LocalDate ycFinAnterior = ycI.getYcFechaFinContrato();
        ycI.setYcFechaFinContrato(ycBase.plusMonths(ycMeses));
        ycAuditar("Contrato renovado", ycI.ycGetNombreCompleto() + ": fin " + (ycFinAnterior == null ? "—" : ycFinAnterior)
                + " → " + ycI.getYcFechaFinContrato());
        YC_LOG.info("Contrato renovado: {} hasta {} por {}", ycI.getYcDni(), ycI.getYcFechaFinContrato(), ycSesion.ycNombreUsuario());
        return ycRepository.save(ycI);
    }

    public long ycContarActivos() {
        return ycRepository.ycCountByActivoTrue();
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
