package com.magnetixdian.domain.pago;

import java.math.BigDecimal;

public record SolicitudPago(
        Long empresaId,
        Long pagoId,
        String referencia,
        String planCodigo,
        BigDecimal monto,
        String moneda,
        String descripcion,
        String urlRetorno,
        MetodoPago metodo,
        String celular,
        String financialInstitutionId,
        String paymentType,
        String documentType,
        String documentNumber,
        String payerName,
        String payerEmail,
        String payerPhone,
        String descripcionPago) {

    public SolicitudPago {
        if (metodo == null) {
            metodo = MetodoPago.CARD;
        }
    }

    public SolicitudPago(Long empresaId, Long pagoId, String referencia, String planCodigo,
                         BigDecimal monto, String moneda, String descripcion, String urlRetorno) {
        this(empresaId, pagoId, referencia, planCodigo, monto, moneda, descripcion, urlRetorno,
                MetodoPago.CARD, null, null, null, null, null, null, null, null, null);
    }

    public SolicitudPago(Long empresaId, Long pagoId, String referencia, String planCodigo,
                         BigDecimal monto, String moneda, String descripcion, String urlRetorno,
                         MetodoPago metodo, String celular) {
        this(empresaId, pagoId, referencia, planCodigo, monto, moneda, descripcion, urlRetorno,
                metodo, celular, null, null, null, null, null, null, null, null);
    }
}
