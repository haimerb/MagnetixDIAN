package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpOrdenVentaRepository extends JpaRepository<ErpOrdenVentaJpa, Long> {
    List<ErpOrdenVentaJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpOrdenVentaJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
