package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.integracion.CapacidadIntegracion;

import java.time.OffsetDateTime;
import java.util.List;

public record IntegracionConfigDto(
        Long id,
        String proveedor,
        String slug,
        boolean activo,
        String estado,
        boolean configurada,
        List<CapacidadIntegracion> capacidades,
        String ultimoError,
        OffsetDateTime ultimaSincronizacion,
        String webhookToken) {
}
