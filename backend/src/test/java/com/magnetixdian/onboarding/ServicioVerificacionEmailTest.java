package com.magnetixdian.onboarding;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioOnboarding;
import com.magnetixdian.application.ServicioVerificacionEmail;
import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenJpa;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.ReenviarVerificacionResponse;
import com.magnetixdian.interfaces.dto.VerificacionEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

class ServicioVerificacionEmailTest {

    private UsuarioRepository usuarioRepo;
    private EmpresaRepository empresaRepo;
    private PasswordResetTokenRepository tokenRepo;
    private NotificadorEmail notificador;
    private AuditoriaService auditoria;
    private ServicioOnboarding onboarding;
    private JdbcTemplate jdbc;
    private ServicioVerificacionEmail servicio;

    @BeforeEach
    void setUp() {
        usuarioRepo = mock(UsuarioRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        tokenRepo = mock(PasswordResetTokenRepository.class);
        notificador = mock(NotificadorEmail.class);
        auditoria = mock(AuditoriaService.class);
        onboarding = mock(ServicioOnboarding.class);
        jdbc = mock(JdbcTemplate.class);
        servicio = new ServicioVerificacionEmail(usuarioRepo, empresaRepo, tokenRepo, notificador,
                auditoria, onboarding, jdbc);
    }

    @Test
    void tokenDeVerificacionCaducaEn24HorasyGuardaSoloHash() {
        UsuarioJpa usuario = usuario(10L);
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(10L);
        when(usuarioRepo.findByEmailIgnoreCase("cliente1@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(empresaRepo.findById(10L)).thenReturn(Optional.of(empresa));
        when(onboarding.emailVerificado(10L)).thenReturn(false);

        servicio.enviarTokenVerificacion(10L, "cliente1@ejemplo.com");

        ArgumentCaptor<PasswordResetTokenJpa> captor = ArgumentCaptor.forClass(PasswordResetTokenJpa.class);
        verify(tokenRepo).save(captor.capture());
        PasswordResetTokenJpa guardado = captor.getValue();
        assertEquals(ServicioVerificacionEmail.TIPO, guardado.getTipo());
        assertEquals(64, guardado.getTokenHash().length());
        assertEquals(usuario, guardado.getUsuario());
        OffsetDateTime esperado = OffsetDateTime.now().plusHours(24);
        assertNotNull(guardado.getExpiraAt());
        assertEquals(true, guardado.getExpiraAt().isAfter(esperado.minusMinutes(2)));
        assertEquals(true, guardado.getExpiraAt().isBefore(esperado.plusMinutes(2)));
        verify(tokenRepo).invalidarPendientes(any(), eq(ServicioVerificacionEmail.TIPO));
        verify(notificador).enviarTexto(eq("cliente1@ejemplo.com"), anyString(), anyString());
    }

    @Test
    void verificarMarcaTokenComoUsadoYAudiita() {
        String raw = "token-verificacion";
        PasswordResetTokenJpa token = token(raw, usuario(10L), OffsetDateTime.now().plusHours(24));
        when(tokenRepo.findByTokenHash(ServicioVerificacionEmail.hash(raw))).thenReturn(Optional.of(token));
        when(onboarding.estadoTenant(10L)).thenReturn("ACTIVO");

        VerificacionEmailResponse respuesta = servicio.verificar(raw);

        assertEquals(10L, respuesta.empresaId());
        assertEquals("ACTIVO", respuesta.estadoTenant());
        assertEquals(true, respuesta.emailVerificado());
        assertNotNull(respuesta.mensaje());
        assertNotNull(token.getUsadoAt());
        verify(jdbc).update(contains("email_verificado_at"), any(OffsetDateTime.class), eq(10L));
        verify(auditoria).registrarEnEmpresa(eq(10L), eq("cliente1"), eq("EMAIL_VERIFICADO"), eq("empresa"),
                eq(10L), eq("cliente1@ejemplo.com"));
        verify(onboarding).activarSiPagoAprobado(10L);

        assertThrows(IllegalArgumentException.class, () -> servicio.verificar(raw));
    }

    @Test
    void tokenVencidoNoSirve() {
        String raw = "token-vencido";
        PasswordResetTokenJpa token = token(raw, usuario(10L), OffsetDateTime.now().minusMinutes(1));
        when(tokenRepo.findByTokenHash(ServicioVerificacionEmail.hash(raw))).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> servicio.verificar(raw));
        verify(jdbc, never()).update(contains("email_verificado_at"), any(), any());
    }

    @Test
    void reenviarRespetaRateLimit() {
        UsuarioJpa usuario = usuario(10L);
        when(usuarioRepo.findByEmailIgnoreCase("cliente1@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(onboarding.emailVerificado(10L)).thenReturn(false);
        when(jdbc.queryForObject(contains("created_at"), eq(Timestamp.class), any(), any()))
                .thenReturn(Timestamp.from(Instant.now()));

        assertThrows(IllegalArgumentException.class, () -> servicio.reenviar("cliente1@ejemplo.com"));
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void reenviarGeneraNuevoTokenUnico() {
        UsuarioJpa usuario = usuario(10L);
        when(usuarioRepo.findByEmailIgnoreCase("cliente1@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(onboarding.emailVerificado(10L)).thenReturn(false);
        when(jdbc.queryForObject(contains("created_at"), eq(Timestamp.class), any(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(notificador.enviarTexto(anyString(), anyString(), anyString())).thenReturn(true);
        when(tokenRepo.save(any(PasswordResetTokenJpa.class))).thenAnswer(invocation -> {
            PasswordResetTokenJpa guardado = invocation.getArgument(0);
            assertNotEquals("", guardado.getTokenHash());
            return guardado;
        });

        ReenviarVerificacionResponse respuesta = servicio.reenviar("cliente1@ejemplo.com");

        assertEquals(true, respuesta.enviado());
        verify(tokenRepo).invalidarPendientes(any(), eq(ServicioVerificacionEmail.TIPO));
        verify(tokenRepo).save(any(PasswordResetTokenJpa.class));
    }

    private static UsuarioJpa usuario(Long empresaId) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(empresaId);
        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername("cliente1");
        usuario.setEmail("cliente1@ejemplo.com");
        usuario.setNombre("Cliente Uno");
        usuario.setEmpresa(empresa);
        return usuario;
    }

    private static PasswordResetTokenJpa token(String raw, UsuarioJpa usuario, OffsetDateTime expiraAt) {
        return new PasswordResetTokenJpa(usuario, ServicioVerificacionEmail.hash(raw),
                ServicioVerificacionEmail.TIPO, expiraAt);
    }
}
