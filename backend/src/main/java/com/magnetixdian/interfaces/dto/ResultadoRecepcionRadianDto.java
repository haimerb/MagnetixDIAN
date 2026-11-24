package com.magnetixdian.interfaces.dto;

public record ResultadoRecepcionRadianDto(
        Long documentoId,
        String tipoDocumento,
        String cufeCude,
        boolean duplicado,
        boolean causado,
        String mensaje) {
}
