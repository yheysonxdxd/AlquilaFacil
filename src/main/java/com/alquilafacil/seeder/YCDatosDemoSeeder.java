package com.alquilafacil.seeder;

import com.alquilafacil.model.YCComodidadesConverter;
import com.alquilafacil.model.YCConsumoServicio;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCModalidad;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCPago;
import com.alquilafacil.repository.YCConsumoServicioRepository;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCInquilinoRepository;
import com.alquilafacil.repository.YCPagoRepository;
import com.alquilafacil.service.YCCalendarioPagos;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class YCDatosDemoSeeder implements ApplicationRunner {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCDatosDemoSeeder.class);
    private final YCCuartoRepository ycCuartoRepository;
    private final YCInquilinoRepository ycInquilinoRepository;
    private final YCConsumoServicioRepository ycConsumoRepository;
    private final YCPagoRepository ycPagoRepository;
    private final YCEstadiaRepository ycEstadiaRepository;
    private final com.alquilafacil.repository.YCGastoRepository ycGastoRepository;

    private record YCDemo(String ycNombre, String ycApellido, String ycDni, String ycTelefono,
                        int ycMesesAntes, int ycDiaPago, int ycMesesQueDebe) {}

    @Override
    @Transactional
    public void run(ApplicationArguments ycArgs) {
        if (ycCuartoRepository.count() == 0) {
            ycSembrarAlquileres();
        }
        boolean ycHayHospedaje = ycCuartoRepository.findAll().stream().anyMatch(YCCuarto::ycIsHospedaje);
        if (!ycHayHospedaje && ycCuartoRepository.ycFindByNumeroIgnoreCase("Habitación 401").isEmpty()) {
            ycSembrarHospedaje();
        }
        ycCompletarDetallesHospedaje();
        if (ycCuartoRepository.ycFindByNumeroIgnoreCase("Habitación 601").isEmpty()) {
            ycSembrarAmpliacion();
        }
        if (ycGastoRepository.count() == 0) {
            ycSembrarGastos();
        }
    }

    private void ycSembrarGastos() {
        LocalDate ycHoy = LocalDate.now();
        Object[][] ycGastos = {
                {2, "YC_LUZ_EDIFICIO", "YC_GENERAL", "Recibo de luz de areas comunes", "185.40", YCMetodoPago.YC_TRANSFERENCIA},
                {3, "YC_AGUA_EDIFICIO", "YC_GENERAL", "Recibo de agua del edificio", "96.00", YCMetodoPago.YC_TRANSFERENCIA},
                {1, "YC_INTERNET", "YC_GENERAL", "Internet fibra 300 Mbps", "119.00", YCMetodoPago.YC_TRANSFERENCIA},
                {1, "YC_LIMPIEZA", "YC_HOSPEDAJE", "Lavanderia de sabanas y toallas", "140.00", YCMetodoPago.YC_YAPE},
                {0, "YC_INSUMOS", "YC_HOSPEDAJE", "Jabones, shampoo y papel higienico", "85.50", YCMetodoPago.YC_EFECTIVO},
                {4, "YC_MANTENIMIENTO", "YC_MENSUAL", "Cambio de chapa del Cuarto 204", "60.00", YCMetodoPago.YC_EFECTIVO},
                {33, "YC_LUZ_EDIFICIO", "YC_GENERAL", "Recibo de luz de areas comunes", "176.20", YCMetodoPago.YC_TRANSFERENCIA},
                {34, "YC_AGUA_EDIFICIO", "YC_GENERAL", "Recibo de agua del edificio", "92.00", YCMetodoPago.YC_TRANSFERENCIA},
                {32, "YC_INTERNET", "YC_GENERAL", "Internet fibra 300 Mbps", "119.00", YCMetodoPago.YC_TRANSFERENCIA},
                {36, "YC_PERSONAL", "YC_HOSPEDAJE", "Pago a recepcionista (medio tiempo)", "600.00", YCMetodoPago.YC_TRANSFERENCIA},
                {38, "YC_IMPUESTOS", "YC_GENERAL", "Arbitrios municipales", "210.00", YCMetodoPago.YC_TRANSFERENCIA},
        };
        for (Object[] ycG : ycGastos) {
            ycGastoRepository.save(com.alquilafacil.model.YCGasto.builder()
                    .ycFecha(ycHoy.minusDays((int) ycG[0]))
                    .ycCategoria(com.alquilafacil.model.YCCategoriaGasto.valueOf((String) ycG[1]))
                    .ycNegocio(com.alquilafacil.model.YCNegocio.valueOf((String) ycG[2]))
                    .ycDescripcion((String) ycG[3]).ycMonto(new BigDecimal((String) ycG[4])).ycMetodo((YCMetodoPago) ycG[5])
                    .ycRegistradoPor("ejemplo").ycFechaRegistro(java.time.LocalDateTime.now()).build());
        }
        YC_LOG.info("Gastos de ejemplo: {}", ycGastos.length);
    }

    private void ycSembrarAmpliacion() {
        YCComodidadesConverter ycComodidades = new YCComodidadesConverter();
        com.alquilafacil.model.YCNormasConverter ycNormas = new com.alquilafacil.model.YCNormasConverter();
        String[][] ycHospedaje = {
                {"Habitación 601", "6", "Simple acogedora con ventana amplia y escritorio. Ideal para viajes de trabajo.",
                        "50", "1", "YC_SIMPLE", "0", "1", "12", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_ESCRITORIO,YC_TOALLAS,YC_ARTICULOS_ASEO",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 602", "6", "Doble con dos camas twin y cabecero tapizado. Perfecta para amigos o colegas.",
                        "95", "2", "YC_DOBLE", "0", "2", "22", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_TV_CABLE",
                        "YC_NO_FUMAR,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 603", "6", "Matrimonial con cama king, luz calida y toallas de algodon.",
                        "100", "2", "YC_MATRIMONIAL", "1", "0", "20", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_DESAYUNO",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 604", "6", "Familiar con dos camas queen y espacio para equipaje. Ideal para familias.",
                        "140", "4", "YC_FAMILIAR", "2", "0", "28", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_TOALLAS,YC_TV_CABLE,YC_FRIGOBAR",
                        "YC_NO_FUMAR,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 605", "6", "Compartida con camarotes y casilleros. Se cobra por cama, para mochileros.",
                        "35", "4", "YC_COMPARTIDA", "0", "4", "18", "YC_AGUA_CALIENTE,YC_WIFI,YC_TOALLAS,YC_CAJA_FUERTE,YC_RECEPCION_24H",
                        "YC_NO_FUMAR,YC_SILENCIO,YC_SOLO_REGISTRADOS,YC_DOCUMENTO,YC_PAGO_ADELANTADO"},
                {"Habitación 606", "7", "Suite con comedor, sala y vista al mar. Incluye desayuno.",
                        "180", "2", "YC_SUITE", "1", "0", "40",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_BALCON,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_DESAYUNO,YC_AIRE_ACONDICIONADO,YC_FRIGOBAR,YC_LIMPIEZA_DIARIA",
                        "YC_NO_FUMAR,YC_MASCOTAS,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 607", "7", "Matrimonial superior con ventanales de piso a techo y baño moderno.",
                        "120", "2", "YC_MATRIMONIAL", "1", "0", "26",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_AIRE_ACONDICIONADO,YC_TV_CABLE",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 608", "7", "Doble clasica con dos camas queen y sillon de lectura.",
                        "110", "4", "YC_DOBLE", "2", "0", "30", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_ESCRITORIO,YC_TOALLAS,YC_TV_CABLE,YC_ESTACIONAMIENTO",
                        "YC_NO_FUMAR,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 609", "7", "Matrimonial de estilo colonial con cabecero tallado y vigas de madera.",
                        "115", "2", "YC_MATRIMONIAL", "1", "0", "24", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_DESAYUNO",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 610", "8", "Suite ejecutiva con vista a la ciudad, sala y escritorio amplio.",
                        "200", "2", "YC_SUITE", "1", "0", "38",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_TELEVISOR,YC_ESCRITORIO,YC_TOALLAS,YC_ARTICULOS_ASEO,YC_AIRE_ACONDICIONADO,YC_TV_CABLE,YC_FRIGOBAR,YC_CAJA_FUERTE,YC_LIMPIEZA_DIARIA",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SIN_FIESTAS,YC_DOCUMENTO"},
        };
        Map<String, YCCuarto> ycCreados = new HashMap<>();
        for (String[] ycD : ycHospedaje) {
            YCCuarto ycC = YCCuarto.builder()
                    .ycNumero(ycD[0]).ycPiso(Integer.parseInt(ycD[1])).ycDescripcion(ycD[2])
                    .ycPrecio(new BigDecimal(ycD[3])).ycCapacidad(Integer.parseInt(ycD[4]))
                    .ycModalidad(YCModalidad.YC_POR_NOCHE)
                    .ycTipoHabitacion(com.alquilafacil.model.YCTipoHabitacion.valueOf(ycD[5]))
                    .ycCamasDobles(Integer.parseInt(ycD[6])).ycCamasSimples(Integer.parseInt(ycD[7]))
                    .ycMetros(Integer.parseInt(ycD[8])).ycHoraEntrada("14:00").ycHoraSalida("12:00")
                    .ycComodidades(ycComodidades.convertToEntityAttribute(ycD[9]))
                    .ycNormas(ycNormas.convertToEntityAttribute(ycD[10]))
                    .build();
            ycCreados.put(ycD[0], ycCuartoRepository.save(ycC));
        }

        String[][] ycMensuales = {
                {"Cuarto 206", "2", "Cuarto sencillo y luminoso con cama de plaza y media.", "300", "1",
                        "YC_CAMA,YC_ROPERO,YC_WIFI,YC_VENTANA_CALLE"},
                {"Cuarto 207", "2", "Con ropero empotrado amplio y piso alfombrado. Muy tranquilo.", "320", "1",
                        "YC_CAMA,YC_ROPERO,YC_WIFI,YC_AGUA_CALIENTE"},
                {"Cuarto 208", "2", "Sin amoblar, piso de parquet y buena luz natural. Ideal para traer tus muebles.",
                        "280", "2", "YC_VENTANA_CALLE,YC_WIFI"},
                {"Altillo 209", "3", "Altillo con techo de madera, acogedor y silencioso.", "330", "1",
                        "YC_CAMA,YC_AMOBLADO,YC_WIFI"},
                {"Cuarto 210", "2", "Cuarto con cama queen, cortinas blackout y veladores.", "380", "2",
                        "YC_CAMA,YC_ROPERO,YC_WIFI,YC_BANO_PROPIO,YC_AGUA_CALIENTE"},
                {"Cuarto 211", "2", "Cuarto doble con dos camas, ideal para dos estudiantes.", "420", "2",
                        "YC_CAMA,YC_ROPERO,YC_ESCRITORIO,YC_WIFI"},
                {"Cuarto 212", "2", "Amoblado con cama queen, sillon y lamparas de lectura.", "400", "2",
                        "YC_CAMA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR,YC_AGUA_CALIENTE"},
                {"Mini depa 304", "3", "Mini departamento con sala amoblada, cocina y baño propio.", "650", "2",
                        "YC_ENTRADA_INDEPENDIENTE,YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR"},
                {"Departamento 305", "3", "Departamento con sala amplia, ventanales y cocina equipada.", "850", "3",
                        "YC_ENTRADA_INDEPENDIENTE,YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR,YC_BALCON"},
                {"Estudio 306", "3", "Estudio tipo loft con mucha luz, comedor y estanteria.", "600", "1",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_ESCRITORIO"},
        };
        for (String[] ycD : ycMensuales) {
            if (ycCuartoRepository.ycFindByNumeroIgnoreCase(ycD[0]).isPresent()) {
                continue;
            }
            YCCuarto ycC = YCCuarto.builder()
                    .ycNumero(ycD[0]).ycPiso(Integer.parseInt(ycD[1])).ycDescripcion(ycD[2])
                    .ycPrecio(new BigDecimal(ycD[3])).ycCapacidad(Integer.parseInt(ycD[4]))
                    .ycModalidad(YCModalidad.YC_MENSUAL)
                    .ycComodidades(ycComodidades.convertToEntityAttribute(ycD[5]))
                    .build();
            ycCuartoRepository.save(ycC);
        }

        LocalDate ycHoy = LocalDate.now();
        Object[][] ycHuespedes = {
                {"Habitación 610", "Rodrigo Valdivia", "40789652", "951234567", 2, -1, 3, YCMetodoPago.YC_TRANSFERENCIA, null},
                {"Habitación 602", "Lucia Ramos", "47123698", "952345678", 2, 0, 2, YCMetodoPago.YC_YAPE, null},
                {"Habitación 605", "Mochileros del Sur", "48521479", "953456789", 3, 1, 2, YCMetodoPago.YC_YAPE, "40"},
                {"Habitación 606", "Carmen Rivas", "41987456", "954567890", 2, 3, 4, YCMetodoPago.YC_PLIN, "200"},
                {"Habitación 603", "Diego Paredes", "46325897", "955678901", 2, 6, 2, YCMetodoPago.YC_EFECTIVO, "50"},
        };
        for (Object[] ycH : ycHuespedes) {
            YCCuarto ycCuarto = ycCreados.get((String) ycH[0]);
            LocalDate ycEntrada = ycHoy.plusDays((int) ycH[5]);
            int ycNoches = (int) ycH[6];
            BigDecimal ycTotal = ycCuarto.getYcPrecio().multiply(BigDecimal.valueOf(ycNoches));
            BigDecimal ycAdelanto = ycH[8] == null ? ycTotal : new BigDecimal((String) ycH[8]);
            ycEstadiaRepository.save(YCEstadia.builder()
                    .ycCuarto(ycCuarto).ycCuartoNumero(ycCuarto.getYcNumero())
                    .ycHuesped((String) ycH[1]).ycDocumento((String) ycH[2]).ycTelefono((String) ycH[3])
                    .ycPersonas((int) ycH[4]).ycEntrada(ycEntrada).ycSalida(ycEntrada.plusDays(ycNoches)).ycNoches(ycNoches)
                    .ycPrecioNoche(ycCuarto.getYcPrecio()).ycTotal(ycTotal)
                    .ycAdelanto(ycAdelanto).ycSaldoPagado(ycAdelanto.compareTo(ycTotal) >= 0).ycRecordatorioEnviado(false)
                    .ycMetodo((YCMetodoPago) ycH[7]).ycCancelada(false)
                    .ycFechaRegistro((ycEntrada.isAfter(ycHoy) ? ycHoy.minusDays(1) : ycEntrada).atTime(13, 0)).build());
        }
        YC_LOG.info("Ampliacion de ejemplo: 10 habitaciones de hospedaje y 10 cuartos mensuales con galeria.");
    }


    private void ycCompletarDetallesHospedaje() {
        String[][] ycDetalles = {
                {"Habitación 401", "YC_SIMPLE", "0", "1", "12", "YC_TOALLAS,YC_ARTICULOS_ASEO",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 402", "YC_MATRIMONIAL", "1", "0", "18",
                        "YC_TOALLAS,YC_ARTICULOS_ASEO,YC_DESAYUNO,YC_TV_CABLE,YC_FRIGOBAR",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SIN_FIESTAS,YC_SILENCIO,YC_DOCUMENTO"},
                {"Habitación 403", "YC_DOBLE", "0", "2", "20", "YC_TOALLAS,YC_ARTICULOS_ASEO,YC_TV_CABLE",
                        "YC_NO_FUMAR,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 404", "YC_COMPARTIDA", "0", "4", "26", "YC_TOALLAS,YC_CAJA_FUERTE,YC_RECEPCION_24H",
                        "YC_NO_FUMAR,YC_SILENCIO,YC_SOLO_REGISTRADOS,YC_DOCUMENTO,YC_PAGO_ADELANTADO"},
                {"Habitación 501", "YC_SUITE", "1", "0", "32",
                        "YC_TOALLAS,YC_ARTICULOS_ASEO,YC_DESAYUNO,YC_AIRE_ACONDICIONADO,YC_TV_CABLE,YC_FRIGOBAR,YC_LIMPIEZA_DIARIA,YC_CAJA_FUERTE",
                        "YC_NO_FUMAR,YC_MASCOTAS,YC_SIN_FIESTAS,YC_DOCUMENTO"},
                {"Habitación 502", "YC_MATRIMONIAL", "1", "0", "22",
                        "YC_TOALLAS,YC_ARTICULOS_ASEO,YC_AIRE_ACONDICIONADO,YC_TV_CABLE,YC_ESTACIONAMIENTO",
                        "YC_NO_FUMAR,YC_NO_MASCOTAS,YC_SILENCIO,YC_DOCUMENTO"},
        };
        YCComodidadesConverter ycComodidades = new YCComodidadesConverter();
        com.alquilafacil.model.YCNormasConverter ycNormas = new com.alquilafacil.model.YCNormasConverter();
        for (String[] ycD : ycDetalles) {
            ycCuartoRepository.ycFindByNumeroIgnoreCase(ycD[0])
                    .filter(ycC -> ycC.ycIsHospedaje() && ycC.getYcTipoHabitacion() == null)
                    .ifPresent(ycC -> {
                        ycC.setYcTipoHabitacion(com.alquilafacil.model.YCTipoHabitacion.valueOf(ycD[1]));
                        ycC.setYcCamasDobles(Integer.parseInt(ycD[2]));
                        ycC.setYcCamasSimples(Integer.parseInt(ycD[3]));
                        ycC.setYcMetros(Integer.parseInt(ycD[4]));
                        ycC.setYcHoraEntrada("14:00");
                        ycC.setYcHoraSalida("12:00");
                        var ycTodas = java.util.EnumSet.noneOf(com.alquilafacil.model.YCComodidad.class);
                        ycTodas.addAll(ycC.ycGetComodidades());
                        ycTodas.remove(com.alquilafacil.model.YCComodidad.YC_CAMA);
                        ycTodas.addAll(ycComodidades.convertToEntityAttribute(ycD[5]));
                        ycC.setYcComodidades(ycTodas);
                        ycC.setYcNormas(ycNormas.convertToEntityAttribute(ycD[6]));
                        ycCuartoRepository.save(ycC);
                    });
        }
    }

    private void ycSembrarAlquileres() {
        Random ycAzar = new Random(7);

        List<YCCuarto> ycCuartos = new ArrayList<>();
        String[][] ycDatos = {
                {"Cuarto 101", "1", "Cuarto iluminado con baño propio y terma. Ideal para una persona.", "380", "1",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_ROPERO"},
                {"Cuarto 102", "1", "Ventana grande a la calle, piso de madera.", "320", "1",
                        "YC_VENTANA_CALLE,YC_WIFI,YC_CAMA"},
                {"Cuarto 103", "1", "Amplio, con closet empotrado y escritorio para estudiar.", "350", "2",
                        "YC_ROPERO,YC_ESCRITORIO,YC_WIFI,YC_CAMA"},
                {"Cuarto 104", "1", "Pequeño y económico, cama de una plaza. Baño compartido.", "250", "1",
                        "YC_CAMA,YC_WIFI"},
                {"Cuarto 201", "2", "Con baño propio y cocina, amoblado completo.", "450", "2",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR,YC_CAMA,YC_ROPERO"},
                {"Cuarto 202", "2", "Con balcón y buena ventilación.", "400", "2",
                        "YC_BALCON,YC_BANO_PROPIO,YC_WIFI,YC_CAMA"},
                {"Cuarto 203", "2", "Muy iluminado y tranquilo, al fondo del pasillo. Baño compartido.", "280", "1",
                        "YC_CAMA,YC_ESCRITORIO,YC_VENTANA_CALLE"},
                {"Mini depa 301", "3", "Sala, cocina y 1 dormitorio. Entrada independiente.", "650", "3",
                        "YC_ENTRADA_INDEPENDIENTE,YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR,YC_BALCON"},
        };
        YCComodidadesConverter ycComodidades = new YCComodidadesConverter();
        for (String[] ycD : ycDatos) {
            ycCuartos.add(ycCuartoRepository.save(YCCuarto.builder()
                    .ycNumero(ycD[0]).ycPiso(Integer.parseInt(ycD[1])).ycDescripcion(ycD[2])
                    .ycPrecio(new BigDecimal(ycD[3])).ycCapacidad(Integer.parseInt(ycD[4]))
                    .ycComodidades(ycComodidades.convertToEntityAttribute(ycD[5])).build()));
        }

        List<YCDemo> ycDemos = List.of(
                new YCDemo("Maria", "Quispe Mamani", "45871236", "951234567", 7, 5, 0),
                new YCDemo("Luis", "Condori Apaza", "47125896", "952345678", 5, 10, 1),
                new YCDemo("Rosa", "Huanca Ccama", "43698521", "953456789", 9, 1, 0),
                new YCDemo("Jorge", "Mamani Flores", "46325874", "954567890", 4, 15, 2),
                new YCDemo("Ana", "Pari Choque", "48521473", "955678901", 3, 20, 0),
                new YCDemo("Carlos", "Ticona Yucra", "44785236", "", 6, 28, 1));

        YearMonth ycActual = YearMonth.now();
        LocalDate ycHoy = LocalDate.now();
        int ycPagos = 0;
        for (int ycI = 0; ycI < ycDemos.size(); ycI++) {
            YCDemo ycD = ycDemos.get(ycI);
            YCCuarto ycCuarto = ycCuartos.get(ycI < 3 ? ycI : ycI + 1);
            LocalDate ycIngreso = ycActual.minusMonths(ycD.ycMesesAntes()).atDay(Math.min(ycD.ycDiaPago(), 28));
            YCInquilino ycInq = ycInquilinoRepository.save(YCInquilino.builder()
                    .ycNombre(ycD.ycNombre()).ycApellido(ycD.ycApellido()).ycDni(ycD.ycDni()).ycTelefono(ycD.ycTelefono())
                    .ycCuarto(ycCuarto).ycFechaIngreso(ycIngreso).ycDiaPago(ycD.ycDiaPago())
                    .ycGarantia(ycCuarto.getYcPrecio()).ycActivo(true).build());

            for (YearMonth ycMes = YearMonth.from(ycIngreso); !ycMes.isAfter(ycActual); ycMes = ycMes.plusMonths(1)) {
                BigDecimal ycLuz = BigDecimal.valueOf(18 + ycAzar.nextInt(25));
                BigDecimal ycAgua = BigDecimal.valueOf(8 + ycAzar.nextInt(8));
                ycConsumoRepository.save(YCConsumoServicio.builder()
                        .ycCuarto(ycCuarto).ycPeriodo(ycMes).ycLuz(ycLuz).ycAgua(ycAgua).build());

                LocalDate ycVence = YCCalendarioPagos.ycVencimiento(ycMes, ycD.ycDiaPago());
                boolean ycDebe = !ycMes.isBefore(ycActual.minusMonths(ycD.ycMesesQueDebe() - 1L)) && ycD.ycMesesQueDebe() > 0;
                if (ycVence.isAfter(ycHoy) || ycDebe) {
                    continue;
                }
                LocalDate ycFechaPago = ycVence.minusDays(ycAzar.nextInt(4));
                ycPagoRepository.save(YCPago.builder()
                        .ycInquilino(ycInq).ycCuarto(ycCuarto).ycCuartoNumero(ycCuarto.getYcNumero()).ycPeriodo(ycMes)
                        .ycAlquiler(ycCuarto.getYcPrecio()).ycLuz(ycLuz).ycAgua(ycAgua)
                        .ycTotal(ycCuarto.getYcPrecio().add(ycLuz).add(ycAgua))
                        .ycFechaPago(ycFechaPago.atTime(9 + ycAzar.nextInt(10), ycAzar.nextInt(60)))
                        .ycMetodo(YCMetodoPago.values()[ycAzar.nextInt(YCMetodoPago.values().length)])
                        .ycAnulado(false).build());
                ycPagos++;
            }
        }
        YC_LOG.info("Datos de ejemplo: {} cuartos, {} inquilinos y {} pagos registrados.",
                ycCuartos.size(), ycDemos.size(), ycPagos);
    }

    private void ycSembrarHospedaje() {
        YCComodidadesConverter ycComodidades = new YCComodidadesConverter();
        String[][] ycDatos = {
                {"Habitación 401", "4", "Habitación simple con cama de plaza y media. Ideal para viajeros de paso.",
                        "45", "1", "YC_POR_NOCHE", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_TELEVISOR"},
                {"Habitación 402", "4", "Matrimonial con cama queen, toallas y kit de bienvenida.",
                        "80", "2", "YC_POR_NOCHE", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_TELEVISOR,YC_AMOBLADO"},
                {"Habitación 403", "4", "Doble con dos camas, perfecta para amigos o compañeros de trabajo.",
                        "90", "2", "YC_POR_NOCHE", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_TELEVISOR"},
                {"Habitación 404", "4", "Compartida con camarotes para mochileros. Casilleros individuales.",
                        "120", "4", "YC_POR_NOCHE", "YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_ROPERO"},
                {"Habitación 501", "5", "Matrimonial superior, amplia, con cortinas blackout y vista a la calle.",
                        "110", "2", "YC_POR_NOCHE",
                        "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_TELEVISOR,YC_ESCRITORIO,YC_AMOBLADO,YC_VENTANA_CALLE"},
                {"Habitación 502", "5", "Ejecutiva con sillón y escritorio, tranquila para descansar.",
                        "85", "2", "YC_POR_NOCHE", "YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_WIFI,YC_CAMA,YC_ESCRITORIO,YC_TELEVISOR"},
                {"Cuarto 204", "2", "Luminoso y minimalista, con cortinas y velador.",
                        "340", "1", "YC_MENSUAL", "YC_VENTANA_CALLE,YC_WIFI,YC_CAMA,YC_ROPERO"},
                {"Cuarto 205", "2", "Con escritorio grande y espacio para plantas. Ideal para estudiantes.",
                        "360", "1", "YC_MENSUAL", "YC_ESCRITORIO,YC_WIFI,YC_CAMA,YC_ROPERO,YC_VENTANA_CALLE"},
                {"Altillo 302", "3", "Altillo amplio con tragaluces, muy iluminado.",
                        "380", "2", "YC_MENSUAL", "YC_BANO_PROPIO,YC_WIFI,YC_CAMA,YC_AMOBLADO"},
                {"Loft 303", "3", "Loft moderno con sala, ventanales y cocina abierta.",
                        "720", "2", "YC_MENSUAL",
                        "YC_ENTRADA_INDEPENDIENTE,YC_BANO_PROPIO,YC_AGUA_CALIENTE,YC_COCINA,YC_AMOBLADO,YC_WIFI,YC_TELEVISOR,YC_ESCRITORIO"},
        };
        Map<String, YCCuarto> ycCreados = new HashMap<>();
        for (String[] ycD : ycDatos) {
            if (ycCuartoRepository.ycFindByNumeroIgnoreCase(ycD[0]).isPresent()) {
                continue;
            }
            ycCreados.put(ycD[0], ycCuartoRepository.save(YCCuarto.builder()
                    .ycNumero(ycD[0]).ycPiso(Integer.parseInt(ycD[1])).ycDescripcion(ycD[2])
                    .ycPrecio(new BigDecimal(ycD[3])).ycCapacidad(Integer.parseInt(ycD[4]))
                    .ycModalidad(YCModalidad.valueOf(ycD[5]))
                    .ycComodidades(ycComodidades.convertToEntityAttribute(ycD[6])).build()));
        }

        LocalDate ycHoy = LocalDate.now();
        int ycEstadias = 0;
        Object[][] ycHuespedes = {
                {"Habitación 401", "Luis Alberto Rojas", "41258963", "961234567", 1, -1, 3, YCMetodoPago.YC_YAPE},
                {"Habitación 401", "Marco Vílchez", "42587413", "962345678", 1, -9, 3, YCMetodoPago.YC_EFECTIVO},
                {"Habitación 401", "Elena Paredes", "40125874", "", 1, -20, 2, YCMetodoPago.YC_PLIN},
                {"Habitación 402", "Andrea Salazar", "45896321", "963456789", 2, 0, 3, YCMetodoPago.YC_TRANSFERENCIA},
                {"Habitación 402", "Ricardo Mendoza", "43258741", "964567890", 2, -40, 3, YCMetodoPago.YC_YAPE},
                {"Habitación 403", "Kevin Huamán", "47852369", "965678901", 2, 2, 3, YCMetodoPago.YC_YAPE},
                {"Habitación 403", "Sofía Cárdenas", "46325871", "", 2, -15, 2, YCMetodoPago.YC_EFECTIVO},
                {"Habitación 404", "Tom Becker (grupo)", "C8F3K2L19", "", 3, -2, 3, YCMetodoPago.YC_EFECTIVO},
                {"Habitación 404", "Julia Martins", "FB9921455", "", 2, -55, 5, YCMetodoPago.YC_TRANSFERENCIA},
                {"Habitación 501", "Patricia Núñez", "44521789", "966789012", 2, 7, 2, YCMetodoPago.YC_PLIN},
                {"Habitación 501", "Fernando Quispe", "41789523", "967890123", 2, -25, 3, YCMetodoPago.YC_YAPE},
                {"Habitación 502", "Gabriel Torres", "48963257", "968901234", 1, -3, 2, YCMetodoPago.YC_EFECTIVO},
        };
        for (Object[] ycH : ycHuespedes) {
            YCCuarto ycCuarto = ycCreados.get((String) ycH[0]);
            if (ycCuarto == null) {
                continue;
            }
            LocalDate ycEntrada = ycHoy.plusDays((int) ycH[5]);
            int ycNoches = (int) ycH[6];
            LocalDate ycRegistro = ycEntrada.isAfter(ycHoy) ? ycHoy.minusDays(1) : ycEntrada;
            String ycTelefono = (String) ycH[3];
            ycEstadiaRepository.save(YCEstadia.builder()
                    .ycCuarto(ycCuarto).ycCuartoNumero(ycCuarto.getYcNumero())
                    .ycHuesped((String) ycH[1]).ycDocumento((String) ycH[2])
                    .ycTelefono(ycTelefono.isBlank() ? null : ycTelefono)
                    .ycPersonas((int) ycH[4]).ycEntrada(ycEntrada).ycSalida(ycEntrada.plusDays(ycNoches)).ycNoches(ycNoches)
                    .ycPrecioNoche(ycCuarto.getYcPrecio())
                    .ycTotal(ycCuarto.getYcPrecio().multiply(BigDecimal.valueOf(ycNoches)))
                    .ycMetodo((YCMetodoPago) ycH[7]).ycCancelada(false)
                    .ycFechaRegistro(ycRegistro.atTime(14, 0)).build());
            ycEstadias++;
        }
        YC_LOG.info("Datos de ejemplo: {} cuartos nuevos (hospedaje y mensuales) y {} estadias.", ycCreados.size(), ycEstadias);
    }

}
