package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.CuentaContableDto;
import com.magnetixdian.interfaces.dto.RegimenContableDto;
import com.magnetixdian.interfaces.dto.SolicitudCuentaContableDto;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ServicioPlanCuentas {

    private final CuentaContableRepository cuentaRepository;
    private JdbcTemplate jdbc;

    public ServicioPlanCuentas(CuentaContableRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Autowired(required = false)
    public void setJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    @Transactional(readOnly = true)
    public List<CuentaContableDto> listarDto(Long empresaId) {
        return listar(empresaId).stream().map(ServicioPlanCuentas::mapear).toList();
    }

    public CuentaContableDto crear(Long empresaId, SolicitudCuentaContableDto solicitud) {
        if (solicitud == null || solicitud.codigo() == null || solicitud.codigo().isBlank()) {
            throw new IllegalArgumentException("El código de la cuenta contable es obligatorio");
        }
        if (solicitud.nombre() == null || solicitud.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la cuenta contable es obligatorio");
        }
        String tipo = solicitud.tipo() == null || solicitud.tipo().isBlank()
                ? "OTRO" : solicitud.tipo().trim().toUpperCase(Locale.ROOT);
        String naturaleza = solicitud.naturaleza() == null || solicitud.naturaleza().isBlank()
                ? (Set.of("PASIVO", "PATRIMONIO", "INGRESO").contains(tipo) ? "ACREEDORA" : "DEUDORA")
                : solicitud.naturaleza().trim().toUpperCase(Locale.ROOT);
        CuentaContableJpa cuenta = new CuentaContableJpa();
        cuenta.setEmpresaId(empresaId);
        cuenta.setCodigo(solicitud.codigo().trim());
        cuenta.setNombre(solicitud.nombre().trim());
        cuenta.setTipo(tipo);
        cuenta.setNaturaleza(naturaleza);
        cuenta.setNivel(solicitud.nivel() == null ? 1 : solicitud.nivel());
        cuenta.setVersionPuc(solicitud.versionPuc() == null || solicitud.versionPuc().isBlank()
                ? versionPuc(regimenNiif(empresaId)) : solicitud.versionPuc().trim());
        cuenta.setAceptaAuxiliar(solicitud.aceptaAuxiliar() != null && solicitud.aceptaAuxiliar());
        cuenta.setActiva(solicitud.activa() == null || solicitud.activa());
        cuenta.setCuentaPadreId(solicitud.cuentaPadreId());
        return mapear(cuentaRepository.save(cuenta));
    }

    @Transactional(readOnly = true)
    public RegimenContableDto regimen(Long empresaId) {
        String valor = regimenNiif(empresaId);
        return new RegimenContableDto(valor, valor, versionPuc(valor), descripcion(valor));
    }

    public RegimenContableDto cambiarRegimen(Long empresaId, String regimenNiif) {
        String valor = normalizarRegimen(regimenNiif);
        if (jdbc != null) {
            jdbc.update("UPDATE empresa SET regimen_niif = ?, updated_at = now() WHERE id = ?", valor, empresaId);
        }
        asegurar(empresaId);
        // Actualizar version_puc para todas las cuentas de la empresa para que coincida con el nuevo régimen
        String nuevaVersion = versionPuc(valor);
        if (jdbc != null) {
            jdbc.update("UPDATE cuenta_contable SET version_puc = ? WHERE empresa_id = ?", nuevaVersion, empresaId);
        }
        return regimen(empresaId);
    }

    private String regimenNiif(Long empresaId) {
        if (jdbc == null) {
            return "NIIF_PYMES";
        }
        try {
            String valor = jdbc.queryForObject("SELECT regimen_niif FROM empresa WHERE id = ?",
                    String.class, empresaId);
            return normalizarRegimen(valor);
        } catch (EmptyResultDataAccessException ex) {
            return "NIIF_PYMES";
        }
    }

    private String normalizarRegimen(String valor) {
        if (valor == null || valor.isBlank()) {
            return "NIIF_PYMES";
        }
        return switch (valor.trim().toUpperCase(Locale.ROOT)) {
            case "NIIF_FULL", "FULL" -> "NIIF_FULL";
            case "NIIF_PYMES", "PYMES" -> "NIIF_PYMES";
            default -> throw new IllegalArgumentException("Régimen NIIF no soportado: " + valor);
        };
    }

    private String versionPuc(String regimenNiif) {
        return "NIIF_FULL".equals(regimenNiif) ? "NIIF_FULL" : "NIIF_PYMES";
    }

    private String descripcion(String regimenNiif) {
        return "NIIF_FULL".equals(regimenNiif) ? "NIIF plenas" : "NIIF para PYMES";
    }

    private static CuentaContableDto mapear(CuentaContableJpa cuenta) {
        return new CuentaContableDto(cuenta.getId(), cuenta.getCodigo(), cuenta.getNombre(), cuenta.getTipo(),
                cuenta.getNaturaleza(), cuenta.getNivel(), cuenta.getVersionPuc(), cuenta.isAceptaAuxiliar(),
                cuenta.isActiva(), cuenta.getCuentaPadreId());
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
