package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCConsumoServicio;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCPago;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCPagoService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCPagoService.class);
    private final YCPagoRepository ycRepository;
    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCConsumoServicioRepository ycConsumoRepository;
    private final YCSesion ycSesion;

    public List<YCDeudaPendiente> ycPendientes(YCInquilino ycInquilino, boolean ycIncluirSiguiente) {
        return ycPendientes(ycInquilino, ycIncluirSiguiente, ycInquilino.getYcFechaSalida());
    }

    public List<YCDeudaPendiente> ycPendientes(YCInquilino ycInquilino, boolean ycIncluirSiguiente, LocalDate ycSalida) {
        Set<YearMonth> ycPagados = ycRepository.ycFindByInquilinoIdAndAnuladoFalse(ycInquilino.getYcId()).stream()
                .map(YCPago::getYcPeriodo)
                .collect(Collectors.toSet());
        LocalDate ycHoy = LocalDate.now();
        List<YCDeudaPendiente> ycDeudas = new ArrayList<>();
        for (YearMonth ycMes : YCCalendarioPagos.ycMesesAdeudados(ycInquilino.getYcFechaIngreso(),
                ycSalida, ycHoy, ycPagados, ycIncluirSiguiente)) {
            ycDeudas.add(ycDetalle(ycInquilino, ycMes, ycHoy, ycSalida));
        }
        return ycDeudas;
    }

    public List<YCDeudaPendiente> ycPendientesDeTodos() {
        return ycInquilinoRepository.ycFindByActivoTrueOrderByApellidoAscNombreAsc().stream()
                .flatMap(ycI -> ycPendientes(ycI, false).stream())
                .sorted(Comparator.comparingLong(YCDeudaPendiente::ycDiasAtraso).reversed())
                .toList();
    }

    static int ycDiasCobrables(YearMonth ycMes, LocalDate ycIngreso, LocalDate ycSalida) {
        LocalDate ycDesde = ycIngreso != null && ycIngreso.isAfter(ycMes.atDay(1)) ? ycIngreso : ycMes.atDay(1);
        LocalDate ycHasta = ycSalida != null && ycSalida.isBefore(ycMes.atEndOfMonth()) ? ycSalida : ycMes.atEndOfMonth();
        return ycHasta.isBefore(ycDesde) ? 0 : (int) ChronoUnit.DAYS.between(ycDesde, ycHasta) + 1;
    }

    static BigDecimal ycRentaDelMes(BigDecimal ycRenta, YearMonth ycMes, int ycDias) {
        if (ycDias >= ycMes.lengthOfMonth()) {
            return ycRenta;
        }
        return ycRenta.multiply(BigDecimal.valueOf(ycDias))
                .divide(BigDecimal.valueOf(ycMes.lengthOfMonth()), 2, RoundingMode.HALF_UP);
    }

    private YCDeudaPendiente ycDetalle(YCInquilino ycInquilino, YearMonth ycMes, LocalDate ycHoy, LocalDate ycSalida) {
        LocalDate ycVence = YCCalendarioPagos.ycVencimiento(ycMes, ycInquilino.getYcDiaPago());
        BigDecimal ycLuz = BigDecimal.ZERO;
        BigDecimal ycAgua = BigDecimal.ZERO;
        var ycConsumo = ycConsumoRepository.ycFindByCuartoIdAndPeriodo(ycInquilino.getYcCuarto().getYcId(), ycMes);
        if (ycConsumo.isPresent()) {
            YCConsumoServicio ycC = ycConsumo.get();
            ycLuz = ycC.getYcLuz();
            ycAgua = ycC.getYcAgua();
        }
        int ycDias = ycDiasCobrables(ycMes, ycInquilino.getYcFechaIngreso(), ycSalida);
        boolean ycPorDias = ycDias < ycMes.lengthOfMonth();
        return new YCDeudaPendiente(ycInquilino, ycMes, ycVence, ycRentaDelMes(ycInquilino.ycGetRentaMensual(), ycMes, ycDias), ycLuz, ycAgua,
                ChronoUnit.DAYS.between(ycVence, ycHoy), ycPorDias ? ycDias : null);
    }

    @Transactional
    public YCPago ycRegistrar(Long ycInquilinoId, YearMonth ycPeriodo, BigDecimal ycAlquiler, BigDecimal ycLuz,
                          BigDecimal ycAgua, YCMetodoPago ycMetodo, String ycNota) {
        YCInquilino ycInquilino = ycInquilinoRepository.findById(ycInquilinoId)
                .orElseThrow(() -> new YCNegocioException("El inquilino ya no existe."));
        if (!ycInquilino.isYcActivo()) {
            throw new YCNegocioException("El inquilino ya se retiro; no se le pueden registrar pagos nuevos.");
        }
        if (ycPeriodo == null) {
            throw new YCNegocioException("Selecciona el mes que se paga.");
        }
        if (ycRepository.ycExistsByInquilinoIdAndPeriodoAndAnuladoFalse(ycInquilinoId, ycPeriodo)) {
            throw new YCNegocioException(YCCalendarioPagos.ycTexto(ycPeriodo) + " ya fue pagado por "
                    + ycInquilino.ycGetNombreCompleto() + ".");
        }
        ycAlquiler = ycImporte(ycAlquiler);
        ycLuz = ycImporte(ycLuz);
        ycAgua = ycImporte(ycAgua);
        BigDecimal ycTotal = ycAlquiler.add(ycLuz).add(ycAgua);
        if (ycTotal.signum() <= 0) {
            throw new YCNegocioException("El total del pago debe ser mayor a 0.");
        }

        YCPago ycPago = YCPago.builder()
                .ycInquilino(ycInquilino)
                .ycCuarto(ycInquilino.getYcCuarto())
                .ycCuartoNumero(ycInquilino.getYcCuarto().getYcNumero())
                .ycPeriodo(ycPeriodo)
                .ycAlquiler(ycAlquiler)
                .ycLuz(ycLuz)
                .ycAgua(ycAgua)
                .ycTotal(ycTotal)
                .ycFechaPago(LocalDateTime.now())
                .ycMetodo(ycMetodo == null ? YCMetodoPago.YC_EFECTIVO : ycMetodo)
                .ycNota(ycNota == null || ycNota.isBlank() ? null : ycNota.trim())
                .ycAnulado(false)
                .ycRegistradoPor(ycSesion.ycNombreUsuario())
                .build();
        YCPago ycGuardado = ycRepository.save(ycPago);
        YC_LOG.info("Pago registrado: {} inquilino={} periodo={} total={}",
                ycGuardado.ycGetNumeroRecibo(), ycInquilino.getYcDni(), ycPeriodo, ycTotal);
        return ycGuardado;
    }

    @Transactional
    public void ycAnular(Long ycId, String ycMotivo) {
        ycSesion.ycExigirAdmin("anular recibos");
        YCPago ycPago = ycObtener(ycId);
        if (ycPago.isYcAnulado()) {
            throw new YCNegocioException("El recibo " + ycPago.ycGetNumeroRecibo() + " ya estaba anulado.");
        }
        String ycPorQue = YCMotivos.ycExigir(ycMotivo, "anular el recibo");
        ycPago.setYcAnulado(true);
        ycPago.setYcMotivoAnulacion(ycPorQue);
        ycPago.setYcAnuladoPor(ycSesion.ycNombreUsuario());
        ycPago.setYcFechaAnulacion(LocalDateTime.now());
        ycAuditar("Recibo anulado", ycPago.ycGetNumeroRecibo() + " · " + ycPago.getYcInquilino().ycGetNombreCompleto() + " · "
                + YCCalendarioPagos.ycTexto(ycPago.getYcPeriodo()) + " · S/ " + ycPago.getYcTotal() + " · motivo: " + ycPorQue);
        ycRepository.save(ycPago);
    }

    public YCPago ycObtener(Long ycId) {
        return ycRepository.findById(ycId)
                .orElseThrow(() -> new YCNegocioException("El pago ya no existe en la base de datos."));
    }

    public Page<YCPago> ycListar(String ycFiltro, int ycPagina, int ycTamanio) {
        String ycTexto = ycFiltro == null ? "" : ycFiltro.trim();
        return ycRepository.ycBuscar(ycTexto, PageRequest.of(ycPagina, ycTamanio, Sort.by("ycId").descending()));
    }

    public List<YCPago> ycBuscarTodos(String ycFiltro) {
        String ycTexto = ycFiltro == null ? "" : ycFiltro.trim();
        return ycRepository.ycBuscar(ycTexto, org.springframework.data.domain.Pageable.unpaged(Sort.by("ycId").descending())).getContent();
    }

    public BigDecimal ycCobradoEn(YearMonth ycMes) {
        BigDecimal ycSuma = ycRepository.ycSumarCobrado(ycMes.atDay(1).atStartOfDay(),
                ycMes.plusMonths(1).atDay(1).atStartOfDay());
        return ycSuma == null ? BigDecimal.ZERO : ycSuma;
    }

    private static BigDecimal ycImporte(BigDecimal ycValor) {
        if (ycValor == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (ycValor.signum() < 0) {
            throw new YCNegocioException("Los montos no pueden ser negativos.");
        }
        return ycValor.setScale(2, RoundingMode.HALF_UP);
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
