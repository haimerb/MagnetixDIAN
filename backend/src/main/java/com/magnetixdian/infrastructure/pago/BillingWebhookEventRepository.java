package com.magnetixdian.infrastructure.pago;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEventJpa, Long> {

    Optional<BillingWebhookEventJpa> findByProveedorAndEventId(String proveedor, String eventId);

    List<BillingWebhookEventJpa> findByReferenciaOrderByCreatedAtDesc(String referencia);
}
