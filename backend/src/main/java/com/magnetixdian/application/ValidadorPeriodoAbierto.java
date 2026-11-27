package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ValidadorPeriodoAbierto {

    private final PeriodoContableRepository periodoRepository;

    public ValidadorPeriodoAbierto(PeriodoContableRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    public void verificar(Long empresaId, LocalDate fecha) {
        if (empresaId == null || fecha == null) {
            return;
        }
        PeriodoContableJpa periodo = periodoRepository.findVigentePorFecha(empresaId, fecha).orElse(null);
        if (periodo != null && !"ABIERTO".equalsIgnoreCase(periodo.getEstado())) {
            throw new IllegalArgumentException(
                    "El periodo contable " + periodo.getAnio() + "-" + periodo.getMes()
                            + " está cerrado; no se pueden registrar movimientos para la fecha " + fecha + ".");
        }
    }
}
