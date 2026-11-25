package com.alquilafacil.repository;

import com.alquilafacil.model.YCPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Repository
public interface YCPagoRepository extends JpaRepository<YCPago, Long> {
    @Query("SELECT p FROM YCPago p WHERE p.ycInquilino.ycId = :inquilinoId AND p.ycAnulado = false")
    List<YCPago> ycFindByInquilinoIdAndAnuladoFalse(@Param("inquilinoId") Long ycInquilinoId);

    @Query("SELECT COUNT(p) > 0 FROM YCPago p WHERE p.ycInquilino.ycId = :inquilinoId AND p.ycPeriodo = :periodo AND p.ycAnulado = false")
    boolean ycExistsByInquilinoIdAndPeriodoAndAnuladoFalse(@Param("inquilinoId") Long ycInquilinoId, @Param("periodo") YearMonth ycPeriodo);

    @Query("SELECT COUNT(p) > 0 FROM YCPago p WHERE p.ycCuarto.ycId = :cuartoId AND p.ycPeriodo = :periodo AND p.ycAnulado = false")
    boolean ycExistsByCuartoIdAndPeriodoAndAnuladoFalse(@Param("cuartoId") Long ycCuartoId, @Param("periodo") YearMonth ycPeriodo);

    @Query("SELECT COUNT(p) > 0 FROM YCPago p WHERE p.ycCuarto.ycId = :cuartoId")
    boolean ycExistsByCuartoId(@Param("cuartoId") Long ycCuartoId);

    @Query("SELECT COUNT(p) > 0 FROM YCPago p WHERE p.ycInquilino.ycId = :inquilinoId")
    boolean ycExistsByInquilinoId(@Param("inquilinoId") Long ycInquilinoId);

    @Query("""
            SELECT SUM(p.ycTotal) FROM YCPago p
            WHERE p.ycAnulado = false AND p.ycFechaPago >= :desde AND p.ycFechaPago < :hasta
            """)
    BigDecimal ycSumarCobrado(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("SELECT p FROM YCPago p WHERE p.ycAnulado = false AND p.ycFechaPago >= :desde AND p.ycFechaPago < :hasta ORDER BY p.ycFechaPago")
    List<YCPago> ycFindByAnuladoFalseAndFechaPagoGreaterThanEqualAndFechaPagoLessThanOrderByFechaPago(
            @Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("SELECT p FROM YCPago p WHERE p.ycFechaPago >= :desde AND p.ycFechaPago < :hasta ORDER BY p.ycFechaPago")
    List<YCPago> ycFindByFechaPagoGreaterThanEqualAndFechaPagoLessThanOrderByFechaPago(@Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("SELECT p FROM YCPago p WHERE p.ycFechaAnulacion >= :desde AND p.ycFechaAnulacion < :hasta ORDER BY p.ycFechaAnulacion")
    List<YCPago> ycFindByFechaAnulacionGreaterThanEqualAndFechaAnulacionLessThanOrderByFechaAnulacion(
            @Param("desde") LocalDateTime ycDesde, @Param("hasta") LocalDateTime ycHasta);

    @Query("""
            SELECT p FROM YCPago p
            WHERE :texto = ''
               OR LOWER(p.ycInquilino.ycNombre)   LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.ycInquilino.ycApellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.ycCuartoNumero)       LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<YCPago> ycBuscar(@Param("texto") String ycTexto, Pageable ycPageable);
}
