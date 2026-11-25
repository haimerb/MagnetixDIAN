package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoriaJpa, Long> {

    List<EventoAuditoriaJpa> findTop50ByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    Page<EventoAuditoriaJpa> findByAccionContainingIgnoreCase(String accion, Pageable pageable);

    Page<EventoAuditoriaJpa> findByUsuarioContainingIgnoreCase(String usuario, Pageable pageable);

    Page<EventoAuditoriaJpa> findByAccionContainingIgnoreCaseAndUsuarioContainingIgnoreCase(
            String accion, String usuario, Pageable pageable);
}
