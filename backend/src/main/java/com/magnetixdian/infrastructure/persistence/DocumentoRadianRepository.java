package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentoRadianRepository extends JpaRepository<DocumentoRadianJpa, Long> {

    Optional<DocumentoRadianJpa> findByEmpresaIdAndDireccionAndCufeCude(
            Long empresaId, String direccion, String cufeCude);

    Optional<DocumentoRadianJpa> findByIdAndEmpresaId(Long id, Long empresaId);

    List<DocumentoRadianJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);

    @Query("select d from DocumentoRadianJpa d where d.empresaId = :empresaId "
            + "and (:direccion is null or d.direccion = :direccion) "
            + "and (:tipoDocumento is null or d.tipoDocumento = :tipoDocumento) "
            + "and (:estadoCausacion is null or d.estadoCausacion = :estadoCausacion) "
            + "and (:fechaDesde is null or d.fecha >= :fechaDesde) "
            + "and (:fechaHasta is null or d.fecha <= :fechaHasta) "
            + "order by d.fecha desc, d.id desc")
    List<DocumentoRadianJpa> buscar(
            @Param("empresaId") Long empresaId,
            @Param("direccion") String direccion,
            @Param("tipoDocumento") String tipoDocumento,
            @Param("estadoCausacion") String estadoCausacion,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);
}
