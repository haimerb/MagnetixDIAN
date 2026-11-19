package com.magnetixdian.application;

import com.magnetixdian.domain.pago.PasarelaPago;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro de pasarelas de pago disponibles, indexadas por proveedor.
 */
@Component
public class PasarelasPago {

    private final Map<String, PasarelaPago> porProveedor;

    public PasarelasPago(List<PasarelaPago> pasarelas) {
        this.porProveedor = pasarelas.stream()
                .collect(Collectors.toMap(p -> p.proveedor().toUpperCase(Locale.ROOT), Function.identity()));
    }

    public PasarelaPago obtener(String proveedor) {
        String clave = proveedor == null || proveedor.isBlank()
                ? "SIMULADO" : proveedor.trim().toUpperCase(Locale.ROOT);
        PasarelaPago pasarela = porProveedor.get(clave);
        if (pasarela == null) {
            throw new IllegalArgumentException("Pasarela no soportada: " + proveedor);
        }
        return pasarela;
    }

    public boolean existe(String proveedor) {
        return porProveedor.containsKey(proveedor == null ? "" : proveedor.trim().toUpperCase(Locale.ROOT));
    }
}
