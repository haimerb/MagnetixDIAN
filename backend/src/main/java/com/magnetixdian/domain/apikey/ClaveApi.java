package com.magnetixdian.domain.apikey;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generación y verificación de claves de API.
 * Formato: {@code mx_<prefijo(8 hex)><secreto(32 hex)>}. El prefijo permite
 * localizar la clave en BD sin exponer el secreto; solo se persiste su SHA-256.
 */
public final class ClaveApi {

    public static final String MARCA = "mx_";
    private static final int LONGITUD_PREFIJO = 8;
    private static final int LONGITUD_SECRETO = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaveApi() {
    }

    public record Generada(String prefijo, String clave) {
    }

    public static Generada generar() {
        String prefijo = aleatorio(LONGITUD_PREFIJO);
        String secreto = aleatorio(LONGITUD_SECRETO);
        return new Generada(prefijo, MARCA + prefijo + secreto);
    }

    /** Devuelve el prefijo de una clave, o {@code null} si el formato no es válido. */
    public static String prefijoDe(String clave) {
        if (clave == null || !clave.startsWith(MARCA)) {
            return null;
        }
        int inicio = MARCA.length();
        int fin = inicio + LONGITUD_PREFIJO;
        if (clave.length() < fin) {
            return null;
        }
        return clave.substring(inicio, fin);
    }

    public static String hash(String clave) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Comparación en tiempo constante de dos hashes. */
    public static boolean hashCoincide(String esperado, String recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(esperado.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                recibido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String aleatorio(int bytes) {
        byte[] buffer = new byte[bytes / 2];
        RANDOM.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }
}
