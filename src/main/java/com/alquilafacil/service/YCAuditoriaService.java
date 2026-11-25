package com.alquilafacil.service;

import com.alquilafacil.model.YCAuditoria;
import com.alquilafacil.repository.YCAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class YCAuditoriaService {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCAuditoriaService.class);
    private final YCAuditoriaRepository ycRepository;
    private final YCSesion ycSesion;

    @Transactional
    public void ycRegistrar(String ycAccion, String ycDetalle) {
        try {
            ycRepository.save(YCAuditoria.builder()
                    .ycFecha(LocalDateTime.now())
                    .ycUsuario(ycSesion.ycNombreUsuario())
                    .ycAccion(ycRecortar(ycAccion, 60))
                    .ycDetalle(ycRecortar(ycDetalle, 600))
                    .build());
            YC_LOG.info("[historial] {} · {} · {}", ycSesion.ycNombreUsuario(), ycAccion, ycDetalle);
        } catch (RuntimeException ycE) {
            YC_LOG.warn("No se pudo anotar en el historial: {} {}", ycAccion, ycDetalle, ycE);
        }
    }

    @Transactional(readOnly = true)
    public List<YCAuditoria> ycBuscar(LocalDateTime ycDesde, String ycTexto) {
        ycSesion.ycExigirAdmin("ver el historial de cambios");
        return ycRepository.ycBuscar(ycDesde == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : ycDesde,
                ycTexto == null ? "" : ycTexto.trim(), PageRequest.of(0, 500));
    }

    public static final class YCCambios {
        private final List<String> ycPartes = new ArrayList<>();

        public YCCambios ycCampo(String ycNombre, Object ycAntes, Object ycDespues) {
            String ycA = ycTexto(ycAntes);
            String ycD = ycTexto(ycDespues);
            if (!Objects.equals(ycA, ycD)) {
                ycPartes.add(ycNombre + " " + ycA + " → " + ycD);
            }
            return this;
        }

        public boolean ycHay() {
            return !ycPartes.isEmpty();
        }

        @Override
        public String toString() {
            return String.join(" · ", ycPartes);
        }

        private static String ycTexto(Object ycV) {
            if (ycV == null) {
                return "—";
            }
            if (ycV instanceof BigDecimal ycB) {
                return "S/ " + ycB.setScale(2, RoundingMode.HALF_UP).toPlainString();
            }
            String ycS = ycV.toString();
            return ycS.isBlank() ? "—" : ycS;
        }
    }

    private static String ycRecortar(String ycS, int ycMax) {
        if (ycS == null) {
            return "";
        }
        return ycS.length() <= ycMax ? ycS : ycS.substring(0, ycMax - 1) + "…";
    }
}
