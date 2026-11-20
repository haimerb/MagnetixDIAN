package com.magnetixdian.domain.integracion;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

public interface AdaptadorIntegracion {

    ProveedorIntegracion proveedor();

    Set<CapacidadIntegracion> capacidades();

    boolean configurada(DatosConexionIntegracion datos);

    ResultadoIntegracion probarConexion(DatosConexionIntegracion datos);

    default ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        return ResultadoIntegracion.noSoportado(proveedor(), CapacidadIntegracion.SYNC_CLIENTES);
    }

    default ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        return ResultadoIntegracion.noSoportado(proveedor(), CapacidadIntegracion.SYNC_PRODUCTOS);
    }

    default ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        return ResultadoIntegracion.noSoportado(proveedor(), CapacidadIntegracion.EMITIR_FACTURA);
    }

    default ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        return ResultadoIntegracion.noSoportado(proveedor(), CapacidadIntegracion.RECIBIR_WEBHOOK);
    }

    default boolean verificarFirma(DatosConexionIntegracion datos, EventoIntegracion evento) {
        String secreto = datos.credencial("webhookSecret");
        String firma = evento.headers() == null ? null : evento.headers().get("X-Integracion-Firma");
        String timestamp = evento.headers() == null ? null : evento.headers().get("X-Integracion-Timestamp");
        if (secreto == null || secreto.isBlank() || firma == null || timestamp == null) {
            return false;
        }
        if (!timestampValido(timestamp)) {
            return false;
        }
        String esperado = hmacSha256Hex(secreto, timestamp + "." + evento.payload());
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                firma.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean timestampValido(String timestamp) {
        try {
            long epoch = Long.parseLong(timestamp);
            return Math.abs(Instant.now().getEpochSecond() - epoch) <= 300;
        } catch (NumberFormatException ex) {
            try {
                Instant valor = Instant.parse(timestamp);
                return Math.abs(Instant.now().getEpochSecond() - valor.getEpochSecond()) <= 300;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
    }

    private static String hmacSha256Hex(String secreto, String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }
}
