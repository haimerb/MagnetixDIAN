package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpCuentaBancariaRepository extends JpaRepository<ErpCuentaBancariaJpa, Long> {
    List<ErpCuentaBancariaJpa> findByEmpresaIdOrderByBancoAscNumeroCuentaAsc(Long empresaId);
    Optional<ErpCuentaBancariaJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
