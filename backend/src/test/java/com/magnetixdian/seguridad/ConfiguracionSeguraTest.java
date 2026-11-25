package com.magnetixdian.seguridad;

import com.magnetixdian.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionSeguraTest {

    private static final String JWT = "jtWZx9pL2qR7sT4vN8mB3kD6fH1jK5cY9wE2uA7zX0r=";
    private static final String CRYPTO = "kR7mQ2xV9pL4nT6sB8cY1wZ5dF3hJ0a=";
    private static final Map<String, String> PROPIEDADES = Map.of(
            "magnetixdian.jwt.secret", "JWT_SECRET",
            "magnetixdian.crypto.key", "CRYPTO_KEY",
            "magnetixdian.crypto.key-id", "CRYPTO_KEY_ID",
            "magnetixdian.app.frontend-url", "FRONTEND_URL",
            "magnetixdian.pagos.wompi.events-secret", "WOMPI_EVENTS_SECRET",
            "magnetixdian.pagos.mercadopago.webhook-secret", "MERCADOPAGO_WEBHOOK_SECRET");

    private MockEnvironment entorno(boolean failFast) {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("magnetixdian.seguridad.fail-fast", Boolean.toString(failFast));
        env.setProperty("magnetixdian.jwt.secret", JWT);
        env.setProperty("magnetixdian.crypto.key", CRYPTO);
        env.setProperty("magnetixdian.crypto.key-id", "primary");
        env.setProperty("magnetixdian.app.frontend-url", "https://magnetixdian.example.workers.dev");
        env.setProperty("magnetixdian.pagos.wompi.events-secret", "wompi-event-secret-value");
        env.setProperty("magnetixdian.pagos.mercadopago.webhook-secret", "mp-webhook-secret-value");
        env.setProperty("magnetixdian.pagos.exigir-firma-webhook", "true");
        env.setProperty("integraciones.exigir-firma-webhook", "true");
        env.setProperty("magnetixdian.seguridad.tenant-filter-enabled", "true");
        env.setProperty("magnetixdian.auth.expose-token", "false");
        return env;
    }

    @Test
    void failFastConTodosLosSecretosArranca() {
        assertDoesNotThrow(() -> SecurityConfig.validarConfiguracionSegura(entorno(true)));
    }

    @Test
    void failFastFallaSiFaltaCualquierSecreto() {
        PROPIEDADES.forEach((propiedad, variable) -> {
            MockEnvironment env = entorno(true);
            env.setProperty(propiedad, "");
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> SecurityConfig.validarConfiguracionSegura(env));
            assertTrue(error.getMessage().contains(variable), error.getMessage());
            assertTrue(!error.getMessage().contains(JWT) && !error.getMessage().contains(CRYPTO));
        });
    }

    @Test
    void failFastRechazaSecretosDeEjemplo() {
        MockEnvironment env = entorno(true);
        env.setProperty("magnetixdian.crypto.key", "magnetixdian-dev-crypto-key-change-me");
        env.setProperty("magnetixdian.jwt.secret", "base64-de-64-bytes");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> SecurityConfig.validarConfiguracionSegura(env));
        assertTrue(error.getMessage().contains("CRYPTO_KEY"));
        assertTrue(error.getMessage().contains("JWT_SECRET"));
        assertTrue(!error.getMessage().contains("change-me") && !error.getMessage().contains("base64-de-64-bytes"));
    }

    @Test
    void failFastRechazaFirmasWebhookDesactivadas() {
        MockEnvironment env = entorno(true);
        env.setProperty("magnetixdian.pagos.exigir-firma-webhook", "false");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> SecurityConfig.validarConfiguracionSegura(env));
        assertTrue(error.getMessage().contains("PAGOS_EXIGIR_FIRMA_WEBHOOK=true"));
    }

    @Test
    void devPermiteSecretosDeEjemploYSIMULADO() {
        MockEnvironment env = entorno(false);
        env.setProperty("magnetixdian.crypto.key", "magnetixdian-dev-crypto-key-change-me");
        assertDoesNotThrow(() -> SecurityConfig.validarConfiguracionSegura(env));
    }
}
