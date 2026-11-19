package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Confirmación de restablecimiento o aceptación de invitación con token de un uso.
 */
public record TokenPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
