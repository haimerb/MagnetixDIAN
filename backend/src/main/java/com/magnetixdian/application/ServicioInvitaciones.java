package com.magnetixdian.application;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Set;

@Service
public class ServicioInvitaciones {

    private static final int DIAS_TOKEN = 7;
    private static final Set<String> ROLES_PERMITIDOS = Set.of("ADMIN", "CONTADOR", "VENDEDOR", "LECTOR");
    private static final Set<String> ESTADOS_BLOQUEADOS = Set.of("SUSPENDIDO", "CANCELADO");

    private final UsuarioRepository usuarioRepo;
    private final EmpresaRepository empresaRepo;
    private final RoleRepository roleRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final NotificadorEmail notificador;
    private final AuditoriaService auditoria;
    private final ServicioRefreshToken refreshTokenService;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    @Value("${magnetixdian.app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public ServicioInvitaciones(UsuarioRepository usuarioRepo,
                                EmpresaRepository empresaRepo,
                                RoleRepository roleRepo,
                                PasswordResetTokenRepository tokenRepo,
                                PasswordEncoder passwordEncoder,
                                NotificadorEmail notificador,
                                AuditoriaService auditoria,
                                ServicioRefreshToken refreshTokenService,
                                JdbcTemplate jdbc) {
        this.usuarioRepo = usuarioRepo;
        this.empresaRepo = empresaRepo;
        this.roleRepo = roleRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.refreshTokenService = refreshTokenService;
        this.jdbc = jdbc;
    }

    @Transactional
    public InvitacionTenantResponse invitar(Long empresaId, InvitacionTenantRequest request) {
        String rol = request.rol() == null ? "" : request.rol().trim().toUpperCase();
        if (!ROLES_PERMITIDOS.contains(rol)) {
            throw new IllegalArgumentException("Rol de invitación no soportado: " + request.rol());
        }
        EstadoTenant estado = estadoTenant(empresaId);
        if (estado == null) {
            throw new IllegalArgumentException("Empresa no encontrada.");
        }
        if (ESTADOS_BLOQUEADOS.contains(estado.estado())) {
            throw new IllegalStateException("La empresa no permite invitaciones en su estado actual.");
        }
        String email = request.email().trim().toLowerCase();
        if (usuarioRepo.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + email + ".");
        }
        RoleJpa role = roleRepo.findByCode("ROLE_" + rol)
                .orElseThrow(() -> new IllegalStateException("No está configurado el rol " + rol + "."));
        EmpresaJpa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));

        String username = usernameDisponible(email);
        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombre(request.nombre().trim());
        usuario.setPasswordHash(passwordEncoder.encode(secretoTemporal()));
        usuario.setEmpresa(empresa);
        usuario.setEnabled(Boolean.FALSE);
        usuario.setInvitacionPendiente(Boolean.TRUE);
        usuario.getRoles().add(role);
        UsuarioJpa guardado = usuarioRepo.save(usuario);

        String token = ServicioVerificacionEmail.generarToken();
        OffsetDateTime expiraAt = OffsetDateTime.now().plusDays(DIAS_TOKEN);
        tokenRepo.save(new PasswordResetTokenJpa(guardado, ServicioVerificacionEmail.hash(token),
                PasswordResetTokenJpa.TIPO_INVITACION, expiraAt));
        jdbc.update("INSERT INTO invitacion_tenant (token_hash, empresa_id, rol, email) "
                        + "VALUES (?, ?, ?, ?) ON CONFLICT (token_hash) DO NOTHING",
                ServicioVerificacionEmail.hash(token), empresaId, rol, email);

        notificador.enviarTexto(email, "Invitación a MagnetixDIAN",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Fuiste invitado a MagnetixDIAN con el rol " + rol + ". "
                        + "Define tu contraseña desde este enlace (vence en " + DIAS_TOKEN + " días):\n\n"
                        + baseUrl() + "/activar?token=" + token + "\n\n"
                        + "Si no esperaba esta invitación, ignore este mensaje.");
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "INVITACION_ENVIADA",
                "usuario", guardado.getId(), email + " · rol " + rol);
        return new InvitacionTenantResponse(empresaId, guardado.getId(), username, email, rol, expiraAt,
                "Invitación enviada.");
    }

    @Transactional
    public AceptarInvitacionResponse aceptar(AceptarInvitacionRequest request) {
        PoliticaPassword.validar(request.password());
        String tokenHash = ServicioVerificacionEmail.hash(request.token());
        PasswordResetTokenJpa registro = tokenRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("La invitación es inválida o ya fue usada."));
        if (!PasswordResetTokenJpa.TIPO_INVITACION.equals(registro.getTipo()) || !registro.vigente()) {
            throw new IllegalArgumentException("La invitación es inválida o venció.");
        }
        Invitacion datos;
        try {
            datos = jdbc.queryForObject(
                    "SELECT empresa_id, rol FROM invitacion_tenant WHERE token_hash = ?",
                    (rs, rowNum) -> new Invitacion(rs.getLong("empresa_id"), rs.getString("rol")), tokenHash);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("La invitación es inválida.");
        }
        if (datos == null) {
            throw new IllegalArgumentException("La invitación es inválida.");
        }
        EstadoTenant estado = estadoTenant(datos.empresaId());
        if (estado == null || ESTADOS_BLOQUEADOS.contains(estado.estado())) {
            throw new IllegalStateException("La empresa no permite aceptar invitaciones en su estado actual.");
        }
        RoleJpa role = roleRepo.findByCode("ROLE_" + datos.rol())
                .orElseThrow(() -> new IllegalStateException("No está configurado el rol " + datos.rol() + "."));
        EmpresaJpa empresa = empresaRepo.findById(datos.empresaId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));

        UsuarioJpa usuario = registro.getUsuario();
        usuario.setEmpresa(empresa);
        usuario.getRoles().clear();
        usuario.getRoles().add(role);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setPasswordActualizadoAt(OffsetDateTime.now());
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setInvitacionPendiente(Boolean.FALSE);
        usuario.setEnabled(Boolean.TRUE);
        usuarioRepo.save(usuario);
        registro.setUsadoAt(OffsetDateTime.now());
        refreshTokenService.revocarTodos(usuario.getId());
        auditoria.registrarEnEmpresa(empresa.getId(), usuario.getUsername(), "INVITACION_ACEPTADA",
                "usuario", usuario.getId(), "Rol " + datos.rol());
        return new AceptarInvitacionResponse(empresa.getId(), usuario.getId(), usuario.getUsername(),
                datos.rol(), "Invitación aceptada.");
    }

    private EstadoTenant estadoTenant(Long empresaId) {
        try {
            return jdbc.queryForObject("SELECT estado_tenant FROM empresa WHERE id = ?",
                    (rs, rowNum) -> new EstadoTenant(rs.getString("estado_tenant")), empresaId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String usernameDisponible(String email) {
        String base = email.substring(0, Math.max(email.indexOf('@'), 1));
        String candidato = base;
        int sufijo = 1;
        while (usuarioRepo.existsByUsername(candidato)) {
            candidato = base + sufijo++;
        }
        return candidato;
    }

    private String secretoTemporal() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String baseUrl() {
        return frontendUrl == null || frontendUrl.isBlank() ? "http://localhost:5173" : frontendUrl;
    }

    private record EstadoTenant(String estado) {
    }

    private record Invitacion(Long empresaId, String rol) {
    }
}
