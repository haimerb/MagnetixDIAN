package com.magnetixdian.application;

import com.magnetixdian.domain.firma.FirmadorNoOp;
import com.magnetixdian.domain.firma.FirmadorXades;
import com.magnetixdian.domain.firma.FirmadorXml;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.stereotype.Service;

/**
 * Selecciona el firmador adecuado según la configuración de la empresa:
 * XAdES si hay certificado, No-Op en caso contrario.
 */
@Service
public class ServicioFirma {

    private final FirmadorNoOp firmadorNoOp = new FirmadorNoOp();

    public String firmar(String xml, EmpresaFeConfigJpa config) {
        boolean hayCertificado = config != null
                && config.getCertificadoPath() != null
                && !config.getCertificadoPath().isBlank();
        FirmadorXml firmador = hayCertificado ? new FirmadorXades() : firmadorNoOp;
        return firmador.firmar(xml, config);
    }

    public boolean hayCertificado(EmpresaFeConfigJpa config) {
        return config != null && config.getCertificadoPath() != null && !config.getCertificadoPath().isBlank();
    }
}
