package com.magnetixdian.application.pago;

import com.magnetixdian.infrastructure.pago.BillingWebhookEventJpa;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistradorEventoWebhook {

    private final BillingWebhookEventRepository eventoRepo;

    public RegistradorEventoWebhook(BillingWebhookEventRepository eventoRepo) {
        this.eventoRepo = eventoRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(BillingWebhookEventJpa evento) {
        eventoRepo.saveAndFlush(evento);
    }
}
