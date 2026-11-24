package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdaptadorSiigoErp extends AdaptadorErpBase {
    @Override public ProveedorErp proveedor() { return ProveedorErp.SIIGO; }
    @Override protected Set<String> credencialesRequeridas() { return Set.of("username", "accessKey"); }
}
