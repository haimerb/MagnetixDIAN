package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ResultadoImportacionUniversalDto(
        String archivoId,
        String estado,
        int filasLeidas,
        int filasImportadas,
        int filasOmitidas,
        int errores,
        List<String> mensajes) {
}
