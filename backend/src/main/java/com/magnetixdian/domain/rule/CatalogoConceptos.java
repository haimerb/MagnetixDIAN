package com.magnetixdian.domain.rule;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Catálogo de conceptos de los formatos de información exógena soportados.
 * Formato 1001: pagos/abonos en cuenta y retenciones.
 * Formato 1002: créditos, descuentos, devoluciones y notas de ajuste.
 *
 * <p>Lista representativa — el catálogo completo se puede versionar por año
 * gravable en BD. Incluye los conceptos de uso común en la práctica.</p>
 */
@Component
public class CatalogoConceptos {

    private static final Map<String, String> CONCEPTOS_1001 = Map.ofEntries(
            Map.entry("2004", "Pagos o abonos en cuenta por servicios"),
            Map.entry("2005", "Pagos o abonos por honorarios"),
            Map.entry("2007", "Pagos o abonos por comisiones"),
            Map.entry("2008", "Pagos o abonos por arrendamiento"),
            Map.entry("2009", "Pagos o abonos por intereses"),
            Map.entry("2012", "Pagos o abonos por compras"),
            Map.entry("2018", "Pagos o abonos por salarios"),
            Map.entry("2025", "Pagos o abonos de dividendos"),
            Map.entry("2036", "Pagos o abonos a proveedores del exterior"),
            Map.entry("2050", "Pagos o abonos por servicios de salud"),
            Map.entry("2071", "Pagos o abonos por regalías"),
            Map.entry("2073", "Pagos o abonos por contratos de construcción"),
            Map.entry("2080", "Pagos o abonos por prestaciones sociales"),
            Map.entry("2086", "Pagos o abonos por servicios públicos"),
            Map.entry("2090", "Pagos o abonos por servicios de vigilancia"),
            Map.entry("2091", "Pagos o abonos por servicios técnicos")
    );

    private static final Map<String, String> CONCEPTOS_1002 = Map.ofEntries(
            Map.entry("7001", "Devoluciones en ventas"),
            Map.entry("7002", "Descuentos otorgados"),
            Map.entry("7003", "Rebajas hechas"),
            Map.entry("7004", "Notas crédito por ajustes de servicios"),
            Map.entry("7005", "Devoluciones de compras"),
            Map.entry("7006", "Descuentos recibidos"),
            Map.entry("7007", "Notas débito de compensación"),
            Map.entry("7008", "Ajustes por diferencias de cambio")
    );

    public Map<String, String> conceptosFormato1001() {
        return CONCEPTOS_1001;
    }

    public Map<String, String> conceptosFormato1002() {
        return CONCEPTOS_1002;
    }

    /**
     * Retorna el catálogo de conceptos vigente para el formato dado.
     */
    public Map<String, String> conceptosParaFormato(String formato) {
        if ("1002".equalsIgnoreCase(formato)) {
            return CONCEPTOS_1002;
        }
        return CONCEPTOS_1001;
    }
}