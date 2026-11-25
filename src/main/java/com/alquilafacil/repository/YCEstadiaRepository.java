package com.alquilafacil.repository;

import com.alquilafacil.model.YCEstadia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface YCEstadiaRepository extends JpaRepository<YCEstadia, Long> {
    @Query("""
            SELECT e FROM YCEstadia e
            WHERE e.ycCuarto.ycId = :cuartoId AND e.ycCancelada = false
              AND e.ycEntrada < :salida AND e.ycSalida > :entrada
              AND (:excluirId IS NULL OR e.ycId <> :excluirId)
            ORDER BY e.ycEntrada
            """)
    List<YCEstadia> ycCruzadas(@Param("cuartoId") Long ycCuartoId, @Param("entrada") LocalDate ycEntrada,
                           @Param("salida") LocalDate ycSalida, @Param("excluirId") Long ycExcluirId);

    @Query("""
            SELECT e FROM YCEstadia e
            WHERE e.ycCancelada = false AND e.ycEntrada <= :hoy AND e.ycSalida > :hoy
            """)
    List<YCEstadia> ycEnCurso(@Param("hoy") LocalDate ycHoy);

    @Query("""
            SELECT e FROM YCEstadia e
            WHERE e.ycCuarto.ycId = :cuartoId AND e.ycCancelada = false AND e.ycSalida > :hoy
            ORDER BY e.ycEntrada
            """)
    List<YCEstadia> ycVigentesDe(@Param("cuartoId") Long ycCuartoId, @Param("hoy") LocalDate ycHoy);

    @Query("SELECT e FROM YCEstadia e ORDER BY e.ycEntrada DESC, e.ycId DESC")
    List<YCEstadia> ycFindAllByOrderByEntradaDescIdDesc();

    @Query("""
            SELECT e FROM YCEstadia e
            WHERE e.ycCancelada = false AND e.ycEntrada < :hasta AND e.ycSalida > :desde
            ORDER BY e.ycEntrada
            """)
    List<YCEstadia> ycEntre(@Param("desde") LocalDate ycDesde, @Param("hasta") LocalDate ycHasta);

    @Query("SELECT COUNT(e) > 0 FROM YCEstadia e WHERE e.ycCuarto.ycId = :cuartoId")
    boolean ycExistsByCuartoId(@Param("cuartoId") Long ycCuartoId);

    @Query("""
            SELECT COUNT(e) > 0 FROM YCEstadia e
            WHERE e.ycCuarto.ycId = :cuartoId AND e.ycCancelada = false AND e.ycSalida > :hoy
            """)
    boolean ycTieneVigentes(@Param("cuartoId") Long ycCuartoId, @Param("hoy") LocalDate ycHoy);

    @Query("""
            SELECT SUM(COALESCE(e.ycAdelanto, e.ycTotal)) FROM YCEstadia e
            WHERE (e.ycCancelada = false OR e.ycNoLlego = true) AND e.ycFechaRegistro >= :desde AND e.ycFechaRegistro < :hasta
            """)
    BigDecimal ycSumarCobrado(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("""
            SELECT SUM(COALESCE(e.ycMontoSaldo, e.ycTotal - e.ycAdelanto)) FROM YCEstadia e
            WHERE e.ycCancelada = false AND e.ycSaldoPagado = true AND e.ycAdelanto IS NOT NULL
              AND e.ycFechaSaldo >= :desde AND e.ycFechaSaldo < :hasta
            """)
    BigDecimal ycSumarSaldos(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("""
            SELECT SUM(e.ycDevuelto) FROM YCEstadia e
            WHERE e.ycDevuelto IS NOT NULL AND e.ycFechaDevolucion >= :desde AND e.ycFechaDevolucion < :hasta
            """)
    BigDecimal ycSumarDevoluciones(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("""
            SELECT e FROM YCEstadia e
            WHERE (e.ycFechaRegistro >= :desde AND e.ycFechaRegistro < :hasta)
               OR (e.ycFechaSaldo >= :desde AND e.ycFechaSaldo < :hasta)
               OR (e.ycFechaDevolucion >= :desde AND e.ycFechaDevolucion < :hasta)
               OR (e.ycFechaCancelacion >= :desde AND e.ycFechaCancelacion < :hasta)
            """)
    List<YCEstadia> ycConMovimientosEntre(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("SELECT e FROM YCEstadia e WHERE LOWER(e.ycDocumento) = LOWER(:documento) ORDER BY e.ycFechaRegistro DESC")
    List<YCEstadia> ycUltimasPorDocumento(@Param("documento") String ycDocumento, Pageable ycPagina);

    default YCEstadia ycFindFirstByDocumentoIgnoreCaseOrderByFechaRegistroDesc(String ycDocumento) {
        return ycUltimasPorDocumento(ycDocumento, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }

    @Query("SELECT COUNT(e) FROM YCEstadia e WHERE LOWER(e.ycDocumento) = LOWER(:documento) AND e.ycCancelada = false")
    long ycCountByDocumentoIgnoreCaseAndCanceladaFalse(@Param("documento") String ycDocumento);

    @Query("SELECT e FROM YCEstadia e WHERE (e.ycGrupo = :grupo OR (:grupo IS NULL AND e.ycGrupo IS NULL)) ORDER BY e.ycId ASC")
    List<YCEstadia> ycFindByGrupoOrderByIdAsc(@Param("grupo") Long ycGrupo);

    @Query("SELECT e FROM YCEstadia e WHERE e.ycEntrada = :entrada AND e.ycCancelada = false ORDER BY e.ycCuartoNumero")
    List<YCEstadia> ycFindByEntradaAndCanceladaFalseOrderByCuartoNumero(@Param("entrada") LocalDate ycEntrada);
}
