package com.magnetixdian.infrastructure.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificadorEmailTest {

    private MimeMessage nuevoMime() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void enviaFacturaConAdjuntosCuandoEstaHabilitado() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(nuevoMime());
        NotificadorEmail notificador = new NotificadorEmail(mailSender, true);

        notificador.enviarFactura("cliente@test.co", "Factura FE-1", "Cuerpo",
                "factura-FE-1.xml", "<xml/>".getBytes(), "factura-FE-1.pdf", new byte[]{1, 2, 3});

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void noEnviaCuandoEstaDeshabilitado() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificadorEmail notificador = new NotificadorEmail(mailSender, false);

        notificador.enviarFactura("cliente@test.co", "Factura FE-1", "Cuerpo",
                "factura-FE-1.xml", "<xml/>".getBytes(), "factura-FE-1.pdf", new byte[]{1, 2, 3});

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void exponeEstadoDeHabilitacion() {
        assertThat(new NotificadorEmail(mock(JavaMailSender.class), true).habilitado()).isTrue();
        assertThat(new NotificadorEmail(mock(JavaMailSender.class), false).habilitado()).isFalse();
    }
}
