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
    public static final String ESTADO_PENDIENTE_GENERADOR = "PENDIENTE_GENERADOR";

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
        formatos.put("1001", activo("1001", "Pagos, abonos y retenciones", "GeneradorXml1001"));
        formatos.put("1002", activo("1002", "Créditos, descuentos, devoluciones y notas", "GeneradorXml1002"));
        CatalogoXsdOficialesDian2025.versiones().forEach((codigo, version) ->
                formatos.putIfAbsent(codigo, conXsdOficial(codigo, version)));
        formatos.putIfAbsent("1125", pendiente("1125"));
        formatos.putIfAbsent("2278", pendiente("2278"));
        formatos.putIfAbsent("5253", pendiente("5253"));
        return Collections.unmodifiableMap(formatos);
    }

    private static FormatoExogena activo(String codigo, String descripcion, String generadorXml) {
        return new FormatoExogena(
                codigo,
                descripcion,
                ESTADO_ACTIVO,
                true,
                "10",
                "xsd/medio-magnetico-" + codigo + ".xsd",
                "clasica",
                generadorXml,
                ENCABEZADOS_LEGACY
        );
    }

    private static FormatoExogena conXsdOficial(String codigo, String version) {
        return new FormatoExogena(
                codigo,
                "Formato DIAN " + codigo + (version == null ? "" : " v" + version),
                ESTADO_PENDIENTE_GENERADOR,
                false,
                version,
                "xsd/dian/2025/" + codigo + ".xsd",
                null,
                null,
                List.of()
        );
    }

    private static FormatoExogena pendiente(String codigo) {
        return new FormatoExogena(
                codigo,
                "Formato " + codigo,
                ESTADO_PENDIENTE_XSD,
                false,
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
