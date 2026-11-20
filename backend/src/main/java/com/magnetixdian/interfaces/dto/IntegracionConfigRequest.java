package com.magnetixdian.interfaces.dto;

import java.util.Map;

public record IntegracionConfigRequest(
        boolean activo,
        Map<String, String> credenciales,
        Map<String, String> configuracion) {
}
