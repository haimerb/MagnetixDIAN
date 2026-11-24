package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record InvitacionTenantResponse(
        Long empresaId,
        Long usuarioId,
        String username,
        String email,
        String rol,
        OffsetDateTime expiraAt,
        String mensaje
) {
}
