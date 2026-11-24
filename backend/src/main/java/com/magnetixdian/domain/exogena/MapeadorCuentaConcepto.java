package com.magnetixdian.domain.exogena;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MapeadorCuentaConcepto {

    public Optional<Long> mapear(String cuentaCodigo, Map<String, Long> mapaCuentas,
                                 Map<String, Long> conceptosPorCodigo) {
        if (cuentaCodigo == null || cuentaCodigo.isBlank() || mapaCuentas == null || conceptosPorCodigo == null) {
            return Optional.empty();
        }
        String codigo = cuentaCodigo.trim();
        Long exacto = mapaCuentas.get(codigo);
        if (exacto != null && conceptosPorCodigo.containsValue(exacto)) {
            return Optional.of(exacto);
        }
        String mejorPrefijo = null;
        for (String prefijo : mapaCuentas.keySet()) {
            if (prefijo != null && codigo.startsWith(prefijo)
                    && (mejorPrefijo == null || prefijo.length() > mejorPrefijo.length())) {
                mejorPrefijo = prefijo;
            }
        }
        if (mejorPrefijo == null) {
            return Optional.empty();
        }
        Long conceptoId = mapaCuentas.get(mejorPrefijo);
        return conceptosPorCodigo.containsValue(conceptoId) ? Optional.of(conceptoId) : Optional.empty();
    }
}
