package com.magnetixdian.application.pago;

import java.time.OffsetDateTime;

public record EstadoWebhookPagoDto(
        String referencia,
        String proveedor,
        String estado,
        boolean firmado,
        String eventId,
        OffsetDateTime procesadoEn,
        String comprobanteNumero,
        String mensaje) {
}
