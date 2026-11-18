package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Genera automáticamente el XML de los medios magnéticos validados sin errores
 * (estado {@code VALIDADO}). Es idempotente: los medios ya generados quedan en
 * estado {@code XML_GENERADO} y no vuelven a procesarse. Un fallo en un medio
 * no detiene el lote.
 */
@Service
public class ServicioGeneracionAutomatica {

    private static final Logger log = LoggerFactory.getLogger(ServicioGeneracionAutomatica.class);

    private final MedioMagneticoRepository medioRepo;
    private final ServicioMedioMagnetico servicioMedio;

    public ServicioGeneracionAutomatica(MedioMagneticoRepository medioRepo,
                                        ServicioMedioMagnetico servicioMedio) {
        this.medioRepo = medioRepo;
        this.servicioMedio = servicioMedio;
    }

    /**
     * Procesa todos los medios validados sin errores.
     *
     * @return cantidad de medios cuyo XML se generó correctamente
     */
    public int generarPendientes() {
        List<MedioMagneticoJpa> validados = medioRepo.findByEstadoIn(List.of("VALIDADO"));
        if (validados.isEmpty()) {
            log.debug("Generación automática: no hay medios validados pendientes de XML");
            return 0;
        }
        int generados = 0;
        for (MedioMagneticoJpa medio : validados) {
            try {
                servicioMedio.generarXml(medio.getId());
                generados++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo generar el XML del medio magnético {}: {}",
                        medio.getId(), ex.getMessage());
            }
        }
        log.info("Generación automática: {}/{} medios magnéticos procesados", generados, validados.size());
        return generados;
    }
}
