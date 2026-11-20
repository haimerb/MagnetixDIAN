package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IntegracionEventoRepository extends JpaRepository<IntegracionEventoJpa, Long> {

    boolean existsByEmpresaIdAndProveedorAndExternalIdAndTipo(
            Long empresaId, String proveedor, String externalId, String tipo);

    Optional<IntegracionEventoJpa> findByEmpresaIdAndProveedorAndExternalIdAndTipo(
            Long empresaId, String proveedor, String externalId, String tipo);

    List<IntegracionEventoJpa> findByEstadoAndProximoIntentoBefore(
            String estado, OffsetDateTime fecha, Pageable pageable);

    List<IntegracionEventoJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId, Pageable pageable);

    Optional<IntegracionEventoJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
