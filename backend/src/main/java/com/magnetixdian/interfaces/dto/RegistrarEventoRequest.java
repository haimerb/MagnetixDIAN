package com.magnetixdian.interfaces.dto;

/**
 * Solicitud para registrar un evento DIAN (030–034) sobre una factura.
 */
public record RegistrarEventoRequest(String tipoEvento) {
}
