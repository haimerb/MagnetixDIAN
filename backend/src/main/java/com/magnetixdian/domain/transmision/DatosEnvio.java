package com.magnetixdian.domain.transmision;

import com.magnetixdian.domain.factura.FacturaDatos;

/**
 * Datos mínimos necesarios para (re)enviar un documento a un canal de
 * transmisión sin reconstruir la factura completa.
 */
public record DatosEnvio(
        String numeroCompleto,
        String prefijo,
        long numero,
        int ambiente
) {

    public static DatosEnvio de(FacturaDatos datos) {
        return new DatosEnvio(datos.numeroCompleto(), datos.prefijo(), datos.numero(), datos.ambiente());
    }
}
