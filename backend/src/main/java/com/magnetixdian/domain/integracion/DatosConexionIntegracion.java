package com.magnetixdian.domain.integracion;

import java.util.Map;

public record DatosConexionIntegracion(
        Long empresaId,
        ProveedorIntegracion proveedor,
        Map<String, String> credenciales,
        Map<String, String> configuracion,
        boolean activo) {

    public String credencial(String clave) {
        return credenciales == null ? null : credenciales.get(clave);
    }

    public String opcion(String clave, String defecto) {
        if (configuracion == null) {
            return defecto;
        }
        String valor = configuracion.get(clave);
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
