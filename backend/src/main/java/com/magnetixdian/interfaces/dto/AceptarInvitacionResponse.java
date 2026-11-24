package com.magnetixdian.interfaces.dto;

public record AceptarInvitacionResponse(
        Long empresaId,
        Long usuarioId,
        String username,
        String rol,
        String mensaje
) {
}
