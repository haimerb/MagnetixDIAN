package com.magnetixdian.application.pago;

import com.magnetixdian.domain.pago.PagoAprobadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProvisioningListener.class);

    @EventListener
    public void alPagoAprobado(PagoAprobadoEvent evento) {
        LOG.info("Provisionamiento de plan solicitado empresa={} plan={} referencia={}",
                evento.empresaId(), evento.planCode(), evento.referencia());
    }
}
