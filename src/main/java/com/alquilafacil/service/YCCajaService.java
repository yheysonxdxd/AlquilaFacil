package com.alquilafacil.service;

import com.alquilafacil.model.YCEstadia;
import com.alquilafacil.model.YCGasto;
import com.alquilafacil.model.YCLiquidacion;
import com.alquilafacil.model.YCMetodoPago;
import com.alquilafacil.model.YCPago;
import com.alquilafacil.repository.YCEstadiaRepository;
import com.alquilafacil.repository.YCGastoRepository;
import com.alquilafacil.repository.YCLiquidacionRepository;
import com.alquilafacil.repository.YCPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YCCajaService {
    static final String YC_NOTA_SALIDA = "Pagado al retirarse";

    public record YCMovimiento(LocalDateTime ycHora, boolean ycEntrada, String ycConcepto, String ycDetalle,
                             YCMetodoPago ycMetodo, BigDecimal ycMonto, String ycUsuario) {}

    public record YCAnulacion(LocalDateTime ycHora, String ycQue, BigDecimal ycMonto, String ycMotivo, String ycUsuario) {}

    public record YCCierre(LocalDate ycDia, String ycUsuario, boolean ycConGastos, List<YCMovimiento> ycMovimientos,
                         List<YCAnulacion> ycAnulaciones) {
        public BigDecimal ycTotalEntradas() {
            return ycSumar(true, null);
        }

        public BigDecimal ycTotalSalidas() {
            return ycSumar(false, null);
        }

        public BigDecimal ycNeto() {
            return ycTotalEntradas().subtract(ycTotalSalidas());
        }

        public BigDecimal ycEntradas(YCMetodoPago ycM) {
            return ycSumar(true, ycM);
        }

        public BigDecimal ycSalidas(YCMetodoPago ycM) {
            return ycSumar(false, ycM);
        }

        public BigDecimal ycEfectivoEnCaja() {
            return ycEntradas(YCMetodoPago.YC_EFECTIVO).subtract(ycSalidas(YCMetodoPago.YC_EFECTIVO));
        }

        public Map<String, BigDecimal> ycCobradoPorUsuario() {
            Map<String, BigDecimal> ycR = new TreeMap<>();
            ycMovimientos.stream().filter(YCMovimiento::ycEntrada)
                    .forEach(ycM -> ycR.merge(ycM.ycUsuario() == null ? "—" : ycM.ycUsuario(), ycM.ycMonto(), BigDecimal::add));
            return ycR;
        }

        private BigDecimal ycSumar(boolean ycEntrada, YCMetodoPago ycMetodo) {
            return ycMovimientos.stream()
                    .filter(ycM -> ycM.ycEntrada() == ycEntrada && (ycMetodo == null || ycM.ycMetodo() == ycMetodo))
                    .map(YCMovimiento::ycMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private final YCPagoRepository ycPagoRepository;
    private final YCEstadiaRepository ycEstadiaRepository;
    private final YCLiquidacionRepository ycLiquidacionRepository;
    private final YCGastoRepository ycGastoRepository;
    private final com.alquilafacil.repository.YCCierreCajaRepository ycCierreRepository;
    private final YCSesion ycSesion;
    private final YCAuditoriaService ycAuditoria;

    public YCCierre ycCierre(LocalDate ycDia, String ycUsuario) {
        boolean ycAdmin = ycSesion.ycEsAdmin();
        String ycFiltro = ycAdmin ? ycUsuario : ycSesion.ycNombreUsuario();
        LocalDateTime ycDesde = ycDia.atStartOfDay();
        LocalDateTime ycHasta = ycDia.plusDays(1).atStartOfDay();
        List<YCMovimiento> ycMovs = new ArrayList<>();
        List<YCAnulacion> ycAnuladas = new ArrayList<>();

        for (YCPago ycP : ycPagoRepository.ycFindByFechaPagoGreaterThanEqualAndFechaPagoLessThanOrderByFechaPago(ycDesde, ycHasta)) {
            if (ycP.isYcAnulado() || (ycP.getYcNota() != null && ycP.getYcNota().startsWith(YC_NOTA_SALIDA))) {
                continue;
            }
            ycMovs.add(new YCMovimiento(ycP.getYcFechaPago(), true, "Alquiler " + ycP.ycGetNumeroRecibo(),
                    ycP.getYcInquilino().ycGetNombreCompleto() + " · " + ycP.getYcCuartoNumero() + " · "
                            + YCCalendarioPagos.ycTexto(ycP.getYcPeriodo()),
                    ycP.getYcMetodo(), ycP.getYcTotal(), ycP.getYcRegistradoPor()));
        }
        for (YCPago ycP : ycPagoRepository.ycFindByFechaAnulacionGreaterThanEqualAndFechaAnulacionLessThanOrderByFechaAnulacion(ycDesde, ycHasta)) {
            ycAnuladas.add(new YCAnulacion(ycP.getYcFechaAnulacion(), "Recibo " + ycP.ycGetNumeroRecibo() + " · "
                    + ycP.getYcInquilino().ycGetNombreCompleto(), ycP.getYcTotal(), ycP.getYcMotivoAnulacion(), ycP.getYcAnuladoPor()));
        }

        for (YCLiquidacion ycL : ycLiquidacionRepository.ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFecha(ycDesde, ycHasta)) {
            YCMetodoPago ycM = ycL.getYcMetodo() == null ? YCMetodoPago.YC_EFECTIVO : ycL.getYcMetodo();
            String ycQuien = ycL.getYcInquilino().ycGetNombreCompleto() + " · " + ycL.getYcCuartoNumero();
            if (ycL.getYcCobrado().signum() > 0) {
                ycMovs.add(new YCMovimiento(ycL.getYcFecha(), true, "Salida de inquilino " + ycL.ycGetNumero(),
                        ycQuien, ycM, ycL.getYcCobrado(), ycL.getYcRegistradoPor()));
            }
            if (ycL.getYcDevuelto().signum() > 0) {
                ycMovs.add(new YCMovimiento(ycL.getYcFecha(), false, "Garantia devuelta " + ycL.ycGetNumero(),
                        ycQuien, ycM, ycL.getYcDevuelto(), ycL.getYcRegistradoPor()));
            }
        }

        for (YCEstadia ycE : ycEstadiaRepository.ycConMovimientosEntre(ycDesde, ycHasta)) {
            String ycQuien = ycE.getYcHuesped() + " · " + ycE.getYcCuartoNumero();
            BigDecimal ycAdelanto = ycE.getYcAdelanto() == null ? ycE.getYcTotal() : ycE.getYcAdelanto();
            if (ycDentro(ycE.getYcFechaRegistro(), ycDesde, ycHasta) && ycAdelanto.signum() > 0
                    && (!ycE.isYcCancelada() || ycE.ycIsNoLlego())) {
                ycMovs.add(new YCMovimiento(ycE.getYcFechaRegistro(), true,
                        (ycE.getYcAdelanto() == null || ycAdelanto.compareTo(ycE.getYcTotal()) >= 0 ? "Hospedaje " : "Adelanto ")
                                + ycE.ycGetNumero(), ycQuien, ycE.getYcMetodo(), ycAdelanto, ycE.getYcRegistradoPor()));
            }
            if (ycDentro(ycE.getYcFechaSaldo(), ycDesde, ycHasta) && Boolean.TRUE.equals(ycE.getYcSaldoPagado())
                    && ycE.getYcAdelanto() != null && !ycE.isYcCancelada()) {
                BigDecimal ycSaldo = ycE.getYcMontoSaldo() != null ? ycE.getYcMontoSaldo() : ycE.getYcTotal().subtract(ycE.getYcAdelanto());
                if (ycSaldo.signum() > 0) {
                    ycMovs.add(new YCMovimiento(ycE.getYcFechaSaldo(), true, "Saldo " + ycE.ycGetNumero(), ycQuien,
                            ycE.getYcMetodoSaldo() == null ? YCMetodoPago.YC_EFECTIVO : ycE.getYcMetodoSaldo(), ycSaldo,
                            ycE.getYcSaldoCobradoPor()));
                }
            }
            if (ycDentro(ycE.getYcFechaDevolucion(), ycDesde, ycHasta) && ycE.getYcDevuelto() != null && ycE.getYcDevuelto().signum() > 0) {
                ycMovs.add(new YCMovimiento(ycE.getYcFechaDevolucion(), false,
                        (ycE.ycIsNoLlego() ? "Devolucion (no vino) " : "Devolucion (salida antes) ") + ycE.ycGetNumero(), ycQuien,
                        ycE.ycGetMetodoDevolucionODefecto(), ycE.getYcDevuelto(), ycE.getYcDevueltoPor()));
            }
            if (ycDentro(ycE.getYcFechaCancelacion(), ycDesde, ycHasta) && ycE.isYcCancelada()) {
                ycAnuladas.add(new YCAnulacion(ycE.getYcFechaCancelacion(),
                        (ycE.ycIsNoLlego() ? "No vino " : "Estadia cancelada ") + ycE.ycGetNumero() + " · " + ycQuien,
                        ycE.ycGetRecibido(), ycE.getYcMotivoCancelacion(), ycE.getYcCanceladaPor()));
            }
        }

        if (ycConsumos != null) {
            for (com.alquilafacil.model.YCConsumoExtra ycC : ycConsumos.ycConMovimientosEntre(ycDesde, ycHasta)) {
                String ycQuien = ycC.getYcEstadia().getYcHuesped() + " · " + ycC.getYcEstadia().getYcCuartoNumero();
                if (ycC.isYcPagado() && !ycC.isYcAnulado() && ycDentro(ycC.getYcFechaPago(), ycDesde, ycHasta)) {
                    ycMovs.add(new YCMovimiento(ycC.getYcFechaPago(), true, "Consumo: " + ycC.ycGetDescripcion(),
                            ycQuien + " · " + ycC.getYcEstadia().ycGetNumero(),
                            ycC.getYcMetodo() == null ? YCMetodoPago.YC_EFECTIVO : ycC.getYcMetodo(), ycC.getYcTotal(), ycC.getYcCobradoPor()));
                }
                if (ycC.isYcAnulado() && ycDentro(ycC.getYcFechaAnulacion(), ycDesde, ycHasta)) {
                    ycAnuladas.add(new YCAnulacion(ycC.getYcFechaAnulacion(), "Consumo " + ycC.ycGetDescripcion() + " · " + ycQuien,
                            ycC.getYcTotal(), ycC.getYcMotivoAnulacion(), ycC.getYcAnuladoPor()));
                }
            }
        }

        boolean ycConGastos = ycAdmin;
        if (ycConGastos) {
            for (YCGasto ycG : ycGastoRepository.ycFindByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(ycDia, ycDia.plusDays(1))) {
                LocalDateTime ycHora = ycG.getYcFechaRegistro() != null && ycG.getYcFechaRegistro().toLocalDate().equals(ycDia)
                        ? ycG.getYcFechaRegistro() : ycDesde;
                ycMovs.add(new YCMovimiento(ycHora, false, "Gasto: " + ycG.getYcCategoria().ycGetTexto(),
                        ycG.getYcDescripcion() == null ? ycG.getYcNegocio().ycGetTexto() : ycG.getYcDescripcion(),
                        ycG.getYcMetodo() == null ? YCMetodoPago.YC_EFECTIVO : ycG.getYcMetodo(), ycG.getYcMonto(), ycG.getYcRegistradoPor()));
            }
        }

        if (ycFiltro != null) {
            ycMovs.removeIf(ycM -> !ycFiltro.equals(ycM.ycUsuario()));
            ycAnuladas.removeIf(ycA -> !ycFiltro.equals(ycA.ycUsuario()));
        }
        ycMovs.sort(Comparator.comparing(YCMovimiento::ycHora));
        ycAnuladas.sort(Comparator.comparing(YCAnulacion::ycHora));
        return new YCCierre(ycDia, ycFiltro, ycConGastos, List.copyOf(ycMovs), List.copyOf(ycAnuladas));
    }

    @Transactional
    public com.alquilafacil.model.YCCierreCaja ycGuardarCierre(LocalDate ycDia, String ycUsuario, BigDecimal ycContado, String ycNota) {
        if (ycContado == null || ycContado.signum() < 0) {
            throw new com.alquilafacil.exception.YCNegocioException("Escribe el efectivo que contaste en la caja.");
        }
        if (ycDia.isAfter(LocalDate.now())) {
            throw new com.alquilafacil.exception.YCNegocioException("No se puede cerrar un dia que aun no llega.");
        }
        YCCierre ycC = ycCierre(ycDia, ycUsuario);
        BigDecimal ycEsperado = ycC.ycEfectivoEnCaja();
        BigDecimal ycDiferencia = ycContado.subtract(ycEsperado);
        String ycLimpia = ycNota == null ? "" : ycNota.trim().replaceAll("\\s+", " ");
        if (ycDiferencia.signum() != 0 && ycLimpia.length() < 5) {
            throw new com.alquilafacil.exception.YCNegocioException((ycDiferencia.signum() > 0 ? "Sobran " : "Faltan ")
                    + "S/ " + ycDiferencia.abs() + ". Escribe en la nota a que se debe (minimo 5 letras).");
        }
        com.alquilafacil.model.YCCierreCaja ycGuardado = ycCierreRepository.save(com.alquilafacil.model.YCCierreCaja.builder()
                .ycDia(ycDia).ycUsuarioCaja(ycC.ycUsuario())
                .ycEntradas(ycC.ycTotalEntradas()).ycSalidas(ycC.ycTotalSalidas())
                .ycEfectivoEsperado(ycEsperado).ycEfectivoContado(ycContado).ycDiferencia(ycDiferencia)
                .ycNota(ycLimpia.isEmpty() ? null : (ycLimpia.length() > 200 ? ycLimpia.substring(0, 200) : ycLimpia))
                .ycCerradoPor(ycSesion.ycNombreUsuario()).ycFechaCierre(LocalDateTime.now())
                .build());
        ycAuditoria.ycRegistrar("Cierre de caja", ycDia + (ycC.ycUsuario() == null ? " (todos)" : " (" + ycC.ycUsuario() + ")")
                + " · esperado S/ " + ycEsperado + " · contado S/ " + ycContado
                + (ycDiferencia.signum() == 0 ? " · cuadra" : (ycDiferencia.signum() > 0 ? " · sobra S/ " : " · falta S/ ")
                + ycDiferencia.abs() + " · " + ycLimpia));
        return ycGuardado;
    }

    public List<com.alquilafacil.model.YCCierreCaja> ycCierresDel(LocalDate ycDia) {
        List<com.alquilafacil.model.YCCierreCaja> ycTodos = ycCierreRepository.ycFindByDiaOrderByFechaCierreDesc(ycDia);
        if (ycSesion.ycEsAdmin()) {
            return ycTodos;
        }
        String ycYo = ycSesion.ycNombreUsuario();
        return ycTodos.stream().filter(ycX -> ycYo.equals(ycX.getYcCerradoPor())).toList();
    }

    private com.alquilafacil.repository.YCConsumoExtraRepository ycConsumos;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void ycSetConsumos(com.alquilafacil.repository.YCConsumoExtraRepository ycConsumos) {
        this.ycConsumos = ycConsumos;
    }

    private static boolean ycDentro(LocalDateTime ycT, LocalDateTime ycDesde, LocalDateTime ycHasta) {
        return ycT != null && !ycT.isBefore(ycDesde) && ycT.isBefore(ycHasta);
    }
}
