package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaEventoRepository extends JpaRepository<FacturaEventoJpa, Long> {

    List<FacturaEventoJpa> findByFacturaIdOrderByFechaDesc(Long facturaId);
}
