package com.magnetixdian.interfaces.dto;

import java.util.List;

/**
 * Perfil del usuario autenticado (GET /api/auth/me): datos de la sesión y,
 * si aplica, la empresa a la que está vinculado.
 */
public record PerfilUsuarioDto(
        Long id,
        String username,
        String nombre,
        String email,
        List<String> roles,
        Long empresaId,
        String empresaNit,
        String empresaRazonSocial) {
}