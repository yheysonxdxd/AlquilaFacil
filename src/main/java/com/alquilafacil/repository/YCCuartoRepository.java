package com.alquilafacil.repository;

import com.alquilafacil.model.YCCuarto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YCCuartoRepository extends JpaRepository<YCCuarto, Long> {
    @Query("SELECT c FROM YCCuarto c WHERE LOWER(c.ycNumero) = LOWER(:numero)")
    Optional<YCCuarto> ycFindByNumeroIgnoreCase(@Param("numero") String ycNumero);

    @Query("SELECT c FROM YCCuarto c ORDER BY c.ycNumero ASC")
    List<YCCuarto> ycFindAllByOrderByNumeroAsc();

    @Query("""
            SELECT c FROM YCCuarto c
            WHERE (c.ycModalidad IS NULL OR c.ycModalidad = com.alquilafacil.model.YCModalidad.YC_MENSUAL)
              AND NOT EXISTS (SELECT i.ycId FROM YCInquilino i WHERE i.ycCuarto = c AND i.ycActivo = true)
            ORDER BY c.ycNumero
            """)
    List<YCCuarto> ycLibres();
}
