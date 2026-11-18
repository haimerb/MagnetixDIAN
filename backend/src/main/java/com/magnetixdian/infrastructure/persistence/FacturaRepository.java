package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<FacturaJpa, Long> {

    List<FacturaJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    Page<FacturaJpa> findByEmpresaId(Long empresaId, Pageable pageable);

    Optional<FacturaJpa> findByEmpresaIdAndNumeroCompleto(Long empresaId, String numeroCompleto);

    List<FacturaJpa> findByEstado(String estado);
}
