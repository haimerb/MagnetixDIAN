package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarVerificacionRequest(
        @NotBlank @Email String email
) {
}
