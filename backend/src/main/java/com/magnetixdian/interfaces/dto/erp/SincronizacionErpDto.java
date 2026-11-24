package com.magnetixdian.interfaces.dto.erp;
import java.time.OffsetDateTime;
import java.util.Map;
public record SincronizacionErpDto(boolean exitoso, String proveedor, String tipo, String mensaje, String idExterno, OffsetDateTime fecha, Map<String, String> detalles) {}
