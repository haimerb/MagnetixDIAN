package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

/**
 * Configuración DIAN de una empresa. Los secretos (pin, api key, contraseña del
 * certificado) no se devuelven en las lecturas; al guardar, un valor nulo los
 * conserva.
 */
public record ConfigFeDto(
        Integer ambiente,
        String softwareId,
        String softwarePin,
        String softwareSecurityCode,
        String testSetId,
        String resolucionNumero,
        LocalDate resolucionFecha,
        String prefijo,
        Long rangoDesde,
        Long rangoHasta,
        LocalDate rangoVigenciaDesde,
        LocalDate rangoVigenciaHasta,
        String canal,
        String pacProveedor,
        String pacBaseUrl,
        String pacApiKey,
        String certificadoNombre,
        String certificadoPassword,
        String certificadoPath,
        String responsabilidadFiscal,
        String codigoMunicipio,
        Boolean activo,
        Boolean tieneCertificado,
        Boolean tienePacKey,
        Boolean notificarAdquirente) {
}
