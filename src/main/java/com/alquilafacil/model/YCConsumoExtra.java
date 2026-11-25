package com.alquilafacil.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumo_extra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCConsumoExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estadia_id", nullable = false)
    @ToString.Exclude
    private YCEstadia ycEstadia;

    @Column(nullable = false, length = 60)
    private String ycConcepto;

    @Column(nullable = false)
    private Integer ycCantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycPrecioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycTotal;

    @Column(nullable = false)
    private LocalDateTime ycFechaRegistro;

    @Column(length = 40)
    private String ycRegistradoPor;

    @Column(nullable = false)
    private boolean ycPagado;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(length = 20)
    private YCMetodoPago ycMetodo;

    private LocalDateTime ycFechaPago;

    @Column(length = 40)
    private String ycCobradoPor;

    @Column(nullable = false)
    private boolean ycAnulado;

    @Column(length = 200)
    private String ycMotivoAnulacion;

    @Column(length = 40)
    private String ycAnuladoPor;

    private LocalDateTime ycFechaAnulacion;

    public boolean ycIsPendiente() {
        return !ycAnulado && !ycPagado;
    }

    public String ycGetDescripcion() {
        return ycCantidad == null || ycCantidad == 1 ? ycConcepto : ycConcepto + " x" + ycCantidad;
    }
}
