package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodoContableRepository extends JpaRepository<PeriodoContableJpa, Long> {

    Optional<PeriodoContableJpa> findByEmpresaIdAndAnioAndMes(Long empresaId, int anio, int mes);

    List<PeriodoContableJpa> findByEmpresaIdOrderByAnioAscMesAsc(Long empresaId);

    List<PeriodoContableJpa> findByEmpresaIdAndEstado(Long empresaId, String estado);

    @Query("select p from PeriodoContableJpa p where p.empresaId = :empresaId " +
            "and p.fechaDesde <= :fecha and p.fechaHasta >= :fecha order by p.fechaDesde desc")
    Optional<PeriodoContableJpa> findVigentePorFecha(@Param("empresaId") Long empresaId,
                                                     @Param("fecha") LocalDate fecha);
}
