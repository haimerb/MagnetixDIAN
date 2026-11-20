package com.magnetixdian.interfaces.dto;

public record RegistroEmpresaResponse(
        Long empresaId,
        Long usuarioId,
        String username,
        String mensaje
) {
}
