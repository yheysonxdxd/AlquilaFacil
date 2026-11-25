package com.alquilafacil.repository;

import com.alquilafacil.model.YCLiquidacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YCLiquidacionRepository extends JpaRepository<YCLiquidacion, Long> {
    @Query("SELECT l FROM YCLiquidacion l WHERE l.ycInquilino.ycId = :inquilinoId ORDER BY l.ycId DESC")
    List<YCLiquidacion> ycUltimasPorInquilino(@Param("inquilinoId") Long ycInquilinoId, Pageable ycPagina);

    default Optional<YCLiquidacion> ycFindFirstByInquilinoIdOrderByIdDesc(Long ycInquilinoId) {
        return ycUltimasPorInquilino(ycInquilinoId, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("SELECT l FROM YCLiquidacion l WHERE l.ycFecha >= :desde AND l.ycFecha < :hasta ORDER BY l.ycFecha")
    List<YCLiquidacion> ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFecha(
            @Param("desde") java.time.LocalDateTime ycDesde, @Param("hasta") java.time.LocalDateTime ycHasta);
}
