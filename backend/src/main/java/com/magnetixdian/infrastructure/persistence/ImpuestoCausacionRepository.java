package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ImpuestoCausacionRepository extends JpaRepository<ImpuestoCausacionJpa, Long> {

    Optional<ImpuestoCausacionJpa> findByEmpresaIdAndOrigenAndOrigenIdAndImpuestoCodigo(
            Long empresaId, String origen, Long origenId, String impuestoCodigo);

    List<ImpuestoCausacionJpa> findByEmpresaIdAndPeriodoOrderByFechaAscIdAsc(Long empresaId, String periodo);

    List<ImpuestoCausacionJpa> findByEmpresaIdAndImpuestoCodigoAndFechaBetweenOrderByFechaAscIdAsc(
            Long empresaId, String impuestoCodigo, LocalDate desde, LocalDate hasta);

    List<ImpuestoCausacionJpa> findByEmpresaIdAndTerceroIdOrderByFechaDescIdDesc(Long empresaId, Long terceroId);
}
