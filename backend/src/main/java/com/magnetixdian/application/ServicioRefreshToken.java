package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.RefreshTokenJpa;
import com.magnetixdian.infrastructure.persistence.RefreshTokenRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Emisión, rotación y revocación de refresh tokens persistidos por JTI.
 */
@Service
public class ServicioRefreshToken {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;

    public ServicioRefreshToken(RefreshTokenRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String emitir(UsuarioJpa usuario) {
        return emitirCon(usuario, UUID.randomUUID().toString());
    }

    @Transactional
    public String rotar(String refreshToken, UsuarioJpa usuario) {
        String jtiAnterior = jwtService.extraerJti(refreshToken);
        RefreshTokenJpa anterior = repository.findByJti(jtiAnterior)
                .orElseThrow(() -> new IllegalArgumentException("Sesión expirada. Inicie sesión nuevamente."));

        if (!anterior.getUsuario().getId().equals(usuario.getId())) {
            reactivarDeteccion(usuario);
            throw new IllegalArgumentException("Sesión inválida. Inicie sesión nuevamente.");
        }
        if (!anterior.activo()) {
            reactivarDeteccion(usuario);
            throw new IllegalArgumentException("Sesión expirada o revocada. Inicie sesión nuevamente.");
        }

        String nuevoJti = UUID.randomUUID().toString();
        anterior.revocar(nuevoJti);
        repository.save(anterior);
        return emitirCon(usuario, nuevoJti);
    }

    @Transactional
    public void revocar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        repository.findByJti(jwtService.extraerJti(refreshToken)).ifPresent(token -> {
            if (token.activo()) {
                token.revocar(null);
            }
        });
    }

    @Transactional
    public void revocarTodos(Long usuarioId) {
        repository.revocarTodos(usuarioId);
    }

    /**
     * Si se reutiliza un token ya rotado, se asume robo: se revocan todas las
     * sesiones del usuario.
     */
    private void reactivarDeteccion(UsuarioJpa usuario) {
        repository.revocarTodos(usuario.getId());
    }

    private String emitirCon(UsuarioJpa usuario, String jti) {
        OffsetDateTime expira = OffsetDateTime.now().plusNanos(jwtService.refreshExpirationMs() * 1_000_000L);
        repository.save(new RefreshTokenJpa(usuario, jti, expira));
        return jwtService.generarRefreshToken(usuario.getUsername(), jti);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void limpiarExpirados() {
        repository.eliminarExpirados(OffsetDateTime.now().minusDays(7));
    }
}
