package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarioDianRepository extends JpaRepository<CalendarioDianJpa, Long> {

    List<CalendarioDianJpa> findByAnioGravableOrderByFechaLimite(Long anioGravable);
}