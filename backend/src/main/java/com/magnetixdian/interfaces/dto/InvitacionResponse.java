package com.magnetixdian.interfaces.dto;

/**
 * Resultado de una invitación. El enlace solo se expone cuando el SMTP está
 * deshabilitado y el entorno lo permite (desarrollo).
 */
public record InvitacionResponse(UsuarioDto usuario, String enlace) {
}
