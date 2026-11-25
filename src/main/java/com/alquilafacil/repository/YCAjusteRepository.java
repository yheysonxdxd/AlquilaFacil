package com.alquilafacil.repository;

import com.alquilafacil.model.YCAjuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YCAjusteRepository extends JpaRepository<YCAjuste, String> {
}
