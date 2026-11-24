package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.evento.EventoDominioErp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosErp {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventosErp.class);

    private final ApplicationEventPublisher publisher;

    public PublicadorEventosErp(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publicar(EventoDominioErp evento) {
        publisher.publishEvent(evento);
        log.info("Evento ERP publicado tipo={} empresaId={}", evento.tipo(), evento.empresaId());
    }
}
