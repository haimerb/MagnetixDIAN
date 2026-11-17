package com.magnetixdian.domain.factura;

/**
 * Datos del adquirente requeridos en el UBL.
 */
public record DatosAdquirente(
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
