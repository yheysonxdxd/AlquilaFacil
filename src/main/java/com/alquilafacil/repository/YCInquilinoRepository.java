package com.alquilafacil.repository;

import com.alquilafacil.model.YCInquilino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YCInquilinoRepository extends JpaRepository<YCInquilino, Long> {
    @Query("SELECT i FROM YCInquilino i WHERE i.ycActivo = true ORDER BY i.ycApellido ASC, i.ycNombre ASC")
    List<YCInquilino> ycFindByActivoTrueOrderByApellidoAscNombreAsc();

    @Query("SELECT i FROM YCInquilino i WHERE i.ycCuarto.ycId = :cuartoId AND i.ycActivo = true")
    Optional<YCInquilino> ycFindByCuartoIdAndActivoTrue(@Param("cuartoId") Long ycCuartoId);

    @Query("SELECT i FROM YCInquilino i WHERE (i.ycDni = :dni OR (:dni IS NULL AND i.ycDni IS NULL)) AND i.ycActivo = true")
    Optional<YCInquilino> ycFindByDniAndActivoTrue(@Param("dni") String ycDni);

    @Query("SELECT i FROM YCInquilino i WHERE i.ycDni = :dni ORDER BY i.ycId DESC")
    List<YCInquilino> ycUltimosPorDni(@Param("dni") String ycDni, Pageable ycPagina);

    default Optional<YCInquilino> ycFindFirstByDniOrderByIdDesc(String ycDni) {
        return ycUltimosPorDni(ycDni, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("SELECT COUNT(i) > 0 FROM YCInquilino i WHERE i.ycCuarto.ycId = :cuartoId")
    boolean ycExistsByCuartoId(@Param("cuartoId") Long ycCuartoId);

    @Query("SELECT COUNT(i) FROM YCInquilino i WHERE i.ycActivo = true")
    long ycCountByActivoTrue();

    @Query("""
            SELECT i FROM YCInquilino i
            WHERE (:todos = true OR i.ycActivo = true)
              AND (:texto = ''
                   OR LOWER(i.ycNombre)   LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(i.ycApellido) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR i.ycDni             LIKE CONCAT('%', :texto, '%')
                   OR LOWER(i.ycCuarto.ycNumero) LIKE LOWER(CONCAT('%', :texto, '%')))
            """)
    Page<YCInquilino> ycBuscar(@Param("texto") String ycTexto, @Param("todos") boolean ycTodos, Pageable ycPageable);
}
