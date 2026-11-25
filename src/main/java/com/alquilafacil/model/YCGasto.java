package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "ycId")
public class YCGasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate ycFecha;

    @NotNull(message = "Elige la categoria")
    @Convert(converter = YCConvertidoresEnum.YCConvertidorCategoriaGasto.class)
    @Column(nullable = false, length = 20)
    private YCCategoriaGasto ycCategoria;

    @NotNull(message = "Elige a que negocio corresponde")
    @Convert(converter = YCConvertidoresEnum.YCConvertidorNegocio.class)
    @Column(nullable = false, length = 12)
    private YCNegocio ycNegocio;

    @Size(max = 150, message = "Maximo 150 caracteres")
    @Column(length = 150)
    private String ycDescripcion;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.10", message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycMonto;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(length = 20)
    private YCMetodoPago ycMetodo;

    @Column(length = 40)
    private String ycRegistradoPor;

    @Column(nullable = false)
    private LocalDateTime ycFechaRegistro;
}
