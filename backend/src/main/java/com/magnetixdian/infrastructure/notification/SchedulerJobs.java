package com.magnetixdian.infrastructure.notification;

import com.magnetixdian.application.ServicioGeneracionAutomatica;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jobs automáticos:
 *  - Recordatorios de vencimiento (diario, configurable vía REMINDER_CRON).
 *  - Generación del XML de medios validados (configurable vía GENERATOR_CRON).
 */
@Component
public class SchedulerJobs {

    private final ServicioNotificaciones servicioNotificaciones;
    private final ServicioGeneracionAutomatica servicioGeneracion;

    public SchedulerJobs(ServicioNotificaciones servicioNotificaciones,
                         ServicioGeneracionAutomatica servicioGeneracion) {
        this.servicioNotificaciones = servicioNotificaciones;
        this.servicioGeneracion = servicioGeneracion;
    }

    @Scheduled(cron = "${magnetixdian.cron.reminder:0 0 9 * * *}")
    public void recordatoriosDiarios() {
        servicioNotificaciones.revisarVencimientos();
    }

    @Scheduled(cron = "${magnetixdian.cron.generador:0 0 2 1 1 *}")
    public void generacionAutomaticaXml() {
        servicioGeneracion.generarPendientes();
    }
}