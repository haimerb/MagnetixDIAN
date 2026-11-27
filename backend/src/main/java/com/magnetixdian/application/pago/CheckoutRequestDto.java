package com.magnetixdian.application.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.magnetixdian.domain.pago.MetodoPago;

public record CheckoutRequestDto(
        @JsonAlias("plan_codigo") String planCode,
        @JsonAlias("empresa_id") Long empresaId,
        String correo,
        String proveedor,
        @JsonAlias("method") MetodoPago metodo,
        String celular,
        @JsonAlias("financial_institution_id") String financialInstitutionId,
        @JsonAlias("payment_type") String paymentType,
        @JsonAlias("document_type") String documentType,
        @JsonAlias("document_number") String documentNumber,
        @JsonAlias("payer_name") String payerName,
        @JsonAlias("payer_email") String payerEmail,
        @JsonAlias("payer_phone") String payerPhone,
        @JsonAlias("description") String descripcion,
        @JsonAlias("accepts_terms") boolean aceptaTerminos) {

    public CheckoutRequestDto {
        if (metodo == null) {
            metodo = MetodoPago.CARD;
        }
    }

    public CheckoutRequestDto(String planCode, Long empresaId, String correo, String proveedor) {
        this(planCode, empresaId, correo, proveedor, MetodoPago.CARD, null, null, null, null, null,
                null, null, null, null, false);
    }
}
