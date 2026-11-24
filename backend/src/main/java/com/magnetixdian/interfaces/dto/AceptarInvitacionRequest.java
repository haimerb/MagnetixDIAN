package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceptarInvitacionRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 72) String password
) {
}
