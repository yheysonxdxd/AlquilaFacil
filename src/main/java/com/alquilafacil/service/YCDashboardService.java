package com.alquilafacil.service;

import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCDashboardService {
    private final YCCuartoRepository ycCuartoRepository;
    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCPagoService ycPagoService;
    private final YCEstadiaService ycEstadiaService;

    public record YCResumen(long ycTotalCuartos, long ycOcupados, BigDecimal ycCobradoMes, BigDecimal ycDeudaVencida,
                          long ycInquilinosAtrasados, List<YCDeudaPendiente> ycPendientes,
                          Map<YearMonth, BigDecimal> ycIngresosPorMes) {
        public long ycLibres() {
            return ycTotalCuartos - ycOcupados;
        }

        public int ycOcupacion() {
            return ycTotalCuartos == 0 ? 0 : (int) Math.round(ycOcupados * 100.0 / ycTotalCuartos);
        }
    }

    public YCResumen ycResumen() {
        List<YCDeudaPendiente> ycPendientes = ycPagoService.ycPendientesDeTodos();
        BigDecimal ycDeuda = ycPendientes.stream()
                .filter(YCDeudaPendiente::ycAtrasado)
                .map(YCDeudaPendiente::ycTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long ycAtrasados = ycPendientes.stream()
                .filter(YCDeudaPendiente::ycAtrasado)
                .map(ycD -> ycD.ycInquilino().getYcId())
                .distinct()
                .count();

        YearMonth ycActual = YearMonth.now();
        Map<YearMonth, BigDecimal> ycIngresos = new LinkedHashMap<>();
        for (int ycI = 5; ycI >= 0; ycI--) {
            YearMonth ycMes = ycActual.minusMonths(ycI);
            ycIngresos.put(ycMes, ycPagoService.ycCobradoEn(ycMes).add(ycEstadiaService.ycCobradoEn(ycMes)));
        }

        long ycOcupados = ycInquilinoRepository.ycCountByActivoTrue() + ycEstadiaService.ycEnCursoPorCuarto().size();
        return new YCResumen(ycCuartoRepository.count(), ycOcupados,
                ycIngresos.get(ycActual), ycDeuda, ycAtrasados, ycPendientes, ycIngresos);
    }
}
