package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCLiquidacion;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCLiquidacionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCSalidaInquilinoService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCSalidaInquilinoService.class);
    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCLiquidacionRepository ycRepository;
    private final YCPagoService ycPagoService;
    private final YCInquilinoService ycInquilinoService;
    private final YCSesion ycSesion;

    public record YCCalculo(BigDecimal ycDeuda, BigDecimal ycDanos, BigDecimal ycGarantia) {
        public BigDecimal ycTotalACubrir() {
            return ycDeuda.add(ycDanos);
        }

        public BigDecimal ycACobrar() {
            BigDecimal ycR = ycTotalACubrir().subtract(ycGarantia);
            return ycR.signum() > 0 ? ycR : BigDecimal.ZERO;
        }

        public BigDecimal ycADevolver() {
            BigDecimal ycR = ycGarantia.subtract(ycTotalACubrir());
            return ycR.signum() > 0 ? ycR : BigDecimal.ZERO;
        }
    }

    public static YCCalculo ycCalcular(BigDecimal ycDeuda, BigDecimal ycDanos, BigDecimal ycGarantia) {
        return new YCCalculo(ycCero(ycDeuda), ycCero(ycDanos), ycCero(ycGarantia));
    }

    @Transactional
    public YCLiquidacion ycRegistrar(Long ycInquilinoId, Set<YearMonth> ycMesesAPagar, BigDecimal ycDanos, String ycMotivoDanos,
                                 BigDecimal ycGarantia, YCMetodoPago ycMetodo) {
        YCInquilino ycI = ycInquilinoRepository.findById(ycInquilinoId)
                .orElseThrow(() -> new YCNegocioException("El inquilino ya no existe."));
        if (!ycI.isYcActivo()) {
            throw new YCNegocioException(ycI.ycGetNombreCompleto() + " ya fue dado de baja.");
        }
        ycDanos = ycCero(ycDanos);
        ycGarantia = ycCero(ycGarantia);
        if (ycDanos.signum() < 0 || ycGarantia.signum() < 0) {
            throw new YCNegocioException("Los montos no pueden ser negativos.");
        }
        if (ycDanos.signum() > 0 && (ycMotivoDanos == null || ycMotivoDanos.isBlank())) {
            throw new YCNegocioException("Escribe el motivo del descuento por daños.");
        }

        List<YCDeudaPendiente> ycPendientes = ycPagoService.ycPendientes(ycI, false, LocalDate.now());
        List<YCDeudaPendiente> ycAPagar = ycPendientes.stream().filter(ycD -> ycMesesAPagar.contains(ycD.ycPeriodo())).toList();
        List<YCDeudaPendiente> ycQuedan = ycPendientes.stream().filter(ycD -> !ycMesesAPagar.contains(ycD.ycPeriodo())).toList();
        BigDecimal ycDeuda = ycAPagar.stream().map(YCDeudaPendiente::ycTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        YCCalculo ycC = ycCalcular(ycDeuda, ycDanos, ycGarantia);
        YCMetodoPago ycForma = ycMetodo == null ? YCMetodoPago.YC_EFECTIVO : ycMetodo;

        for (YCDeudaPendiente ycD : ycAPagar) {
            ycPagoService.ycRegistrar(ycI.getYcId(), ycD.ycPeriodo(), ycD.ycAlquiler(), ycD.ycLuz(), ycD.ycAgua(), ycForma,
                    "Pagado al retirarse" + (ycGarantia.signum() > 0 ? " (se uso la garantia)" : ""));
        }
        ycInquilinoService.ycDarDeBaja(ycI.getYcId());

        YCLiquidacion ycL = ycRepository.save(YCLiquidacion.builder()
                .ycInquilino(ycI).ycCuartoNumero(ycI.getYcCuarto().getYcNumero()).ycFecha(LocalDateTime.now())
                .ycMesesPagados(ycTexto(ycAPagar)).ycDeudaPagada(ycDeuda.setScale(2, RoundingMode.HALF_UP))
                .ycMesesPendientes(ycTexto(ycQuedan))
                .ycDeudaPendiente(ycQuedan.stream().map(YCDeudaPendiente::ycTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP))
                .ycDanos(ycDanos.setScale(2, RoundingMode.HALF_UP))
                .ycMotivoDanos(ycMotivoDanos == null || ycMotivoDanos.isBlank() ? null : ycMotivoDanos.trim())
                .ycGarantia(ycGarantia.setScale(2, RoundingMode.HALF_UP))
                .ycCobrado(ycC.ycACobrar().setScale(2, RoundingMode.HALF_UP))
                .ycDevuelto(ycC.ycADevolver().setScale(2, RoundingMode.HALF_UP))
                .ycMetodo(ycForma).ycRegistradoPor(ycSesion.ycNombreUsuario())
                .build());
        YC_LOG.info("Salida registrada: {} inquilino={} cobrado={} devuelto={}",
                ycL.ycGetNumero(), ycI.getYcDni(), ycL.getYcCobrado(), ycL.getYcDevuelto());
        return ycL;
    }

    public YCLiquidacion ycObtener(Long ycId) {
        return ycRepository.findById(ycId).orElseThrow(() -> new YCNegocioException("La liquidacion ya no existe."));
    }

    private static String ycTexto(List<YCDeudaPendiente> ycMeses) {
        return ycMeses.isEmpty() ? null : ycMeses.stream().map(YCDeudaPendiente::ycPeriodoTexto).collect(Collectors.joining(", "));
    }

    private static BigDecimal ycCero(BigDecimal ycV) {
        return ycV == null ? BigDecimal.ZERO : ycV;
    }
}
