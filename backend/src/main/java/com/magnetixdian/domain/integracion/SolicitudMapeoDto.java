package com.magnetixdian.domain.integracion;

import java.util.List;
import java.util.Map;

public record SolicitudMapeoDto(
        String archivoId,
        String nombreArchivo,
        String formatoDetectado,
        String contenidoBase64,
        List<MapeoColumnaDto> mapeos
) {
    public SolicitudMapeoDto {
        archivoId = archivoId == null ? "" : archivoId;
        nombreArchivo = nombreArchivo == null ? "" : nombreArchivo;
        formatoDetectado = formatoDetectado == null || formatoDetectado.isBlank()
                ? "GENERICO" : formatoDetectado;
        contenidoBase64 = contenidoBase64 == null ? "" : contenidoBase64;
        mapeos = mapeos == null ? List.of() : List.copyOf(mapeos);
    }

    public Map<String, Integer> aIndicePorCampo() {
        return mapeos.stream()
                .filter(m -> m.indice() >= 0)
                .collect(java.util.stream.Collectors.toMap(
                        MapeoColumnaDto::campo,
                        MapeoColumnaDto::indice,
                        (primero, segundo) -> primero));
    }
}
