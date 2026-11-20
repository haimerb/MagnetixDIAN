package com.magnetixdian.domain.pago;

import java.util.Optional;

public interface ConsultorPagoRemoto {

    String proveedor();

    boolean configurado();

    Optional<EventoPago> consultar(String referencia);
}
