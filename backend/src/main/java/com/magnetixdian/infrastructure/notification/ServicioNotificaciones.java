package com.magnetixdian.infrastructure.notification;

import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Envía notificaciones a clientes cuando se acerca la fecha límite de
 * presentación (según el calendario DIAN por último dígito del NIT).
 */
@Service
public class ServicioNotificaciones {

    private final MedioMagneticoRepository medioRepo;
    private final NotificadorEmail notificador;
    private final List<Integer> diasAviso;

    public ServicioNotificaciones(MedioMagneticoRepository medioRepo,
                                  NotificadorEmail notificador,
                                  @Value("${magnetixdian.notifications.days-before-deadline:15,7,1}")
                                  List<Integer> diasAviso) {
        this.medioRepo = medioRepo;
        this.notificador = notificador;
        this.diasAviso = diasAviso;
    }

    /**
     * Revisa todos los medios magnéticos con fecha límite próxima y envía
     * recordatorios a la empresa correspondiente.
     */
    public void revisarVencimientos() {
        List<MediaMagnéticoXPendiente> pendientes = medioRepo.findByEstadoIn(
                        List.of("BORRADOR", "CARGADO", "VALIDADO_CON_ERRORES", "VALIDADO"))
                .stream()
                .map(this::mapear)
                .filter(p -> p.dias() >= 0)
                .toList();

        for (MediaMagnéticoXPendiente p : pendientes) {
            if (diasAviso.contains(p.dias())) {
                String destinatario = p.empresa().getEmail();
                if (destinatario == null || destinatario.isBlank()) {
                    continue;
                }
                notificador.enviarRecordatorio(
                        destinatario,
                        "Recordatorio: información exógena formato " + p.medio().getFormato()
                                + " vence en " + p.dias() + " días",
                        "Estimado cliente,\n\nLe recordamos que el medio magnético formato "
                                + p.medio().getFormato() + " (año gravable "
                                + p.medio().getAnioGravable() + ") tiene fecha límite el "
                                + p.medio().getFechaLimite() + ".\n"
                                + "Estado actual: " + p.medio().getEstado() + "\n\n"
                                + "Por favor ingrese a MagnetixDIAN para validar y presentar su información.");
            }
        }
    }

    private MediaMagnéticoXPendiente mapear(MedioMagneticoJpa medio) {
        long dias = medio.getFechaLimite() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), medio.getFechaLimite())
                : Long.MAX_VALUE;
        return new MediaMagnéticoXPendiente(medio, medio.getEmpresa(), dias);
    }

    private record MediaMagnéticoXPendiente(MedioMagneticoJpa medio, EmpresaJpa empresa, long dias) {
    }
}