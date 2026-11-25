package com.alquilafacil.repository;

import com.alquilafacil.model.YCCierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface YCCierreCajaRepository extends JpaRepository<YCCierreCaja, Long> {
    @Query("SELECT c FROM YCCierreCaja c WHERE c.ycDia = :dia ORDER BY c.ycFechaCierre DESC")
    List<YCCierreCaja> ycFindByDiaOrderByFechaCierreDesc(@Param("dia") LocalDate ycDia);
}
