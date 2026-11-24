package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.application.erp.ServicioIntegracionesErp;
import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdaptadorWorldOfficeErp extends AdaptadorErpBase {
    public AdaptadorWorldOfficeErp(ServicioIntegracionesErp servicioIntegraciones) { super(servicioIntegraciones); }
    @Override public ProveedorErp proveedor() { return ProveedorErp.WORLD_OFFICE; }
    @Override protected Set<String> credencialesRequeridas() { return Set.of("apiKey"); }
}
