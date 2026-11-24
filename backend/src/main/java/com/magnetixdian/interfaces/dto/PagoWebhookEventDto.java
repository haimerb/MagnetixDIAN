package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record PagoWebhookEventDto(
        String proveedor,
        String eventId,
        String referencia,
        String estado,
        BigDecimal monto,
        String moneda) {
}
