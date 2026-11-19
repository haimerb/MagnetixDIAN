package com.magnetixdian.infrastructure.pago;

import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Pasarela de desarrollo: aprueba el pago de inmediato sin redirección. Es la
 * opción por defecto y no requiere credenciales.
 */
@Component
public class PasarelaSimulada implements PasarelaPago {

    public static final String NOMBRE = "SIMULADO";

    @Override
    public String proveedor() {
        return NOMBRE;
    }

    @Override
    public boolean configurada() {
        return true;
    }

    @Override
    public CheckoutPago crearCheckout(SolicitudPago solicitud) {
        return CheckoutPago.inmediato();
    }

    @Override
    public Optional<EventoPago> procesarWebhook(String payload, Map<String, String> headers) {
        return Optional.empty();
    }
}
