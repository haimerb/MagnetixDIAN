package com.magnetixdian.domain.integracion;

import java.util.Map;

public record ResultadoIntegracion(
        boolean exitoso,
        String mensaje,
        String idExterno,
        String estado,
        Map<String, String> detalles) {

    public static ResultadoIntegracion ok(String mensaje, String idExterno) {
        return new ResultadoIntegracion(true, mensaje, idExterno, "EXITOSO", Map.of());
    }

    public static ResultadoIntegracion fallo(String mensaje) {
        return new ResultadoIntegracion(false, mensaje, null, "ERROR", Map.of());
    }

    public static ResultadoIntegracion noSoportado(ProveedorIntegracion proveedor, CapacidadIntegracion capacidad) {
        return fallo(proveedor + " no soporta " + capacidad);
    }
}
