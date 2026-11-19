package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<FacturaJpa, Long> {

    List<FacturaJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    Page<FacturaJpa> findByEmpresaId(Long empresaId, Pageable pageable);

    Optional<FacturaJpa> findByEmpresaIdAndNumeroCompleto(Long empresaId, String numeroCompleto);

    List<FacturaJpa> findByEstado(String estado);

    @Query("select f from FacturaJpa f where f.tipoDocumento = '01' and f.estado = 'ACEPTADO' "
            + "and f.fechaEmision <= :limite order by f.fechaEmision asc")
    List<FacturaJpa> candidatasAceptacionTacita(@Param("limite") LocalDate limite);

    @Query("select count(f) from FacturaJpa f where f.empresa.id = :empresaId "
            + "and f.estado <> 'BORRADOR' and f.fechaEmision >= :desde and f.fechaEmision < :hasta")
    long contarEmitidasEntre(@Param("empresaId") Long empresaId, @Param("desde") LocalDate desde,
                             @Param("hasta") LocalDate hasta);
}
