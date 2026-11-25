package com.alquilafacil.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", indexes = @Index(name = "ix_auditoria_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YCAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ycId;

    @Column(nullable = false)
    private LocalDateTime ycFecha;

    @Column(nullable = false, length = 40)
    private String ycUsuario;

    @Column(nullable = false, length = 60)
    private String ycAccion;

    @Column(length = 600)
    private String ycDetalle;
}
