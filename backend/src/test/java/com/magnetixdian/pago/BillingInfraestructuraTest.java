package com.magnetixdian.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.pago.ComprobantePagoDto;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.infrastructure.pago.GeneradorPdfComprobante;
import com.magnetixdian.infrastructure.pago.LectorEventoPagoWebhook;
import com.magnetixdian.interfaces.dto.PagoWebhookEventDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingInfraestructuraTest {

    @Test
    void lectorReconoceCamposAnidadosYNormalizaEstado() {
        String payload = """
                {"data":{"id":"evt-2","reference":"MP-REF","status":"approved",
                "transaction_amount":45000,"currency_id":"COP"}}
                """;
        LectorEventoPagoWebhook lector = new LectorEventoPagoWebhook(new ObjectMapper());

        PagoWebhookEventDto evento = lector.leer(ProveedorPago.MERCADOPAGO, payload);

        assertEquals("evt-2", evento.eventId());
        assertEquals("MP-REF", evento.referencia());
        assertEquals("APPROVED", evento.estado());
        assertEquals(new BigDecimal("45000"), evento.monto());
        assertEquals("COP", evento.moneda());
    }

    @Test
    void generadorPdfProduceDocumentoValido() {
        ComprobantePagoDto comprobante = new ComprobantePagoDto("CP-000001", LocalDate.now(),
                "900123456", "PRO", new BigDecimal("99000"), new BigDecimal("15798"),
                "PAGADO", "/api/pagos/comprobantes/CP-000001/pdf");

        byte[] pdf = new GeneradorPdfComprobante().generar(comprobante);

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.ISO_8859_1));
    }
}
