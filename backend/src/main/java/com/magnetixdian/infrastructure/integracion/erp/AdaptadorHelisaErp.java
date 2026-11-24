package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.application.erp.ServicioIntegracionesErp;
import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdaptadorHelisaErp extends AdaptadorErpBase {
    public AdaptadorHelisaErp(ServicioIntegracionesErp servicioIntegraciones) { super(servicioIntegraciones); }
    @Override public ProveedorErp proveedor() { return ProveedorErp.HELISA; }
    @Override protected Set<String> credencialesRequeridas() { return Set.of("apiKey"); }
}
