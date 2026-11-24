package com.magnetixdian.dian;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioConfigFeTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final EmpresaFeConfigRepository configRepository = mock(EmpresaFeConfigRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final CifradorAes cifrador = mock(CifradorAes.class);
    private final ClienteDianSoap cliente = mock(ClienteDianSoap.class);

    @Test
    void guardarNoPermiteCambioDirectoAProduccion() {
        EmpresaFeConfigJpa config = configuracion();
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(config));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio(false).guardar(1L, dto(1, "DIAN")));

        assertTrue(error.getMessage().contains("switchover"));
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), eq("CAMBIO_PRODUCCION_BLOQUEADO"),
                eq("empresa"), eq(1L), any());
        verify(configRepository, never()).save(any());
    }

    @Test
    void mascaraNoExponeSecretos() {
        EmpresaFeConfigJpa config = configuracion();
        config.setCertificadoNombre("certificado.p12");
        config.setCertificadoPassword("enc:v2:primary:cifrado");
        config.setPacApiKey("enc:v2:primary:cifrado");
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(config));
        when(cliente.versionPorDefecto()).thenReturn("1.8");

        var mascara = servicio(false).obtenerMascara(1L);

        assertEquals("********", mascara.certificadoMascara());
        assertTrue(mascara.tieneCertificado());
        assertTrue(mascara.tienePacKey());
        assertEquals("1.8", mascara.versionWsdl());
    }

    private ServicioConfigFe servicio(boolean failFast) {
        return new ServicioConfigFe(empresaRepository, configRepository, auditoria, cifrador, cliente,
                failFast, "./uploads", null, null);
    }

    private EmpresaFeConfigJpa configuracion() {
        EmpresaFeConfigJpa config = new EmpresaFeConfigJpa();
        config.setAmbiente((short) 2);
        config.setCanal("DIAN");
        config.setCertificadoPath("/tmp/certificado.p12");
        config.setCertificadoPassword("enc:v2:primary:cifrado");
        return config;
    }

    private ConfigFeDto dto(Integer ambiente, String canal) {
        return new ConfigFeDto(ambiente, null, null, null, null, null, null, null, null, null, null, null,
                canal, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
