package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ResultadoTraerDianDto(
        String origen,
        String archivo,
        int documentos,
        String mensaje,
        List<DocumentoRadianDto> registros) {
}
