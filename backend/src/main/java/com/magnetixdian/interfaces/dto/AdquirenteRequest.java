package com.magnetixdian.interfaces.dto;

/**
 * Datos del adquirente enviados al emitir un documento electrónico.
 */
public record AdquirenteRequest(
        String tipoDocumento,
        String numeroDocumento,
        Integer digitoVerificacion,
        String nombre,
        String email,
        String direccion,
        String municipio,
        String regimen,
        String responsabilidadFiscal) {
}
