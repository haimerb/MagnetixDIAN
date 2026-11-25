package com.magnetixdian.infrastructure.integracion.erp.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ErpPedidoRepository extends JpaRepository<ErpPedidoJpa, Long> {
    List<ErpPedidoJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpPedidoJpa> findByIdAndEmpresaId(Long id, Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ErpPedidoJpa p where p.id = :id and p.empresaId = :empresaId")
    Optional<ErpPedidoJpa> findByIdAndEmpresaIdForUpdate(@Param("id") Long id,
                                                          @Param("empresaId") Long empresaId);
}
