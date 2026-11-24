package com.magnetixdian.dian;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.transmision.CanalSimulado;
import com.magnetixdian.infrastructure.transmision.pac.ClientePac;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioTransmisionAuditoriaTest {

    private final ClientePac canalPac = mock(ClientePac.class);
    private final ClienteDianSoap canalDian = mock(ClienteDianSoap.class);
    private final CanalSimulado canalSimulado = mock(CanalSimulado.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);

    @Test
    void rechazoDeEmisionSeAudita() {
        EmpresaFeConfigJpa config = new EmpresaFeConfigJpa();
        config.setCanal("SIMULADO");
        when(canalSimulado.enviar(any(), any(), any(), eq(config)))
                .thenReturn(ResultadoTransmision.error("Rechazado por prueba", "<xml/>"));

        ResultadoTransmision resultado = new ServicioTransmision(canalPac, canalDian, canalSimulado, auditoria)
                .enviar("<xml/>", "CUFE-1", new DatosEnvio("FE-1", "FE", 1L, 2), config);

        assertEquals("RECHAZADO", resultado.estado());
        verify(auditoria).registrarEnEmpresa(isNull(), any(), eq("EMISION_RECHAZADA"),
                eq("factura"), eq("CUFE-1"), any());
    }
}
