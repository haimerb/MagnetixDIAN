package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SolicitudCausacionDto(
        LocalDate fechaCausacion,
        String observaciones) {
}
