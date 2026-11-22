package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MigracionContableRepository extends JpaRepository<MigracionContableJpa, Long> {

    Optional<MigracionContableJpa> findByIdAndEmpresaIdAndProveedor(Long id, Long empresaId, String proveedor);
}
