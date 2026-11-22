package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MigracionContableDetalleRepository extends JpaRepository<MigracionContableDetalleJpa, Long> {

    List<MigracionContableDetalleJpa> findByMigracionIdOrderByCreatedAtAsc(Long migracionId);

    Optional<MigracionContableDetalleJpa> findByEmpresaIdAndProveedorAndTipoAndExternalId(
            Long empresaId, String proveedor, String tipo, String externalId);
}
