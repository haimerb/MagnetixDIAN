package com.magnetixdian.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.magnetixdian.domain.pago.MetodoPago;

public record CheckoutRequest(
        String planCodigo,
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

    public CheckoutRequest {
        if (metodo == null) {
            metodo = MetodoPago.CARD;
        }
    }

    public CheckoutRequest(String planCodigo, String proveedor) {
        this(planCodigo, proveedor, MetodoPago.CARD, null, null, null, null, null, null, null, null, null, false);
    }
}
