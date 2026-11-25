package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "cuarto",
        uniqueConstraints = @UniqueConstraint(name = "uk_cuarto_numero", columnNames = "numero"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCCuarto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotBlank(message = "El numero o nombre del cuarto es obligatorio")
    @Size(max = 30, message = "Maximo 30 caracteres")
    @Column(nullable = false, length = 30)
    private String ycNumero;

    @Min(value = 0, message = "El piso no puede ser negativo")
    @Max(value = 30, message = "Piso no valido")
    private Integer ycPiso;

    @Size(max = 300, message = "Maximo 300 caracteres")
    @Column(length = 300)
    private String ycDescripcion;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorModalidad.class)
    @Column(length = 12)
    private YCModalidad ycModalidad;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "1.00", message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycPrecio;

    @Min(value = 1, message = "Minimo 1 persona")
    @Max(value = 20, message = "Maximo 20 personas")
    private Integer ycCapacidad;

    @Column(length = 120)
    private String ycFoto;

    @Convert(converter = YCComodidadesConverter.class)
    @Column(length = 400)
    @Builder.Default
    private Set<YCComodidad> ycComodidades = EnumSet.noneOf(YCComodidad.class);

    @Convert(converter = YCConvertidoresEnum.YCConvertidorTipoHabitacion.class)
    @Column(length = 15)
    private YCTipoHabitacion ycTipoHabitacion;

    @Min(value = 0, message = "No puede ser negativo")
    @Max(value = 10, message = "Maximo 10")
    private Integer ycCamasDobles;

    @Min(value = 0, message = "No puede ser negativo")
    @Max(value = 20, message = "Maximo 20")
    private Integer ycCamasSimples;

    @Min(value = 1, message = "Tamano no valido")
    @Max(value = 500, message = "Tamano no valido")
    private Integer ycMetros;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "Usa el formato 14:00")
    @Column(length = 5)
    private String ycHoraEntrada;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "Usa el formato 12:00")
    @Column(length = 5)
    private String ycHoraSalida;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorEstadoLimpieza.class)
    @Column(length = 15)
    private YCEstadoLimpieza ycLimpieza;

    @Column(length = 60)
    private String ycFoto360;

    @Column(length = 800)
    private String ycFotosExtra;

    @Convert(converter = YCNormasConverter.class)
    @Column(length = 300)
    @Builder.Default
    private Set<YCNorma> ycNormas = EnumSet.noneOf(YCNorma.class);

    public Set<YCComodidad> ycGetComodidades() {
        return ycComodidades == null ? EnumSet.noneOf(YCComodidad.class) : ycComodidades;
    }

    public YCEstadoLimpieza ycGetLimpieza() {
        return ycLimpieza == null ? YCEstadoLimpieza.YC_LIMPIA : ycLimpieza;
    }

    public java.util.List<String> ycGetGaleria() {
        if (ycFotosExtra == null || ycFotosExtra.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(ycFotosExtra.split(",")).map(String::trim).filter(ycF -> !ycF.isEmpty()).toList();
    }

    public void ycSetGaleria(java.util.List<String> ycFotos) {
        ycFotosExtra = ycFotos == null || ycFotos.isEmpty() ? null : String.join(",", ycFotos);
    }

    public Set<YCNorma> ycGetNormas() {
        return ycNormas == null ? EnumSet.noneOf(YCNorma.class) : ycNormas;
    }

    public String ycGetCamasTexto() {
        int ycDobles = ycCamasDobles == null ? 0 : ycCamasDobles;
        int ycSimples = ycCamasSimples == null ? 0 : ycCamasSimples;
        if (ycDobles == 0 && ycSimples == 0) {
            return null;
        }
        StringBuilder ycSb = new StringBuilder();
        if (ycDobles > 0) {
            ycSb.append(ycDobles).append(ycDobles == 1 ? " cama doble" : " camas dobles");
        }
        if (ycSimples > 0) {
            if (!ycSb.isEmpty()) {
                ycSb.append(" + ");
            }
            ycSb.append(ycSimples).append(ycDobles > 0
                    ? (ycSimples == 1 ? " simple" : " simples")
                    : (ycSimples == 1 ? " cama simple" : " camas simples"));
        }
        return ycSb.toString();
    }

    public String ycGetHoraEntradaODefecto() {
        return ycHoraEntrada == null || ycHoraEntrada.isBlank() ? "14:00" : ycHoraEntrada;
    }

    public String ycGetHoraSalidaODefecto() {
        return ycHoraSalida == null || ycHoraSalida.isBlank() ? "12:00" : ycHoraSalida;
    }

    public YCModalidad ycGetModalidad() {
        return ycModalidad == null ? YCModalidad.YC_MENSUAL : ycModalidad;
    }

    public boolean ycIsHospedaje() {
        return ycGetModalidad() == YCModalidad.YC_POR_NOCHE;
    }

    public String ycGetEtiqueta() {
        return ycNumero + " · S/ " + ycPrecio.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                + " " + ycGetModalidad().ycGetSufijo();
    }
}
