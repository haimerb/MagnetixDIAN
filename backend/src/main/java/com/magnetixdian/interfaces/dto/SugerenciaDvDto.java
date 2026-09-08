package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

/**
 * Sugerencia de corrección del dígito de verificación de un NIT reportado.
 */
public record SugerenciaDvDto(
        Long operacionId,
        Integer linea,
        String tipoDocumento,
        String numero,
        Integer dvActual,
        Integer dvEsperado,
        String concepto,
        BigDecimal valorPago
) {
}