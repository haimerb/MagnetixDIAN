package com.magnetixdian.interfaces.dto;

/**
 * Error o advertencia de validación de un documento electrónico.
 */
public record ErrorValidacionDto(
        String codigo,
        String severidad,
        String mensaje,
        Integer linea,
        String campo,
        String valorEsperado,
        String valorEncontrado) {
}
