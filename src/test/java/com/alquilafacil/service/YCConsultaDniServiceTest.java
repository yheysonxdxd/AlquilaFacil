package com.alquilafacil.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YCConsultaDniServiceTest {
    @Test
    void ycLeeFormatoDecolecta() {
        var ycP = YCConsultaDniService.ycInterpretar("""
                {"first_name":"MARIA DEL PILAR","first_last_name":"QUISPE","second_last_name":"MAMANI",
                 "full_name":"QUISPE MAMANI MARIA DEL PILAR","document_number":"46027897"}""").orElseThrow();
        assertThat(ycP.ycNombres()).isEqualTo("Maria Del Pilar");
        assertThat(ycP.ycApellidos()).isEqualTo("Quispe Mamani");
    }

    @Test
    void ycLeeFormatoApisNetPe() {
        var ycP = YCConsultaDniService.ycInterpretar("""
                {"nombres":"JOS\u00c9 LUIS","apellidoPaterno":"PE\u00d1A","apellidoMaterno":"ROJAS","numeroDocumento":"12345678"}""")
                .orElseThrow();
        assertThat(ycP.ycNombreCompleto()).isEqualTo("José Luis Peña Rojas");
    }

    @Test
    void ycLeeDatosAnidadosYNombreCompleto() {
        var ycP = YCConsultaDniService.ycInterpretar("""
                {"success":true,"data":{"nombre_completo":"TORRES DIAZ, GABRIEL"}}""").orElseThrow();
        assertThat(ycP.ycNombres()).isEqualTo("Gabriel");
        assertThat(ycP.ycApellidos()).isEqualTo("Torres Diaz");
    }

    @Test
    void ycRespuestaSinDatos() {
        assertThat(YCConsultaDniService.ycInterpretar("{\"error\":\"no encontrado\"}")).isEmpty();
    }
}
