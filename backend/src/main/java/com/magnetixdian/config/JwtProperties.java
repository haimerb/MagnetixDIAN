package com.magnetixdian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

/**
 * Propiedades de JWT (secret, expiración) cargadas desde application.yml.
 */
@ConfigurationProperties(prefix = "magnetixdian.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs
) {

    public byte[] secretBytes() {
        return Base64.getDecoder().decode(secret);
    }
}