package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCGasto;
import com.alquilafacil.model.YCNegocio;
import com.alquilafacil.repository.YCGastoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCGastoService {
    private final YCGastoRepository ycRepository;
    private final YCPagoService ycPagoService;
    private final YCEstadiaService ycEstadiaService;
    private final YCSesion ycSesion;

    public record YCResultado(YearMonth ycMes, BigDecimal ycIngresosMensual, BigDecimal ycIngresosHospedaje,
                            BigDecimal ycGastosMensual, BigDecimal ycGastosHospedaje, BigDecimal ycGastosGenerales) {
        public BigDecimal ycIngresos() {
            return ycIngresosMensual.add(ycIngresosHospedaje);
        }

        public BigDecimal ycGastos() {
            return ycGastosMensual.add(ycGastosHospedaje).add(ycGastosGenerales);
        }

        public BigDecimal ycGanancia() {
            return ycIngresos().subtract(ycGastos());
        }

        public BigDecimal ycGananciaMensual() {
            return ycIngresosMensual.subtract(ycGastosMensual);
        }

        public BigDecimal ycGananciaHospedaje() {
            return ycIngresosHospedaje.subtract(ycGastosHospedaje);
        }

        public int ycMargen() {
            return ycIngresos().signum() == 0 ? 0
                    : ycGanancia().multiply(BigDecimal.valueOf(100)).divide(ycIngresos(), 0, RoundingMode.HALF_UP).intValue();
        }
    }

    @Transactional
    public YCGasto ycGuardar(@Valid YCGasto ycGasto) {
        if (ycGasto.getYcId() != null) {
            ycSesion.ycExigirAdmin("modificar gastos");
        }
        if (ycGasto.getYcFecha().isAfter(LocalDate.now().plusDays(1))) {
            throw new YCNegocioException("La fecha del gasto no puede ser futura.");
        }
        if (ycGasto.getYcFechaRegistro() == null) {
            ycGasto.setYcFechaRegistro(LocalDateTime.now());
            ycGasto.setYcRegistradoPor(ycSesion.ycNombreUsuario());
        }
        String ycCambios = ycGasto.getYcId() == null ? null : ycRepository.findById(ycGasto.getYcId())
                .map(ycAntes -> new YCAuditoriaService.YCCambios()
                        .ycCampo("monto", ycAntes.getYcMonto(), ycGasto.getYcMonto())
                        .ycCampo("fecha", ycAntes.getYcFecha(), ycGasto.getYcFecha())
                        .ycCampo("categoria", ycAntes.getYcCategoria().ycGetTexto(), ycGasto.getYcCategoria().ycGetTexto())
                        .ycCampo("negocio", ycAntes.getYcNegocio().ycGetTexto(), ycGasto.getYcNegocio().ycGetTexto())
                        .ycCampo("descripcion", ycAntes.getYcDescripcion(), ycGasto.getYcDescripcion())
                        .toString())
                .orElse(null);
        YCGasto ycGuardado = ycRepository.save(ycGasto);
        if (ycCambios != null && !ycCambios.isEmpty()) {
            ycAuditar("Gasto editado", "#" + ycGuardado.getYcId() + ": " + ycCambios);
        }
        return ycGuardado;
    }

    @Transactional
    public void ycEliminar(Long ycId) {
        ycSesion.ycExigirAdmin("borrar gastos");
        ycRepository.findById(ycId).ifPresent(ycG -> ycAuditar("Gasto eliminado", ycG.getYcCategoria().ycGetTexto() + " · S/ "
                + ycG.getYcMonto() + " del " + ycG.getYcFecha() + (ycG.getYcDescripcion() == null ? "" : " · " + ycG.getYcDescripcion())
                + " (registrado por " + ycG.getYcRegistradoPor() + ")"));
        ycRepository.deleteById(ycId);
    }

    public List<YCGasto> ycDelMes(YearMonth ycMes) {
        return ycRepository.ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(
                ycMes.atDay(1), ycMes.plusMonths(1).atDay(1));
    }

    public YCResultado ycResultado(YearMonth ycMes) {
        LocalDate ycDesde = ycMes.atDay(1);
        LocalDate ycHasta = ycMes.plusMonths(1).atDay(1);
        return new YCResultado(ycMes, ycPagoService.ycCobradoEn(ycMes), ycEstadiaService.ycCobradoEn(ycMes),
                ycCero(ycRepository.ycSumar(YCNegocio.YC_MENSUAL, ycDesde, ycHasta)),
                ycCero(ycRepository.ycSumar(YCNegocio.YC_HOSPEDAJE, ycDesde, ycHasta)),
                ycCero(ycRepository.ycSumar(YCNegocio.YC_GENERAL, ycDesde, ycHasta)));
    }

    private static BigDecimal ycCero(BigDecimal ycV) {
        return ycV == null ? BigDecimal.ZERO : ycV;
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
