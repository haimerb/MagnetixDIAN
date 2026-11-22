package com.magnetixdian.interfaces.dto;

public record EntregaWhatsAppResponse(
        Long empresaId,
        Long facturaId,
        String numero,
        String enlace,
        String mensaje,
        boolean enviado) {
}
