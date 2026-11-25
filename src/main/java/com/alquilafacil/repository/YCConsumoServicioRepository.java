package com.alquilafacil.repository;

import com.alquilafacil.model.YCConsumoServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface YCConsumoServicioRepository extends JpaRepository<YCConsumoServicio, Long> {
    @Query("SELECT s FROM YCConsumoServicio s WHERE s.ycCuarto.ycId = :cuartoId AND s.ycPeriodo = :periodo")
    Optional<YCConsumoServicio> ycFindByCuartoIdAndPeriodo(@Param("cuartoId") Long ycCuartoId, @Param("periodo") YearMonth ycPeriodo);

    @Query("SELECT s FROM YCConsumoServicio s WHERE s.ycCuarto.ycId = :cuartoId")
    List<YCConsumoServicio> ycFindByCuartoId(@Param("cuartoId") Long ycCuartoId);

    default void ycDeleteByCuartoId(Long ycCuartoId) {
        deleteAll(ycFindByCuartoId(ycCuartoId));
    }

    @Query("""
            SELECT s FROM YCConsumoServicio s
            WHERE :texto = '' OR LOWER(s.ycCuarto.ycNumero) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<YCConsumoServicio> ycBuscar(@Param("texto") String ycTexto, Pageable ycPageable);
}
