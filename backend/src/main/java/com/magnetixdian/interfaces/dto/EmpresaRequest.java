package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creación/actualización de una empresa.
 */
public record EmpresaRequest(
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Size(max = 200) String razonSocial,
        @Size(max = 30) String regimen,
        @Size(max = 10) String tipoDocumento,
        boolean granContribuyente,
        @Size(max = 150) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 80) String departamento,
        @Size(max = 10) String codigoDane,
        @Size(max = 150) String email
) {
}