package com.magnetixdian.infrastructure.notification;

import com.magnetixdian.domain.notificacion.NotificadorWhatsApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class NotificadorWhatsAppClickToChat implements NotificadorWhatsApp {

    private static final int MIN_DIGITOS = 7;
    private static final int MAX_DIGITOS = 13;

    private final String baseUrl;

    public NotificadorWhatsAppClickToChat(
            @Value("${magnetixdian.whatsapp.base-url:https://wa.me}") String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://wa.me" : baseUrl.trim();
    }

    @Override
    public String normalizarNumero(String numero) {
        if (numero == null) {
            return null;
        }
        String normalizado = numero.replaceAll("[\\s+()\\-]", "").trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    @Override
    public boolean numeroValido(String numero) {
        String normalizado = normalizarNumero(numero);
        if (normalizado == null || normalizado.length() < MIN_DIGITOS || normalizado.length() > MAX_DIGITOS) {
            return false;
        }
        for (int i = 0; i < normalizado.length(); i++) {
            if (!Character.isDigit(normalizado.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String construirEnlace(String numero, String mensaje) {
        String destino = normalizarNumero(numero);
        if (!numeroValido(destino)) {
            throw new IllegalArgumentException("Número de WhatsApp inválido.");
        }
        String texto = mensaje == null ? "" : mensaje;
        String raiz = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return raiz + "/" + destino + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}
