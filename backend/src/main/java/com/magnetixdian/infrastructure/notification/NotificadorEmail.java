package com.magnetixdian.infrastructure.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de notificaciones por email a clientes (recordatorios de vencimiento).
 * Se desactiva con la propiedad magnetixdian.notifications.enabled=false.
 */
@Service
public class NotificadorEmail {

    private final JavaMailSender mailSender;
    private final boolean enabled;

    public NotificadorEmail(JavaMailSender mailSender,
                            @Value("${magnetixdian.notifications.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.enabled = enabled;
    }

    /**
     * Envía un recordatorio de vencimiento al destinatario indicado.
     */
    public void enviarRecordatorio(String destinatario, String asunto, String cuerpo) {
        if (!enabled) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
    }
}