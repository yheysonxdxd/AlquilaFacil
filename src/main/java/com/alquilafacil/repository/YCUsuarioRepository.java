package com.alquilafacil.repository;

import com.alquilafacil.model.YCUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YCUsuarioRepository extends JpaRepository<YCUsuario, Long> {
    @Query("SELECT u FROM YCUsuario u WHERE LOWER(u.ycUsuario) = LOWER(:usuario)")
    Optional<YCUsuario> ycFindByUsuarioIgnoreCase(@Param("usuario") String ycUsuario);

    @Query("SELECT u FROM YCUsuario u ORDER BY u.ycNombre ASC")
    List<YCUsuario> ycFindAllByOrderByNombreAsc();

    @Query("SELECT COUNT(u) FROM YCUsuario u WHERE u.ycRol = :rol AND u.ycActivo = true")
    long ycCountByRolAndActivoTrue(@Param("rol") com.alquilafacil.model.YCRol ycRol);
}
