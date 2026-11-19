package com.magnetixdian.interfaces.dto;

/**
 * Resultado de un reenvío de factura por correo.
 */
public record ReenvioFacturaDto(boolean enviado, String destinatario) {
}
