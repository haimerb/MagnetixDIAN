package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudResetRequest(@NotBlank String identificador) {
}
