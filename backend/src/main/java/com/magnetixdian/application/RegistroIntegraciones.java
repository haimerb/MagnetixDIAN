package com.magnetixdian.application;

import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.CapacidadIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RegistroIntegraciones {

    private final Map<ProveedorIntegracion, AdaptadorIntegracion> adaptadores = new EnumMap<>(ProveedorIntegracion.class);

    public RegistroIntegraciones(List<AdaptadorIntegracion> adaptadores) {
        for (AdaptadorIntegracion adaptador : adaptadores) {
            this.adaptadores.put(adaptador.proveedor(), adaptador);
        }
    }

    public AdaptadorIntegracion obtener(ProveedorIntegracion proveedor) {
        AdaptadorIntegracion adaptador = adaptadores.get(proveedor);
        if (adaptador == null) {
            throw new IllegalArgumentException("Integración no disponible: " + proveedor);
        }
        return adaptador;
    }

    public boolean existe(ProveedorIntegracion proveedor) {
        return adaptadores.containsKey(proveedor);
    }

    public List<ProveedorIntegracion> proveedores() {
        return Arrays.stream(ProveedorIntegracion.values())
                .filter(adaptadores::containsKey)
                .toList();
    }

    public List<CapacidadIntegracion> capacidades(ProveedorIntegracion proveedor) {
        AdaptadorIntegracion adaptador = adaptadores.get(proveedor);
        return adaptador == null ? List.of() : List.copyOf(adaptador.capacidades());
    }
}
