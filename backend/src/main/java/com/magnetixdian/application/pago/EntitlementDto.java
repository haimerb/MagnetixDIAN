package com.magnetixdian.application.pago;

import java.time.LocalDate;

public record EntitlementDto(
        Long empresaId,
        String planCode,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaExpiracion,
        String proveedor,
        String referencia) {
}
