package com.magnetixdian.domain.firma;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.stereotype.Component;

/**
 * Firmador por defecto cuando no hay certificado configurado: devuelve el XML
 * sin modificar. Permite generar CUFE, QR y PDF en ambiente de habilitación.
 */
@Component
public class FirmadorNoOp implements FirmadorXml {

    @Override
    public String firmar(String xml, EmpresaFeConfigJpa config) {
        return xml;
    }
}
