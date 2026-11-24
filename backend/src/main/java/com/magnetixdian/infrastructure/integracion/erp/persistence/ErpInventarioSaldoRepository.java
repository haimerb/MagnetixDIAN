package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpInventarioSaldoRepository extends JpaRepository<ErpInventarioSaldoJpa, Long> {
    Optional<ErpInventarioSaldoJpa> findByEmpresaIdAndBodegaAndProductoId(Long empresaId, String bodega, String productoId);
    List<ErpInventarioSaldoJpa> findByEmpresaIdAndBodegaOrderByProductoIdAsc(Long empresaId, String bodega);
    List<ErpInventarioSaldoJpa> findByEmpresaIdOrderByBodegaAscProductoIdAsc(Long empresaId);
}
