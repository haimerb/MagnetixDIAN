package com.magnetixdian.domain.erp.evento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DepreciacionCalculadaEvent(
        Long empresaId,
        Long activoId,
        Long depreciacionId,
        String codigoActivo,
        String periodo,
        LocalDate fecha,
        BigDecimal valor,
        BigDecimal depreciacionAcumulada,
        BigDecimal valorLibros,
        OffsetDateTime ocurridoEn) implements EventoDominioErp {
    @Override
    public String tipo() {
        return "DEPRECIACION_CALCULADA";
    }
}
