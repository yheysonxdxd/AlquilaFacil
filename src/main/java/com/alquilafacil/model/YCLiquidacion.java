package com.alquilafacil.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "ycId")
public class YCLiquidacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private YCInquilino ycInquilino;

    @Column(nullable = false, length = 30)
    private String ycCuartoNumero;

    @Column(nullable = false)
    private LocalDateTime ycFecha;

    @Column(length = 300)
    private String ycMesesPagados;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycDeudaPagada;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycDeudaPendiente;

    @Column(length = 300)
    private String ycMesesPendientes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycDanos;

    @Column(length = 200)
    private String ycMotivoDanos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycGarantia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycCobrado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycDevuelto;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(length = 20)
    private YCMetodoPago ycMetodo;

    @Column(length = 40)
    private String ycRegistradoPor;

    public String ycGetNumero() {
        return ycId == null ? "L-------" : String.format("L-%06d", ycId);
    }
}
