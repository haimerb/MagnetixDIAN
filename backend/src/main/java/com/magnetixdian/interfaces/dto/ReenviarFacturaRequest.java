package com.magnetixdian.interfaces.dto;

/**
 * Solicitud de reenvío manual de una factura electrónica al adquirente.
 */
public record ReenviarFacturaRequest(String destinatario) {
}
