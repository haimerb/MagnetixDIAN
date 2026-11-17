package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.ConsecutivoJpa;
import com.magnetixdian.infrastructure.persistence.ConsecutivoRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna el siguiente número consecutivo de forma transaccional y con bloqueo
 * pesimista, garantizando unicidad por empresa, prefijo y tipo de documento.
 */
@Service
public class ServicioNumeracion {

    private final ConsecutivoRepository consecutivoRepository;
    private final EmpresaFeConfigRepository configRepository;

    public ServicioNumeracion(ConsecutivoRepository consecutivoRepository,
                              EmpresaFeConfigRepository configRepository) {
        this.consecutivoRepository = consecutivoRepository;
        this.configRepository = configRepository;
    }

    @Transactional
    public long siguiente(Long empresaId, String prefijo, String tipoDocumento) {
        String p = prefijo == null ? "" : prefijo;
        ConsecutivoJpa consecutivo = consecutivoRepository.bloquear(empresaId, p, tipoDocumento)
                .orElseGet(() -> crear(empresaId, p, tipoDocumento));
        long siguiente = consecutivo.getUltimoNumero() + 1;
        consecutivo.setUltimoNumero(siguiente);
        consecutivoRepository.save(consecutivo);
        return siguiente;
    }

    private ConsecutivoJpa crear(Long empresaId, String prefijo, String tipoDocumento) {
        ConsecutivoJpa c = new ConsecutivoJpa();
        c.setEmpresaId(empresaId);
        c.setPrefijo(prefijo);
        c.setTipoDocumento(tipoDocumento);
        Long inicio = configRepository.findByEmpresaId(empresaId)
                .map(EmpresaFeConfigJpa::getRangoDesde)
                .orElse(null);
        c.setUltimoNumero(inicio != null && inicio > 0 ? inicio - 1 : 0);
        return consecutivoRepository.save(c);
    }
}
