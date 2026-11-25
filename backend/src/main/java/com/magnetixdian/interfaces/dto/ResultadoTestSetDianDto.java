package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ResultadoTestSetDianDto(
        String testSetId,
        String estado,
        int total,
        int aprobados,
        int rechazados,
        List<DocumentoTestSetDianDto> documentos) {
}
