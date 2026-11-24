package com.magnetixdian.interfaces.dto.erp;
import java.util.Map;
public record IntegracionErpConfigRequestDto(Boolean activo, Map<String, String> credenciales, Map<String, String> configuracion) {}
