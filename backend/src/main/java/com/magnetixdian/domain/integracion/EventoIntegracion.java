package com.magnetixdian.domain.integracion;

import java.util.Map;

public record EventoIntegracion(
        String idExterno,
        String tipo,
        String payload,
        Map<String, String> headers) {
}
