package com.alquilafacil.service;

import com.alquilafacil.model.YCInquilino;
import com.alquilafacil.repository.YCInquilinoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class YCMigracionRentas implements ApplicationRunner {

    private static final Logger YC_LOG = LoggerFactory.getLogger(YCMigracionRentas.class);
    private final YCInquilinoRepository ycRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments ycArgs) {
        try {
            List<YCInquilino> ycSinRenta = ycRepository.ycFindByActivoTrueOrderByApellidoAscNombreAsc().stream()
                    .filter(ycI -> ycI.getYcRenta() == null).toList();
            ycSinRenta.forEach(ycI -> ycI.setYcRenta(ycI.getYcCuarto().getYcPrecio()));
            ycRepository.saveAll(ycSinRenta);
            if (!ycSinRenta.isEmpty()) {
                YC_LOG.info("Renta pactada fijada para {} inquilinos (precio actual de su cuarto)", ycSinRenta.size());
            }
        } catch (RuntimeException ycE) {
            YC_LOG.warn("No se pudo fijar la renta de los inquilinos antiguos", ycE);
        }
    }
}
