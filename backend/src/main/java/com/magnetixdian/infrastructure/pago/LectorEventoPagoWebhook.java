package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.interfaces.dto.PagoWebhookEventDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class LectorEventoPagoWebhook {

    private final ObjectMapper mapper;

    public LectorEventoPagoWebhook(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PagoWebhookEventDto leer(ProveedorPago proveedor, String payload) {
        JsonNode raiz;
        try {
            raiz = mapper.readTree(payload == null ? "" : payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("El cuerpo del webhook no es JSON válido.");
        }
        if (raiz == null || !raiz.isObject()) {
            throw new IllegalArgumentException("El cuerpo del webhook debe ser un objeto JSON.");
        }
        String eventId = texto(raiz, "/eventId", "/event_id", "/id", "/data/transaction/id", "/data/id");
        String referencia = texto(raiz, "/referencia", "/reference", "/external_reference",
                "/data/transaction/reference", "/data/reference", "/data/transaction/external_reference");
        String estado = normalizarEstado(texto(raiz, "/estado", "/status", "/data/transaction/status",
                "/data/status", "/data/transaction/estado"));
        BigDecimal monto = numero(raiz, "/monto", "/amount", "/total", "/transaction_amount",
                "/data/transaction_amount", "/data/transaction/amount", "/data/transaction/monto",
                "/data/transaction/transaction_amount");
        if (monto == null) {
            BigDecimal centavos = numero(raiz, "/amount_in_cents", "/data/transaction/amount_in_cents");
            if (centavos != null) {
                monto = centavos.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
        String moneda = texto(raiz, "/moneda", "/currency", "/currency_id", "/data/transaction/currency",
                "/data/transaction/currency_id", "/data/currency_id");
        return new PagoWebhookEventDto(proveedor.name(), eventId, referencia, estado, monto,
                moneda == null || moneda.isBlank() ? "COP" : moneda.toUpperCase(Locale.ROOT));
    }

    private static String texto(JsonNode raiz, String... rutas) {
        for (String ruta : rutas) {
            JsonNode nodo = raiz.at(ruta);
            if (!nodo.isMissingNode() && !nodo.isNull()) {
                String valor = nodo.asText("");
                if (!valor.isBlank()) {
                    return valor.trim();
                }
            }
        }
        return null;
    }

    private static BigDecimal numero(JsonNode raiz, String... rutas) {
        for (String ruta : rutas) {
            JsonNode nodo = raiz.at(ruta);
            if (nodo.isMissingNode() || nodo.isNull()) {
                continue;
            }
            if (nodo.isNumber()) {
                return nodo.decimalValue();
            }
            String valor = nodo.asText("").trim();
            if (!valor.isBlank()) {
                try {
                    return new BigDecimal(valor);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "PENDIENTE";
        }
        return switch (estado.trim().toLowerCase(Locale.ROOT)) {
            case "approved", "aprobado", "captured" -> "APPROVED";
            case "paid", "pagado" -> "PAID";
            case "pending", "pendiente", "in_process" -> "PENDIENTE";
            case "rejected", "declined", "cancelled", "canceled", "voided", "error", "refunded",
                 "charged_back", "rechazado" -> "RECHAZADO";
            default -> estado.trim().toUpperCase(Locale.ROOT);
        };
    }
}
