package com.magnetixdian.domain.integracion;

public record SugerenciaMapeoDto(
        String campo,
        String cabeceraOrigen,
        int indice,
        double confianza,
        String formatoDetectado) {
}
