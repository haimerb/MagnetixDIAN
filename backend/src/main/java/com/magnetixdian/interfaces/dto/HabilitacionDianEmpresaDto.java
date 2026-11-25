package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record HabilitacionDianEmpresaDto(
        Long empresaId,
        String modo,
        String estado,
        String resolucionNumero,
        LocalDate resolucionFecha,
        String prefijo,
        Integer rangoDesde,
        Integer rangoHasta,
        String testSetId,
        String softwareId,
        boolean notificarAdquirente,
        String certificadoNombre,
        OffsetDateTime aprobadoEn,
        OffsetDateTime switchoverEn,
        List<AuditoriaDianDto> auditoria) {
}
