package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud de creación de usuario (rol y contraseña en claro).
 */
public record UsuarioRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 150) String nombre,
        @NotEmpty List<String> roles,
        Long empresaId,
        Boolean enabled
) {
}