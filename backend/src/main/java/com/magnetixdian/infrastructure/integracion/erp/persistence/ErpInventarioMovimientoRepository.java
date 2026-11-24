package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ErpInventarioMovimientoRepository extends JpaRepository<ErpInventarioMovimientoJpa, Long> {
    List<ErpInventarioMovimientoJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    List<ErpInventarioMovimientoJpa> findByEmpresaIdAndBodegaOrderByFechaDescIdDesc(Long empresaId, String bodega);
}
