package com.magnetixdian.interfaces.dto;

public record ReenviarVerificacionResponse(
        boolean enviado,
        String mensaje,
        String token
) {
}
