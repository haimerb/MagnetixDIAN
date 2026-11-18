package com.magnetixdian.infrastructure.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simétrico AES-256-GCM para credenciales sensibles (PIN, API key del
 * PAC y contraseña del certificado). Los valores cifrados se almacenan con el
 * prefijo {@code enc:}; la clave se deriva (SHA-256) de la propiedad
 * {@code magnetixdian.crypto.key}.
 */
@Component
public class CifradorAes {

    public static final String PREFIJO = "enc:";
    private static final int IV_LONGITUD = 12;
    private static final int TAG_BITS = 128;

    private static volatile CifradorAes instancia;

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public CifradorAes(@Value("${magnetixdian.crypto.key:magnetixdian-dev-crypto-key-change-me}") String secreto) {
        this.clave = new SecretKeySpec(sha256(secreto), "AES");
        instancia = this;
    }

    public static CifradorAes instancia() {
        return instancia;
    }

    public boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public String cifrar(String plano) {
        if (plano == null) {
            return null;
        }
        if (estaCifrado(plano)) {
            return plano;
        }
        try {
            byte[] iv = new byte[IV_LONGITUD];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(plano.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, iv.length);
            System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el valor", e);
        }
    }

    public String descifrar(String valor) {
        if (!estaCifrado(valor)) {
            return valor;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            byte[] iv = new byte[IV_LONGITUD];
            System.arraycopy(datos, 0, iv, 0, IV_LONGITUD);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plano = cipher.doFinal(datos, IV_LONGITUD, datos.length - IV_LONGITUD);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el valor", e);
        }
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
