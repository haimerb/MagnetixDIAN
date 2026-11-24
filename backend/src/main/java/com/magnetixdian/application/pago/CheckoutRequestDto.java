package com.magnetixdian.application.pago;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CheckoutRequestDto(
        @JsonAlias("plan_codigo") String planCode,
        @JsonAlias("empresa_id") Long empresaId,
        String correo,
        String proveedor) {
}
