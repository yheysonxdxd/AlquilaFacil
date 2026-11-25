package com.alquilafacil.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ajuste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YCAjuste {
    @Id
    @Column(length = 60)
    private String ycClave;

    @Column(length = 300)
    private String ycValor;
}
