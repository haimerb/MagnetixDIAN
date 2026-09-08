package com.magnetixdian.interfaces.dto;

import java.util.List;

/**
 * Usuario del sistema (listados de administración).
 */
public record UsuarioDto(
        Long id,
        String username,
        String email,
        String nombre,
        boolean enabled,
        List<String> roles,
        Long empresaId
) {
}