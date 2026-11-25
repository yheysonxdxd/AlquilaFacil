package com.alquilafacil.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario", columnNames = "usuario"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "ycId")
public class YCUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @Column(nullable = false, length = 40)
    private String ycUsuario;

    @Column(nullable = false, length = 80)
    private String ycNombre;

    @Convert(converter = YCConvertidoresEnum.YCConvertidorRol.class)
    @Column(nullable = false, length = 15)
    private YCRol ycRol;

    @Column(nullable = false, length = 120)
    private String ycHash;

    @Column(nullable = false, length = 40)
    private String ycSal;

    @Column(nullable = false)
    private boolean ycActivo;

    private LocalDateTime ycUltimoIngreso;

    @Column(length = 120)
    private String ycCodigoRecuperacionHash;

    @Column(length = 40)
    private String ycCodigoRecuperacionSal;

    public boolean ycTieneCodigoRecuperacion() {
        return ycCodigoRecuperacionHash != null && ycCodigoRecuperacionSal != null;
    }

    public boolean ycEsAdmin() {
        return ycRol == YCRol.YC_ADMINISTRADOR;
    }
}
