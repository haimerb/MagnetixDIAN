package com.magnetixdian.onboarding;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioInvitaciones;
import com.magnetixdian.application.ServicioRefreshToken;
import com.magnetixdian.application.ServicioVerificacionEmail;
import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenJpa;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenRepository;
import com.magnetixdian.infrastructure.persistence.RoleJpa;
import com.magnetixdian.infrastructure.persistence.RoleRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.AceptarInvitacionRequest;
import com.magnetixdian.interfaces.dto.AceptarInvitacionResponse;
import com.magnetixdian.interfaces.dto.InvitacionTenantRequest;
import com.magnetixdian.interfaces.dto.InvitacionTenantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioInvitacionesTest {

    private UsuarioRepository usuarioRepo;
    private EmpresaRepository empresaRepo;
    private RoleRepository roleRepo;
    private PasswordResetTokenRepository tokenRepo;
    private PasswordEncoder passwordEncoder;
    private NotificadorEmail notificador;
    private AuditoriaService auditoria;
    private ServicioRefreshToken refreshTokenService;
    private JdbcTemplate jdbc;
    private ServicioInvitaciones servicio;

    @BeforeEach
    void setUp() {
        usuarioRepo = mock(UsuarioRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        roleRepo = mock(RoleRepository.class);
        tokenRepo = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        notificador = mock(NotificadorEmail.class);
        auditoria = mock(AuditoriaService.class);
        refreshTokenService = mock(ServicioRefreshToken.class);
        jdbc = mock(JdbcTemplate.class);
        servicio = new ServicioInvitaciones(usuarioRepo, empresaRepo, roleRepo, tokenRepo, passwordEncoder,
                notificador, auditoria, refreshTokenService, jdbc);
    }

    @Test
    void invitarCreaUsuarioPendienteRolTokenYAudi() {
        stubEstado("ACTIVO");
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresaRepo.findById(42L)).thenReturn(Optional.of(empresa));
        when(usuarioRepo.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarioRepo.existsByUsername(anyString())).thenReturn(false);
        when(roleRepo.findByCode("ROLE_CONTADOR")).thenReturn(Optional.of(new RoleJpa("ROLE_CONTADOR")));
        when(passwordEncoder.encode(anyString())).thenReturn("temporal-hash");
        when(usuarioRepo.save(any(UsuarioJpa.class))).thenAnswer(invocation -> {
            UsuarioJpa guardado = mock(UsuarioJpa.class);
            when(guardado.getId()).thenReturn(5L);
            when(guardado.getNombre()).thenReturn("Invitado");
            return guardado;
        });

        InvitacionTenantResponse respuesta = servicio.invitar(42L,
                new InvitacionTenantRequest("invitado@ejemplo.com", "Invitado", "contador"));

        assertEquals(42L, respuesta.empresaId());
        assertEquals(5L, respuesta.usuarioId());
        assertEquals("invitado", respuesta.username());
        assertEquals("CONTADOR", respuesta.rol());
        ArgumentCaptor<UsuarioJpa> usuarioCaptor = ArgumentCaptor.forClass(UsuarioJpa.class);
        verify(usuarioRepo).save(usuarioCaptor.capture());
        UsuarioJpa creado = usuarioCaptor.getValue();
        assertEquals(false, creado.getEnabled());
        assertEquals(true, creado.getInvitacionPendiente());
        assertEquals(1, creado.getRoles().size());
        assertEquals("ROLE_CONTADOR", creado.getRoles().iterator().next().getCode());
        assertEquals(empresa, creado.getEmpresa());
        ArgumentCaptor<PasswordResetTokenJpa> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenJpa.class);
        verify(tokenRepo).save(tokenCaptor.capture());
        PasswordResetTokenJpa token = tokenCaptor.getValue();
        assertEquals(PasswordResetTokenJpa.TIPO_INVITACION, token.getTipo());
        assertEquals(64, token.getTokenHash().length());
        OffsetDateTime esperado = OffsetDateTime.now().plusDays(7);
        assertTrue(token.getExpiraAt().isAfter(esperado.minusMinutes(2)));
        assertTrue(token.getExpiraAt().isBefore(esperado.plusMinutes(2)));
        verify(jdbc).update(contains("INSERT INTO invitacion_tenant"), anyString(), eq(42L),
                eq("CONTADOR"), eq("invitado@ejemplo.com"));
        verify(notificador).enviarTexto(eq("invitado@ejemplo.com"), anyString(), anyString());
        verify(auditoria).registrarEnEmpresa(eq(42L), nullable(String.class), eq("INVITACION_ENVIADA"),
                eq("usuario"), eq(5L), contains("CONTADOR"));
    }

    @Test
    void invitarRechazaRolNoPermitido() {
        assertThrows(IllegalArgumentException.class, () -> servicio.invitar(42L,
                new InvitacionTenantRequest("invitado@ejemplo.com", "Invitado", "OWNER")));
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void invitarRechazaEmpresaSuspendida() {
        stubEstado("SUSPENDIDO");
        assertThrows(IllegalStateException.class, () -> servicio.invitar(42L,
                new InvitacionTenantRequest("invitado@ejemplo.com", "Invitado", "CONTADOR")));
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void invitarRechazaCorreoDuplicado() {
        stubEstado("ACTIVO");
        when(usuarioRepo.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(new UsuarioJpa()));
        assertThrows(IllegalArgumentException.class, () -> servicio.invitar(42L,
                new InvitacionTenantRequest("invitado@ejemplo.com", "Invitado", "CONTADOR")));
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void aceptarFuerzaEmpresaRolClaveYRevocaRefresh() {
        String raw = "token-invitacion";
        UsuarioJpa usuario = usuario("invitado", "invitado@ejemplo.com");
        PasswordResetTokenJpa token = new PasswordResetTokenJpa(usuario, ServicioVerificacionEmail.hash(raw),
                PasswordResetTokenJpa.TIPO_INVITACION, OffsetDateTime.now().plusDays(7));
        when(tokenRepo.findByTokenHash(ServicioVerificacionEmail.hash(raw))).thenReturn(Optional.of(token));
        stubInvitacion(42L, "LECTOR");
        stubEstado("ACTIVO");
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(42L);
        when(empresaRepo.findById(42L)).thenReturn(Optional.of(empresa));
        RoleJpa role = new RoleJpa("ROLE_LECTOR");
        when(roleRepo.findByCode("ROLE_LECTOR")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("clave1234")).thenReturn("clave-hash");

        AceptarInvitacionResponse respuesta = servicio.aceptar(
                new AceptarInvitacionRequest(raw, "clave1234"));

        assertEquals(42L, respuesta.empresaId());
        assertEquals("LECTOR", respuesta.rol());
        assertEquals(empresa, usuario.getEmpresa());
        assertTrue(usuario.getEnabled());
        assertFalse(usuario.getInvitacionPendiente());
        assertEquals("clave-hash", usuario.getPasswordHash());
        assertEquals(1, usuario.getRoles().size());
        assertEquals(role, usuario.getRoles().iterator().next());
        assertNotNull(token.getUsadoAt());
        verify(usuarioRepo).save(usuario);
        verify(refreshTokenService).revocarTodos(any());
        verify(auditoria).registrarEnEmpresa(eq(42L), eq("invitado"), eq("INVITACION_ACEPTADA"),
                eq("usuario"), any(), eq("Rol LECTOR"));
    }

    @Test
    void aceptarRechazaTokenVencido() {
        String raw = "token-vencido";
        PasswordResetTokenJpa token = new PasswordResetTokenJpa(usuario("invitado", "invitado@ejemplo.com"),
                ServicioVerificacionEmail.hash(raw), PasswordResetTokenJpa.TIPO_INVITACION,
                OffsetDateTime.now().minusMinutes(1));
        when(tokenRepo.findByTokenHash(ServicioVerificacionEmail.hash(raw))).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> servicio.aceptar(
                new AceptarInvitacionRequest(raw, "clave1234")));
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void aceptarRechazaEmpresaCancelada() {
        String raw = "token-invitacion";
        PasswordResetTokenJpa token = new PasswordResetTokenJpa(usuario("invitado", "invitado@ejemplo.com"),
                ServicioVerificacionEmail.hash(raw), PasswordResetTokenJpa.TIPO_INVITACION,
                OffsetDateTime.now().plusDays(7));
        when(tokenRepo.findByTokenHash(ServicioVerificacionEmail.hash(raw))).thenReturn(Optional.of(token));
        stubInvitacion(42L, "LECTOR");
        stubEstado("CANCELADO");

        assertThrows(IllegalStateException.class, () -> servicio.aceptar(
                new AceptarInvitacionRequest(raw, "clave1234")));
        verify(usuarioRepo, never()).save(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubEstado(String estado) {
        when(jdbc.queryForObject(contains("SELECT estado_tenant"), any(RowMapper.class), any()))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("estado_tenant")).thenReturn(estado);
                    return mapper.mapRow(rs, 0);
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubInvitacion(Long empresaId, String rol) {
        when(jdbc.queryForObject(contains("SELECT empresa_id, rol"), any(RowMapper.class), any()))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("empresa_id")).thenReturn(empresaId);
                    when(rs.getString("rol")).thenReturn(rol);
                    return mapper.mapRow(rs, 0);
                });
    }

    private static UsuarioJpa usuario(String username, String email) {
        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombre("Invitado");
        usuario.setEmpresa(mock(EmpresaJpa.class));
        return usuario;
    }
}
