package com.magnetixdian.domain.transmision;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;

/**
 * Contrato de transmisión de documentos electrónicos.
 * Implementaciones: PAC (REST), DIAN directo (SOAP) y simulada.
 */
public interface CanalTransmision {

    String nombre();

    ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config);
}
