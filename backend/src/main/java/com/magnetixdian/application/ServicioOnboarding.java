package com.magnetixdian.application;

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
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ServicioOnboarding {

    public static final String ESTADO_PENDIENTE = "PENDIENTE_VERIFICACION";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    private static final Set<String> ESTADOS_BLOQUEADOS = Set.of("SUSPENDIDO", "CANCELADO");
    private static final String ROL_OWNER = "ROLE_OWNER";

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final ServicioSuscripciones suscripciones;
    private final AuditoriaService auditoria;
    private final JdbcTemplate jdbc;
    private final ModoOperacionDianResolver modoResolver;

    public ServicioOnboarding(EmpresaRepository empresaRepo,
                              UsuarioRepository usuarioRepo,
                              RoleRepository roleRepo,
                              PasswordEncoder passwordEncoder,
                              ServicioSuscripciones suscripciones,
                              AuditoriaService auditoria,
                              JdbcTemplate jdbc,
                              ModoOperacionDianResolver modoResolver) {
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.suscripciones = suscripciones;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.modoResolver = modoResolver;
    }

    @Transactional
    public RegistroEmpresaResponse crearTenant(RegistroEmpresaRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            throw new IllegalArgumentException("No se pudo completar el registro.");
        }
        if (!request.aceptaTerminos()) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones.");
        }
        if (request.planCode() == null || request.planCode().isBlank()) {
            throw new IllegalArgumentException("Debe indicar el plan.");
        }
        String nit = normalizarNit(request.nit());
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();
        if (nit.isBlank()) {
            throw new IllegalArgumentException("El NIT no es válido.");
        }
        if (empresaRepo.findByNit(nit).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con el NIT " + nit + ".");
        }
        if (usuarioRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre " + username + ".");
        }
        if (usuarioRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + email + ".");
        }
        RoleJpa owner = roleRepo.findByCode(ROL_OWNER)
                .orElseThrow(() -> new IllegalStateException("No está configurado el rol de propietario."));

        EmpresaJpa empresa = new EmpresaJpa(nit, request.razonSocial().trim());
        empresa.setEmail(email);
        EmpresaJpa guardadaEmpresa = empresaRepo.save(empresa);
        var modoEnum = modoResolver.modoPorDefectoParaCliente();
        String modo = modoEnum.name();
        jdbc.update("UPDATE empresa SET estado_tenant = ?, plan_codigo = ?, modo_operacion_dian = ? "
                        + "WHERE id = ?",
                ESTADO_PENDIENTE, request.planCode().trim().toUpperCase(), modo, guardadaEmpresa.getId());

        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setEmail(email);
        usuario.setNombre(request.nombre().trim());
        usuario.setEmpresa(guardadaEmpresa);
        usuario.setEnabled(Boolean.TRUE);
        usuario.setTerminosAceptadosEn(OffsetDateTime.now());
        usuario.getRoles().add(owner);
        UsuarioJpa guardadoUsuario = usuarioRepo.save(usuario);
        jdbc.update("UPDATE empresa SET owner_usuario_id = ? WHERE id = ?",
                guardadoUsuario.getId(), guardadaEmpresa.getId());

        auditoria.registrarEnEmpresa(guardadaEmpresa.getId(), username, "REGISTRO_CREADO", "empresa",
                guardadaEmpresa.getId(), "NIT " + nit + " · plan " + request.planCode() + " · modo " + modo);

        return new RegistroEmpresaResponse(guardadaEmpresa.getId(), guardadoUsuario.getId(), username,
                request.planCode().trim().toUpperCase(), ESTADO_PENDIENTE, modoEnum,
                "Cuenta creada. Verifique su correo para continuar.");
    }

    @EventListener
    @Transactional
    public void alPagoAprobado(PagoAprobadoEvent evento) {
        Long empresaId = evento.empresaId();
        String planCode = evento.planCode() == null ? null : evento.planCode().trim().toUpperCase();
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("El evento de pago no indica el plan.");
        }
        Integer registrados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM onboarding_pago WHERE event_id = ? AND empresa_id = ?",
                Integer.class, evento.eventId(), empresaId);
        if (registrados != null && registrados > 0) {
            activarSiPagoAprobado(empresaId);
            return;
        }
        try {
            jdbc.update("INSERT INTO onboarding_pago (event_id, empresa_id, plan_codigo, referencia, estado) "
                            + "VALUES (?, ?, ?, ?, 'PENDIENTE_EMAIL') "
                            + "ON CONFLICT (event_id, empresa_id) DO NOTHING",
                    evento.eventId(), empresaId, planCode, evento.referencia());
        } catch (DuplicateKeyException ex) {
            activarSiPagoAprobado(empresaId);
            return;
        }
        activarSiPagoAprobado(empresaId);
    }

    @Transactional
    public void activarSiPagoAprobado(Long empresaId) {
        if (empresaId == null) {
            return;
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT email_verificado_at IS NOT NULL FROM empresa WHERE id = ?", Boolean.class, empresaId))) {
            return;
        }
        String estado = estadoTenant(empresaId);
        if (estado == null || ESTADOS_BLOQUEADOS.contains(estado)) {
            return;
        }
        Integer activados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM onboarding_pago WHERE empresa_id = ? AND estado = 'ACTIVADO'",
                Integer.class, empresaId);
        if (activados != null && activados > 0) {
            return;
        }
        List<PagoPendiente> pendientes = jdbc.query(
                "SELECT event_id, plan_codigo, referencia FROM onboarding_pago "
                        + "WHERE empresa_id = ? AND estado = 'PENDIENTE_EMAIL' "
                        + "ORDER BY created_at ASC FOR UPDATE SKIP LOCKED",
                (rs, rowNum) -> new PagoPendiente(rs.getString("event_id"),
                        rs.getString("plan_codigo"), rs.getString("referencia")), empresaId);
        if (pendientes.isEmpty()) {
            return;
        }
        PagoPendiente primero = pendientes.get(0);
        suscripciones.cambiarPlan(empresaId, primero.planCode(), proveedor(primero.referencia()));
        jdbc.update("UPDATE empresa SET estado_tenant = ?, plan_codigo = ?, tenant_activado_at = ? WHERE id = ?",
                ESTADO_ACTIVO, primero.planCode(), OffsetDateTime.now(), empresaId);
        jdbc.update("UPDATE onboarding_pago SET estado = 'ACTIVADO', activado_at = ? "
                + "WHERE empresa_id = ? AND estado = 'PENDIENTE_EMAIL'", OffsetDateTime.now(), empresaId);
        auditoria.registrarEnEmpresa(empresaId, "PAGO", "TENANT_ACTIVADO", "empresa", empresaId,
                "Plan " + primero.planCode() + " · referencia " + primero.referencia());
    }

    public String estadoTenant(Long empresaId) {
        try {
            return jdbc.queryForObject("SELECT estado_tenant FROM empresa WHERE id = ?", String.class, empresaId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean emailVerificado(Long empresaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT email_verificado_at IS NOT NULL FROM empresa WHERE id = ?", Boolean.class, empresaId));
    }

    private static String proveedor(String referencia) {
        String valor = referencia == null ? "" : referencia.toUpperCase();
        if (valor.startsWith("WOMPI") || valor.contains("WOMPI")) {
            return "WOMPI";
        }
        if (valor.contains("MERCADOPAGO") || valor.startsWith("MP-")) {
            return "MERCADOPAGO";
        }
        return "PAGO";
    }

    private static String normalizarNit(String nit) {
        if (nit == null) {
            return "";
        }
        return nit.trim().replaceAll("[\\s.\\-]", "").toUpperCase();
    }

    private record PagoPendiente(String eventId, String planCode, String referencia) {
    }
}
