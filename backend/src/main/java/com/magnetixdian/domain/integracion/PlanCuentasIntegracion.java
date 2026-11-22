package com.magnetixdian.domain.integracion;

import java.util.LinkedHashMap;
import java.util.Map;

public record PlanCuentasIntegracion(
        String externalId,
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        String cuentaPadreCodigo,
        Map<String, String> datos) {

    public PlanCuentasIntegracion {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    public Map<String, String> aRegistro() {
        Map<String, String> registro = new LinkedHashMap<>();
        registro.put("externalId", externalId == null ? codigo : externalId);
        registro.put("codigo", codigo == null ? "" : codigo);
        registro.put("nombre", nombre == null ? "" : nombre);
        registro.put("tipo", tipo == null ? "" : tipo);
        registro.put("naturaleza", naturaleza == null ? "" : naturaleza);
        registro.put("cuentaPadreCodigo", cuentaPadreCodigo == null ? "" : cuentaPadreCodigo);
        registro.putAll(datos);
        return registro;
    }
}
