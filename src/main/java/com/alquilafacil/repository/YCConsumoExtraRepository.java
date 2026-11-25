package com.alquilafacil.repository;

import com.alquilafacil.model.YCConsumoExtra;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface YCConsumoExtraRepository extends JpaRepository<YCConsumoExtra, Long> {
    @Query("SELECT c FROM YCConsumoExtra c WHERE c.ycEstadia.ycId IN :estadiaIds ORDER BY c.ycFechaRegistro ASC, c.ycId ASC")
    List<YCConsumoExtra> ycFindByEstadiaIdInOrderByFechaRegistroAscIdAsc(@Param("estadiaIds") Collection<Long> ycEstadiaIds);

    @Query("""
            SELECT SUM(c.ycTotal) FROM YCConsumoExtra c
            WHERE c.ycPagado = true AND c.ycAnulado = false AND c.ycFechaPago >= :desde AND c.ycFechaPago < :hasta
            """)
    BigDecimal ycSumarCobrado(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("""
            SELECT c FROM YCConsumoExtra c
            WHERE (c.ycFechaPago >= :desde AND c.ycFechaPago < :hasta)
               OR (c.ycFechaAnulacion >= :desde AND c.ycFechaAnulacion < :hasta)
            """)
    List<YCConsumoExtra> ycConMovimientosEntre(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("SELECT c FROM YCConsumoExtra c WHERE c.ycPagado = false AND c.ycAnulado = false")
    List<YCConsumoExtra> ycPendientes();

    @Query("SELECT c FROM YCConsumoExtra c WHERE LOWER(c.ycConcepto) = LOWER(:concepto) AND c.ycAnulado = false ORDER BY c.ycFechaRegistro DESC")
    List<YCConsumoExtra> ycUltimosPorConcepto(@Param("concepto") String ycConcepto, Pageable ycPagina);

    default YCConsumoExtra ycFindFirstByConceptoIgnoreCaseAndAnuladoFalseOrderByFechaRegistroDesc(String ycConcepto) {
        return ycUltimosPorConcepto(ycConcepto, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }

    @Query("SELECT DISTINCT c.ycConcepto FROM YCConsumoExtra c WHERE c.ycAnulado = false ORDER BY c.ycConcepto")
    List<String> ycConceptosUsados();
}
