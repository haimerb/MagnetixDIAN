package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import com.magnetixdian.domain.erp.modelo.ResultadoSincronizacionErp;
import com.magnetixdian.domain.erp.puerto.AdaptadorErp;

import java.util.Map;
import java.util.Set;

public abstract class AdaptadorErpBase implements AdaptadorErp {

    @Override
    public ResultadoSincronizacionErp sincronizar(Long empresaId, String tipo, Map<String, String> credenciales) {
        String tipoSeguro = (tipo == null || tipo.isBlank()) ? "GENERAL" : tipo.trim().toUpperCase();
        Map<String, String> cred = credenciales == null ? Map.of() : credenciales;
        if (cred.isEmpty()) {
            return ResultadoSincronizacionErp.ok(proveedor().name(), tipoSeguro,
                    "Sincronización simulada: proveedor sin credenciales configuradas", "SIM-" + proveedor().name() + "-" + empresaId);
        }
        Set<String> requeridas = credencialesRequeridas();
        for (String clave : requeridas) {
            String valor = cred.get(clave);
            if (valor == null || valor.isBlank()) {
                return ResultadoSincronizacionErp.fallo(proveedor().name(), tipoSeguro,
                        "Faltan credenciales requeridas: " + clave);
            }
        }
        return ResultadoSincronizacionErp.ok(proveedor().name(), tipoSeguro,
                "Sincronización simulada en modo configurado", "CFG-" + proveedor().name() + "-" + empresaId);
    }

    protected abstract Set<String> credencialesRequeridas();
}
