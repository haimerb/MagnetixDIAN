package com.magnetixdian.domain.exogena;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormatoExogenaRegistry {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_PENDIENTE_XSD = "PENDIENTE_XSD";

    private static final List<String> ENCABEZADOS_LEGACY = List.of(
            "TIPO", "NIT", "DV", "CONCEPTO", "VALOR_PAGO", "RET_RENTA",
            "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC",
            "VALOR_GASTO", "VALOR_COSTO", "NOTA_CREDITO"
    );

    private static final Map<String, FormatoExogena> FORMATOS = construir();

    public List<FormatoExogena> todos() {
        return FORMATOS.values().stream().toList();
    }

    public List<FormatoExogena> activos() {
        return FORMATOS.values().stream()
                .filter(FormatoExogena::activo)
                .toList();
    }

    public boolean existe(String codigo) {
        return codigo != null && FORMATOS.containsKey(codigo.trim());
    }

    public FormatoExogena obtener(String codigo) {
        String normalizado = codigo == null ? "" : codigo.trim();
        FormatoExogena formato = FORMATOS.get(normalizado);
        if (formato == null) {
            throw new IllegalArgumentException("Formato exógena no soportado: " + normalizado);
        }
        return formato;
    }

    public FormatoExogena validarActivo(String codigo) {
        FormatoExogena formato = obtener(codigo);
        if (!formato.activo()) {
            throw new IllegalArgumentException(
                    "Formato exógena no disponible: " + formato.codigo() + " (" + formato.estado() + ")");
        }
        return formato;
    }

    private static Map<String, FormatoExogena> construir() {
        Map<String, FormatoExogena> formatos = new LinkedHashMap<>();
        formatos.put("1001", activo("1001", "Pagos, abonos y retenciones"));
        formatos.put("1002", activo("1002", "Créditos, descuentos, devoluciones y notas"));
        formatos.put("1003", pendiente("1003"));
        formatos.put("1004", pendiente("1004"));
        formatos.put("1005", pendiente("1005"));
        formatos.put("1006", pendiente("1006"));
        formatos.put("1007", pendiente("1007"));
        formatos.put("1008", pendiente("1008"));
        formatos.put("1009", pendiente("1009"));
        formatos.put("1010", pendiente("1010"));
        formatos.put("1011", pendiente("1011"));
        formatos.put("1012", pendiente("1012"));
        formatos.put("1125", pendiente("1125"));
        formatos.put("1647", pendiente("1647"));
        formatos.put("2276", pendiente("2276"));
        formatos.put("2278", pendiente("2278"));
        formatos.put("5253", pendiente("5253"));
        formatos.put("1035", pendiente("1035"));
        return Collections.unmodifiableMap(formatos);
    }

    private static FormatoExogena activo(String codigo, String descripcion) {
        return new FormatoExogena(codigo, descripcion, ESTADO_ACTIVO, true, ENCABEZADOS_LEGACY);
    }

    private static FormatoExogena pendiente(String codigo) {
        return new FormatoExogena(codigo, "Formato " + codigo, ESTADO_PENDIENTE_XSD, false, List.of());
    }
}
