package com.magnetixdian.application;

import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.transmision.CanalSimulado;
import com.magnetixdian.infrastructure.transmision.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.transmision.pac.ClientePac;
import org.springframework.stereotype.Service;

/**
 * Selecciona el canal de transmisión (PAC, DIAN directo o simulado) según la
 * configuración de la empresa.
 */
@Service
public class ServicioTransmision {

    private final ClientePac canalPac;
    private final ClienteDianSoap canalDian;
    private final CanalSimulado canalSimulado;

    public ServicioTransmision(ClientePac canalPac, ClienteDianSoap canalDian, CanalSimulado canalSimulado) {
        this.canalPac = canalPac;
        this.canalDian = canalDian;
        this.canalSimulado = canalSimulado;
    }

    public ResultadoTransmision enviar(String xmlFirmado, String cufe, FacturaDatos datos, EmpresaFeConfigJpa config) {
        CanalTransmision canal = seleccionar(config);
        return canal.enviar(xmlFirmado, cufe, datos, config);
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
