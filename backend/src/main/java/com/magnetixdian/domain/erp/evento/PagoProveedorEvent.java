package com.magnetixdian.domain.erp.evento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PagoProveedorEvent(
        Long empresaId,
        Long pagoId,
        Long compraId,
        Long cuentaBancariaId,
        String proveedorId,
        String proveedorNombre,
        LocalDate fecha,
        BigDecimal valor,
        String referencia,
        OffsetDateTime ocurridoEn) implements EventoDominioErp {
    @Override
    public String tipo() {
        return "PAGO_PROVEEDOR";
    }
}
