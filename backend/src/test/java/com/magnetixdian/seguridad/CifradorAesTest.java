package com.magnetixdian.seguridad;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CifradorAesTest {

    private static final String CLAVE_NUEVA = "clave-nueva-de-32-bytes-1234567890";
    private static final String CLAVE_ANTIGUA = "clave-antigua-de-32-bytes-123456789";
    private static final String PLANO = "NIT-900123456-token-sensible";

    @Test
    void cifrarYDescifrarConClaveActiva() {
        CifradorAes cifrador = new CifradorAes(CLAVE_NUEVA, "", true, "k2026");
        String cifrado = cifrador.cifrar(PLANO);

        assertTrue(cifrado.startsWith(CifradorAes.PREFIJO_V2 + "k2026:"));
        assertFalse(cifrado.contains(PLANO));
        assertFalse(cifrado.contains(CLAVE_NUEVA));
        assertTrue(cifrador.estaCifrado(cifrado));
        assertEquals(PLANO, cifrador.descifrar(cifrado));
        assertEquals(cifrado, cifrador.cifrar(cifrado));
    }

    @Test
    void rotacionDescifraConClavesExtraYCifraConLaActiva() {
        CifradorAes anterior = new CifradorAes(CLAVE_ANTIGUA, "", true, "k2025");
        String cifradoAnterior = anterior.cifrar(PLANO);

        CifradorAes rotado = new CifradorAes(
                CLAVE_NUEVA, "k2025=" + CLAVE_ANTIGUA, true, "k2026");

        assertEquals("k2026", rotado.keyId());
        assertEquals(PLANO, rotado.descifrar(cifradoAnterior));
        String cifradoNuevo = rotado.cifrar(PLANO);
        assertTrue(cifradoNuevo.startsWith(CifradorAes.PREFIJO_V2 + "k2026:"));
        assertEquals(PLANO, rotado.descifrar(cifradoNuevo));
    }

    @Test
    void rotacionDescifraValoresLegacySinIdentificador() throws Exception {
        String legacy = cifrarLegacy(CLAVE_ANTIGUA, PLANO);
        assertTrue(legacy.startsWith(CifradorAes.PREFIJO));
        assertFalse(legacy.startsWith(CifradorAes.PREFIJO_V2));

        CifradorAes rotado = new CifradorAes(
                CLAVE_NUEVA, "k2025:" + CLAVE_ANTIGUA, true, "k2026");
        assertEquals(PLANO, rotado.descifrar(legacy));
    }

    @Test
    void tamperingEsRechazado() {
        CifradorAes cifrador = new CifradorAes(CLAVE_NUEVA, "", true, "k2026");
        String cifrado = cifrador.cifrar(PLANO);
        int indice = CifradorAes.PREFIJO_V2.length() + cifrador.keyId().length() + 1 + 30;
        char reemplazo = cifrado.charAt(indice) == 'A' ? 'B' : 'A';
        String manipulado = cifrado.substring(0, indice) + reemplazo + cifrado.substring(indice + 1);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> cifrador.descifrar(manipulado));
        assertFalse(error.getMessage().contains(PLANO));
        assertEquals(PLANO, cifrador.descifrar(cifrado));
    }

    @Test
    void identificadorDesconocidoEsRechazadoSinFiltrarDatos() {
        CifradorAes cifrador = new CifradorAes(CLAVE_NUEVA, "", true, "k2026");
        Logger logger = (Logger) LoggerFactory.getLogger(CifradorAes.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String sobre = CifradorAes.PREFIJO_V2 + "clave-fantasma:" + Base64.getEncoder()
                    .encodeToString(new byte[32]);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> cifrador.descifrar(sobre));
            assertFalse(error.getMessage().contains("clave-fantasma"));
            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
            assertFalse(logs.contains(PLANO));
            assertFalse(logs.contains(CLAVE_NUEVA));
            assertFalse(logs.contains("clave-fantasma"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void failFastRechazaClavesDeEjemplo() {
        assertThrows(IllegalStateException.class,
                () -> new CifradorAes(CifradorAes.CLAVE_DEV, "", true, "k2026"));
        assertThrows(IllegalStateException.class,
                () -> new CifradorAes(CLAVE_NUEVA, "", true, ""));
        assertThrows(IllegalStateException.class,
                () -> new CifradorAes(CLAVE_NUEVA, "k2025=base64-de-32-bytes", true, "k2026"));
    }

    private static String cifrarLegacy(String secreto, String plano) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] llave = MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(llave, "AES"), new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(plano.getBytes(StandardCharsets.UTF_8));
        byte[] salida = new byte[iv.length + cifrado.length];
        System.arraycopy(iv, 0, salida, 0, iv.length);
        System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
        return CifradorAes.PREFIJO + Base64.getEncoder().encodeToString(salida);
    }
}
