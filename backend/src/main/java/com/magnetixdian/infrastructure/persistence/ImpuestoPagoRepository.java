package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ImpuestoPagoRepository extends JpaRepository<ImpuestoPagoJpa, Long> {

    @Query("select coalesce(sum(p.valor), 0) from ImpuestoPagoJpa p where p.causacionId = :causacionId")
    BigDecimal sumarPorCausacion(@Param("causacionId") Long causacionId);

    @Query(value = """
            select coalesce(sum(p.valor), 0)
            from impuesto_pago p
            join impuesto_causacion c on c.id = p.causacion_id
            where p.empresa_id = :empresaId
              and c.impuesto_codigo = :impuestoCodigo
              and c.fecha between :desde and :hasta
            """, nativeQuery = true)
    BigDecimal sumarPorEmpresaImpuestoPeriodo(@Param("empresaId") Long empresaId,
                                              @Param("impuestoCodigo") String impuestoCodigo,
                                              @Param("desde") LocalDate desde,
                                              @Param("hasta") LocalDate hasta);

    List<ImpuestoPagoJpa> findByCausacionIdOrderByFechaAscIdAsc(Long causacionId);
}
