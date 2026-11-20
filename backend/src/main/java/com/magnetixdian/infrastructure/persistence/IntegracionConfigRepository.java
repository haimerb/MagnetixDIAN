package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegracionConfigRepository extends JpaRepository<IntegracionConfigJpa, Long> {

    Optional<IntegracionConfigJpa> findByEmpresaIdAndProveedor(Long empresaId, String proveedor);

    Optional<IntegracionConfigJpa> findByWebhookTokenHash(String hash);

    List<IntegracionConfigJpa> findByActivoTrue();
}
