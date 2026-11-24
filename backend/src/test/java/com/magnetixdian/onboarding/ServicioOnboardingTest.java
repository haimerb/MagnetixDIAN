package com.magnetixdian.onboarding;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioOnboarding;
import com.magnetixdian.application.ServicioSuscripciones;
import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDianResolver;
import com.magnetixdian.domain.pago.PagoAprobadoEvent;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioOnboardingTest {

    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private RoleRepository roleRepo;
    private PasswordEncoder passwordEncoder;
    private ServicioSuscripciones suscripciones;
    private AuditoriaService auditoria;
    private JdbcTemplate jdbc;
    private ServicioOnboarding servicio;

    @BeforeEach
    void setUp() {
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        roleRepo = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        suscripciones = mock(ServicioSuscripciones.class);
        auditoria = mock(AuditoriaService.class);
        jdbc = mock(JdbcTemplate.class);
        ModoOperacionDianResolver resolver = new ModoOperacionDianResolver();
        servicio = new ServicioOnboarding(empresaRepo, usuarioRepo, roleRepo, passwordEncoder,
                suscripciones, auditoria, jdbc, resolver);
    }

    @Test
    void crearTenantGuardaEstadoPlanModoYOwner() {
        when(roleRepo.findByCode("ROLE_OWNER")).thenReturn(Optional.of(new RoleJpa("ROLE_OWNER")));
        when(empresaRepo.findByNit(anyString())).thenReturn(Optional.empty());
        when(usuarioRepo.existsByUsername(anyString())).thenReturn(false);
        when(usuarioRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(empresaRepo.save(any(EmpresaJpa.class))).thenAnswer(invocation -> {
            EmpresaJpa guardada = mock(EmpresaJpa.class);
            when(guardada.getId()).thenReturn(10L);
            return guardada;
        });
        when(usuarioRepo.save(any(UsuarioJpa.class))).thenAnswer(invocation -> {
            UsuarioJpa guardado = mock(UsuarioJpa.class);
            when(guardado.getId()).thenReturn(20L);
            when(guardado.getUsername()).thenReturn("cliente1");
            return guardado;
        });

        RegistroEmpresaResponse respuesta = servicio.crearTenant(solicitud("pro"));

        assertEquals(10L, respuesta.empresaId());
        assertEquals("PRO", respuesta.planCode());
        assertEquals("PENDIENTE_VERIFICACION", respuesta.estadoTenant());
        assertEquals(ModoOperacionDian.ADQUIRIDO_CLIENTE, respuesta.modoOperacionDian());
        verify(jdbc).update(contains("estado_tenant = ?"), eq("PENDIENTE_VERIFICACION"), eq("PRO"),
                eq("ADQUIRIDO_CLIENTE"), eq(10L));
        verify(jdbc).update(contains("owner_usuario_id = ?"), eq(20L), eq(10L));
        verify(auditoria).registrarEnEmpresa(eq(10L), eq("cliente1"), eq("REGISTRO_CREADO"), eq("empresa"),
                eq(10L), contains("plan pro"));
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
    }

    @Test
    void crearTenantRechazaPlanVacio() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crearTenant(solicitud(" ")));
        verify(empresaRepo, never()).save(any());
    }

    @Test
    void crearTenantRechazaRolOwnerAusente() {
        when(roleRepo.findByCode("ROLE_OWNER")).thenReturn(Optional.empty());
        when(empresaRepo.findByNit(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> servicio.crearTenant(solicitud("PRO")));
        verify(empresaRepo, never()).save(any());
    }

    @Test
    void pagoAprobadoSinEmailVerificadoNoActiva() {
        when(jdbc.queryForObject(contains("COUNT(*) FROM onboarding_pago WHERE event_id"),
                eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbc.queryForObject(contains("email_verificado_at IS NOT NULL"),
                eq(Boolean.class), any())).thenReturn(false);

        servicio.alPagoAprobado(new PagoAprobadoEvent(10L, "PRO", "WOMPI-123", "evt-1"));

        verify(jdbc).update(contains("INSERT INTO onboarding_pago"), eq("evt-1"), eq(10L), eq("PRO"),
                eq("WOMPI-123"));
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
        verify(jdbc, never()).update(contains("UPDATE empresa SET estado_tenant"), any(), any(), any(), any());
    }

    @Test
    void pagoAprobadoConEmailVerificadoActivaUnaSolaVez() {
        when(jdbc.queryForObject(contains("COUNT(*) FROM onboarding_pago WHERE event_id"),
                eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbc.queryForObject(contains("email_verificado_at IS NOT NULL"),
                eq(Boolean.class), any())).thenReturn(true);
        when(jdbc.queryForObject(contains("SELECT estado_tenant"), eq(String.class), any()))
                .thenReturn("PENDIENTE_VERIFICACION");
        when(jdbc.queryForObject(contains("COUNT(*) FROM onboarding_pago WHERE empresa_id"),
                eq(Integer.class), any())).thenReturn(0);
        stubPagosPendientes();

        servicio.alPagoAprobado(new PagoAprobadoEvent(10L, "PRO", "WOMPI-123", "evt-1"));

        verify(suscripciones).cambiarPlan(10L, "PRO", "WOMPI");
        verify(jdbc).update(contains("UPDATE empresa SET estado_tenant"), eq("ACTIVO"), eq("PRO"),
                any(OffsetDateTime.class), eq(10L));
        verify(jdbc).update(contains("UPDATE onboarding_pago SET estado"), any(OffsetDateTime.class), eq(10L));
        verify(auditoria).registrarEnEmpresa(eq(10L), eq("PAGO"), eq("TENANT_ACTIVADO"), eq("empresa"),
                eq(10L), contains("PRO"));
    }

    @Test
    void eventoDuplicadoNoInsertaNiActiva() {
        when(jdbc.queryForObject(contains("COUNT(*) FROM onboarding_pago WHERE event_id"),
                eq(Integer.class), any(), any())).thenReturn(1);
        when(jdbc.queryForObject(contains("email_verificado_at IS NOT NULL"),
                eq(Boolean.class), any())).thenReturn(false);

        servicio.alPagoAprobado(new PagoAprobadoEvent(10L, "PRO", "WOMPI-123", "evt-1"));
        servicio.alPagoAprobado(new PagoAprobadoEvent(10L, "PRO", "WOMPI-123", "evt-1"));

        verify(jdbc, never()).update(contains("INSERT INTO onboarding_pago"), any(), any(), any(), any());
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPagosPendientes() {
        when(jdbc.query(contains("SELECT event_id"), any(RowMapper.class), any())).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("event_id")).thenReturn("evt-1");
            when(rs.getString("plan_codigo")).thenReturn("PRO");
            when(rs.getString("referencia")).thenReturn("WOMPI-123");
            return List.of(mapper.mapRow(rs, 0));
        });
    }

    private static RegistroEmpresaRequest solicitud(String planCode) {
        return new RegistroEmpresaRequest("90012345678", "Empresa Prueba SAS", "Cliente Uno",
                "cliente1@ejemplo.com", "Cliente1", "clave1234", true, "", planCode);
    }
}
