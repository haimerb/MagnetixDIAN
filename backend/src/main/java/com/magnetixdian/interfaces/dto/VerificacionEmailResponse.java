package com.magnetixdian.interfaces.dto;

public record VerificacionEmailResponse(
        Long empresaId,
        String estadoTenant,
        boolean emailVerificado,
        String mensaje
) {
}
