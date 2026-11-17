package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ConsecutivoRepository extends JpaRepository<ConsecutivoJpa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsecutivoJpa c where c.empresaId = :empresaId and c.prefijo = :prefijo and c.tipoDocumento = :tipo")
    Optional<ConsecutivoJpa> bloquear(@Param("empresaId") Long empresaId,
                                      @Param("prefijo") String prefijo,
                                      @Param("tipo") String tipo);
}
