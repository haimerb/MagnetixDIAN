package com.magnetixdian.domain.erp.evento;

import java.time.OffsetDateTime;

public interface EventoDominioErp {
    Long empresaId();
    String tipo();
    OffsetDateTime ocurridoEn();
}
