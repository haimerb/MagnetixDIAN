package com.magnetixdian.domain.factura;

/**
 * Ciclo de vida de una factura electrónica.
 */
public enum EstadoFactura {
    BORRADOR,
    GENERADO,
    FIRMADO,
    ENVIADO,
    ACEPTADO,
    RECHAZADO,
    ANULADA
}
