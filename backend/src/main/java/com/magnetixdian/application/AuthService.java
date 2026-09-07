package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Autenticación: valida credenciales y emite tokens JWT (access + refresh).
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    public TokenRespuesta login(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        List<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        String access = jwtService.generarAccessToken(username, roles);
        String refresh = jwtService.generarRefreshToken(username);

        return new TokenRespuesta(access, refresh, username, roles);
    }

    public TokenRespuesta refresh(String refreshToken) {
        String username = jwtService.extraerUsername(refreshToken);
        if (username == null || !jwtService.esValido(refreshToken, username)) {
            throw new IllegalArgumentException("Token de refresco inválido o vencido.");
        }
        UsuarioJpa usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getCode())
                .toList();

        String access = jwtService.generarAccessToken(username, roles);
        String newRefresh = jwtService.generarRefreshToken(username);
        return new TokenRespuesta(access, newRefresh, username, roles);
    }

    public record TokenRespuesta(String accessToken, String refreshToken, String username, List<String> roles) {
    }
}