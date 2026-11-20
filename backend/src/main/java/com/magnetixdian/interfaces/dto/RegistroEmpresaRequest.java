package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroEmpresaRequest(
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Size(max = 200) String razonSocial,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "La contraseña debe tener letras y números")
        String password,
        @AssertTrue(message = "Debe aceptar los términos y condiciones") boolean aceptaTerminos
) {
}
