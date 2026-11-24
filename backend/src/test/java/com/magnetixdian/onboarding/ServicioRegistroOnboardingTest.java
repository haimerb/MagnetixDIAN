package com.magnetixdian.onboarding;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioOnboarding;
import com.magnetixdian.application.ServicioRegistro;
import com.magnetixdian.application.ServicioSuscripciones;
import com.magnetixdian.application.ServicioVerificacionEmail;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioRegistroOnboardingTest {

    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private RoleRepository roleRepo;
    private PasswordEncoder passwordEncoder;
    private ServicioSuscripciones suscripciones;
    private AuditoriaService auditoria;
    private ServicioOnboarding onboarding;
    private ServicioVerificacionEmail verificacion;

    @BeforeEach
    void setUp() {
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        roleRepo = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        suscripciones = mock(ServicioSuscripciones.class);
        auditoria = mock(AuditoriaService.class);
        onboarding = mock(ServicioOnboarding.class);
        verificacion = mock(ServicioVerificacionEmail.class);
    }

    @Test
    void registroCreaTenantPendienteYEnviaVerificacion() {
        RegistroEmpresaRequest request = solicitud("PRO");
        RegistroEmpresaResponse esperada = new RegistroEmpresaResponse(10L, 20L, "cliente1", "PRO",
                "PENDIENTE_VERIFICACION", null, "Cuenta creada.");
        when(onboarding.crearTenant(request)).thenReturn(esperada);
        ServicioRegistro servicio = servicio();

        RegistroEmpresaResponse respuesta = servicio.registrar(request);

        assertEquals(esperada, respuesta);
        verify(onboarding).crearTenant(request);
        verify(verificacion).enviarTokenVerificacion(10L, "cliente1@ejemplo.com");
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
        verify(empresaRepo, never()).save(any());
    }

    @Test
    void constructorLegadoConservaComportamientoPrevio() {
        when(roleRepo.findByCode("ROLE_CLIENTE")).thenReturn(Optional.of(new RoleJpa("ROLE_CLIENTE")));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(empresaRepo.findByNit(anyString())).thenReturn(Optional.empty());
        when(usuarioRepo.existsByUsername(anyString())).thenReturn(false);
        when(usuarioRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(empresaRepo.save(any(EmpresaJpa.class))).thenAnswer(invocation -> {
            EmpresaJpa guardada = mock(EmpresaJpa.class);
            when(guardada.getId()).thenReturn(1L);
            return guardada;
        });
        when(usuarioRepo.save(any(UsuarioJpa.class))).thenAnswer(invocation -> {
            UsuarioJpa guardado = mock(UsuarioJpa.class);
            when(guardado.getId()).thenReturn(2L);
            return guardado;
        });
        ServicioRegistro legacy = new ServicioRegistro(empresaRepo, usuarioRepo, roleRepo, passwordEncoder,
                suscripciones, auditoria);

        RegistroEmpresaResponse respuesta = legacy.registrar(solicitud("FREE"));

        assertEquals(1L, respuesta.empresaId());
        assertNotNull(respuesta.mensaje());
        verify(suscripciones).cambiarPlan(1L, "FREE", "SIMULADO");
        verify(auditoria).registrar(eq("REGISTRO"), eq("USUARIO_REGISTRADO"), eq("usuario"), eq(2L),
                contains("empresa 90012345678"));
        verify(verificacion, never()).enviarTokenVerificacion(any(), anyString());
    }

    private ServicioRegistro servicio() {
        return new ServicioRegistro(empresaRepo, usuarioRepo, roleRepo, passwordEncoder, suscripciones,
                auditoria, onboarding, verificacion);
    }

    private static RegistroEmpresaRequest solicitud(String planCode) {
        return new RegistroEmpresaRequest("900.123.456-78", "Empresa Prueba SAS", "Cliente Uno",
                "cliente1@ejemplo.com", "Cliente1", "clave1234", true, "", planCode);
    }
}
