package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificadoImpuestoRepository extends JpaRepository<CertificadoImpuestoJpa, Long> {

    Optional<CertificadoImpuestoJpa> findByEmpresaIdAndTerceroIdAndImpuestoCodigoAndAnioAndNumero(
            Long empresaId, Long terceroId, String impuestoCodigo, Integer anio, String numero);

    List<CertificadoImpuestoJpa> findByEmpresaIdAndAnioOrderByFechaEmisionDescIdDesc(Long empresaId, Integer anio);
}
