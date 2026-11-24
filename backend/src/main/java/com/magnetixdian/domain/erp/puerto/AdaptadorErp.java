package com.magnetixdian.domain.erp.puerto;

import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import com.magnetixdian.domain.erp.modelo.ResultadoSincronizacionErp;

public interface AdaptadorErp {
    ProveedorErp proveedor();
    ResultadoSincronizacionErp sincronizar(Long empresaId, String tipo);
}
