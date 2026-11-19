package com.magnetixdian.interfaces.dto;

/**
 * Respuesta de creación: la {@code clave} en claro solo se entrega una vez.
 */
public record ApiKeyCreadaDto(
        Long id,
        String nombre,
        String prefijo,
        String clave) {
}
