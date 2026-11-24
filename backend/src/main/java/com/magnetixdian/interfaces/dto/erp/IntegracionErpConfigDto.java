package com.magnetixdian.interfaces.dto.erp;
import java.time.OffsetDateTime;
import java.util.List;
public record IntegracionErpConfigDto(Long id, Long empresaId, String proveedor, String tipo, boolean activo, String estado, boolean configurada, String ultimoError, OffsetDateTime ultimaSincronizacion, List<String> credencialesConfiguradas) {}
