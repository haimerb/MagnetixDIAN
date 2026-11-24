package com.magnetixdian.application.pago;

public record WebhookResultDto(
        String estado,
        String referencia,
        String comprobanteNumero,
        boolean duplicado,
        String mensaje) {
}
