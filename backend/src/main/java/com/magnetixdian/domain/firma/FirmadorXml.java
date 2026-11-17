package com.magnetixdian.domain.firma;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;

/**
 * Contrato de firma digital de documentos electrónicos.
 * Implementaciones: {@code FirmadorNoOp} (sin certificado) y {@code FirmadorXades}
 * (XAdES-EPES con certificado .p12).
 */
public interface FirmadorXml {

    /**
     * Firma el documento XML.
     *
     * @param xml    documento UBL sin firmar
     * @param config configuración de la empresa (certificado, etc.)
     * @return XML firmado (o el mismo XML si no hay firma configurada)
     */
    String firmar(String xml, EmpresaFeConfigJpa config);
}
