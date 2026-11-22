package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.integracion.MapeoColumnaDto;

import java.util.List;

public record ConfirmacionImportacionDto(
        String archivoId,
        String nombreArchivo,
        String formatoDetectado,
        String contenidoBase64,
        List<MapeoColumnaDto> columnas) {
}
