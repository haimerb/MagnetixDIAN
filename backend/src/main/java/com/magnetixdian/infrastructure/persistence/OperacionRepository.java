package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperacionRepository extends JpaRepository<OperacionJpa, Long> {

    List<OperacionJpa> findByMedioMagneticoId(Long medioMagneticoId);

    long countByMedioMagneticoId(Long medioMagneticoId);

    /**
     * Eliminación masiva con flush automático: garantiza que las filas se
     * borran en BD antes de insertar las nuevas (evita colisión de la
     * constraint única (medio, linea) en recargas).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from OperacionJpa o where o.medioMagnetico.id = :medioId")
    void borrarPorMedio(@Param("medioId") Long medioId);
}