package com.alquilafacil.repository;

import com.alquilafacil.model.YCGasto;
import com.alquilafacil.model.YCNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface YCGastoRepository extends JpaRepository<YCGasto, Long> {
    @Query("SELECT g FROM YCGasto g WHERE g.ycFecha >= :desde AND g.ycFecha < :hasta ORDER BY g.ycFecha DESC, g.ycId DESC")
    List<YCGasto> ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(@Param("desde") LocalDate ycDesde, @Param("hasta") LocalDate ycHasta);

    @Query("SELECT SUM(g.ycMonto) FROM YCGasto g WHERE g.ycNegocio = :negocio AND g.ycFecha >= :desde AND g.ycFecha < :hasta")
    BigDecimal ycSumar(@Param("negocio") YCNegocio ycNegocio, @Param("desde") LocalDate ycDesde, @Param("hasta") LocalDate ycHasta);
}
