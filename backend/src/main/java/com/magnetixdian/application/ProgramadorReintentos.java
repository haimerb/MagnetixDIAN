package com.magnetixdian.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dispara los reintentos automáticos de transmisión. Se separa de
 * {@link ServicioEnvios} para que cada reintento use su propia transacción.
 */
@Component
public class ProgramadorReintentos {

    private static final Logger log = LoggerFactory.getLogger(ProgramadorReintentos.class);

    private final ServicioEnvios envios;

    public ProgramadorReintentos(ServicioEnvios envios) {
        this.envios = envios;
    }

    @Scheduled(fixedDelayString = "${magnetixdian.transmision.reintento-ms:60000}")
    public void reintentarPendientes() {
        List<Long> ids = envios.pendientesParaReintentar();
        for (Long id : ids) {
            try {
                envios.reintentar(id, false);
            } catch (RuntimeException ex) {
                log.warn("No se pudo reintentar el envío {}: {}", id, ex.getMessage());
            }
        }
    }
}
