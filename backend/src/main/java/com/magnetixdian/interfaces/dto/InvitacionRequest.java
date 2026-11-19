package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Invitación de un usuario: la contraseña la define el invitado desde el enlace.
 */
public record InvitacionRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 150) String nombre,
        @NotEmpty List<String> roles,
        Long empresaId
) {
}
