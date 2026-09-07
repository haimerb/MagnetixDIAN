package com.magnetixdian.domain.model;

import java.math.BigDecimal;

/**
 * Registro de una operación del formato 1001 (pago/abono en cuenta + retenciones).
 * Representa una fila cargada desde Excel o ingresada manualmente.
 */
public record OperacionDatos(
        Integer linea,
        String tipoDocumento,
        String numeroIdentificacion,
        Integer dv,
        String concepto,
        BigDecimal valorPago,
        BigDecimal retencionRenta,
        BigDecimal retencionIva,
        BigDecimal retencionIca,
        BigDecimal retencionTimbre,
        BigDecimal ivaPagado,
        BigDecimal ivaDescontable,
        BigDecimal valorGasto,
        BigDecimal valorCosto,
        BigDecimal valorNc,
        boolean cuantiaMenor
) {}