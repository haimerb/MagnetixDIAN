package com.magnetixdian.infrastructure.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CifradorAes {

    public static final String PREFIJO = "enc:";
    public static final String PREFIJO_V2 = "enc:v2:";
    public static final String CLAVE_DEV = "magnetixdian-dev-crypto-key-change-me";

    private static final Logger log = LoggerFactory.getLogger(CifradorAes.class);
    private static final int IV_LONGITUD = 12;
    private static final int TAG_BITS = 128;
    private static final String KEY_ID_DEFAULT = "primary";
    private static final Set<String> CLAVES_EJEMPLO = Set.of(
            CLAVE_DEV,
            "cambiar",
            "change-me",
            "changeme",
            "replace-me",
            "example",
            "secret",
            "base64-de-32-bytes");

    private static volatile CifradorAes instancia;

    private final SecretKeySpec clave;
    private final Map<String, SecretKeySpec> claves;
    private final String keyId;
    private final SecureRandom random = new SecureRandom();

    public CifradorAes(String secreto) {
        this(secreto, "", false, KEY_ID_DEFAULT);
    }

    public CifradorAes(String secreto, String clavesExtra, boolean failFast) {
        this(secreto, clavesExtra, failFast, KEY_ID_DEFAULT);
    }

    @Autowired
    public CifradorAes(@Value("${magnetixdian.crypto.key:magnetixdian-dev-crypto-key-change-me}") String secreto,
                       @Value("${magnetixdian.crypto.extra-keys:}") String clavesExtra,
                       @Value("${magnetixdian.seguridad.fail-fast:false}") boolean failFast,
                       @Value("${magnetixdian.crypto.key-id:primary}") String keyId) {
        if (failFast && esClaveDeEjemplo(secreto)) {
            throw new IllegalStateException("Se requiere una clave de cifrado segura cuando fail-fast está activo");
        }
        String identificador = keyId == null || keyId.isBlank() ? KEY_ID_DEFAULT : keyId.trim();
        if (failFast && (keyId == null || keyId.isBlank())) {
            throw new IllegalStateException("Se requiere CRYPTO_KEY_ID cuando fail-fast está activo");
        }
        String claveActiva = secreto == null ? "" : secreto;
        Map<String, SecretKeySpec> disponibles = new HashMap<>();
        disponibles.put(identificador, new SecretKeySpec(sha256(claveActiva), "AES"));
        agregarClavesExtra(clavesExtra, disponibles, identificador, failFast);
        this.claves = Map.copyOf(disponibles);
        this.clave = this.claves.get(identificador);
        this.keyId = identificador;
        instancia = this;
    }

    public static CifradorAes instancia() {
        return instancia;
    }

    public String keyId() {
        return keyId;
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
            return PREFIJO_V2 + keyId + ":" + Base64.getEncoder().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el valor", e);
        }
    }

    public String descifrar(String valor) {
        if (!estaCifrado(valor)) {
            return valor;
        }
        try {
            if (valor.startsWith(PREFIJO_V2)) {
                return descifrarV2(valor);
            }
            return descifrarV1(valor.substring(PREFIJO.length()));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el valor", e);
        }
    }

    private String descifrarV2(String valor) throws Exception {
        String[] partes = valor.substring(PREFIJO_V2.length()).split(":", 2);
        if (partes.length != 2 || partes[0].isBlank() || partes[1].isBlank()) {
            throw new IllegalStateException("Sobre cifrado v2 inválido");
        }
        SecretKeySpec seleccionada = claves.get(partes[0]);
        if (seleccionada == null) {
            log.warn("Sobre cifrado v2 con identificador de clave no registrado");
            throw new IllegalStateException("Identificador de clave desconocido");
        }
        byte[] datos = Base64.getDecoder().decode(partes[1]);
        return descifrarCon(seleccionada, datos);
    }

    /**
     * Los valores legacy {@code enc:} no llevan identificador de clave. Durante una rotación
     * se prueba la clave activa y, a continuación, las claves anteriores registradas.
     */
    private String descifrarV1(String codificado) throws Exception {
        byte[] datos = Base64.getDecoder().decode(codificado);
        for (SecretKeySpec candidata : clavesParaLegacy()) {
            try {
                return descifrarCon(candidata, datos);
            } catch (Exception ignorada) {
                // Se prueba la siguiente clave disponible; nunca se registra el valor.
            }
        }
        throw new IllegalStateException("No se pudo descifrar el valor cifrado");
    }

    private List<SecretKeySpec> clavesParaLegacy() {
        List<SecretKeySpec> candidatas = new ArrayList<>();
        candidatas.add(clave);
        claves.forEach((id, secrets) -> {
            if (!id.equals(keyId)) {
                candidatas.add(secrets);
            }
        });
        return candidatas;
    }

    private String descifrarCon(SecretKeySpec seleccionada, byte[] datos) throws Exception {
        if (datos.length <= IV_LONGITUD) {
            throw new IllegalStateException("Sobre cifrado inválido");
        }
        byte[] iv = new byte[IV_LONGITUD];
        System.arraycopy(datos, 0, iv, 0, IV_LONGITUD);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, seleccionada, new GCMParameterSpec(TAG_BITS, iv));
        byte[] plano = cipher.doFinal(datos, IV_LONGITUD, datos.length - IV_LONGITUD);
        return new String(plano, StandardCharsets.UTF_8);
    }

    private void agregarClavesExtra(String clavesExtra, Map<String, SecretKeySpec> destino,
                                    String identificadorActivo, boolean failFast) {
        if (clavesExtra == null || clavesExtra.isBlank()) {
            return;
        }
        Set<String> identificadores = new LinkedHashSet<>(destino.keySet());
        for (String entrada : clavesExtra.split(",")) {
            String par = entrada.trim();
            if (par.isEmpty()) {
                continue;
            }
            int separador = par.indexOf('=');
            if (separador < 0) {
                separador = par.indexOf(':');
            }
            if (separador <= 0 || separador >= par.length() - 1) {
                if (failFast) {
                    throw new IllegalStateException("Entrada de CRYPTO_EXTRA_KEYS inválida");
                }
                continue;
            }
            String identificador = par.substring(0, separador).trim();
            String secreto = par.substring(separador + 1).trim();
            if (identificador.isEmpty() || secreto.isEmpty() || esClaveDeEjemplo(secreto)) {
                if (failFast) {
                    throw new IllegalStateException("Entrada de CRYPTO_EXTRA_KEYS incompleta o insegura");
                }
                continue;
            }
            if (!identificadores.add(identificador)) {
                if (failFast) {
                    throw new IllegalStateException("Identificador de clave duplicado en CRYPTO_EXTRA_KEYS");
                }
                continue;
            }
            if (!identificador.equals(identificadorActivo)) {
                destino.put(identificador, new SecretKeySpec(sha256(secreto), "AES"));
            }
        }
    }

    private static boolean esClaveDeEjemplo(String valor) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return CLAVES_EJEMPLO.stream().anyMatch(ejemplo -> normalizado.equals(ejemplo.toLowerCase(Locale.ROOT)))
                || normalizado.startsWith("magnetixdian-dev-")
                || normalizado.startsWith("base64-de-");
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
