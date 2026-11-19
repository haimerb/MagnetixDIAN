package com.magnetixdian.application;

import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenJpa;
import com.magnetixdian.infrastructure.persistence.PasswordResetTokenRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.SolicitudResetResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperación de contraseña e invitaciones mediante tokens de un solo uso.
 * El token viaja en el enlace del correo y solo se persiste su hash SHA-256.
 */
@Service
public class ServicioRecuperacion {

    private static final int MINUTOS_RESET = 30;
    private static final int DIAS_INVITACION = 7;

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final ServicioRefreshToken refreshTokenService;
    private final NotificadorEmail notificador;
    private final AuditoriaService auditoria;
    private final SecureRandom random = new SecureRandom();

    @Value("${magnetixdian.app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${magnetixdian.auth.expose-token:true}")
    private boolean exposeToken;

    public ServicioRecuperacion(UsuarioRepository usuarioRepo,
                                PasswordResetTokenRepository tokenRepo,
                                PasswordEncoder passwordEncoder,
                                ServicioRefreshToken refreshTokenService,
                                NotificadorEmail notificador,
                                AuditoriaService auditoria) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    @Transactional
    public SolicitudResetResponse solicitarReset(String identificador) {
        Optional<UsuarioJpa> encontrado = usuarioRepo.findByUsername(identificador)
                .or(() -> usuarioRepo.findByEmailIgnoreCase(identificador));
        String mensaje = "Si el usuario existe, enviamos un enlace de recuperación a su correo.";

        if (encontrado.isEmpty() || !Boolean.TRUE.equals(encontrado.get().getEnabled())) {
            return new SolicitudResetResponse(mensaje, null);
        }

        UsuarioJpa usuario = encontrado.get();
        String token = generarToken();
        tokenRepo.invalidarPendientes(usuario.getId(), PasswordResetTokenJpa.TIPO_RESET);
        tokenRepo.save(new PasswordResetTokenJpa(usuario, hash(token), PasswordResetTokenJpa.TIPO_RESET,
                OffsetDateTime.now().plusMinutes(MINUTOS_RESET)));

        String enlace = frontendUrl + "/restablecer?token=" + token;
        boolean enviado = notificador.enviarTexto(usuario.getEmail(),
                "Restablezca su contraseña — MagnetixDIAN",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Recibimos una solicitud para restablecer su contraseña. "
                        + "El enlace vence en " + MINUTOS_RESET + " minutos y solo puede usarse una vez:\n\n"
                        + enlace + "\n\n"
                        + "Si no solicitó este cambio, ignore este mensaje.");

        auditoria.registrar(usuario.getUsername(), "PASSWORD_RESET_SOLICITADO", "usuario",
                usuario.getId(), enviado ? "Correo enviado" : "Correo no enviado (SMTP deshabilitado)");
        return new SolicitudResetResponse(mensaje, exposeToken && !enviado ? enlace : null);
    }

    @Transactional
    public void restablecer(String token, String passwordNueva) {
        PoliticaPassword.validar(passwordNueva);
        PasswordResetTokenJpa registro = tokenVigente(token, PasswordResetTokenJpa.TIPO_RESET);
        UsuarioJpa usuario = registro.getUsuario();
        aplicarPassword(usuario, passwordNueva);
        registro.setUsadoAt(OffsetDateTime.now());
        refreshTokenService.revocarTodos(usuario.getId());
        auditoria.registrar(usuario.getUsername(), "PASSWORD_RESET_CONFIRMADO", "usuario", usuario.getId(), null);
    }

    @Transactional
    public void aceptarInvitacion(String token, String passwordNueva) {
        PoliticaPassword.validar(passwordNueva);
        PasswordResetTokenJpa registro = tokenVigente(token, PasswordResetTokenJpa.TIPO_INVITACION);
        UsuarioJpa usuario = registro.getUsuario();
        aplicarPassword(usuario, passwordNueva);
        usuario.setInvitacionPendiente(Boolean.FALSE);
        usuario.setEnabled(Boolean.TRUE);
        registro.setUsadoAt(OffsetDateTime.now());
        refreshTokenService.revocarTodos(usuario.getId());
        auditoria.registrar(usuario.getUsername(), "INVITACION_ACEPTADA", "usuario", usuario.getId(), null);
    }

    @Transactional
    public String crearInvitacion(UsuarioJpa usuario) {
        String token = generarToken();
        tokenRepo.invalidarPendientes(usuario.getId(), PasswordResetTokenJpa.TIPO_INVITACION);
        tokenRepo.save(new PasswordResetTokenJpa(usuario, hash(token), PasswordResetTokenJpa.TIPO_INVITACION,
                OffsetDateTime.now().plusDays(DIAS_INVITACION)));

        String enlace = frontendUrl + "/activar?token=" + token;
        boolean enviado = notificador.enviarTexto(usuario.getEmail(),
                "Active su cuenta — MagnetixDIAN",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Su cuenta fue creada en MagnetixDIAN. Defina su contraseña desde este enlace "
                        + "(vence en " + DIAS_INVITACION + " días):\n\n"
                        + enlace + "\n\n"
                        + "Si no esperaba esta invitación, ignore este mensaje.");

        auditoria.registrar(usuario.getUsername(), "INVITACION_ENVIADA", "usuario", usuario.getId(),
                enviado ? "Correo enviado" : "Correo no enviado (SMTP deshabilitado)");
        return exposeToken && !enviado ? enlace : null;
    }

    private PasswordResetTokenJpa tokenVigente(String token, String tipo) {
        PasswordResetTokenJpa registro = tokenRepo.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace es inválido o ya fue usado."));
        if (!tipo.equals(registro.getTipo()) || !registro.vigente()) {
            throw new IllegalArgumentException("El enlace es inválido o venció. Solicite uno nuevo.");
        }
        return registro;
    }

    private void aplicarPassword(UsuarioJpa usuario, String password) {
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setPasswordActualizadoAt(OffsetDateTime.now());
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
