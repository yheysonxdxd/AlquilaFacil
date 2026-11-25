package com.alquilafacil.service;

import com.alquilafacil.exception.YCNegocioException;
import com.alquilafacil.model.YCCanalReserva;
import com.alquilafacil.model.YCCuarto;
import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.repository.YCCuartoRepository;
import com.alquilafacil.repository.YCEstadiaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCEstadiaService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCEstadiaService.class);
    public static final int YC_MAXIMO_NOCHES = 60;
    private static final DateTimeFormatter YC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final YCEstadiaRepository ycRepository;
    private final YCCuartoRepository ycCuartoRepository;
    private final YCSesion ycSesion;

    public record YCSolicitud(Long ycCuartoId, String ycHuesped, String ycDocumento, String ycTelefono, Integer ycPersonas,
                            LocalDate ycEntrada, LocalDate ycSalida, BigDecimal ycPrecioNoche,
                            YCMetodoPago ycMetodo, String ycNota, BigDecimal ycAdelanto, YCCanalReserva ycCanal) {}

    public record YCHuespedFrecuente(YCEstadia ycUltima, long ycVisitas) {}

    @Transactional
    public YCEstadia ycRegistrar(YCSolicitud ycS) {
        if (ycS.ycCuartoId() == null) {
            throw new YCNegocioException("Selecciona la habitacion.");
        }
        YCCuarto ycCuarto = ycCuartoRepository.findById(ycS.ycCuartoId())
                .orElseThrow(() -> new YCNegocioException("La habitacion ya no existe."));
        if (!ycCuarto.ycIsHospedaje()) {
            throw new YCNegocioException(ycCuarto.getYcNumero() + " es de alquiler mensual, no de hospedaje.");
        }
        if (ycS.ycHuesped() == null || ycS.ycHuesped().isBlank()) {
            throw new YCNegocioException("Escribe el nombre del huesped.");
        }
        if (ycS.ycEntrada() == null || ycS.ycSalida() == null) {
            throw new YCNegocioException("Indica las fechas de entrada y salida.");
        }
        if (!ycS.ycSalida().isAfter(ycS.ycEntrada())) {
            throw new YCNegocioException("La salida debe ser al menos un dia despues de la entrada.");
        }
        LocalDate ycHoy = LocalDate.now();
        if (ycS.ycEntrada().isBefore(ycHoy.minusDays(30))) {
            throw new YCNegocioException("La fecha de entrada es demasiado antigua.");
        }
        int ycNoches = (int) ChronoUnit.DAYS.between(ycS.ycEntrada(), ycS.ycSalida());
        if (ycNoches > YC_MAXIMO_NOCHES) {
            throw new YCNegocioException("Maximo " + YC_MAXIMO_NOCHES
                    + " noches. Para estadias largas usa un cuarto de alquiler mensual.");
        }
        int ycPersonas = ycS.ycPersonas() == null ? 1 : ycS.ycPersonas();
        if (ycPersonas < 1) {
            throw new YCNegocioException("Debe haber al menos 1 persona.");
        }
        if (ycCuarto.getYcCapacidad() != null && ycPersonas > ycCuarto.getYcCapacidad()) {
            throw new YCNegocioException(ycCuarto.getYcNumero() + " es para maximo " + ycCuarto.getYcCapacidad()
                    + (ycCuarto.getYcCapacidad() == 1 ? " persona." : " personas."));
        }
        List<YCEstadia> ycCruzadas = ycRepository.ycCruzadas(ycCuarto.getYcId(), ycS.ycEntrada(), ycS.ycSalida(), null);
        if (!ycCruzadas.isEmpty()) {
            YCEstadia ycOtra = ycCruzadas.get(0);
            throw new YCNegocioException(ycCuarto.getYcNumero() + " ya esta ocupada del " + ycOtra.getYcEntrada().format(YC_FECHA)
                    + " al " + ycOtra.getYcSalida().format(YC_FECHA) + " (" + ycOtra.getYcHuesped() + ").");
        }
        BigDecimal ycPrecio = ycS.ycPrecioNoche() == null ? ycCuarto.getYcPrecio() : ycS.ycPrecioNoche();
        if (ycPrecio.signum() <= 0) {
            throw new YCNegocioException("El precio por noche debe ser mayor a 0.");
        }
        ycPrecio = ycPrecio.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ycTotal = ycPrecio.multiply(BigDecimal.valueOf(ycNoches));
        BigDecimal ycAdelanto = ycS.ycAdelanto() == null ? ycTotal : ycS.ycAdelanto().setScale(2, RoundingMode.HALF_UP);
        if (ycAdelanto.signum() < 0) {
            throw new YCNegocioException("El adelanto no puede ser negativo.");
        }
        if (ycAdelanto.compareTo(ycTotal) > 0) {
            throw new YCNegocioException("El adelanto no puede ser mayor que el total (" + ycTotal + ").");
        }

        YCCanalReserva ycCanal = ycS.ycCanal() == null ? YCCanalReserva.YC_EN_PERSONA : ycS.ycCanal();
        boolean ycYaLlego = ycCanal == YCCanalReserva.YC_EN_PERSONA && !ycS.ycEntrada().isAfter(ycHoy);

        YCEstadia ycEstadia = YCEstadia.builder()
                .ycCuarto(ycCuarto)
                .ycCuartoNumero(ycCuarto.getYcNumero())
                .ycHuesped(ycS.ycHuesped().trim())
                .ycDocumento(ycVacioANull(ycS.ycDocumento()))
                .ycTelefono(ycVacioANull(ycS.ycTelefono()))
                .ycPersonas(ycPersonas)
                .ycEntrada(ycS.ycEntrada())
                .ycSalida(ycS.ycSalida())
                .ycNoches(ycNoches)
                .ycPrecioNoche(ycPrecio)
                .ycTotal(ycTotal)
                .ycAdelanto(ycAdelanto)
                .ycSaldoPagado(ycAdelanto.compareTo(ycTotal) >= 0)
                .ycRecordatorioEnviado(false)
                .ycCanal(ycCanal)
                .ycLlego(ycYaLlego)
                .ycFechaLlegada(ycYaLlego ? LocalDateTime.now() : null)
                .ycNoLlego(false)
                .ycMetodo(ycS.ycMetodo() == null ? YCMetodoPago.YC_EFECTIVO : ycS.ycMetodo())
                .ycNota(ycVacioANull(ycS.ycNota()))
                .ycCancelada(false)
                .ycFechaRegistro(LocalDateTime.now())
                .ycRegistradoPor(ycSesion.ycNombreUsuario())
                .build();
        YCEstadia ycGuardada = ycRepository.save(ycEstadia);
        YC_LOG.info("Estadia registrada: {} cuarto={} noches={} total={}",
                ycGuardada.ycGetNumero(), ycCuarto.getYcNumero(), ycNoches, ycGuardada.getYcTotal());
        return ycGuardada;
    }

    public record YCHabitacionGrupo(Long ycCuartoId, Integer ycPersonas, BigDecimal ycPrecioNoche) {}

    @Transactional
    public List<YCEstadia> ycRegistrarGrupo(YCSolicitud ycDatos, List<YCHabitacionGrupo> ycHabitaciones) {
        if (ycHabitaciones == null || ycHabitaciones.isEmpty()) {
            throw new YCNegocioException("Elige al menos una habitacion.");
        }
        java.util.Set<Long> ycVistas = new java.util.HashSet<>();
        for (YCHabitacionGrupo ycH : ycHabitaciones) {
            if (ycH.ycCuartoId() == null) {
                throw new YCNegocioException("Elige la habitacion en cada fila.");
            }
            if (!ycVistas.add(ycH.ycCuartoId())) {
                throw new YCNegocioException("Elegiste la misma habitacion dos veces.");
            }
        }
        if (ycDatos.ycEntrada() == null || ycDatos.ycSalida() == null || !ycDatos.ycSalida().isAfter(ycDatos.ycEntrada())) {
            throw new YCNegocioException("Indica las fechas de entrada y salida.");
        }
        long ycNoches = ChronoUnit.DAYS.between(ycDatos.ycEntrada(), ycDatos.ycSalida());
        List<BigDecimal> ycTotales = new java.util.ArrayList<>();
        for (YCHabitacionGrupo ycH : ycHabitaciones) {
            YCCuarto ycCuarto = ycCuartoRepository.findById(ycH.ycCuartoId())
                    .orElseThrow(() -> new YCNegocioException("La habitacion ya no existe."));
            BigDecimal ycPrecio = ycH.ycPrecioNoche() == null ? ycCuarto.getYcPrecio() : ycH.ycPrecioNoche();
            ycTotales.add(ycPrecio.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(ycNoches)));
        }
        BigDecimal ycTotalGrupo = ycTotales.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ycPorRepartir = ycDatos.ycAdelanto() == null ? ycTotalGrupo : ycDatos.ycAdelanto().setScale(2, RoundingMode.HALF_UP);
        if (ycPorRepartir.signum() < 0) {
            throw new YCNegocioException("El adelanto no puede ser negativo.");
        }
        if (ycPorRepartir.compareTo(ycTotalGrupo) > 0) {
            throw new YCNegocioException("El adelanto no puede ser mayor que el total del grupo (" + ycTotalGrupo + ").");
        }

        List<YCEstadia> ycGrupo = new java.util.ArrayList<>();
        for (int ycI = 0; ycI < ycHabitaciones.size(); ycI++) {
            YCHabitacionGrupo ycH = ycHabitaciones.get(ycI);
            BigDecimal ycParte = ycPorRepartir.min(ycTotales.get(ycI));
            ycPorRepartir = ycPorRepartir.subtract(ycParte);
            ycGrupo.add(ycRegistrar(new YCSolicitud(ycH.ycCuartoId(), ycDatos.ycHuesped(), ycDatos.ycDocumento(), ycDatos.ycTelefono(),
                    ycH.ycPersonas(), ycDatos.ycEntrada(), ycDatos.ycSalida(), ycH.ycPrecioNoche(), ycDatos.ycMetodo(), ycDatos.ycNota(),
                    ycParte, ycDatos.ycCanal())));
        }
        if (ycGrupo.size() > 1) {
            Long ycIdGrupo = ycGrupo.get(0).getYcId();
            ycGrupo.forEach(ycE -> ycE.setYcGrupo(ycIdGrupo));
            ycRepository.saveAll(ycGrupo);
            ycAuditar("Reserva de grupo", ycGrupo.get(0).ycGetNumero() + " · " + ycDatos.ycHuesped().trim() + " · "
                    + ycGrupo.stream().map(YCEstadia::getYcCuartoNumero).collect(Collectors.joining(", "))
                    + " · total S/ " + ycTotalGrupo);
        }
        return ycGrupo;
    }

    public List<YCEstadia> ycDelGrupo(YCEstadia ycE) {
        if (ycE == null || ycE.getYcGrupo() == null) {
            return ycE == null ? List.of() : List.of(ycE);
        }
        List<YCEstadia> ycGrupo = ycRepository.ycFindByGrupoOrderByIdAsc(ycE.getYcGrupo());
        return ycGrupo.isEmpty() ? List.of(ycE) : ycGrupo;
    }

    @Transactional
    public List<YCEstadia> ycCobrarSaldoGrupo(YCEstadia ycE, YCMetodoPago ycMetodo) {
        List<YCEstadia> ycConSaldo = ycDelGrupo(ycE).stream().filter(YCEstadia::ycTieneSaldo).toList();
        if (ycConSaldo.isEmpty()) {
            throw new YCNegocioException("Este grupo no tiene saldo pendiente.");
        }
        return ycConSaldo.stream().map(ycX -> ycCobrarSaldo(ycX.getYcId(), ycMetodo)).toList();
    }

    @Transactional
    public List<YCEstadia> ycRegistrarLlegadaGrupo(YCEstadia ycE) {
        LocalDate ycHoy = LocalDate.now();
        List<YCEstadia> ycPendientes = ycDelGrupo(ycE).stream()
                .filter(ycX -> !ycX.isYcCancelada() && !ycX.ycIsLlego() && !ycX.getYcEntrada().isAfter(ycHoy)).toList();
        if (ycPendientes.isEmpty()) {
            return List.of(ycRegistrarLlegada(ycE.getYcId()));
        }
        return ycPendientes.stream().map(ycX -> ycRegistrarLlegada(ycX.getYcId())).toList();
    }

    public record YCEdicion(Long ycCuartoId, String ycHuesped, String ycDocumento, String ycTelefono, Integer ycPersonas,
                          LocalDate ycEntrada, LocalDate ycSalida, BigDecimal ycPrecioNoche, String ycNota, YCCanalReserva ycCanal) {}

    @Transactional
    public YCEstadia ycEditar(Long ycId, YCEdicion ycD) {
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("Una estadia cancelada no se puede editar.");
        }
        if (ycD.ycHuesped() == null || ycD.ycHuesped().isBlank()) {
            throw new YCNegocioException("Escribe el nombre del huesped.");
        }
        if (ycD.ycEntrada() == null || ycD.ycSalida() == null) {
            throw new YCNegocioException("Indica las fechas de entrada y salida.");
        }
        if (!ycD.ycSalida().isAfter(ycD.ycEntrada())) {
            throw new YCNegocioException("La salida debe ser al menos un dia despues de la entrada.");
        }
        YCCuarto ycCuarto = ycCuartoRepository.findById(ycD.ycCuartoId() == null ? ycE.getYcCuarto().getYcId() : ycD.ycCuartoId())
                .orElseThrow(() -> new YCNegocioException("La habitacion ya no existe."));
        if (!ycCuarto.ycIsHospedaje()) {
            throw new YCNegocioException(ycCuarto.getYcNumero() + " es de alquiler mensual, no de hospedaje.");
        }
        int ycPersonas = ycD.ycPersonas() == null ? ycE.getYcPersonas() : ycD.ycPersonas();
        if (ycPersonas < 1) {
            throw new YCNegocioException("Debe haber al menos 1 persona.");
        }
        if (ycCuarto.getYcCapacidad() != null && ycPersonas > ycCuarto.getYcCapacidad()) {
            throw new YCNegocioException(ycCuarto.getYcNumero() + " es para maximo " + ycCuarto.getYcCapacidad()
                    + (ycCuarto.getYcCapacidad() == 1 ? " persona." : " personas."));
        }
        BigDecimal ycPrecio = (ycD.ycPrecioNoche() == null ? ycE.getYcPrecioNoche() : ycD.ycPrecioNoche()).setScale(2, RoundingMode.HALF_UP);
        if (ycPrecio.signum() <= 0) {
            throw new YCNegocioException("El precio por noche debe ser mayor a 0.");
        }
        LocalDate ycHoy = LocalDate.now();
        boolean ycCambiaHabitacion = !ycCuarto.getYcId().equals(ycE.getYcCuarto().getYcId());
        boolean ycCambiaFechas = !ycD.ycEntrada().equals(ycE.getYcEntrada()) || !ycD.ycSalida().equals(ycE.getYcSalida());
        boolean ycCambiaPrecio = ycPrecio.compareTo(ycE.getYcPrecioNoche()) != 0;
        boolean ycCambiaReserva = ycCambiaHabitacion || ycCambiaFechas || ycCambiaPrecio;
        if (ycCambiaReserva && ycE.ycEstado(ycHoy) == YCEstadia.YCEstado.YC_TERMINADA) {
            throw new YCNegocioException("La estadia ya termino: solo se pueden corregir los datos del huesped.");
        }
        if (ycCambiaReserva && ycE.ycSalioAntes()) {
            throw new YCNegocioException("Ya se registro su salida anticipada: solo se pueden corregir los datos del huesped.");
        }
        if (ycCambiaPrecio) {
            ycSesion.ycExigirAdmin("cambiar el precio por noche");
        }
        if (ycCambiaFechas && !ycD.ycEntrada().equals(ycE.getYcEntrada()) && ycD.ycEntrada().isBefore(ycHoy.minusDays(30))) {
            throw new YCNegocioException("La fecha de entrada es demasiado antigua.");
        }
        int ycNoches = (int) ChronoUnit.DAYS.between(ycD.ycEntrada(), ycD.ycSalida());
        if (ycNoches > YC_MAXIMO_NOCHES) {
            throw new YCNegocioException("Maximo " + YC_MAXIMO_NOCHES + " noches.");
        }
        if (ycCambiaHabitacion || ycCambiaFechas) {
            List<YCEstadia> ycCruzadas = ycRepository.ycCruzadas(ycCuarto.getYcId(), ycD.ycEntrada(), ycD.ycSalida(), ycE.getYcId());
            if (!ycCruzadas.isEmpty()) {
                YCEstadia ycOtra = ycCruzadas.get(0);
                throw new YCNegocioException(ycCuarto.getYcNumero() + " ya esta ocupada del " + ycOtra.getYcEntrada().format(YC_FECHA)
                        + " al " + ycOtra.getYcSalida().format(YC_FECHA) + " (" + ycOtra.getYcHuesped() + ").");
            }
        }

        BigDecimal ycTotalNuevo = ycPrecio.multiply(BigDecimal.valueOf(ycNoches));
        if (ycTotalNuevo.compareTo(ycE.getYcTotal()) != 0) {
            if (ycE.getYcAdelanto() == null) {
                ycE.setYcAdelanto(ycE.getYcTotal());
                ycE.setYcSaldoPagado(true);
                ycE.setYcMontoSaldo(BigDecimal.ZERO);
            } else if (Boolean.TRUE.equals(ycE.getYcSaldoPagado()) && ycE.getYcMontoSaldo() == null) {
                ycE.setYcMontoSaldo(ycE.getYcTotal().subtract(ycE.getYcAdelanto()));
            }
            BigDecimal ycPagado = ycE.ycGetPagado();
            boolean ycSaldoCobradoAparte = Boolean.TRUE.equals(ycE.getYcSaldoPagado())
                    && ycE.getYcMontoSaldo() != null && ycE.getYcMontoSaldo().signum() > 0;
            if (ycTotalNuevo.compareTo(ycPagado) < 0) {
                throw new YCNegocioException("El nuevo total (S/ " + ycTotalNuevo + ") es menor a lo que ya pago (S/ " + ycPagado
                        + "). Si se va antes, usa \"Registrar salida\": calcula la devolucion.");
            }
            if (ycTotalNuevo.compareTo(ycPagado) > 0 && ycSaldoCobradoAparte) {
                throw new YCNegocioException("El saldo de esta estadia ya se cobro. Para agregar noches, registra una estadia nueva.");
            }
            if (!ycSaldoCobradoAparte) {
                boolean ycCompleta = ycTotalNuevo.compareTo(ycPagado) == 0;
                ycE.setYcSaldoPagado(ycCompleta);
                ycE.setYcMontoSaldo(ycCompleta ? BigDecimal.ZERO : null);
            }
        }

        String ycCambios = new YCAuditoriaService.YCCambios()
                .ycCampo("huesped", ycE.getYcHuesped(), ycD.ycHuesped().trim())
                .ycCampo("documento", ycE.getYcDocumento(), ycVacioANull(ycD.ycDocumento()))
                .ycCampo("habitacion", ycE.getYcCuartoNumero(), ycCuarto.getYcNumero())
                .ycCampo("entrada", ycE.getYcEntrada(), ycD.ycEntrada())
                .ycCampo("salida", ycE.getYcSalida(), ycD.ycSalida())
                .ycCampo("personas", ycE.getYcPersonas(), ycPersonas)
                .ycCampo("precio", ycE.getYcPrecioNoche(), ycPrecio)
                .ycCampo("total", ycE.getYcTotal(), ycTotalNuevo)
                .toString();

        ycE.setYcCuarto(ycCuarto);
        ycE.setYcCuartoNumero(ycCuarto.getYcNumero());
        ycE.setYcHuesped(ycD.ycHuesped().trim());
        ycE.setYcDocumento(ycVacioANull(ycD.ycDocumento()));
        ycE.setYcTelefono(ycVacioANull(ycD.ycTelefono()));
        ycE.setYcPersonas(ycPersonas);
        ycE.setYcEntrada(ycD.ycEntrada());
        ycE.setYcSalida(ycD.ycSalida());
        ycE.setYcNoches(ycNoches);
        ycE.setYcPrecioNoche(ycPrecio);
        ycE.setYcTotal(ycTotalNuevo);
        ycE.setYcNota(ycVacioANull(ycD.ycNota()));
        if (ycD.ycCanal() != null) {
            ycE.setYcCanal(ycD.ycCanal());
        }
        if (ycD.ycEntrada().isAfter(ycHoy) && ycE.ycIsLlego()) {
            ycE.setYcLlego(false);
            ycE.setYcFechaLlegada(null);
        }
        YCEstadia ycGuardada = ycRepository.save(ycE);
        if (!ycCambios.isEmpty()) {
            ycAuditar("Estadia editada", ycGuardada.ycGetNumero() + ": " + ycCambios);
        }
        return ycGuardada;
    }

    @Transactional
    public void ycCancelar(Long ycId, String ycMotivo) {
        ycSesion.ycExigirAdmin("cancelar estadias");
        String ycPorQue = YCMotivos.ycExigir(ycMotivo, "cancelar la estadia");
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("Esta estadia ya estaba cancelada.");
        }
        if (ycE.ycEstado(LocalDate.now()) == YCEstadia.YCEstado.YC_TERMINADA) {
            throw new YCNegocioException("La estadia ya termino; no se puede cancelar.");
        }
        if (ycDebeAntesDeCancelar(ycE)) {
            throw new YCNegocioException(ycE.getYcHuesped() + " aun debe " + ycE.ycGetSaldo()
                    + ". Cobra el saldo antes de cancelar la estadia.");
        }
        ycExigirConsumosPagados(ycE, "cancelar la estadia");
        ycE.setYcCancelada(true);
        ycE.setYcMotivoCancelacion(ycPorQue);
        ycE.setYcCanceladaPor(ycSesion.ycNombreUsuario());
        ycE.setYcFechaCancelacion(LocalDateTime.now());
        ycRepository.save(ycE);
        ycAuditar("Estadia cancelada", ycE.ycGetNumero() + " · " + ycE.getYcHuesped() + " · " + ycE.getYcCuartoNumero()
                + " · pagado S/ " + ycE.ycGetPagado() + " · motivo: " + ycPorQue);
    }

    public static boolean ycDebeAntesDeCancelar(YCEstadia ycE) {
        return ycE.ycHospedado(LocalDate.now()) && ycE.ycTieneSaldo();
    }

    @Transactional
    public YCEstadia ycCobrarSaldo(Long ycId, YCMetodoPago ycMetodo) {
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("La estadia esta cancelada.");
        }
        if (!ycE.ycTieneSaldo()) {
            throw new YCNegocioException("Esta estadia no tiene saldo pendiente.");
        }
        ycE.setYcMontoSaldo(ycE.ycGetSaldo());
        ycE.setYcSaldoPagado(true);
        ycE.setYcFechaSaldo(LocalDateTime.now());
        ycE.setYcMetodoSaldo(ycMetodo == null ? YCMetodoPago.YC_EFECTIVO : ycMetodo);
        ycE.setYcSaldoCobradoPor(ycSesion.ycNombreUsuario());
        if (!ycE.ycIsLlego() && !ycE.getYcEntrada().isAfter(LocalDate.now())) {
            ycE.setYcLlego(true);
            ycE.setYcFechaLlegada(LocalDateTime.now());
        }
        return ycRepository.save(ycE);
    }

    @Transactional
    public YCEstadia ycRegistrarLlegada(Long ycId) {
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("La estadia esta cancelada.");
        }
        if (ycE.getYcEntrada().isAfter(LocalDate.now())) {
            throw new YCNegocioException("Su reserva empieza el " + ycE.getYcEntrada().format(YC_FECHA)
                    + ". Si llego antes, cambia las fechas registrando una nueva estadia.");
        }
        if (ycE.ycIsLlego()) {
            throw new YCNegocioException(ycE.getYcHuesped() + " ya figura como llegado.");
        }
        ycE.setYcLlego(true);
        ycE.setYcFechaLlegada(LocalDateTime.now());
        return ycRepository.save(ycE);
    }

    @Transactional
    public void ycRegistrarNoLlego(Long ycId, boolean ycDevolverAdelanto) {
        ycRegistrarNoLlego(ycId, ycDevolverAdelanto, YCMetodoPago.YC_EFECTIVO);
    }

    @Transactional
    public void ycRegistrarNoLlego(Long ycId, boolean ycDevolverAdelanto, YCMetodoPago ycMetodoDevolucion) {
        ycSesion.ycExigirAdmin("liberar reservas");
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("Esta estadia ya estaba cancelada.");
        }
        if (ycE.ycIsLlego()) {
            throw new YCNegocioException(ycE.getYcHuesped() + " ya llego; si se fue, registra su salida.");
        }
        if (ycE.getYcEntrada().isAfter(LocalDate.now())) {
            throw new YCNegocioException("Su reserva aun no empieza; si avisa que no vendra, cancelala.");
        }
        BigDecimal ycRecibido = ycE.ycGetRecibido();
        if (ycDevolverAdelanto && ycRecibido.signum() > 0) {
            ycE.setYcDevuelto(ycRecibido);
            ycE.setYcFechaDevolucion(LocalDateTime.now());
            ycE.setYcDevueltoPor(ycSesion.ycNombreUsuario());
            ycE.setYcMetodoDevolucion(ycMetodoDevolucion == null ? YCMetodoPago.YC_EFECTIVO : ycMetodoDevolucion);
        }
        ycE.setYcNoLlego(true);
        ycE.setYcCancelada(true);
        ycE.setYcMotivoCancelacion("No se presento");
        ycAuditar("No vino", ycE.ycGetNumero() + " · " + ycE.getYcHuesped() + " · " + ycE.getYcCuartoNumero()
                + (ycDevolverAdelanto ? " · adelanto devuelto S/ " + ycRecibido + " en " + ycE.ycGetMetodoDevolucionODefecto()
                : " · adelanto retenido S/ " + ycRecibido));
        ycE.setYcCanceladaPor(ycSesion.ycNombreUsuario());
        ycE.setYcFechaCancelacion(LocalDateTime.now());
        ycRepository.save(ycE);
        YC_LOG.info("No llego {}: {} · adelanto {} {}", ycE.ycGetNumero(), ycE.getYcHuesped(), ycRecibido,
                ycDevolverAdelanto ? "devuelto" : "retenido");
    }

    public List<YCEstadia> ycEsperandoLlegada() {
        LocalDate ycHoy = LocalDate.now();
        return ycRepository.ycEnCurso(ycHoy).stream()
                .filter(ycE -> ycE.ycEsperandoLlegada(ycHoy))
                .sorted(java.util.Comparator.comparing(YCEstadia::getYcEntrada).thenComparing(YCEstadia::getYcCuartoNumero))
                .toList();
    }

    public Map<YCCanalReserva, Long> ycPorCanalDesde(LocalDate ycDesde) {
        return ycRepository.ycFindAllByOrderByEntradaDescIdDesc().stream()
                .filter(ycE -> ycE.getYcCanal() != null && !ycE.isYcCancelada()
                        && !ycE.getYcFechaRegistro().toLocalDate().isBefore(ycDesde))
                .collect(Collectors.groupingBy(YCEstadia::getYcCanal, () -> new java.util.EnumMap<>(YCCanalReserva.class),
                        Collectors.counting()));
    }

    @Transactional
    public void ycMarcarRecordatorio(Long ycId) {
        YCEstadia ycE = ycObtener(ycId);
        ycE.setYcRecordatorioEnviado(true);
        ycRepository.save(ycE);
    }

    public List<YCEstadia> ycLleganManana() {
        return ycRepository.ycFindByEntradaAndCanceladaFalseOrderByCuartoNumero(LocalDate.now().plusDays(1));
    }

    public java.util.Optional<YCHuespedFrecuente> ycHuespedPorDocumento(String ycDocumento) {
        if (ycDocumento == null || ycDocumento.isBlank()) {
            return java.util.Optional.empty();
        }
        YCEstadia ycUltima = ycRepository.ycFindFirstByDocumentoIgnoreCaseOrderByFechaRegistroDesc(ycDocumento.trim());
        if (ycUltima == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new YCHuespedFrecuente(ycUltima,
                ycRepository.ycCountByDocumentoIgnoreCaseAndCanceladaFalse(ycDocumento.trim())));
    }

    @Transactional
    public void ycRegistrarSalidaHoy(Long ycId) {
        YCEstadia ycE = ycObtener(ycId);
        LocalDate ycHoy = LocalDate.now();
        boolean ycSaleHoy = !ycE.isYcCancelada() && ycE.getYcSalida().equals(ycHoy);
        if (ycE.ycEstado(ycHoy) != YCEstadia.YCEstado.YC_EN_CURSO && !ycSaleHoy) {
            throw new YCNegocioException("Solo se puede registrar la salida de un huesped que esta hospedado.");
        }
        if (ycE.ycTieneSaldo()) {
            throw new YCNegocioException(ycE.getYcHuesped() + " aun debe " + ycE.ycGetSaldo()
                    + ". Cobra el saldo antes de registrar su salida.");
        }
        ycExigirConsumosPagados(ycE, "registrar su salida");
        if (!ycSaleHoy) {
            ycE.setYcSalida(ycHoy.isAfter(ycE.getYcEntrada()) ? ycHoy : ycE.getYcEntrada().plusDays(1));
            ycRepository.save(ycE);
        }
        YCCuarto ycCuarto = ycE.getYcCuarto();
        ycCuarto.setYcLimpieza(com.alquilafacil.model.YCEstadoLimpieza.YC_POR_LIMPIAR);
        ycCuartoRepository.save(ycCuarto);
    }

    public record YCSalidaAnticipada(int ycNochesAntes, int ycNochesAhora, BigDecimal ycTotalAntes, BigDecimal ycTotalAhora,
                                   BigDecimal ycRecibido, BigDecimal ycADevolver, BigDecimal ycACobrar) {}

    public static YCSalidaAnticipada ycCalcularSalida(YCEstadia ycE, LocalDate ycFecha, boolean ycRecalcular) {
        int ycNochesAhora = (int) ChronoUnit.DAYS.between(ycE.getYcEntrada(), ycFecha);
        BigDecimal ycTotalAhora = ycRecalcular
                ? ycE.getYcPrecioNoche().multiply(BigDecimal.valueOf(ycNochesAhora)).setScale(2, RoundingMode.HALF_UP)
                : ycE.getYcTotal();
        BigDecimal ycRecibido = ycE.ycGetRecibido();
        BigDecimal ycDiferencia = ycRecibido.subtract(ycTotalAhora);
        return new YCSalidaAnticipada(ycE.getYcNoches(), ycNochesAhora, ycE.getYcTotal(), ycTotalAhora, ycRecibido,
                ycDiferencia.signum() > 0 ? ycDiferencia : BigDecimal.ZERO,
                ycDiferencia.signum() < 0 ? ycDiferencia.negate() : BigDecimal.ZERO);
    }

    @Transactional
    public YCSalidaAnticipada ycRegistrarSalidaAnticipada(Long ycId, LocalDate ycFecha, boolean ycRecalcular,
                                                      YCMetodoPago ycMetodoCobro) {
        return ycRegistrarSalidaAnticipada(ycId, ycFecha, ycRecalcular, ycMetodoCobro, YCMetodoPago.YC_EFECTIVO);
    }

    @Transactional
    public YCSalidaAnticipada ycRegistrarSalidaAnticipada(Long ycId, LocalDate ycFecha, boolean ycRecalcular,
                                                      YCMetodoPago ycMetodoCobro, YCMetodoPago ycMetodoDevolucion) {
        YCEstadia ycE = ycObtener(ycId);
        if (ycE.isYcCancelada()) {
            throw new YCNegocioException("La estadia esta cancelada.");
        }
        if (ycFecha == null || !ycFecha.isAfter(ycE.getYcEntrada())) {
            throw new YCNegocioException("La salida debe ser al menos un dia despues de la entrada ("
                    + ycE.getYcEntrada().format(YC_FECHA) + ").");
        }
        if (!ycFecha.isBefore(ycE.getYcSalida())) {
            throw new YCNegocioException("Para irse antes, la fecha debe ser antes del " + ycE.getYcSalida().format(YC_FECHA) + ".");
        }
        ycExigirConsumosPagados(ycE, "registrar su salida");
        if (ycE.getYcAdelanto() == null) {
            ycE.setYcAdelanto(ycE.getYcTotal());
            ycE.setYcSaldoPagado(true);
            ycE.setYcMontoSaldo(BigDecimal.ZERO);
        } else if (Boolean.TRUE.equals(ycE.getYcSaldoPagado()) && ycE.getYcMontoSaldo() == null) {
            ycE.setYcMontoSaldo(ycE.getYcTotal().subtract(ycE.getYcAdelanto()));
        }
        YCSalidaAnticipada ycC = ycCalcularSalida(ycE, ycFecha, ycRecalcular);
        if (ycC.ycACobrar().signum() > 0 && ycMetodoCobro == null) {
            throw new YCNegocioException(ycE.getYcHuesped() + " aun debe " + ycC.ycACobrar()
                    + ". Cobra el saldo para registrar su salida.");
        }
        if (ycE.getYcSalidaOriginal() == null) {
            ycE.setYcSalidaOriginal(ycE.getYcSalida());
            ycE.setYcNochesOriginales(ycE.getYcNoches());
        }
        ycE.setYcSalida(ycFecha);
        ycE.setYcNoches(ycC.ycNochesAhora());
        ycE.setYcTotal(ycC.ycTotalAhora());
        if (ycC.ycADevolver().signum() > 0) {
            ycE.setYcDevuelto(ycC.ycADevolver());
            ycE.setYcFechaDevolucion(LocalDateTime.now());
            ycE.setYcDevueltoPor(ycSesion.ycNombreUsuario());
            ycE.setYcMetodoDevolucion(ycMetodoDevolucion == null ? YCMetodoPago.YC_EFECTIVO : ycMetodoDevolucion);
            if (!Boolean.TRUE.equals(ycE.getYcSaldoPagado())) {
                ycE.setYcSaldoPagado(true);
                ycE.setYcMontoSaldo(BigDecimal.ZERO);
            }
        } else {
            ycE.setYcDevuelto(null);
            ycE.setYcFechaDevolucion(null);
            ycE.setYcDevueltoPor(null);
            ycE.setYcMetodoDevolucion(null);
        }
        if (ycC.ycACobrar().signum() > 0) {
            ycE.setYcMontoSaldo(ycC.ycACobrar());
            ycE.setYcSaldoPagado(true);
            ycE.setYcFechaSaldo(LocalDateTime.now());
            ycE.setYcMetodoSaldo(ycMetodoCobro);
            ycE.setYcSaldoCobradoPor(ycSesion.ycNombreUsuario());
        }
        ycRepository.save(ycE);
        if (!ycFecha.isAfter(LocalDate.now())) {
            YCCuarto ycCuarto = ycE.getYcCuarto();
            ycCuarto.setYcLimpieza(com.alquilafacil.model.YCEstadoLimpieza.YC_POR_LIMPIAR);
            ycCuartoRepository.save(ycCuarto);
        }
        ycAuditar("Salida anticipada", ycE.ycGetNumero() + " · " + ycE.getYcHuesped() + " · " + ycE.getYcCuartoNumero()
                + " · sale el " + ycFecha.format(YC_FECHA) + " · noches " + ycC.ycNochesAntes() + " → " + ycC.ycNochesAhora()
                + " · total S/ " + ycC.ycTotalAntes() + " → S/ " + ycC.ycTotalAhora()
                + (ycC.ycADevolver().signum() > 0 ? " · DEVUELTO S/ " + ycC.ycADevolver() + " en " + ycE.ycGetMetodoDevolucionODefecto()
                : ycC.ycACobrar().signum() > 0 ? " · cobrado S/ " + ycC.ycACobrar() + " en " + ycMetodoCobro : " · a mano"));
        YC_LOG.info("Salida anticipada {}: {} -> {} noches, total {} -> {}, devolver {}, cobrar {}", ycE.ycGetNumero(),
                ycC.ycNochesAntes(), ycC.ycNochesAhora(), ycC.ycTotalAntes(), ycC.ycTotalAhora(), ycC.ycADevolver(), ycC.ycACobrar());
        return ycC;
    }

    public YCEstadia ycObtener(Long ycId) {
        return ycRepository.findById(ycId)
                .orElseThrow(() -> new YCNegocioException("La estadia ya no existe."));
    }

    public List<YCEstadia> ycTodas() {
        return ycRepository.ycFindAllByOrderByEntradaDescIdDesc();
    }

    public Map<Long, YCEstadia> ycEnCursoPorCuarto() {
        return ycRepository.ycEnCurso(LocalDate.now()).stream()
                .collect(Collectors.toMap(ycE -> ycE.getYcCuarto().getYcId(), Function.identity(), (ycA, ycB) -> ycA));
    }

    public List<YCEstadia> ycVigentesDe(Long ycCuartoId) {
        return ycRepository.ycVigentesDe(ycCuartoId, LocalDate.now());
    }

    public List<YCEstadia> ycEntre(LocalDate ycDesde, LocalDate ycHasta) {
        return ycRepository.ycEntre(ycDesde, ycHasta);
    }

    public BigDecimal ycCobradoEn(YearMonth ycMes) {
        LocalDateTime ycDesde = ycMes.atDay(1).atStartOfDay();
        LocalDateTime ycHasta = ycMes.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal ycAdelantos = ycRepository.ycSumarCobrado(ycDesde, ycHasta);
        BigDecimal ycSaldos = ycRepository.ycSumarSaldos(ycDesde, ycHasta);
        BigDecimal ycDevoluciones = ycRepository.ycSumarDevoluciones(ycDesde, ycHasta);
        BigDecimal ycExtras = ycConsumos == null ? null : ycConsumos.ycSumarCobrado(ycDesde, ycHasta);
        return (ycAdelantos == null ? BigDecimal.ZERO : ycAdelantos).add(ycSaldos == null ? BigDecimal.ZERO : ycSaldos)
                .add(ycExtras == null ? BigDecimal.ZERO : ycExtras)
                .subtract(ycDevoluciones == null ? BigDecimal.ZERO : ycDevoluciones);
    }

    public static long ycNoches(LocalDate ycEntrada, LocalDate ycSalida) {
        return ycEntrada == null || ycSalida == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(ycEntrada, ycSalida));
    }

    private static String ycVacioANull(String ycTexto) {
        return ycTexto == null || ycTexto.isBlank() ? null : ycTexto.trim();
    }

    private com.alquilafacil.repository.YCConsumoExtraRepository ycConsumos;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetConsumos(com.alquilafacil.repository.YCConsumoExtraRepository ycConsumos) {
        this.ycConsumos = ycConsumos;
    }

    BigDecimal ycConsumosPendientes(YCEstadia ycE) {
        if (ycConsumos == null || ycE.getYcId() == null) {
            return BigDecimal.ZERO;
        }
        return ycConsumos.ycFindByEstadiaIdInOrderByFechaRegistroAscIdAsc(List.of(ycE.getYcId())).stream()
                .filter(com.alquilafacil.model.YCConsumoExtra::ycIsPendiente)
                .map(com.alquilafacil.model.YCConsumoExtra::getYcTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ycExigirConsumosPagados(YCEstadia ycE, String ycAccion) {
        BigDecimal ycDebe = ycConsumosPendientes(ycE);
        if (ycDebe.signum() > 0) {
            throw new YCNegocioException(ycE.getYcHuesped() + " debe S/ " + ycDebe + " de consumos extra. Cobralos (o anulalos) antes de "
                    + ycAccion + ".");
        }
    }

    private YCAuditoriaService ycAuditoria;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetAuditoria(YCAuditoriaService ycAuditoria) {
        this.ycAuditoria = ycAuditoria;
    }

    private void ycAuditar(String ycAccion, String ycDetalle) {
        if (ycAuditoria != null) {
            ycAuditoria.ycRegistrar(ycAccion, ycDetalle);
        }
    }
}
