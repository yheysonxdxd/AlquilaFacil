package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCConsumoExtra;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.repository.YCConsumoExtraRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCConsumoExtraService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCConsumoExtraService.class);
    public static final List<String> YC_SUGERIDOS = List.of("Desayuno", "Lavanderia", "Gaseosa", "Agua",
            "Cochera", "Llegada temprano", "Salida tarde", "Persona adicional", "Toalla extra");

    private static final BigDecimal YC_MAXIMO = new BigDecimal("10000");

    private final YCConsumoExtraRepository ycRepository;
    private final YCEstadiaService ycEstadiaService;
    private final YCSesion ycSesion;
    private final YCAuditoriaService ycAuditoria;

    @Transactional
    public YCConsumoExtra ycAgregar(Long ycEstadiaId, String ycConcepto, Integer ycCantidad, BigDecimal ycPrecio, YCMetodoPago ycPagaAhora) {
        YCEstadia ycE = ycEstadiaService.ycObtener(ycEstadiaId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("La estadia esta cancelada: no se le pueden cargar consumos.");
        }
        String ycNombre = ycConcepto == null ? "" : ycConcepto.trim().replaceAll("\\s+", " ");
        if (ycNombre.length() < 2) {
            throw new YCNegocioException("Escribe que consumio (por ejemplo, Desayuno).");
        }
        if (ycNombre.length() > 60) {
            ycNombre = ycNombre.substring(0, 60);
        }
        int ycCuantos = ycCantidad == null ? 1 : ycCantidad;
        if (ycCuantos < 1 || ycCuantos > 99) {
            throw new YCNegocioException("La cantidad va de 1 a 99.");
        }
        if (ycPrecio == null || ycPrecio.signum() <= 0) {
            throw new YCNegocioException("El precio debe ser mayor a 0.");
        }
        BigDecimal ycUnitario = ycPrecio.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ycTotal = ycUnitario.multiply(BigDecimal.valueOf(ycCuantos));
        if (ycTotal.compareTo(YC_MAXIMO) > 0) {
            throw new YCNegocioException("El consumo no puede pasar de S/ " + YC_MAXIMO + ".");
        }
        LocalDateTime ycAhora = LocalDateTime.now();
        YCConsumoExtra ycC = YCConsumoExtra.builder()
                .ycEstadia(ycE).ycConcepto(ycNombre).ycCantidad(ycCuantos).ycPrecioUnitario(ycUnitario).ycTotal(ycTotal)
                .ycFechaRegistro(ycAhora).ycRegistradoPor(ycSesion.ycNombreUsuario())
                .ycPagado(ycPagaAhora != null).ycMetodo(ycPagaAhora)
                .ycFechaPago(ycPagaAhora != null ? ycAhora : null)
                .ycCobradoPor(ycPagaAhora != null ? ycSesion.ycNombreUsuario() : null)
                .ycAnulado(false)
                .build();
        YCConsumoExtra ycGuardado = ycRepository.save(ycC);
        YC_LOG.info("Consumo {} en {}: {} = {} ({})", ycGuardado.ycGetDescripcion(), ycE.ycGetNumero(), ycUnitario, ycTotal,
                ycPagaAhora == null ? "a la cuenta" : ycPagaAhora);
        return ycGuardado;
    }

    @Transactional
    public BigDecimal ycCobrarPendientes(YCEstadia ycE, YCMetodoPago ycMetodo) {
        List<YCConsumoExtra> ycPendientes = ycDeEstadia(ycE).stream().filter(YCConsumoExtra::ycIsPendiente).toList();
        if (ycPendientes.isEmpty()) {
            throw new YCNegocioException("No hay consumos por cobrar.");
        }
        LocalDateTime ycAhora = LocalDateTime.now();
        BigDecimal ycTotal = BigDecimal.ZERO;
        for (YCConsumoExtra ycC : ycPendientes) {
            ycC.setYcPagado(true);
            ycC.setYcMetodo(ycMetodo == null ? YCMetodoPago.YC_EFECTIVO : ycMetodo);
            ycC.setYcFechaPago(ycAhora);
            ycC.setYcCobradoPor(ycSesion.ycNombreUsuario());
            ycTotal = ycTotal.add(ycC.getYcTotal());
        }
        ycRepository.saveAll(ycPendientes);
        return ycTotal;
    }

    @Transactional
    public void ycAnular(Long ycId, String ycMotivo) {
        ycSesion.ycExigirAdmin("anular consumos");
        String ycPorQue = YCMotivos.ycExigir(ycMotivo, "anular el consumo");
        YCConsumoExtra ycC = ycRepository.findById(ycId).orElseThrow(() -> new YCNegocioException("El consumo ya no existe."));
        if (ycC.isYcAnulado()) {
            throw new YCNegocioException("Este consumo ya estaba anulado.");
        }
        ycC.setYcAnulado(true);
        ycC.setYcMotivoAnulacion(ycPorQue);
        ycC.setYcAnuladoPor(ycSesion.ycNombreUsuario());
        ycC.setYcFechaAnulacion(LocalDateTime.now());
        ycRepository.save(ycC);
        ycAuditoria.ycRegistrar("Consumo anulado", ycC.getYcEstadia().ycGetNumero() + " · " + ycC.getYcEstadia().getYcHuesped()
                + " · " + ycC.ycGetDescripcion() + " · S/ " + ycC.getYcTotal() + (ycC.isYcPagado() ? " (ya estaba pagado)" : "")
                + " · motivo: " + ycPorQue);
    }

    public List<YCConsumoExtra> ycDeEstadia(YCEstadia ycE) {
        List<Long> ycIds = ycEstadiaService.ycDelGrupo(ycE).stream().map(YCEstadia::getYcId).toList();
        return ycIds.isEmpty() ? List.of() : ycRepository.ycFindByEstadiaIdInOrderByFechaRegistroAscIdAsc(ycIds);
    }

    public BigDecimal ycPendienteDe(YCEstadia ycE) {
        return ycDeEstadia(ycE).stream().filter(YCConsumoExtra::ycIsPendiente).map(YCConsumoExtra::getYcTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, BigDecimal> ycPendientesPorEstadia() {
        return ycRepository.ycPendientes().stream().collect(Collectors.groupingBy(ycC -> ycC.getYcEstadia().getYcId(),
                Collectors.reducing(BigDecimal.ZERO, YCConsumoExtra::getYcTotal, BigDecimal::add)));
    }

    public List<String> ycSugerencias() {
        Set<String> ycTodos = new LinkedHashSet<>(YC_SUGERIDOS);
        ycTodos.addAll(ycRepository.ycConceptosUsados());
        return List.copyOf(ycTodos);
    }

    public BigDecimal ycPrecioSugerido(String ycConcepto) {
        if (ycConcepto == null || ycConcepto.isBlank()) {
            return null;
        }
        YCConsumoExtra ycUltimo = ycRepository.ycFindFirstByConceptoIgnoreCaseAndAnuladoFalseOrderByFechaRegistroDesc(ycConcepto.trim());
        return ycUltimo == null ? null : ycUltimo.getYcPrecioUnitario();
    }
}
