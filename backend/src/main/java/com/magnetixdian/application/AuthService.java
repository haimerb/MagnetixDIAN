package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.PerfilUsuarioDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Autenticación: valida credenciales, aplica bloqueo por intentos fallidos y
 * emite tokens JWT (access + refresh rotativo).
 */
@Service
public class AuthService {

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRefreshToken refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UsuarioRepository usuarioRepository,
                       ServicioRefreshToken refreshTokenService,
                       PasswordEncoder passwordEncoder,
                       AuditoriaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    public TokenRespuesta login(String username, String password) {
        UsuarioJpa usuario = usuarioRepository.findByUsername(username).orElse(null);
        if (usuario != null && usuario.estaBloqueado()) {
            long minutos = Math.max(1, Duration.between(OffsetDateTime.now(), usuario.getBloqueadoHasta()).toMinutes());
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada por intentos fallidos. Intente de nuevo en " + minutos + " minuto(s).");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            List<String> roles = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();
            if (usuario != null) {
                restablecerIntentos(usuario);
            }
            String access = jwtService.generarAccessToken(username, roles);
            String refresh = refreshTokenService.emitir(usuario);
            auditoria.registrar(username, "LOGIN_OK", "usuario", usuario.getId(), null);
            return new TokenRespuesta(access, refresh, username, roles);
        } catch (AuthenticationException ex) {
            if (usuario != null) {
                registrarIntentoFallido(usuario);
            }
            auditoria.registrar(username, "LOGIN_FALLIDO", "usuario",
                    usuario == null ? null : usuario.getId(), "Credenciales inválidas");
            throw ex;
        }
    }

    public TokenRespuesta refresh(String refreshToken) {
        String username;
        try {
            username = jwtService.extraerUsername(refreshToken);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Token de refresco inválido o vencido.");
        }
        if (username == null || !jwtService.esValido(refreshToken, username)) {
            throw new IllegalArgumentException("Token de refresco inválido o vencido.");
        }
        UsuarioJpa usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(usuario.getEnabled())) {
            throw new IllegalArgumentException("Usuario inactivo.");
        }

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getCode())
                .toList();

        String newRefresh = refreshTokenService.rotar(refreshToken, usuario);
        String access = jwtService.generarAccessToken(username, roles);
        return new TokenRespuesta(access, newRefresh, username, roles);
    }

    public void logout(String refreshToken, String username) {
        refreshTokenService.revocar(refreshToken);
        auditoria.registrar(username, "LOGOUT", "usuario", null, null);
    }

    @Transactional
    public void cambiarPassword(String username, String passwordActual, String passwordNueva) {
        PoliticaPassword.validar(passwordNueva);
        UsuarioJpa usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            auditoria.registrar(username, "PASSWORD_CAMBIO_FALLIDO", "usuario", usuario.getId(),
                    "Contraseña actual incorrecta");
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setPasswordActualizadoAt(OffsetDateTime.now());
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        refreshTokenService.revocarTodos(usuario.getId());
        auditoria.registrar(username, "PASSWORD_CAMBIADO", "usuario", usuario.getId(),
                "Sesiones revocadas");
    }

    @Transactional
    public void registrarIntentoFallido(UsuarioJpa usuario) {
        int intentos = usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos();
        intentos++;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(OffsetDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            usuario.setIntentosFallidos(0);
            auditoria.registrar(usuario.getUsername(), "CUENTA_BLOQUEADA", "usuario", usuario.getId(),
                    "Bloqueo de " + MINUTOS_BLOQUEO + " minutos");
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void restablecerIntentos(UsuarioJpa usuario) {
        if ((usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0)
                || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }
    }

    public record TokenRespuesta(String accessToken, String refreshToken, String username, List<String> roles) {
    }

    public PerfilUsuarioDto perfil(String username) {
        UsuarioJpa usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getCode())
                .toList();

        Long empresaId = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
        String empresaNit = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getNit();
        String empresaRazonSocial = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getRazonSocial();

        return new PerfilUsuarioDto(usuario.getId(), usuario.getUsername(), usuario.getNombre(),
                usuario.getEmail(), roles, empresaId, empresaNit, empresaRazonSocial);
    }
}
