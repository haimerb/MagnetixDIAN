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
import java.util.UUID;

/**
 * Emisión y validación de tokens JWT (access + refresh).
 * Incluye el username, los roles y un identificador único (jti) en los claims.
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
        return construir(username, roles, properties.expirationMs(), UUID.randomUUID().toString());
    }

    public String generarRefreshToken(String username, String jti) {
        return construir(username, List.of(), properties.refreshExpirationMs(), jti);
    }

    private String construir(String username, List<String> roles, long expMs, String jti) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expMs);
        return Jwts.builder()
                .id(jti)
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

    public String extraerJti(String token) {
        return claims(token).getId();
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

    public long refreshExpirationMs() {
        return properties.refreshExpirationMs();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
