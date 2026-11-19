package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Registra automáticamente la aceptación tácita (034) de facturas electrónicas de venta aceptadas
 * cuando han transcurrido 3 días hábiles sin reclamo (031), aceptación expresa (033) ni aceptación
 * tácita (034). Desactivado por defecto; se habilita con {@code magnetixdian.radian.aceptacion-tacita=true}.
 */
@Component
public class ProgramadorAceptacionTacita {

    private static final Logger log = LoggerFactory.getLogger(ProgramadorAceptacionTacita.class);
    private static final int DIAS_HABILES = 3;

    private final FacturaRepository facturaRepository;
    private final ServicioFacturacion facturacion;

    @Value("${magnetixdian.radian.aceptacion-tacita:false}")
    private boolean activo;

    public ProgramadorAceptacionTacita(FacturaRepository facturaRepository, ServicioFacturacion facturacion) {
        this.facturaRepository = facturaRepository;
        this.facturacion = facturacion;
    }

    @Scheduled(cron = "${magnetixdian.radian.aceptacion-tacita-cron:0 0 6 * * *}")
    public void registrarPendientes() {
        if (!activo) {
            return;
        }
        LocalDate limite = restarDiasHabiles(LocalDate.now(), DIAS_HABILES);
        List<FacturaJpa> candidatas = facturaRepository.candidatasAceptacionTacita(limite);
        for (FacturaJpa f : candidatas) {
            try {
                facturacion.registrarAceptacionTacita(f.getId()).ifPresent(ev ->
                        log.info("Aceptación tácita (034) registrada para la factura {}", f.getNumeroCompleto()));
            } catch (RuntimeException ex) {
                log.warn("No se pudo registrar la aceptación tácita de {}: {}", f.getNumeroCompleto(), ex.getMessage());
            }
        }
    }

    /**
     * Resta días hábiles (lunes a viernes) a una fecha, sin considerar festivos.
     */
    public static LocalDate restarDiasHabiles(LocalDate fecha, int dias) {
        LocalDate resultado = fecha;
        int restantes = dias;
        while (restantes > 0) {
            resultado = resultado.minusDays(1);
            DayOfWeek dow = resultado.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                restantes--;
            }
        }
        return resultado;
    }
}
