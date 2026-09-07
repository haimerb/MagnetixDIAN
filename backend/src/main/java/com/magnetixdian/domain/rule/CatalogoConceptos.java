package com.magnetixdian.domain.rule;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Catálogo de conceptos del formato 1001 (pagos/abonos en cuenta y
 * retenciones) para la vigencia Resolución 000227/2025.
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

    public Map<String, String> conceptosFormato1001() {
        return CONCEPTOS_1001;
    }
}