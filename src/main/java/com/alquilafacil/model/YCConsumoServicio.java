package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "consumo_servicio",
        uniqueConstraints = @UniqueConstraint(name = "uk_consumo_cuarto_periodo",
                columnNames = {"cuarto_id", "periodo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCConsumoServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotNull(message = "Selecciona un cuarto")
    @ManyToOne(optional = false)
    @JoinColumn(name = "cuarto_id", nullable = false)
    private YCCuarto ycCuarto;

    @NotNull(message = "Selecciona el mes")
    @Convert(converter = YCPeriodoConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth ycPeriodo;

    @NotNull
    @DecimalMin(value = "0.00", message = "No puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ycLuz = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "No puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ycAgua = BigDecimal.ZERO;

    public BigDecimal ycGetTotal() {
        return ycLuz.add(ycAgua);
    }
}
