package com.alquilafacil.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YCUsuarioServiceTest {
    @Test
    void ycLaContrasenaSeGuardaCifradaYSeVerifica() {
        String ycSal = "c2FsLWRlLXBydWViYS0xMjM=";
        String ycHash = YCUsuarioService.ycCifrar("secreta123", ycSal);
        assertThat(ycHash).doesNotContain("secreta123");
        assertThat(YCUsuarioService.ycCoincide("secreta123", ycSal, ycHash)).isTrue();
        assertThat(YCUsuarioService.ycCoincide("otra", ycSal, ycHash)).isFalse();
        assertThat(YCUsuarioService.ycCoincide(null, ycSal, ycHash)).isFalse();
    }

    @Test
    void ycCoincideConElCifradoHechoFueraDelPrograma() {
        assertThat(YCUsuarioService.ycCifrar("yheyson2026", "c2FsLWRlLXBydWViYS0xMjM="))
                .isEqualTo("bxmelq/EnAvZT49q8w7ZqpDuKKSBcqSBhsNrO5xu9aM=");
    }

    @Test
    void ycElUsuarioSeLimpiaSolo() {
        assertThat(YCUsuarioService.ycNormalizar("  Yhéyson ")).isEqualTo("yheyson");
    }
}
