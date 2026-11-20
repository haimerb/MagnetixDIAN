package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.RoleJpa;
import com.magnetixdian.infrastructure.persistence.RoleRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.RegistroEmpresaRequest;
import com.magnetixdian.interfaces.dto.RegistroEmpresaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioRegistroTest {

    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private RoleRepository roleRepo;
    private PasswordEncoder passwordEncoder;
    private ServicioSuscripciones suscripciones;
    private AuditoriaService auditoria;
    private ServicioRegistro servicio;

    @BeforeEach
    void setUp() {
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        roleRepo = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        suscripciones = mock(ServicioSuscripciones.class);
        auditoria = mock(AuditoriaService.class);

        when(roleRepo.findByCode("ROLE_CLIENTE")).thenReturn(Optional.of(new RoleJpa("ROLE_CLIENTE")));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(empresaRepo.findByNit(anyString())).thenReturn(Optional.empty());
        when(usuarioRepo.existsByUsername(anyString())).thenReturn(false);
        when(usuarioRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(empresaRepo.save(any(EmpresaJpa.class))).thenAnswer(invocation -> {
            EmpresaJpa empresa = invocation.getArgument(0);
            EmpresaJpa guardada = mock(EmpresaJpa.class);
            when(guardada.getId()).thenReturn(1L);
            when(guardada.getNit()).thenReturn(empresa.getNit());
            return guardada;
        });
        when(usuarioRepo.save(any(UsuarioJpa.class))).thenAnswer(invocation -> {
            UsuarioJpa usuario = invocation.getArgument(0);
            UsuarioJpa guardado = mock(UsuarioJpa.class);
            when(guardado.getId()).thenReturn(2L);
            when(guardado.getUsername()).thenReturn(usuario.getUsername());
            return guardado;
        });

        servicio = new ServicioRegistro(empresaRepo, usuarioRepo, roleRepo, passwordEncoder,
                suscripciones, auditoria);
    }

    @Test
    void registroCreaEmpresaUsuarioYPlanFree() {
        RegistroEmpresaResponse respuesta = servicio.registrar(solicitud("900.123.456-78", true));

        assertEquals(1L, respuesta.empresaId());
        assertEquals(2L, respuesta.usuarioId());
        assertEquals("cliente1", respuesta.username());
        assertNotNull(respuesta.mensaje());

        ArgumentCaptor<EmpresaJpa> captorEmpresa = ArgumentCaptor.forClass(EmpresaJpa.class);
        verify(empresaRepo).save(captorEmpresa.capture());
        assertEquals("90012345678", captorEmpresa.getValue().getNit());
        assertEquals("cliente1@ejemplo.com", captorEmpresa.getValue().getEmail());

        ArgumentCaptor<UsuarioJpa> captorUsuario = ArgumentCaptor.forClass(UsuarioJpa.class);
        verify(usuarioRepo).save(captorUsuario.capture());
        UsuarioJpa usuario = captorUsuario.getValue();
        assertEquals("cliente1", usuario.getUsername());
        assertEquals("hash", usuario.getPasswordHash());
        assertNotNull(usuario.getTerminosAceptadosEn());
        assertEquals(1, usuario.getRoles().size());

        verify(suscripciones).cambiarPlan(1L, "FREE", "SIMULADO");
        verify(auditoria).registrar(eq("REGISTRO"), eq("USUARIO_REGISTRADO"), eq("usuario"), eq(2L),
                contains("empresa 90012345678"));
    }

    @Test
    void registroRechazaNitDuplicado() {
        when(empresaRepo.findByNit("90012345678"))
                .thenReturn(Optional.of(mock(EmpresaJpa.class)));

        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(solicitud("90012345678", true)));
        verify(usuarioRepo, never()).save(any());
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
    }

    @Test
    void registroRechazaCorreoDuplicado() {
        when(usuarioRepo.existsByEmailIgnoreCase("cliente1@ejemplo.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(solicitud("90012345678", true)));
        verify(empresaRepo, never()).save(any());
    }

    @Test
    void registroRechazaSinAceptarTerminos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(solicitud("90012345678", false)));
        verify(empresaRepo, never()).save(any());
    }

    @Test
    void registroRechazaHoneypot() {
        RegistroEmpresaRequest request = new RegistroEmpresaRequest("90012345678", "Empresa Prueba SAS",
                "Cliente Uno", "cliente1@ejemplo.com", "Cliente1", "clave1234", true, "https://spam.example");

        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(request));
        verify(empresaRepo, never()).save(any());
    }

    private static RegistroEmpresaRequest solicitud(String nit, boolean aceptaTerminos) {
        return new RegistroEmpresaRequest(nit, "Empresa Prueba SAS", "Cliente Uno",
                "cliente1@ejemplo.com", "Cliente1", "clave1234", aceptaTerminos, "");
    }
}
