package com.magnetixdian.domain.erp.evento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CobroClienteEvent(
        Long empresaId,
        Long cobroId,
        Long pedidoId,
        Long cuentaBancariaId,
        String clienteId,
        String clienteNombre,
        LocalDate fecha,
        BigDecimal valor,
        String referencia,
        OffsetDateTime ocurridoEn) implements EventoDominioErp {
    @Override
    public String tipo() {
        return "COBRO_CLIENTE";
    }
}
