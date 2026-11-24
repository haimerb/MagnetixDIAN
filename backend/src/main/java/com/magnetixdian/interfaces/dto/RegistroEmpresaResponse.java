package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.dian.ModoOperacionDian;

public record RegistroEmpresaResponse(
        Long empresaId,
        Long usuarioId,
        String username,
        String planCode,
        String estadoTenant,
        ModoOperacionDian modoOperacionDian,
        String mensaje
) {
    public RegistroEmpresaResponse(Long empresaId, Long usuarioId, String username, String mensaje) {
        this(empresaId, usuarioId, username, null, null, null, mensaje);
    }
}
