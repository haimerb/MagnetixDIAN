package com.magnetixdian.pago;

import com.magnetixdian.application.ServicioPagos;
import com.magnetixdian.application.pago.CheckoutRequestDto;
import com.magnetixdian.application.pago.CheckoutResponseDto;
import com.magnetixdian.application.pago.EstadoWebhookPagoDto;
import com.magnetixdian.application.pago.ServicioCheckoutBilling;
import com.magnetixdian.application.pago.ServicioComprobante;
import com.magnetixdian.application.pago.ServicioEntitlement;
import com.magnetixdian.application.pago.ServicioEstadoWebhookBilling;
import com.magnetixdian.application.pago.ServicioWebhookBilling;
import com.magnetixdian.infrastructure.pago.GeneradorPdfComprobante;
import com.magnetixdian.interfaces.rest.GlobalExceptionHandler;
import com.magnetixdian.interfaces.rest.PagoController;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PagoControllerBillingTest {

    private ServicioCheckoutBilling checkoutBilling;
    private ServicioEstadoWebhookBilling estadoWebhook;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        checkoutBilling = mock(ServicioCheckoutBilling.class);
        estadoWebhook = mock(ServicioEstadoWebhookBilling.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PagoController(
                        mock(ServicioPagos.class), checkoutBilling, mock(ServicioWebhookBilling.class),
                        estadoWebhook, mock(ServicioComprobante.class), mock(GeneradorPdfComprobante.class),
                        mock(ServicioEntitlement.class), mock(AutorizacionEmpresa.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkoutPublicoDevuelveUrlYPendiente() throws Exception {
        when(checkoutBilling.iniciar(any())).thenReturn(new CheckoutResponseDto(
                "https://checkout.test/p/abc", "WOMPI-REF", new BigDecimal("99000"), "COP", "PENDIENTE"));

        mockMvc.perform(post("/api/pagos/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planCode":"PRO","proveedor":"WOMPI","correo":"cliente@ejemplo.com","empresaId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.test/p/abc"))
                .andExpect(jsonPath("$.referencia").value("WOMPI-REF"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        ArgumentCaptor<CheckoutRequestDto> captor = ArgumentCaptor.forClass(CheckoutRequestDto.class);
        verify(checkoutBilling).iniciar(captor.capture());
        assertEquals("PRO", captor.getValue().planCode());
        assertEquals("WOMPI", captor.getValue().proveedor());
        assertEquals("cliente@ejemplo.com", captor.getValue().correo());
    }

    @Test
    void estadoWebhookExponeFirmaYComprobante() throws Exception {
        when(estadoWebhook.estado("WOMPI-REF")).thenReturn(new EstadoWebhookPagoDto(
                "WOMPI-REF", "WOMPI", "APROBADO", true, "evt-1", OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                "CP-000001", "Pago aprobado."));

        mockMvc.perform(get("/api/pagos/WOMPI-REF/webhook-estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.firmado").value(true))
                .andExpect(jsonPath("$.comprobanteNumero").value("CP-000001"));
    }

    @Test
    void estadoWebhookConReferenciaDesconocidaDevuelveBadRequest() throws Exception {
        when(estadoWebhook.estado("NO-EXISTE")).thenThrow(
                new IllegalArgumentException("Referencia de pago no encontrada: NO-EXISTE"));

        mockMvc.perform(get("/api/pagos/NO-EXISTE/webhook-estado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Referencia de pago no encontrada: NO-EXISTE"));
    }
}
