package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdaptadorAlegraErp extends AdaptadorErpBase {
    @Override public ProveedorErp proveedor() { return ProveedorErp.ALEGRA; }
    @Override protected Set<String> credencialesRequeridas() { return Set.of("email", "token"); }
}
