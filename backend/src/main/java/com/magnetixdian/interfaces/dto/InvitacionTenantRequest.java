package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitacionTenantRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 30) String rol
) {
}
