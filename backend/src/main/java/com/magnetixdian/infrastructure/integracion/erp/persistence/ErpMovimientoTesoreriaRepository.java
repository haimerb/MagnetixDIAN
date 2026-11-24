package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ErpMovimientoTesoreriaRepository extends JpaRepository<ErpMovimientoTesoreriaJpa, Long> {
    List<ErpMovimientoTesoreriaJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
}
