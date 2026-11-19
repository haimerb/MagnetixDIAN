package com.magnetixdian.application;

import com.magnetixdian.domain.apikey.ClaveApi;
import com.magnetixdian.infrastructure.persistence.ApiKeyJpa;
import com.magnetixdian.infrastructure.persistence.ApiKeyRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.ApiKeyCreadaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioApiKeysTest {

    private ApiKeyRepository apiKeyRepo;
    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private AuditoriaService auditoria;
    private ServicioApiKeys servicio;

    @BeforeEach
    void setUp() {
        apiKeyRepo = mock(ApiKeyRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        auditoria = mock(AuditoriaService.class);
        servicio = new ServicioApiKeys(apiKeyRepo, empresaRepo, usuarioRepo, auditoria);
    }

    private UsuarioJpa usuario(long empresaId, boolean enabled) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(empresaId);
        UsuarioJpa u = mock(UsuarioJpa.class);
        when(u.getEmpresa()).thenReturn(empresa);
        when(u.getEnabled()).thenReturn(enabled);
        when(u.getUsername()).thenReturn("admin");
        return u;
    }

    @Test
    void crearPersisteHashYDevuelveClaveUnaVez() {
        UsuarioJpa propietario = usuario(1L, true);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(mock(EmpresaJpa.class)));
        when(usuarioRepo.findById(5L)).thenReturn(Optional.of(propietario));
        when(apiKeyRepo.save(any(ApiKeyJpa.class))).thenAnswer(i -> i.getArgument(0));

        ApiKeyCreadaDto dto = servicio.crear(1L, 5L, "Integración");

        ArgumentCaptor<ApiKeyJpa> captor = ArgumentCaptor.forClass(ApiKeyJpa.class);
        verify(apiKeyRepo).save(captor.capture());
        assertEquals(ClaveApi.hash(dto.clave()), captor.getValue().getHash());
        assertNotEquals(dto.clave(), captor.getValue().getHash());
        assertEquals(dto.prefijo(), ClaveApi.prefijoDe(dto.clave()));
        assertEquals("Integración", dto.nombre());
    }

    @Test
    void crearRechazaUsuarioDeOtraEmpresa() {
        UsuarioJpa ajeno = usuario(2L, true);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(mock(EmpresaJpa.class)));
        when(usuarioRepo.findById(5L)).thenReturn(Optional.of(ajeno));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 5L, "x"));
        verify(apiKeyRepo, never()).save(any());
    }

    @Test
    void crearExigeNombre() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 5L, " "));
    }

    @Test
    void autenticarConClaveValidaDevuelveUsername() {
        ClaveApi.Generada generada = ClaveApi.generar();
        ApiKeyJpa registro = mock(ApiKeyJpa.class);
        UsuarioJpa propietario = usuario(1L, true);
        when(registro.getHash()).thenReturn(ClaveApi.hash(generada.clave()));
        when(registro.getUsuario()).thenReturn(propietario);
        when(apiKeyRepo.findFirstByPrefijoAndActivoTrue(generada.prefijo())).thenReturn(Optional.of(registro));

        Optional<String> username = servicio.autenticar(generada.clave());

        assertEquals(Optional.of("admin"), username);
        verify(apiKeyRepo).save(registro);
    }

    @Test
    void autenticarConClaveInvalidaEsVacio() {
        ClaveApi.Generada generada = ClaveApi.generar();
        ApiKeyJpa registro = mock(ApiKeyJpa.class);
        when(registro.getHash()).thenReturn(ClaveApi.hash("otra-clave"));
        when(apiKeyRepo.findFirstByPrefijoAndActivoTrue(generada.prefijo())).thenReturn(Optional.of(registro));

        assertTrue(servicio.autenticar(generada.clave()).isEmpty());
        verify(apiKeyRepo, never()).save(any());
    }

    @Test
    void autenticarConFormatoInvalidoEsVacio() {
        assertTrue(servicio.autenticar("no-es-una-clave").isEmpty());
        verify(apiKeyRepo, never()).findFirstByPrefijoAndActivoTrue(anyString());
    }

    @Test
    void autenticarConUsuarioDeshabilitadoEsVacio() {
        ClaveApi.Generada generada = ClaveApi.generar();
        ApiKeyJpa registro = mock(ApiKeyJpa.class);
        UsuarioJpa inactivo = usuario(1L, false);
        when(registro.getHash()).thenReturn(ClaveApi.hash(generada.clave()));
        when(registro.getUsuario()).thenReturn(inactivo);
        when(apiKeyRepo.findFirstByPrefijoAndActivoTrue(generada.prefijo())).thenReturn(Optional.of(registro));

        assertTrue(servicio.autenticar(generada.clave()).isEmpty());
    }

    @Test
    void revocarDesactivaLaClave() {
        ApiKeyJpa clave = mock(ApiKeyJpa.class);
        when(clave.isActivo()).thenReturn(true);
        when(clave.getNombre()).thenReturn("Integración");
        when(apiKeyRepo.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.of(clave));

        servicio.revocar(1L, 9L);

        verify(clave).setActivo(false);
        verify(apiKeyRepo).save(clave);
    }

    @Test
    void listarMapeaResultados() {
        ApiKeyJpa clave = mock(ApiKeyJpa.class);
        when(clave.getId()).thenReturn(1L);
        when(clave.getNombre()).thenReturn("n");
        when(clave.getPrefijo()).thenReturn("abcdef12");
        when(apiKeyRepo.findByEmpresaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(clave));

        var lista = servicio.listar(1L);

        assertEquals(1, lista.size());
        assertEquals("abcdef12", lista.get(0).prefijo());
    }
}
