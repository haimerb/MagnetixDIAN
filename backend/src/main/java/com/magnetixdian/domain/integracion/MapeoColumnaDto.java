package com.magnetixdian.domain.integracion;

public record MapeoColumnaDto(
        String campo,
        String cabeceraOrigen,
        int indice,
        double confianza,
        String formatoDetectado) {
}
