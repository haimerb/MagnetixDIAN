package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AsientoPendienteRepository extends JpaRepository<AsientoPendienteJpa, Long> {

    List<AsientoPendienteJpa> findByEstadoOrderByCreatedAtAsc(String estado);

    boolean existsByEmpresaIdAndOrigenAndOrigenIdAndTipoEventoAndEstadoIn(
            Long empresaId, String origen, Long origenId, String tipoEvento, Collection<String> estados);
}
