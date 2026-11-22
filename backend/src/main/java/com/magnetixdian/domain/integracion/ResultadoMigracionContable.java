package com.magnetixdian.domain.integracion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ResultadoMigracionContable(
        boolean exitoso,
        String mensaje,
        String etapa,
        List<Map<String, String>> items,
        int exitosos,
        int omitidos,
        int errores,
        int total) {

    public ResultadoMigracionContable {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static ResultadoMigracionContable ok(String etapa, List<Map<String, String>> items) {
        List<Map<String, String>> copia = items == null ? List.of() : List.copyOf(items);
        return new ResultadoMigracionContable(true, "OK", etapa, copia, copia.size(), 0, 0, copia.size());
    }

    public static ResultadoMigracionContable fallo(String etapa, String mensaje) {
        return new ResultadoMigracionContable(false, mensaje, etapa, List.of(), 0, 0, 0, 0);
    }

    public static ResultadoMigracionContable desde(ResultadoIntegracion resultado, String etapa) {
        if (resultado == null) {
            return fallo(etapa, "El adaptador no devolvió resultado.");
        }
        return resultado.exitoso() ? ok(etapa, List.of()) : fallo(etapa, resultado.mensaje());
    }

    public static ResultadoMigracionContable noSoportado(ProveedorIntegracion proveedor,
                                                        CapacidadIntegracion capacidad,
                                                        String etapa) {
        return desde(ResultadoIntegracion.noSoportado(proveedor, capacidad), etapa);
    }

    public List<Map<String, String>> itemsSeguros() {
        return items == null ? new ArrayList<>() : items;
    }
}
