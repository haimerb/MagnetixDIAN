package com.magnetixdian.domain.pago;

import java.util.Map;
import java.util.Optional;

/**
 * Puerto de integración con una pasarela de pago. Cada implementación se
 * registra como bean y se selecciona por {@link #proveedor()}.
 */
public interface PasarelaPago {

    /** Identificador de la pasarela (SIMULADO, WOMPI, MERCADOPAGO). */
    String proveedor();

    /** Indica si la pasarela tiene credenciales configuradas. */
    boolean configurada();

    /** Inicia el checkout y devuelve la URL de redirección (o confirmación inmediata). */
    CheckoutPago crearCheckout(SolicitudPago solicitud);

    /** Normaliza una notificación de la pasarela; vacío si no es procesable. */
    Optional<EventoPago> procesarWebhook(String payload, Map<String, String> headers);
}
