package com.magnetixdian.interfaces.dto;

import java.util.List;
import java.util.Map;

public record SolicitudGeneracionExogenaDto(
        String formato,
        Integer anioGravable,
        Integer numeroEnvio,
        String fechaEnvio,
        String fechaInicial,
        String fechaFinal,
        String concepto,
        List<String> encabezados,
        List<Map<String, String>> filas
) {
}
