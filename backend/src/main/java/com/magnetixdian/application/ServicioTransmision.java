package com.magnetixdian.application;

import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.transmision.CanalSimulado;
import com.magnetixdian.infrastructure.transmision.pac.ClientePac;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Selecciona el canal de transmisión (PAC, DIAN directo o simulado) según la
 * configuración de la empresa.
 */
@Service
public class ServicioTransmision {

    private final ClientePac canalPac;
    private final com.magnetixdian.infrastructure.dian.ClienteDianSoap canalDian;
    private final CanalSimulado canalSimulado;
    private final AuditoriaService auditoria;

    public ServicioTransmision(ClientePac canalPac,
                               @Qualifier("clienteDianSoapMotor") com.magnetixdian.infrastructure.dian.ClienteDianSoap canalDian,
                               CanalSimulado canalSimulado, AuditoriaService auditoria) {
        this.canalPac = canalPac;
        this.canalDian = canalDian;
        this.canalSimulado = canalSimulado;
        this.auditoria = auditoria;
    }

    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        CanalTransmision canal = seleccionar(config);
        ResultadoTransmision resultado = canal.enviar(xmlFirmado, cufe, datos, config);
        if (!resultado.exitoso()) {
            Long empresaId = config == null || config.getEmpresa() == null ? null : config.getEmpresa().getId();
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "EMISION_RECHAZADA",
                    "factura", cufe, "canal=" + canal.nombre() + ", motivo=" + resultado.mensaje());
        }
        return resultado;
    }

    private CanalTransmision seleccionar(EmpresaFeConfigJpa config) {
        String c = config == null || config.getCanal() == null ? "SIMULADO" : config.getCanal().toUpperCase();
        return switch (c) {
            case "PAC" -> canalPac;
            case "DIAN" -> canalDian;
            default -> canalSimulado;
        };
    }
}
