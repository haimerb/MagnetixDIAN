package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioPagos;
import com.magnetixdian.application.pago.CheckoutRequestDto;
import com.magnetixdian.application.pago.CheckoutResponseDto;
import com.magnetixdian.application.pago.ComprobantePagoDto;
import com.magnetixdian.application.pago.EntitlementDto;
import com.magnetixdian.application.pago.ServicioCheckoutBilling;
import com.magnetixdian.application.pago.ServicioComprobante;
import com.magnetixdian.application.pago.ServicioEntitlement;
import com.magnetixdian.application.pago.ServicioWebhookBilling;
import com.magnetixdian.application.pago.WebhookResultDto;
import com.magnetixdian.domain.pago.FirmaWebhookException;
import com.magnetixdian.domain.pago.SecretoWebhookAusenteException;
import com.magnetixdian.infrastructure.pago.GeneradorPdfComprobante;
import com.magnetixdian.interfaces.dto.CheckoutRequest;
import com.magnetixdian.interfaces.dto.PagoDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pagos de suscripción y notificaciones (webhooks) de las pasarelas.
 */
@RestController
@RequestMapping("/api")
public class PagoController {

    private final ServicioPagos servicio;
    private final ServicioCheckoutBilling checkoutBilling;
    private final ServicioWebhookBilling webhookBilling;
    private final ServicioComprobante servicioComprobante;
    private final GeneradorPdfComprobante generadorPdf;
    private final ServicioEntitlement servicioEntitlement;
    private final AutorizacionEmpresa autorizacion;

    public PagoController(ServicioPagos servicio,
                          ServicioCheckoutBilling checkoutBilling,
                          ServicioWebhookBilling webhookBilling,
                          ServicioComprobante servicioComprobante,
                          GeneradorPdfComprobante generadorPdf,
                          ServicioEntitlement servicioEntitlement,
                          AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.checkoutBilling = checkoutBilling;
        this.webhookBilling = webhookBilling;
        this.servicioComprobante = servicioComprobante;
        this.generadorPdf = generadorPdf;
        this.servicioEntitlement = servicioEntitlement;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/empresas/{empresaId}/pagos")
    public ResponseEntity<List<PagoDto>> listar(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.listar(empresaId));
    }

    @GetMapping("/empresas/{empresaId}/pagos/{referencia}")
    public ResponseEntity<PagoDto> obtener(@PathVariable Long empresaId,
                                           @PathVariable String referencia,
                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId, referencia));
    }

    @PostMapping("/empresas/{empresaId}/pagos/{referencia}/reconciliar")
    public ResponseEntity<PagoDto> reconciliar(@PathVariable Long empresaId,
                                               @PathVariable String referencia,
                                               Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.reconciliar(empresaId, referencia));
    }

    @PostMapping("/empresas/{empresaId}/pagos/checkout")
    public ResponseEntity<PagoDto> checkout(@PathVariable Long empresaId,
                                            @RequestBody CheckoutRequest request,
                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.iniciar(empresaId, request));
    }

    @PostMapping("/pagos/checkout")
    public ResponseEntity<CheckoutResponseDto> checkoutBilling(@RequestBody CheckoutRequestDto request) {
        return ResponseEntity.ok(checkoutBilling.iniciar(request));
    }

    @PostMapping("/pagos/webhook/{proveedor}")
    public ResponseEntity<?> webhook(@PathVariable String proveedor,
                                     @RequestBody(required = false) String payload,
                                     HttpServletRequest request) {
        try {
            WebhookResultDto resultado = webhookBilling.procesar(proveedor, payload == null ? "" : payload,
                    cabeceras(request));
            return ResponseEntity.ok(resultado);
        } catch (SecretoWebhookAusenteException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (FirmaWebhookException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/comprobantes/{numero}")
    public ResponseEntity<ComprobantePagoDto> comprobante(@PathVariable String numero) {
        return ResponseEntity.ok(servicioComprobante.obtener(numero));
    }

    @GetMapping("/pagos/comprobantes/{numero}/pdf")
    public ResponseEntity<byte[]> comprobantePdf(@PathVariable String numero) {
        ComprobantePagoDto comprobante = servicioComprobante.obtener(numero);
        byte[] pdf = generadorPdf.generar(comprobante);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + comprobante.numero() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/empresas/{empresaId}/billing/entitlement")
    public ResponseEntity<EntitlementDto> entitlement(@PathVariable Long empresaId,
                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioEntitlement.actual(empresaId));
    }

    private Map<String, String> cabeceras(HttpServletRequest request) {
        Map<String, String> cabeceras = new LinkedHashMap<>();
        Enumeration<String> nombres = request.getHeaderNames();
        while (nombres != null && nombres.hasMoreElements()) {
            String nombre = nombres.nextElement();
            cabeceras.put(nombre, request.getHeader(nombre));
        }
        String dataId = request.getParameter("data.id");
        if (dataId != null) {
            cabeceras.put("data.id", dataId);
        }
        return cabeceras;
    }
}
