package com.magnetixdian.infrastructure.integracion.erp;

import com.magnetixdian.application.erp.ServicioIntegracionesErp;
import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import com.magnetixdian.domain.erp.modelo.ResultadoSincronizacionErp;
import com.magnetixdian.domain.erp.puerto.AdaptadorErp;

import java.util.Map;
import java.util.Set;

public abstract class AdaptadorErpBase implements AdaptadorErp {

    private final ServicioIntegracionesErp servicioIntegraciones;

    protected AdaptadorErpBase(ServicioIntegracionesErp servicioIntegraciones) {
        this.servicioIntegraciones = servicioIntegraciones;
    }

    @Override
    public ResultadoSincronizacionErp sincronizar(Long empresaId, String tipo) {
        String tipoSeguro = (tipo == null || tipo.isBlank()) ? "GENERAL" : tipo.trim().toUpperCase();
        Map<String, String> credenciales = servicioIntegraciones.credenciales(empresaId, proveedor());
        if (credenciales.isEmpty()) {
            return ResultadoSincronizacionErp.ok(proveedor().name(), tipoSeguro,
                    "Sincronización simulada: proveedor sin credenciales configuradas", "SIM-" + proveedor().name() + "-" + empresaId);
        }
        Set<String> requeridas = credencialesRequeridas();
        for (String clave : requeridas) {
            String valor = credenciales.get(clave);
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
