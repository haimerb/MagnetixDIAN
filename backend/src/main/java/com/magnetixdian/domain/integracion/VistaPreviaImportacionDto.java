package com.magnetixdian.domain.integracion;

import java.util.List;
import java.util.Map;

public record VistaPreviaImportacionDto(
        String archivoId,
        String nombreArchivo,
        String formatoDetectado,
        List<String> cabeceras,
        List<Map<String, String>> filasEjemplo,
        List<SugerenciaMapeoDto> sugerencias,
        int filasLeidas,
        double confianza,
        String contenidoBase64
) {
    public VistaPreviaImportacionDto {
        archivoId = archivoId == null ? "" : archivoId;
        nombreArchivo = nombreArchivo == null ? "" : nombreArchivo;
        formatoDetectado = formatoDetectado == null || formatoDetectado.isBlank()
                ? "GENERICO" : formatoDetectado;
        cabeceras = cabeceras == null ? List.of() : List.copyOf(cabeceras);
        filasEjemplo = filasEjemplo == null ? List.of() : List.copyOf(filasEjemplo);
        sugerencias = sugerencias == null ? List.of() : List.copyOf(sugerencias);
        contenidoBase64 = contenidoBase64 == null ? "" : contenidoBase64;
    }

    public boolean tieneSugerencias() {
        return !sugerencias.isEmpty();
    }
}
