package com.magnetixdian.infrastructure.transmision;

import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.stereotype.Component;

/**
 * Canal simulado para ambiente de habilitación/pruebas: acepta el documento sin
 * contactar a la DIAN ni al PAC. Útil para validar el flujo completo.
 */
@Component
public class CanalSimulado implements CanalTransmision {

    @Override
    public String nombre() {
        return "SIMULADO";
    }

    @Override
    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        String respuesta = "{\"canal\":\"SIMULADO\",\"resultado\":\"ACEPTADO\",\"cufe\":\"" + cufe
                + "\",\"numero\":\"" + datos.numeroCompleto() + "\"}";
        return ResultadoTransmision.ok(cufe,
                "Documento aceptado (canal simulado, ambiente " + datos.ambiente() + ")", respuesta);
    }
}
