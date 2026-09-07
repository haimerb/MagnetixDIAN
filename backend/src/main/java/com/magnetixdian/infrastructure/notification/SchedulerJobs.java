package com.magnetixdian.infrastructure.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jobs automáticos:
 *  - Recordatorios de vencimiento (diario, configurable vía REMINDER_CRON).
 */
@Component
public class SchedulerJobs {

    private final ServicioNotificaciones servicioNotificaciones;

    public SchedulerJobs(ServicioNotificaciones servicioNotificaciones) {
        this.servicioNotificaciones = servicioNotificaciones;
    }

    @Scheduled(cron = "${magnetixdian.cron.reminder:0 0 9 * * *}")
    public void recordatoriosDiarios() {
        servicioNotificaciones.revisarVencimientos();
    }
}