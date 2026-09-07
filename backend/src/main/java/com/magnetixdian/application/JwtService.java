package com.magnetixdian.application;

import com.magnetixdian.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Emisión y validación de tokens JWT (access + refresh).
 * Incluye el username y los roles del usuario en los claims.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secretBytes());
    }

    public String generarAccessToken(String username, List<String> roles) {
        return construir(username, roles, properties.expirationMs());
    }

    public String generarRefreshToken(String username) {
        return construir(username, List.of(), properties.refreshExpirationMs());
    }

    private String construir(String username, List<String> roles, long expMs) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expMs);
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("roles", roles))
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }

    public String extraerUsername(String token) {
        return claims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        Object roles = claims(token).get("roles");
        return roles instanceof List<?> lista
                ? lista.stream().map(String::valueOf).toList()
                : List.of();
    }

    public boolean esValido(String token, String username) {
        Claims claims = claims(token);
        return claims.getSubject().equals(username) && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}