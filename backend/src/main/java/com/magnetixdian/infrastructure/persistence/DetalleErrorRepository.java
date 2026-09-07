package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleErrorRepository extends JpaRepository<DetalleErrorJpa, Long> {

    List<DetalleErrorJpa> findByValidacionIdOrderByLineaAsc(Long validacionId);
}