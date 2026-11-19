package com.magnetixdian.interfaces.dto;

/**
 * Respuesta neutra para no revelar si el usuario existe. El enlace solo se
 * incluye cuando la entrega de correo está deshabilitada y el entorno lo permite.
 */
public record SolicitudResetResponse(String mensaje, String enlace) {
}
