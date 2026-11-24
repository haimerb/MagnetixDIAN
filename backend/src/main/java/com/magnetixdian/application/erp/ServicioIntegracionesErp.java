package com.magnetixdian.application.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import com.magnetixdian.domain.erp.modelo.ResultadoSincronizacionErp;
import com.magnetixdian.domain.erp.puerto.AdaptadorErp;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpIntegracionConfigJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpIntegracionConfigRepository;
import com.magnetixdian.interfaces.dto.erp.IntegracionErpConfigDto;
import com.magnetixdian.interfaces.dto.erp.IntegracionErpConfigRequestDto;
import com.magnetixdian.interfaces.dto.erp.SincronizacionErpDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ServicioIntegracionesErp {

    private final ErpIntegracionConfigRepository configs;
    private final CifradorAes cifrador;
    private final ObjectMapper mapper;
    private final Map<ProveedorErp, AdaptadorErp> adaptadores = new EnumMap<>(ProveedorErp.class);

    public ServicioIntegracionesErp(ErpIntegracionConfigRepository configs, CifradorAes cifrador, ObjectMapper mapper, List<AdaptadorErp> adaptadores) {
        this.configs = configs;
        this.cifrador = cifrador;
        this.mapper = mapper;
        for (AdaptadorErp a : adaptadores) this.adaptadores.put(a.proveedor(), a);
    }

    public IntegracionErpConfigDto guardar(Long empresaId, String proveedorRaw, IntegracionErpConfigRequestDto request) {
        ProveedorErp proveedor = proveedor(proveedorRaw);
        ErpIntegracionConfigJpa e = configs.findByEmpresaIdAndProveedorAndTipo(empresaId, proveedor.name(), "ERP").orElseGet(ErpIntegracionConfigJpa::new);
        e.setEmpresaId(empresaId);
        e.setProveedor(proveedor.name());
        e.setTipo("ERP");
        e.setActivo(request.activo() == null || request.activo());
        e.setConfiguracion(escribir(request.configuracion() == null ? Map.of() : request.configuracion()));
        Map<String, String> cred = request.credenciales() == null ? Map.of() : request.credenciales();
        e.setCredencialesCifradas(cred.isEmpty() ? e.getCredencialesCifradas() : cifrador.cifrar(escribir(cred)));
        e.setEstado(e.getCredencialesCifradas() == null || e.getCredencialesCifradas().isBlank() ? "SIMULADO" : "CONFIGURADA");
        e.setUltimoError(null);
        return toDto(configs.save(e), cred.keySet().stream().sorted().toList());
    }

    @Transactional(readOnly = true)
    public List<IntegracionErpConfigDto> listar(Long empresaId) {
        List<IntegracionErpConfigDto> out = new ArrayList<>();
        for (ProveedorErp p : ProveedorErp.values()) {
            ErpIntegracionConfigJpa e = configs.findByEmpresaIdAndProveedorAndTipo(empresaId, p.name(), "ERP").orElse(null);
            out.add(e == null ? new IntegracionErpConfigDto(null, empresaId, p.name(), "ERP", true, "SIMULADO", false, null, null, List.of()) : toDto(e, claves(e)));
        }
        return out;
    }

    public SincronizacionErpDto sincronizar(Long empresaId, String proveedorRaw, String tipo) {
        ProveedorErp proveedor = proveedor(proveedorRaw);
        AdaptadorErp adaptador = adaptadores.get(proveedor);
        if (adaptador == null) throw new IllegalArgumentException("Proveedor ERP no soportado: " + proveedorRaw);
        ErpIntegracionConfigJpa config = configs.findByEmpresaIdAndProveedorAndTipo(empresaId, proveedor.name(), "ERP").orElse(null);
        ResultadoSincronizacionErp res;
        try {
            res = adaptador.sincronizar(empresaId, tipo == null ? "GENERAL" : tipo.trim().toUpperCase());
        } catch (RuntimeException ex) {
            res = ResultadoSincronizacionErp.fallo(proveedor.name(), tipo, "Error de sincronización");
        }
        if (config != null) {
            config.setEstado(res.exitoso() ? "SINCRONIZADA" : "ERROR");
            config.setUltimoError(res.exitoso() ? null : res.mensaje());
            config.setUltimaSincronizacion(OffsetDateTime.now());
            configs.save(config);
        }
        return new SincronizacionErpDto(res.exitoso(), res.proveedor(), res.tipo(), res.mensaje(), res.idExterno(), res.fecha(), res.detalles());
    }

    @Transactional(readOnly = true)
    public Map<String, String> credenciales(Long empresaId, ProveedorErp proveedor) {
        ErpIntegracionConfigJpa e = configs.findByEmpresaIdAndProveedorAndTipo(empresaId, proveedor.name(), "ERP").orElse(null);
        if (e == null || e.getCredencialesCifradas() == null || e.getCredencialesCifradas().isBlank()) return Map.of();
        try {
            return mapper.readValue(cifrador.descifrar(e.getCredencialesCifradas()), new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<String> claves(ErpIntegracionConfigJpa e) {
        if (e.getCredencialesCifradas() == null || e.getCredencialesCifradas().isBlank()) return List.of();
        try {
            Map<String, String> m = mapper.readValue(cifrador.descifrar(e.getCredencialesCifradas()), new TypeReference<Map<String, String>>() {});
            return m.keySet().stream().sorted().toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private IntegracionErpConfigDto toDto(ErpIntegracionConfigJpa e, List<String> claves) {
        boolean configurada = e.getCredencialesCifradas() != null && !e.getCredencialesCifradas().isBlank();
        return new IntegracionErpConfigDto(e.getId(), e.getEmpresaId(), e.getProveedor(), e.getTipo(), e.isActivo(), e.getEstado(), configurada, e.getUltimoError(), e.getUltimaSincronizacion(), claves);
    }

    private ProveedorErp proveedor(String raw) {
        return ProveedorErp.desde(raw).orElseThrow(() -> new IllegalArgumentException("Proveedor ERP no soportado: " + raw));
    }

    private String escribir(Object o) {
        try { return mapper.writeValueAsString(o); } catch (Exception ex) { throw new IllegalArgumentException("Configuración ERP inválida"); }
    }
}
