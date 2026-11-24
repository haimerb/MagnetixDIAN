package com.magnetixdian.domain.pago;

public record PagoAprobadoEvent(
        Long empresaId,
        String planCode,
        String referencia,
        String eventId
) {
    public PagoAprobadoEvent {
        if (empresaId == null) {
            throw new IllegalArgumentException("empresaId es obligatorio.");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId es obligatorio.");
        }
    }
}
