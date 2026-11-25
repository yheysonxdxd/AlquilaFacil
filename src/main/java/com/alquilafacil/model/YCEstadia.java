package com.alquilafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estadia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "ycId")
public class YCEstadia {
    public enum YCEstado {
        YC_PROXIMA("Proxima"),
        YC_EN_CURSO("Hospedado"),
        YC_TERMINADA("Terminada"),
        YC_CANCELADA("Cancelada");

        private final String ycTexto;

        YCEstado(String ycTexto) {
            this.ycTexto = ycTexto;
        }

        public String ycGetTexto() {
            return ycTexto;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "cuarto_id", nullable = false)
    private YCCuarto ycCuarto;

    @Column(nullable = false, length = 30)
    private String ycCuartoNumero;

    @Column(nullable = false, length = 120)
    private String ycHuesped;

    @Column(length = 20)
    private String ycDocumento;

    @Column(length = 15)
    private String ycTelefono;

    @Column(nullable = false)
    private Integer ycPersonas;

    @Column(nullable = false)
    private LocalDate ycEntrada;

    @Column(nullable = false)
    private LocalDate ycSalida;

    @Column(nullable = false)
    private Integer ycNoches;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycPrecioNoche;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ycTotal;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(nullable = false, length = 20)
    private YCMetodoPago ycMetodo;

    @Column(length = 200)
    private String ycNota;

    @Column(nullable = false)
    private boolean ycCancelada;

    @Column(nullable = false)
    private LocalDateTime ycFechaRegistro;

    @Column(precision = 10, scale = 2)
    private BigDecimal ycAdelanto;

    private Boolean ycSaldoPagado;

    private LocalDateTime ycFechaSaldo;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(length = 20)
    private YCMetodoPago ycMetodoSaldo;

    @Column(precision = 10, scale = 2)
    private BigDecimal ycMontoSaldo;

    @Column(precision = 10, scale = 2)
    private BigDecimal ycDevuelto;

    private LocalDateTime ycFechaDevolucion;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorMetodoPago.class)
    @Column(length = 20)
    private YCMetodoPago ycMetodoDevolucion;

    public YCMetodoPago ycGetMetodoDevolucionODefecto() {
        return ycMetodoDevolucion == null ? YCMetodoPago.YC_EFECTIVO : ycMetodoDevolucion;
    }

    private LocalDate ycSalidaOriginal;

    private Integer ycNochesOriginales;

    private Boolean ycRecordatorioEnviado;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorCanalReserva.class)
    @Column(length = 20)
    private YCCanalReserva ycCanal;

    private Boolean ycLlego;

    private LocalDateTime ycFechaLlegada;

    private Boolean ycNoLlego;

    @Column(length = 40)
    private String ycRegistradoPor;

    @Column(length = 40)
    private String ycSaldoCobradoPor;

    @Column(length = 40)
    private String ycDevueltoPor;

    @Column(length = 40)
    private String ycCanceladaPor;

    private LocalDateTime ycFechaCancelacion;

    @Column(length = 200)
    private String ycMotivoCancelacion;

    private Long ycGrupo;

    public boolean ycIsDeGrupo() {
        return ycGrupo != null;
    }

    public BigDecimal ycGetRecibido() {
        if (ycAdelanto == null) {
            return ycTotal;
        }
        BigDecimal ycR = ycAdelanto;
        if (Boolean.TRUE.equals(ycSaldoPagado)) {
            ycR = ycR.add(ycMontoSaldo != null ? ycMontoSaldo : ycTotal.subtract(ycAdelanto));
        }
        return ycR;
    }

    public BigDecimal ycGetPagado() {
        return ycGetRecibido().subtract(ycDevuelto == null ? BigDecimal.ZERO : ycDevuelto);
    }

    public boolean ycSalioAntes() {
        return ycSalidaOriginal != null;
    }

    public BigDecimal ycGetSaldo() {
        if (ycCancelada) {
            return BigDecimal.ZERO;
        }
        BigDecimal ycSaldo = ycTotal.subtract(ycGetPagado());
        return ycSaldo.signum() < 0 ? BigDecimal.ZERO : ycSaldo;
    }

    public boolean ycTieneSaldo() {
        return ycGetSaldo().signum() > 0;
    }

    public boolean ycIsRecordatorioEnviado() {
        return Boolean.TRUE.equals(ycRecordatorioEnviado);
    }

    public boolean ycIsLlego() {
        return !Boolean.FALSE.equals(ycLlego);
    }

    public boolean ycIsNoLlego() {
        return Boolean.TRUE.equals(ycNoLlego);
    }

    public boolean ycEsperandoLlegada(LocalDate ycHoy) {
        return ycEstado(ycHoy) == YCEstado.YC_EN_CURSO && !ycIsLlego();
    }

    public boolean ycHospedado(LocalDate ycHoy) {
        return ycEstado(ycHoy) == YCEstado.YC_EN_CURSO && ycIsLlego();
    }

    public YCEstado ycEstado(LocalDate ycHoy) {
        if (ycCancelada) {
            return YCEstado.YC_CANCELADA;
        }
        if (ycHoy.isBefore(ycEntrada)) {
            return YCEstado.YC_PROXIMA;
        }
        return ycHoy.isBefore(ycSalida) ? YCEstado.YC_EN_CURSO : YCEstado.YC_TERMINADA;
    }

    public String ycGetNumero() {
        return ycId == null ? "H-------" : String.format("H-%06d", ycId);
    }
}
