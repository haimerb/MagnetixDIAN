package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SuscripcionDto(
        Long id,
        Long empresaId,
        PlanDto plan,
        String estado,
        String proveedor,
        String referenciaExterna,
        LocalDate periodoInicio,
        LocalDate periodoFin,
        UsoDto uso) {

    public record UsoDto(
            int facturasMes,
            Integer facturasRestantes,
            boolean facturasExcedidas,
            int usuarios,
            Integer usuariosRestantes,
            boolean usuariosExcedidos) {
    }
}
