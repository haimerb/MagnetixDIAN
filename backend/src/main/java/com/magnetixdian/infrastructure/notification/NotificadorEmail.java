package com.magnetixdian.infrastructure.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envío de notificaciones por email a clientes (recordatorios de vencimiento y
 * facturas electrónicas al adquirente).
 * Se desactiva con la propiedad magnetixdian.notifications.enabled=false.
 */
@Service
public class NotificadorEmail {

    private final JavaMailSender mailSender;
    private final boolean enabled;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public NotificadorEmail(JavaMailSender mailSender,
                            @Value("${magnetixdian.notifications.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.enabled = enabled;
    }

    public boolean habilitado() {
        return enabled;
    }

    /**
     * Envía un correo de texto plano (recuperación de contraseña, invitaciones).
     * Devuelve true si el correo se entregó al servidor SMTP.
     */
    public boolean enviarTexto(String destinatario, String asunto, String cuerpo) {
        if (!enabled) {
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
        return true;
    }

    /**
     * Envía un recordatorio de vencimiento al destinatario indicado.
     */
    public void enviarRecordatorio(String destinatario, String asunto, String cuerpo) {
        enviarTexto(destinatario, asunto, cuerpo);
    }

    /**
     * Envía la factura electrónica (XML firmado + PDF) al adquirente como adjuntos.
     */
    public void enviarFactura(String destinatario, String asunto, String cuerpo,
                              String nombreXml, byte[] xml,
                              String nombrePdf, byte[] pdf) throws MessagingException {
        if (!enabled) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        if (mailFrom != null && !mailFrom.isBlank()) {
            helper.setFrom(mailFrom);
        }
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(cuerpo, false);
        if (xml != null && xml.length > 0) {
            helper.addAttachment(nombreXml, new ByteArrayResource(xml), "application/xml");
        }
        if (pdf != null && pdf.length > 0) {
            helper.addAttachment(nombrePdf, new ByteArrayResource(pdf), "application/pdf");
        }
        mailSender.send(message);
    }
}