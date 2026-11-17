package com.magnetixdian.domain.factura;

import java.math.BigDecimal;

/**
 * Línea de detalle de un documento electrónico.
 */
public record LineaFactura(
        int numero,
        String codigoProducto,
        String descripcion,
        BigDecimal cantidad,
        String unidadMedida,
        BigDecimal valorUnitario,
        BigDecimal valorDescuento,
        BigDecimal ivaPct,
        BigDecimal ivaValor,
        BigDecimal incPct,
        BigDecimal incValor,
        BigDecimal totalLinea,
        String codigoUnspsc) {

    public static LineaFactura of(int numero, String descripcion, BigDecimal cantidad,
                                  BigDecimal valorUnitario, BigDecimal ivaPct, BigDecimal incPct,
                                  String unidadMedida, String codigoUnspsc) {
        BigDecimal cant = nz(cantidad, BigDecimal.ONE);
        BigDecimal vu = nz(valorUnitario, BigDecimal.ZERO);
        BigDecimal bruto = cant.multiply(vu).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal iva = bruto.multiply(nz(ivaPct, BigDecimal.ZERO))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal inc = bruto.multiply(nz(incPct, BigDecimal.ZERO))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = bruto.add(iva).add(inc).setScale(2, java.math.RoundingMode.HALF_UP);
        return new LineaFactura(numero, null, descripcion, cant,
                unidadMedida == null ? "94" : unidadMedida, vu, BigDecimal.ZERO,
                nz(ivaPct, BigDecimal.ZERO), iva, nz(incPct, BigDecimal.ZERO), inc, total, codigoUnspsc);
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }
}
