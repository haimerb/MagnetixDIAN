package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record AsientoIntegracion(
        String externalId,
        String fecha,
        String descripcion,
        String cuentaCodigo,
        BigDecimal debito,
        BigDecimal credito,
        String terceroDocumento,
        String centroCosto,
        Map<String, String> datos) {

    public AsientoIntegracion {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    public Map<String, String> aRegistro() {
        Map<String, String> registro = new LinkedHashMap<>();
        registro.put("externalId", externalId == null ? fecha + "|" + cuentaCodigo : externalId);
        registro.put("fecha", fecha == null ? "" : fecha);
        registro.put("descripcion", descripcion == null ? "" : descripcion);
        registro.put("cuentaCodigo", cuentaCodigo == null ? "" : cuentaCodigo);
        registro.put("debito", debito == null ? "0" : debito.toPlainString());
        registro.put("credito", credito == null ? "0" : credito.toPlainString());
        registro.put("terceroDocumento", terceroDocumento == null ? "" : terceroDocumento);
        registro.put("centroCosto", centroCosto == null ? "" : centroCosto);
        registro.putAll(datos);
        return registro;
    }
}
