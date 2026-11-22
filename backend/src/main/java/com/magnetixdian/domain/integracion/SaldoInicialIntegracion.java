package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record SaldoInicialIntegracion(
        String externalId,
        String cuentaCodigo,
        BigDecimal debito,
        BigDecimal credito,
        String periodo,
        String moneda,
        Map<String, String> datos) {

    public SaldoInicialIntegracion {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    public Map<String, String> aRegistro() {
        Map<String, String> registro = new LinkedHashMap<>();
        registro.put("externalId", externalId == null ? cuentaCodigo + "|" + periodo : externalId);
        registro.put("cuentaCodigo", cuentaCodigo == null ? "" : cuentaCodigo);
        registro.put("debito", debito == null ? "0" : debito.toPlainString());
        registro.put("credito", credito == null ? "0" : credito.toPlainString());
        registro.put("periodo", periodo == null ? "" : periodo);
        registro.put("moneda", moneda == null ? "COP" : moneda);
        registro.putAll(datos);
        return registro;
    }
}
