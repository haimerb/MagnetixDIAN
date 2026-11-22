package com.magnetixdian.domain.integracion;

import java.util.List;
import java.util.Map;

public record SolicitudMigracionContable(
        List<String> etapas,
        String periodo,
        Map<String, String> opciones) {

    public SolicitudMigracionContable {
        etapas = etapas == null ? List.of() : List.copyOf(etapas);
        opciones = opciones == null ? Map.of() : Map.copyOf(opciones);
    }
}
