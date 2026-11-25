package com.magnetixdian.application.pago;

import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventJpa;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEstadoWebhookBilling {

    private final BillingCheckoutRepository checkoutRepo;
    private final BillingWebhookEventRepository eventoRepo;
    private final ServicioComprobante comprobantes;

    public ServicioEstadoWebhookBilling(BillingCheckoutRepository checkoutRepo,
                                        BillingWebhookEventRepository eventoRepo,
                                        ServicioComprobante comprobantes) {
        this.checkoutRepo = checkoutRepo;
        this.eventoRepo = eventoRepo;
        this.comprobantes = comprobantes;
    }

    @Transactional(readOnly = true)
    public EstadoWebhookPagoDto estado(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La referencia del pago es obligatoria.");
        }
        BillingCheckoutJpa checkout = checkoutRepo.findByReferencia(referencia)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Referencia de pago no encontrada: " + referencia));
        return eventoRepo.findByReferenciaOrderByCreatedAtDesc(referencia).stream()
                .findFirst()
                .map(evento -> estado(checkout, evento))
                .orElseGet(() -> new EstadoWebhookPagoDto(referencia, checkout.getProveedor(),
                        checkout.getEstado(), false, null, null, null,
                        "El webhook todavía no fue recibido."));
    }

    private EstadoWebhookPagoDto estado(BillingCheckoutJpa checkout, BillingWebhookEventJpa evento) {
        String estado = evento.getResultado() == null || evento.getResultado().isBlank()
                ? checkout.getEstado()
                : evento.getResultado();
        String comprobanteNumero = evento.getComprobanteNumero();
        if (comprobanteNumero == null || comprobanteNumero.isBlank()) {
            comprobanteNumero = comprobantes.porReferencia(checkout.getReferencia())
                    .map(ComprobantePagoDto::numero)
                    .orElse(null);
        }
        return new EstadoWebhookPagoDto(checkout.getReferencia(), checkout.getProveedor(), estado,
                evento.isFirmado(), evento.getEventId(), evento.getCreatedAt(), comprobanteNumero,
                evento.getMensaje());
    }
}
