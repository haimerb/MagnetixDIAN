package com.magnetixdian.domain.erp.evento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CompraRecibidaEvent(
        Long empresaId,
        Long compraId,
        Long recepcionId,
        String numeroCompra,
        String proveedorId,
        String proveedorNombre,
        String bodega,
        LocalDate fecha,
        BigDecimal total,
        OffsetDateTime ocurridoEn) implements EventoDominioErp {
    @Override
    public String tipo() {
        return "COMPRA_RECIBIDA";
    }
}
