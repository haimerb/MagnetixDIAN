package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EnvioFacturaRepository extends JpaRepository<EnvioFacturaJpa, Long> {

    Optional<EnvioFacturaJpa> findByFacturaId(Long facturaId);

    Page<EnvioFacturaJpa> findByEstado(String estado, Pageable pageable);

    List<EnvioFacturaJpa> findTop20ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(
            String estado, OffsetDateTime momento);
}
