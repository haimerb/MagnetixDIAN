package com.magnetixdian.interfaces.dto;

import java.util.List;
import java.util.Map;

public record FormatoDetectadoDto(
        String codigo,
        String hoja,
        String estado,
        boolean activo,
        int filas,
        List<String> encabezados,
        List<Map<String, String>> datos,
        Long medioMagneticoId
) {
}
