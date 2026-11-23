package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ServicioPlanCuentas {

    private final CuentaContableRepository cuentaRepository;

    public ServicioPlanCuentas(CuentaContableRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    public Map<String, CuentaContableJpa> asegurar(Long empresaId) {
        List<CuentaContableJpa> propias = cuentaRepository.findByEmpresaId(empresaId);
        List<CuentaContableJpa> globales = cuentaRepository.findByEmpresaIdIsNull();
        Map<String, CuentaContableJpa> porCodigo = new LinkedHashMap<>();
        for (CuentaContableJpa cuenta : globales) {
            porCodigo.put(cuenta.getCodigo(), cuenta);
        }
        for (CuentaContableJpa cuenta : propias) {
            porCodigo.put(cuenta.getCodigo(), cuenta);
        }
        List<CuentaContableJpa> faltantes = new ArrayList<>();
        for (CuentaContableJpa cuenta : globales) {
            boolean existe = propias.stream()
                    .anyMatch(propia -> propia.getCodigo().equals(cuenta.getCodigo()));
            if (!existe) {
                faltantes.add(copia(cuenta, empresaId));
            }
        }
        if (!faltantes.isEmpty()) {
            List<CuentaContableJpa> guardadas = cuentaRepository.saveAll(faltantes);
            if (guardadas == null) {
                guardadas = faltantes;
            }
            for (CuentaContableJpa cuenta : guardadas) {
                porCodigo.put(cuenta.getCodigo(), cuenta);
            }
        }
        return porCodigo;
    }

    @Transactional(readOnly = true)
    public CuentaContableJpa obtener(Long empresaId, String codigo) {
        return cuentaRepository.findByEmpresaIdAndCodigo(empresaId, codigo)
                .or(() -> cuentaRepository.findByEmpresaIdIsNullAndCodigo(codigo))
                .orElseThrow(() -> new IllegalArgumentException("Cuenta contable no configurada: " + codigo));
    }

    @Transactional(readOnly = true)
    public List<CuentaContableJpa> listar(Long empresaId) {
        Map<String, CuentaContableJpa> cuentas = asegurar(empresaId);
        return List.copyOf(cuentas.values());
    }

    private static CuentaContableJpa copia(CuentaContableJpa cuenta, Long empresaId) {
        CuentaContableJpa copia = new CuentaContableJpa();
        copia.setEmpresaId(empresaId);
        copia.setCodigo(cuenta.getCodigo());
        copia.setNombre(cuenta.getNombre());
        copia.setTipo(cuenta.getTipo());
        copia.setActiva(cuenta.isActiva());
        copia.setNaturaleza(cuenta.getNaturaleza());
        copia.setNivel(cuenta.getNivel());
        copia.setVersionPuc(cuenta.getVersionPuc());
        copia.setAceptaAuxiliar(cuenta.isAceptaAuxiliar());
        return copia;
    }
}
