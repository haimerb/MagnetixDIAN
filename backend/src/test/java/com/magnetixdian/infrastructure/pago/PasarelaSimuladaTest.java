package com.magnetixdian.infrastructure.pago;

import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelaSimuladaTest {

    private final PasarelaSimulada pasarela = new PasarelaSimulada();

    @Test
    void siempreConfigurada() {
        assertEquals("SIMULADO", pasarela.proveedor());
        assertTrue(pasarela.configurada());
    }

    @Test
    void checkoutEsInmediatoSinUrl() {
        SolicitudPago solicitud = new SolicitudPago(1L, 2L, "SIMULADO-1", "PRO",
                new BigDecimal("99000"), "COP", "Plan Profesional", "http://localhost/suscripcion");
        CheckoutPago checkout = pasarela.crearCheckout(solicitud);
        assertTrue(checkout.confirmadoInmediato());
        assertNull(checkout.url());
    }

    @Test
    void noProcesaWebhooks() {
        assertTrue(pasarela.procesarWebhook("{}", Map.of()).isEmpty());
    }
}
