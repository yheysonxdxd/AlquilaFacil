package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private YCInquilino ycInquilino;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "cuarto_id", nullable = false)
    private YCCuarto ycCuarto;

    @Column(nullable = false, length = 30)
    private String ycCuartoNumero;

    @NotNull
    @Convert(converter = YCPeriodoConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth ycPeriodo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycAlquiler;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycLuz;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycAgua;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycTotal;

    @Column(nullable = false)
    private LocalDateTime ycFechaPago;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(nullable = false, length = 20)
    private YCMetodoPago ycMetodo;

    @Column(length = 200)
    private String ycNota;

    @Column(nullable = false)
    private boolean ycAnulado;

    @Column(length = 40)
    private String ycRegistradoPor;

    @Column(length = 40)
    private String ycAnuladoPor;

    private LocalDateTime ycFechaAnulacion;

    @Column(length = 200)
    private String ycMotivoAnulacion;

    public String ycGetNumeroRecibo() {
        return ycId == null ? "R-------" : String.format("R-%06d", ycId);
    }
}
