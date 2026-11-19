package com.magnetixdian.interfaces.dto;

/**
 * Solicitud de cambio de plan. {@code proveedor} es la pasarela de pago
 * (SIMULADO, WOMPI, MERCADOPAGO); por defecto SIMULADO.
 */
public record CambiarPlanRequest(String planCodigo, String proveedor) {
}
