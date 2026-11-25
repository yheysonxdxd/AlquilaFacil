package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inquilino")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCInquilino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    @Column(nullable = false, length = 60)
    private String ycNombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String ycApellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos")
    @Column(nullable = false, length = 8)
    private String ycDni;

    @Pattern(regexp = "|\\d{9}", message = "El celular debe tener 9 digitos")
    @Column(length = 9)
    private String ycTelefono;

    @NotNull(message = "Selecciona un cuarto")
    @ManyToOne(optional = false)
    @JoinColumn(name = "cuarto_id", nullable = false)
    private YCCuarto ycCuarto;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    @Column(nullable = false)
    private LocalDate ycFechaIngreso;

    @NotNull(message = "El dia de pago es obligatorio")
    @Min(value = 1, message = "El dia de pago va de 1 a 31")
    @Max(value = 31, message = "El dia de pago va de 1 a 31")
    @Column(nullable = false)
    private Integer ycDiaPago;

    @DecimalMin(value = "0.00", message = "La garantia no puede ser negativa")
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ycGarantia = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean ycActivo = true;

    private LocalDate ycFechaSalida;

    @DecimalMin(value = "0.10", message = "La renta debe ser mayor a 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal ycRenta;

    private LocalDate ycFechaFinContrato;

    public BigDecimal ycGetRentaMensual() {
        return ycRenta != null ? ycRenta : ycCuarto == null ? BigDecimal.ZERO : ycCuarto.getYcPrecio();
    }

    public String ycGetNombreCompleto() {
        return ycNombre + " " + ycApellido;
    }

    public Long ycDiasParaFinContrato(LocalDate ycHoy) {
        return ycFechaFinContrato == null ? null : java.time.temporal.ChronoUnit.DAYS.between(ycHoy, ycFechaFinContrato);
    }

    public boolean ycContratoPorVencer(LocalDate ycHoy, int ycDias) {
        Long ycFaltan = ycDiasParaFinContrato(ycHoy);
        return ycFaltan != null && ycFaltan <= ycDias;
    }
}
