package com.alquilafacil.service;

import com.alquilafacil.config.YCDatosArrendador;
import com.alquilafacil.model.YCComodidad;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCModalidad;
import com.alquilafacil.model.YCNorma;
import com.alquilafacil.model.YCTipoHabitacion;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class YCComprobanteHospedajeServiceTest {
    @Test
    void ycGeneraUnPdfConLosDatosDeLaEstadia() throws Exception {
        YCDatosArrendador ycDatos = new YCDatosArrendador();
        ycDatos.setYcDireccion("Av. Principal 123");
        ycDatos.setYcTelefono("987654321");
        YCComprobanteHospedajeService ycServicio = new YCComprobanteHospedajeService(null, ycDatos);

        YCCuarto ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Habitación 402").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("80.00")).ycCapacidad(2).ycTipoHabitacion(YCTipoHabitacion.YC_MATRIMONIAL)
                .ycCamasDobles(1).ycHoraEntrada("14:00").ycHoraSalida("12:00")
                .ycComodidades(EnumSet.of(YCComodidad.YC_WIFI, YCComodidad.YC_TOALLAS, YCComodidad.YC_DESAYUNO))
                .ycNormas(EnumSet.of(YCNorma.YC_NO_FUMAR, YCNorma.YC_SILENCIO)).build();
        YCEstadia ycEstadia = YCEstadia.builder().ycId(12L).ycCuarto(ycCuarto).ycCuartoNumero("Habitación 402")
                .ycHuesped("Andrea Salazar").ycDocumento("45896321").ycTelefono("963456789").ycPersonas(2)
                .ycEntrada(LocalDate.of(2026, 11, 3)).ycSalida(LocalDate.of(2026, 11, 6)).ycNoches(3)
                .ycPrecioNoche(new BigDecimal("80.00")).ycTotal(new BigDecimal("240.00"))
                .ycMetodo(YCMetodoPago.YC_YAPE).ycNota("Llega tarde").ycCancelada(false)
                .ycFechaRegistro(LocalDateTime.of(2026, 11, 3, 14, 30)).build();

        ByteArrayOutputStream ycSalida = new ByteArrayOutputStream();
        ycServicio.ycEscribir(ycEstadia, ycSalida);

        byte[] ycPdf = ycSalida.toByteArray();
        assertThat(new String(ycPdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(ycPdf.length).isGreaterThan(1500);
        assertThat(YCComprobanteHospedajeService.ycNombreArchivo(ycEstadia))
                .isEqualTo("Comprobante H-000012 - Andrea Salazar");

        ycEstadia.setYcAdelanto(new BigDecimal("50.00"));
        ycEstadia.setYcSaldoPagado(false);
        try (FileOutputStream ycF = new FileOutputStream("target/comprobante-ejemplo.pdf")) {
            ycServicio.ycEscribir(ycEstadia, ycF);
        }
    }

    @Test
    void ycElComprobanteTraeLosConsumosYElQrDeYape(@org.junit.jupiter.api.io.TempDir java.nio.file.Path ycCarpeta) throws Exception {
        YCDatosArrendador ycDatos = new YCDatosArrendador();
        YCComprobanteHospedajeService ycServicio = new YCComprobanteHospedajeService(null, ycDatos);
        YCCuarto ycCuarto = YCCuarto.builder().ycId(1L).ycNumero("Habitación 402").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("80.00")).build();
        YCEstadia ycEstadia = YCEstadia.builder().ycId(12L).ycCuarto(ycCuarto).ycCuartoNumero("Habitación 402")
                .ycHuesped("Andrea Salazar").ycPersonas(2).ycEntrada(LocalDate.of(2026, 11, 3))
                .ycSalida(LocalDate.of(2026, 11, 5)).ycNoches(2).ycPrecioNoche(new BigDecimal("80.00"))
                .ycTotal(new BigDecimal("160.00")).ycMetodo(YCMetodoPago.YC_YAPE).ycCancelada(false)
                .ycFechaRegistro(LocalDateTime.of(2026, 11, 3, 14, 30)).build();
        LocalDateTime ycHora = LocalDateTime.of(2026, 11, 4, 8, 0);
        var ycDesayuno = com.alquilafacil.model.YCConsumoExtra.builder().ycId(1L).ycEstadia(ycEstadia).ycConcepto("Desayuno")
                .ycCantidad(2).ycPrecioUnitario(new BigDecimal("12.00")).ycTotal(new BigDecimal("24.00"))
                .ycFechaRegistro(ycHora).ycPagado(false).ycAnulado(false).build();
        var ycLavanderia = com.alquilafacil.model.YCConsumoExtra.builder().ycId(2L).ycEstadia(ycEstadia).ycConcepto("Lavanderia")
                .ycCantidad(1).ycPrecioUnitario(new BigDecimal("15.00")).ycTotal(new BigDecimal("15.00"))
                .ycFechaRegistro(ycHora).ycPagado(true).ycMetodo(YCMetodoPago.YC_YAPE).ycAnulado(false).build();
        YCConsumoExtraService ycConsumos = org.mockito.Mockito.mock(YCConsumoExtraService.class);
        org.mockito.Mockito.when(ycConsumos.ycDeEstadia(ycEstadia)).thenReturn(java.util.List.of(ycDesayuno, ycLavanderia));
        ycServicio.ycSetConsumos(ycConsumos);

        java.awt.image.BufferedImage ycImg = new java.awt.image.BufferedImage(120, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D ycG = ycImg.createGraphics();
        ycG.setColor(java.awt.Color.WHITE);
        ycG.fillRect(0, 0, 120, 120);
        ycG.setColor(java.awt.Color.BLACK);
        for (int ycI = 0; ycI < 12; ycI++) {
            for (int ycJ = 0; ycJ < 12; ycJ++) {
                if ((ycI * 7 + ycJ * 3) % 5 < 2) ycG.fillRect(ycI * 10, ycJ * 10, 10, 10);
            }
        }
        ycG.dispose();
        java.nio.file.Path ycPng = ycCarpeta.resolve("mi-qr.png");
        javax.imageio.ImageIO.write(ycImg, "png", ycPng.toFile());
        YCSesion ycSesion = new YCSesion();
        ycSesion.ycIniciar(com.alquilafacil.model.YCUsuario.builder().ycId(1L).ycUsuario("admin")
                .ycRol(com.alquilafacil.model.YCRol.YC_ADMINISTRADOR).build());
        YCPagoDigitalService ycPago = new YCPagoDigitalService(
                org.mockito.Mockito.mock(com.alquilafacil.repository.YCAjusteRepository.class), ycDatos, ycSesion,
                new YCAuditoriaService(org.mockito.Mockito.mock(com.alquilafacil.repository.YCAuditoriaRepository.class), ycSesion),
                ycCarpeta.toString());
        ycPago.ycGuardar("987 654 321", "Rosa Quispe", ycPng, false);
        assertThat(ycPago.ycTexto()).isEqualTo("Yape / Plin al 987 654 321 (Rosa Quispe)");
        assertThat(ycPago.ycQr()).isPresent();
        ycServicio.ycSetPagoDigital(ycPago);

        ByteArrayOutputStream ycSalida = new ByteArrayOutputStream();
        ycServicio.ycEscribir(ycEstadia, ycSalida);
        assertThat(new String(ycSalida.toByteArray(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        try (FileOutputStream ycF = new FileOutputStream("target/comprobante-consumos-ejemplo.pdf")) {
            ycServicio.ycEscribir(ycEstadia, ycF);
        }
    }

    @Test
    void ycUnSoloComprobanteParaUnaReservaDeGrupo() throws Exception {
        YCComprobanteHospedajeService ycServicio = new YCComprobanteHospedajeService(null, new YCDatosArrendador());
        YCCuarto ycC1 = YCCuarto.builder().ycId(1L).ycNumero("Habitación 401").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("80.00")).ycNormas(EnumSet.of(YCNorma.YC_NO_FUMAR)).build();
        YCCuarto ycC2 = YCCuarto.builder().ycId(2L).ycNumero("Habitación 402").ycModalidad(YCModalidad.YC_POR_NOCHE)
                .ycPrecio(new BigDecimal("60.00")).build();
        YCEstadia ycA = YCEstadia.builder().ycId(30L).ycGrupo(30L).ycCuarto(ycC1).ycCuartoNumero("Habitación 401")
                .ycHuesped("Carlos Ramos").ycDocumento("41234567").ycTelefono("963456789").ycPersonas(2)
                .ycEntrada(LocalDate.of(2026, 11, 3)).ycSalida(LocalDate.of(2026, 11, 5)).ycNoches(2)
                .ycPrecioNoche(new BigDecimal("80.00")).ycTotal(new BigDecimal("160.00")).ycAdelanto(new BigDecimal("160.00"))
                .ycSaldoPagado(true).ycMetodo(YCMetodoPago.YC_EFECTIVO).ycCancelada(false)
                .ycFechaRegistro(LocalDateTime.of(2026, 11, 3, 14, 30)).build();
        YCEstadia ycB = YCEstadia.builder().ycId(31L).ycGrupo(30L).ycCuarto(ycC2).ycCuartoNumero("Habitación 402")
                .ycHuesped("Carlos Ramos").ycDocumento("41234567").ycTelefono("963456789").ycPersonas(3)
                .ycEntrada(LocalDate.of(2026, 11, 3)).ycSalida(LocalDate.of(2026, 11, 5)).ycNoches(2)
                .ycPrecioNoche(new BigDecimal("60.00")).ycTotal(new BigDecimal("120.00")).ycAdelanto(new BigDecimal("40.00"))
                .ycSaldoPagado(false).ycMetodo(YCMetodoPago.YC_EFECTIVO).ycCancelada(false)
                .ycFechaRegistro(LocalDateTime.of(2026, 11, 3, 14, 30)).build();

        ByteArrayOutputStream ycSalida = new ByteArrayOutputStream();
        ycServicio.ycEscribirGrupo(java.util.List.of(ycA, ycB), ycSalida);
        assertThat(new String(ycSalida.toByteArray(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        try (FileOutputStream ycF = new FileOutputStream("target/comprobante-grupo-ejemplo.pdf")) {
            ycServicio.ycEscribirGrupo(java.util.List.of(ycA, ycB), ycF);
        }
    }
}
