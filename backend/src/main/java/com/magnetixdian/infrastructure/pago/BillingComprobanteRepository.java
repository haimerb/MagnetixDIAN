package com.magnetixdian.infrastructure.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BillingComprobanteRepository extends JpaRepository<BillingComprobanteJpa, Long> {

    Optional<BillingComprobanteJpa> findByNumero(String numero);

    Optional<BillingComprobanteJpa> findByReferencia(String referencia);

    @Query(value = "SELECT nextval('billing_comprobante_numero_seq')", nativeQuery = true)
    long siguienteNumero();
}
