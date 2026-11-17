package com.magnetixdian.domain.factura;

/**
 * Datos del emisor (obligado a facturar) requeridos en el UBL.
 */
public record DatosEmisor(
        String tipoDocumento,
        String numeroIdentificacion,
        Integer digitoVerificacion,
        String razonSocial,
        String regimen,
        String responsabilidadFiscal,
        String direccion,
        String municipio,
        String departamento,
        String email,
        boolean granContribuyente) {
}
