package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<FacturaJpa, Long> {

    List<FacturaJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    Optional<FacturaJpa> findByEmpresaIdAndNumeroCompleto(Long empresaId, String numeroCompleto);

    List<FacturaJpa> findByEstado(String estado);
}
