package com.magnetixdian.domain.erp.evento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PedidoAprobadoEvent(
        Long empresaId,
        Long pedidoId,
        String numero,
        String clienteId,
        String clienteNombre,
        LocalDate fecha,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        OffsetDateTime ocurridoEn) implements EventoDominioErp {
    @Override
    public String tipo() {
        return "PEDIDO_APROBADO";
    }
}
