package com.magnetixdian.interfaces.dto;

import java.util.Map;

public record IntegracionSincronizacionRequest(
        String tipo,
        Map<String, Object> datos) {
}
