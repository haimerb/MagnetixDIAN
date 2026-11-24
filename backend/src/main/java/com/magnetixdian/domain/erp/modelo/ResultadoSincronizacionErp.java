package com.magnetixdian.domain.erp.modelo;

import java.time.OffsetDateTime;
import java.util.Map;

public record ResultadoSincronizacionErp(
        boolean exitoso,
        String proveedor,
        String tipo,
        String mensaje,
        String idExterno,
        OffsetDateTime fecha,
        Map<String, String> detalles) {
    public static ResultadoSincronizacionErp ok(String proveedor, String tipo, String mensaje, String idExterno) {
        return new ResultadoSincronizacionErp(true, proveedor, tipo, mensaje, idExterno, OffsetDateTime.now(), Map.of());
    }

    public static ResultadoSincronizacionErp fallo(String proveedor, String tipo, String mensaje) {
        return new ResultadoSincronizacionErp(false, proveedor, tipo, mensaje, null, OffsetDateTime.now(), Map.of());
    }
}
