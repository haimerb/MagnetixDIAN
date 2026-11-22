package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MapeoArchivoRepository extends JpaRepository<MapeoArchivoJpa, Long> {

    List<MapeoArchivoJpa> findByEmpresaIdOrderByFormatoDetectadoAscCabeceraOrigenAsc(Long empresaId);

    List<MapeoArchivoJpa> findByEmpresaIdAndFormatoDetectado(Long empresaId, String formatoDetectado);

    Optional<MapeoArchivoJpa> findByEmpresaIdAndFormatoDetectadoAndCabeceraOrigen(
            Long empresaId, String formatoDetectado, String cabeceraOrigen);
}
