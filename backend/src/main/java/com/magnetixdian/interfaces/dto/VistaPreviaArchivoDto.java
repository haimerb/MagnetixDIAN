package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.integracion.MapeoColumnaDto;

import java.util.List;

public record VistaPreviaArchivoDto(
        String archivoId,
        String nombreArchivo,
        String formatoDetectado,
        List<String> cabeceras,
        List<List<String>> filasEjemplo,
        List<MapeoColumnaDto> sugerencias,
        double confianza,
        int totalFilas,
        String contenidoBase64) {
}
