package com.magnetixdian.interfaces.dto;

import java.util.List;

/**
 * Resultado de la emisión de un documento electrónico.
 */
public record ResultadoEmisionDto(
        boolean exitoso,
        Long id,
        String numeroCompleto,
        String cufe,
        String estado,
        String qrUrl,
        String mensaje,
        List<ErrorValidacionDto> errores) {
}
