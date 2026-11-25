package com.alquilafacil.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierre_caja", indexes = @Index(name = "ix_cierre_dia", columnList = "dia"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YCCierreCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @Column(nullable = false)
    private LocalDate ycDia;

    @Column(length = 40)
    private String ycUsuarioCaja;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycEntradas;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycSalidas;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycEfectivoEsperado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycEfectivoContado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycDiferencia;

    @Column(length = 200)
    private String ycNota;

    @Column(nullable = false, length = 40)
    private String ycCerradoPor;

    @Column(nullable = false)
    private LocalDateTime ycFechaCierre;
}
