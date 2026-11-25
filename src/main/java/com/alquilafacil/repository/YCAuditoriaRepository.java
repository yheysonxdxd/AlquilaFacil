package com.alquilafacil.repository;

import com.alquilafacil.model.YCAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface YCAuditoriaRepository extends JpaRepository<YCAuditoria, Long> {
    @Query("""
            SELECT a FROM YCAuditoria a
            WHERE a.ycFecha >= :desde
              AND (:texto = '' OR LOWER(a.ycAccion) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(a.ycDetalle) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(a.ycUsuario) LIKE LOWER(CONCAT('%', :texto, '%')))
            ORDER BY a.ycFecha DESC, a.ycId DESC
            """)
    List<YCAuditoria> ycBuscar(@Param("desde") LocalDateTime ycDesde, @Param("texto") String ycTexto, Pageable ycPagina);
}
