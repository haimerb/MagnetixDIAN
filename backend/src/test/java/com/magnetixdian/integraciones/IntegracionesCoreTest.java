package com.magnetixdian.integraciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.RegistroIntegraciones;
import com.magnetixdian.application.ServicioIntegraciones;
import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionEventoJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionEventoRepository;
import com.magnetixdian.interfaces.dto.IntegracionConfigDto;
import com.magnetixdian.interfaces.dto.IntegracionConfigRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegracionesCoreTest {

    private final IntegracionConfigRepository configuraciones = mock(IntegracionConfigRepository.class);
    private final IntegracionEventoRepository eventos = mock(IntegracionEventoRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final RegistroIntegraciones registro = mock(RegistroIntegraciones.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);

    private ServicioIntegraciones servicio(boolean exigirFirma) {
        return new ServicioIntegraciones(configuraciones, eventos, empresas, registro, auditoria,
                new ObjectMapper(), exigirFirma);
    }

    @Test
    void tokenDeWebhookInvalidoSeRechaza() {
        when(configuraciones.findByWebhookTokenHash(anyString())).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio(true).webhook("siigo", "token-malo", "{}", Map.of()));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void webhookSinFirmaSeRechazaCuandoEsObligatoria() {
        IntegracionConfigJpa config = configuracionSinFirma();
        when(configuraciones.findByWebhookTokenHash(sha256("token-valido")))
                .thenReturn(Optional.of(config));
        when(registro.obtener(ProveedorIntegracion.SIIGO)).thenReturn(mock(AdaptadorIntegracion.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio(true).webhook("siigo", "token-valido", "{}", Map.of()));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(eventos, never()).save(any());
    }

    @Test
    void webhookDuplicadoEsIdempotente() {
        EmpresaJpa empresa = empresa(1L);
        IntegracionConfigJpa config = configuracionSinFirma();
        config.setEmpresa(empresa);
        AdaptadorIntegracion adaptador = adaptadorConFirmaValida();
        when(configuraciones.findByWebhookTokenHash(sha256("token-valido"))).thenReturn(Optional.of(config));
        when(registro.obtener(ProveedorIntegracion.SIIGO)).thenReturn(adaptador);
        IntegracionEventoJpa existente = new IntegracionEventoJpa();
        ReflectionTestUtils.setField(existente, "id", 99L);
        existente.setProveedor("SIIGO");
        existente.setTipo("WEBHOOK");
        existente.setEstado("EXITOSO");
        when(eventos.findByEmpresaIdAndProveedorAndExternalIdAndTipo(1L, "SIIGO", sha256("SIIGO|1|{}"), "WEBHOOK"))
                .thenReturn(Optional.of(existente));

        var respuesta = servicio(true).webhook("siigo", "token-valido", "{}", Map.of());

        assertEquals(99L, respuesta.id());
        assertEquals("EXITOSO", respuesta.estado());
        verify(eventos, never()).save(any());
    }

    @Test
    void guardarGeneraTokenDeWebhookUnaSolaVez() {
        EmpresaJpa empresa = empresa(1L);
        AdaptadorIntegracion adaptador = adaptadorConFirmaValida();
        when(empresas.findById(1L)).thenReturn(Optional.of(empresa));
        when(configuraciones.findByEmpresaIdAndProveedor(1L, "SIIGO")).thenReturn(Optional.empty());
        when(registro.existe(ProveedorIntegracion.SIIGO)).thenReturn(true);
        when(registro.obtener(ProveedorIntegracion.SIIGO)).thenReturn(adaptador);

        IntegracionConfigDto primera = servicio(false).guardar(1L, ProveedorIntegracion.SIIGO,
                new IntegracionConfigRequest(true, Map.of("token", "x"), Map.of()));
        IntegracionConfigJpa guardada = new IntegracionConfigJpa();
        guardada.setEmpresa(empresa(1L));
        guardada.setProveedor("SIIGO");
        guardada.setWebhookTokenHash(sha256(primera.webhookToken()));
        when(configuraciones.findByEmpresaIdAndProveedor(1L, "SIIGO")).thenReturn(Optional.of(guardada));
        IntegracionConfigDto segunda = servicio(false).guardar(1L, ProveedorIntegracion.SIIGO,
                new IntegracionConfigRequest(true, Map.of("token", "y"), Map.of()));

        assertNotNull(primera.webhookToken());
        assertFalse(primera.webhookToken().isBlank());
        assertEquals(null, segunda.webhookToken());
        verify(configuraciones, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void reintentoUsaBackoffProgresivo() {
        ServicioIntegraciones servicio = servicio(true);
        IntegracionEventoJpa evento = new IntegracionEventoJpa();
        evento.setIntentos(1);

        ReflectionTestUtils.invokeMethod(servicio, "aplicarFallo", evento, "fallo");

        assertEquals("PENDIENTE", evento.getEstado());
        long minutos = ChronoUnit.MINUTES.between(OffsetDateTime.now(), evento.getProximoIntento());
        assertTrue(minutos >= 0 && minutos <= 1);
        evento.setIntentos(5);
        ReflectionTestUtils.invokeMethod(servicio, "aplicarFallo", evento, "fallo");
        assertEquals("ERROR", evento.getEstado());
        assertEquals(null, evento.getProximoIntento());
    }

    private EmpresaJpa empresa(Long id) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(id);
        return empresa;
    }

    private IntegracionConfigJpa configuracionSinFirma() {
        IntegracionConfigJpa config = new IntegracionConfigJpa();
        config.setEmpresa(empresa(1L));
        config.setProveedor("SIIGO");
        config.setActivo(true);
        config.setWebhookTokenHash(sha256("token-valido"));
        config.setCredenciales("{}");
        config.setConfiguracion("{}");
        return config;
    }

    private AdaptadorIntegracion adaptadorConFirmaValida() {
        AdaptadorIntegracion adaptador = mock(AdaptadorIntegracion.class);
        when(adaptador.verificarFirma(any(), any())).thenReturn(true);
        when(adaptador.configurada(any())).thenReturn(false);
        return adaptador;
    }

    private static String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
