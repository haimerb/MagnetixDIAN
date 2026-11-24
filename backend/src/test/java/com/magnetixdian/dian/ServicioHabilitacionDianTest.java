package com.magnetixdian.dian;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.application.ServicioHabilitacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDianResolver;
import com.magnetixdian.domain.dian.RespuestaDian;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.interfaces.dto.DianHabilitacionDto;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServicioHabilitacionDianTest {

    private final EmpresaFeConfigRepository configRepository = mock(EmpresaFeConfigRepository.class);
    private final ClienteDianSoap cliente = mock(ClienteDianSoap.class);
    private final ServicioConfigFe servicioConfigFe = mock(ServicioConfigFe.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final ModoOperacionDianResolver resolver = new ModoOperacionDianResolver();

    @Test
    void failFastBloqueaSwitchoverConCanalSimulado() {
        EmpresaFeConfigJpa config = configuracionValida();
        config.setCanal("SIMULADO");
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(config));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio(true).switchoverProduccion(1L, solicitud(), "TRACK-1", null));

        assertTrue(error.getMessage().contains("SEGURIDAD_FAIL_FAST"));
        verifyNoInteractions(cliente);
    }

    @Test
    void validarExigeCertificadoConfigurado() {
        EmpresaFeConfigJpa config = configuracionValida();
        config.setCertificadoPath(null);
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(config));

        assertThrows(IllegalArgumentException.class, () -> servicio(false).validar(1L, solicitud()));
    }

    @Test
    void setDePruebasRequiereOchoFacturasUnaNotaDebitoYUnaCredito() {
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(configuracionValida()));
        List<byte[]> documentos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            documentos.add("<Invoice></Invoice>".getBytes(StandardCharsets.UTF_8));
        }

        assertThrows(IllegalArgumentException.class,
                () -> servicio(false).enviarLotePruebas(1L, solicitud(), documentos, "1.8"));
        verifyNoInteractions(cliente);
    }

    @Test
    void enviarSetDePruebasValidoAuditaEnvioYAprobacion() {
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(configuracionValida()));
        when(cliente.sendTestSetAsync(any(byte[].class), anyString(), eq("TS-1"), eq(2), eq("1.8"), any()))
                .thenReturn(respuesta("TRACK-1", "00"));

        var resultado = servicio(false).enviarLotePruebas(1L, solicitud(), documentosValidos(), "1.8");

        assertTrue(resultado.aprobado());
        assertEquals("TRACK-1", resultado.trackId());
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), eq("TEST_SET_ENVIADO"), eq("empresa"), eq(1L), anyString());
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), eq("TEST_SET_APROBADO"), eq("empresa"), eq(1L), anyString());
    }

    @Test
    void switchoverSoloSeActivaConRespuestaAprobada() {
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(configuracionValida()));
        when(cliente.versionPorDefecto()).thenReturn("1.8");
        when(cliente.getStatusByTrackId(eq("TRACK-1"), eq(1), eq("1.8"), any()))
                .thenReturn(respuesta("TRACK-1", "00"));

        var resultado = servicio(false).switchoverProduccion(1L, solicitud(), "TRACK-1", null);

        assertTrue(resultado.aprobado());
        verify(servicioConfigFe).activarProduccion(1L, "TRACK-1");
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), eq("SWITCHOVER_PRODUCCION"), eq("empresa"), eq(1L), anyString());
    }

    @Test
    void switchoverRechazadoNoActivaProduccion() {
        when(configRepository.findConEmpresa(1L)).thenReturn(Optional.of(configuracionValida()));
        when(cliente.versionPorDefecto()).thenReturn("1.8");
        when(cliente.getStatusByTrackId(eq("TRACK-1"), eq(1), eq("1.8"), any()))
                .thenReturn(respuesta("TRACK-1", "01"));

        assertThrows(IllegalStateException.class,
                () -> servicio(false).switchoverProduccion(1L, solicitud(), "TRACK-1", null));
        verify(servicioConfigFe, never()).activarProduccion(any(), anyString());
    }

    private ServicioHabilitacionDian servicio(boolean failFast) {
        return new ServicioHabilitacionDian(configRepository, cliente, resolver, servicioConfigFe, auditoria, failFast);
    }

    private EmpresaFeConfigJpa configuracionValida() {
        EmpresaFeConfigJpa config = new EmpresaFeConfigJpa();
        config.setAmbiente((short) 2);
        config.setCanal("DIAN");
        config.setCertificadoPath("/tmp/certificado.p12");
        config.setCertificadoPassword("enc:v2:primary:cifrado");
        config.setResolucionNumero("RES-1");
        config.setResolucionFecha(LocalDate.of(2026, 1, 1));
        config.setPrefijo("FE");
        config.setRangoDesde(1L);
        config.setRangoHasta(1000L);
        config.setSoftwareId("SW-1");
        return config;
    }

    private DianHabilitacionDto solicitud() {
        return new DianHabilitacionDto(ModoOperacionDian.ADQUIRIDO_CLIENTE, "RES-1",
                LocalDate.of(2026, 1, 1), "FE", 1, 1000, "TS-1", "SW-1", false);
    }

    private List<byte[]> documentosValidos() {
        List<byte[]> documentos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            documentos.add("<Invoice></Invoice>".getBytes(StandardCharsets.UTF_8));
        }
        documentos.add("<DebitNote></DebitNote>".getBytes(StandardCharsets.UTF_8));
        documentos.add("<CreditNote></CreditNote>".getBytes(StandardCharsets.UTF_8));
        return documentos;
    }

    private RespuestaDian respuesta(String trackId, String statusCode) {
        return new RespuestaDian(trackId, statusCode, "Estado DIAN", null, null, null, List.of());
    }
}
