package com.magnetixdian.application;

import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenJpa;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.ReenviarVerificacionResponse;
import com.magnetixdian.interfaces.dto.VerificacionEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ServicioVerificacionEmail {

    public static final String TIPO = "EMAIL_VERIFICACION";
    private static final int HORAS_TOKEN = 24;

    private final UsuarioRepository usuarioRepo;
    private final EmpresaRepository empresaRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final NotificadorEmail notificador;
    private final AuditoriaService auditoria;
    private final ServicioOnboarding onboarding;
    private final JdbcTemplate jdbc;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${magnetixdian.app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${magnetixdian.auth.expose-token:false}")
    private boolean exposeToken;

    @Value("${magnetixdian.onboarding.reenvio-segundos:60}")
    private long reenvioSegundos = 60;

    public ServicioVerificacionEmail(UsuarioRepository usuarioRepo,
                                     EmpresaRepository empresaRepo,
                                     PasswordResetTokenRepository tokenRepo,
                                     NotificadorEmail notificador,
                                     AuditoriaService auditoria,
                                     ServicioOnboarding onboarding,
                                     JdbcTemplate jdbc) {
        this.usuarioRepo = usuarioRepo;
        this.empresaRepo = empresaRepo;
        this.tokenRepo = tokenRepo;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.onboarding = onboarding;
        this.jdbc = jdbc;
    }

    @Transactional
    public void enviarTokenVerificacion(Long empresaId, String email) {
        UsuarioJpa usuario = usuarioRepo.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario del registro."));
        validarEmpresa(usuario, empresaId);
        enviarCorreo(usuario, emitirToken(usuario));
    }

    @Transactional
    public VerificacionEmailResponse verificar(String token) {
        PasswordResetTokenJpa registro = tokenVigente(token);
        UsuarioJpa usuario = registro.getUsuario();
        Long empresaId = usuario.getEmpresa().getId();
        registro.setUsadoAt(OffsetDateTime.now());
        jdbc.update("UPDATE empresa SET email_verificado_at = ? WHERE id = ? AND email_verificado_at IS NULL",
                OffsetDateTime.now(), empresaId);
        auditoria.registrarEnEmpresa(empresaId, usuario.getUsername(), "EMAIL_VERIFICADO", "empresa",
                empresaId, usuario.getEmail());
        onboarding.activarSiPagoAprobado(empresaId);
        return new VerificacionEmailResponse(empresaId, onboarding.estadoTenant(empresaId), true,
                "Correo verificado correctamente.");
    }

    @Transactional
    public ReenviarVerificacionResponse reenviar(String email) {
        String correo = email == null ? "" : email.trim().toLowerCase();
        UsuarioJpa usuario = usuarioRepo.findByEmailIgnoreCase(correo).orElse(null);
        if (usuario == null) {
            return new ReenviarVerificacionResponse(false,
                    "Si la cuenta existe, se envió un nuevo enlace de verificación.", null);
        }
        if (usuario.getEmpresa() == null) {
            return new ReenviarVerificacionResponse(false, "La cuenta no tiene una empresa asociada.", null);
        }
        if (onboarding.emailVerificado(usuario.getEmpresa().getId())) {
            return new ReenviarVerificacionResponse(false, "El correo ya fue verificado.", null);
        }
        verificarIntervalo(usuario.getId());
        String token = emitirToken(usuario);
        boolean enviado = enviarCorreo(usuario, token);
        return new ReenviarVerificacionResponse(enviado,
                "Si la cuenta existe, se envió un nuevo enlace de verificación.",
                exposeToken && !enviado ? token : null);
    }

    private String emitirToken(UsuarioJpa usuario) {
        String token = generarToken();
        tokenRepo.invalidarPendientes(usuario.getId(), TIPO);
        tokenRepo.save(new PasswordResetTokenJpa(usuario, hash(token), TIPO,
                OffsetDateTime.now().plusHours(HORAS_TOKEN)));
        auditoria.registrar(usuario.getUsername(), "EMAIL_VERIFICACION_ENVIADA", "usuario",
                usuario.getId(), "Enlace de verificación generado");
        return token;
    }

    private boolean enviarCorreo(UsuarioJpa usuario, String token) {
        return notificador.enviarTexto(usuario.getEmail(), "Verifique su correo — MagnetixDIAN",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Verifique su correo para activar su empresa en MagnetixDIAN. "
                        + "El enlace vence en " + HORAS_TOKEN + " horas y solo puede usarse una vez:\n\n"
                        + enlace(token) + "\n\n"
                        + "Si no solicitó este registro, ignore este mensaje.");
    }

    private void verificarIntervalo(Long usuarioId) {
        Timestamp ultimo;
        try {
            ultimo = jdbc.queryForObject(
                    "SELECT created_at FROM password_reset_token "
                            + "WHERE usuario_id = ? AND tipo = ? ORDER BY created_at DESC LIMIT 1",
                    Timestamp.class, usuarioId, TIPO);
        } catch (EmptyResultDataAccessException ex) {
            return;
        }
        if (ultimo != null) {
            long segundos = reenvioSegundos <= 0 ? 60 : reenvioSegundos;
            Duration transcurrido = Duration.between(ultimo.toInstant(), Instant.now());
            if (transcurrido.compareTo(Duration.ofSeconds(segundos)) < 0) {
                throw new IllegalArgumentException(
                        "Debe esperar " + segundos + " segundos antes de reenviar el enlace.");
            }
        }
    }

    private void validarEmpresa(UsuarioJpa usuario, Long empresaId) {
        EmpresaJpa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        if (usuario.getEmpresa() == null || !empresaId.equals(usuario.getEmpresa().getId())) {
            throw new IllegalArgumentException("El usuario no pertenece a la empresa indicada.");
        }
        if (onboarding.emailVerificado(empresa.getId())) {
            throw new IllegalStateException("El correo de esta empresa ya fue verificado.");
        }
    }

    private PasswordResetTokenJpa tokenVigente(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El enlace es inválido.");
        }
        PasswordResetTokenJpa registro = tokenRepo.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace es inválido o ya fue usado."));
        if (!TIPO.equals(registro.getTipo()) || !registro.vigente()) {
            throw new IllegalArgumentException("El enlace es inválido o venció. Solicite uno nuevo.");
        }
        return registro;
    }

    private String enlace(String token) {
        String base = frontendUrl == null || frontendUrl.isBlank() ? "http://localhost:5173" : frontendUrl;
        return base + "/verificar-email?token=" + token;
    }

    public static String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
