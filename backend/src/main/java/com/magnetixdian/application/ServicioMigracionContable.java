package com.magnetixdian.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoMigracionContable;
import com.magnetixdian.domain.integracion.SolicitudMigracionContable;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.infrastructure.persistence.MigracionContableDetalleJpa;
import com.magnetixdian.infrastructure.persistence.MigracionContableDetalleRepository;
import com.magnetixdian.infrastructure.persistence.MigracionContableJpa;
import com.magnetixdian.infrastructure.persistence.MigracionContableRepository;
import com.magnetixdian.interfaces.dto.MigracionContableDetalleDto;
import com.magnetixdian.interfaces.dto.MigracionContableDto;
import com.magnetixdian.interfaces.dto.ResultadoMigracionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ServicioMigracionContable {

    private static final List<String> ETAPAS_POR_DEFECTO = List.of(
            "PLAN_CUENTAS", "SALDO_INICIAL", "ASIENTO", "TERCERO");

    private final IntegracionConfigRepository configuraciones;
    private final RegistroIntegraciones registro;
    private final MigracionContableRepository migraciones;
    private final MigracionContableDetalleRepository detalles;
    private final ObjectMapper mapper;

    public ServicioMigracionContable(IntegracionConfigRepository configuraciones,
                                     RegistroIntegraciones registro,
                                     MigracionContableRepository migraciones,
                                     MigracionContableDetalleRepository detalles,
                                     ObjectMapper mapper) {
        this.configuraciones = configuraciones;
        this.registro = registro;
        this.migraciones = migraciones;
        this.detalles = detalles;
        this.mapper = mapper;
    }

    @Transactional
    public MigracionContableDto migrar(Long empresaId, ProveedorIntegracion proveedor,
                                       SolicitudMigracionContable solicitud) {
        IntegracionConfigJpa config = configRequerida(empresaId, proveedor);
        List<String> etapas = etapas(solicitud);
        MigracionContableJpa migracion = new MigracionContableJpa();
        migracion.setEmpresaId(empresaId);
        migracion.setProveedor(proveedor.name());
        migracion.setEtapas(escribirEtapas(etapas));
        migracion.setPeriodo(solicitud == null ? null : solicitud.periodo());
        migracion.setEstado("EN_CURSO");
        migraciones.save(migracion);
        return ejecutar(migracion, proveedor, config, solicitud, etapas, false);
    }

    @Transactional
    public ResultadoMigracionDto reporte(Long empresaId, ProveedorIntegracion proveedor, Long migracionId) {
        MigracionContableJpa migracion = migracionRequerida(empresaId, proveedor, migracionId);
        List<MigracionContableDetalleDto> items = detalles
                .findByMigracionIdOrderByCreatedAtAsc(migracion.getId())
                .stream().map(this::aDto).toList();
        return new ResultadoMigracionDto(migracion.getExitosos(), migracion.getOmitidos(),
                migracion.getErrores(), migracion.getTotal(), items);
    }

    @Transactional
    public MigracionContableDto reintentar(Long empresaId, ProveedorIntegracion proveedor, Long migracionId) {
        MigracionContableJpa migracion = migracionRequerida(empresaId, proveedor, migracionId);
        IntegracionConfigJpa config = configRequerida(empresaId, proveedor);
        List<String> etapas = leerEtapas(migracion.getEtapas());
        List<MigracionContableDetalleJpa> fallidos = detalles.findByMigracionIdOrderByCreatedAtAsc(migracion.getId())
                .stream().filter(detalle -> "ERROR".equals(detalle.getEstado())).toList();
        if (!fallidos.isEmpty()) {
            detalles.deleteAll(fallidos);
        }
        SolicitudMigracionContable solicitud = new SolicitudMigracionContable(
                etapas, migracion.getPeriodo(), Map.of());
        return ejecutar(migracion, proveedor, config, solicitud, etapas, true);
    }

    private MigracionContableDto ejecutar(MigracionContableJpa migracion, ProveedorIntegracion proveedor,
                                          IntegracionConfigJpa config, SolicitudMigracionContable solicitud,
                                          List<String> etapas, boolean reintentar) {
        AdaptadorIntegracion adaptador = registro.obtener(proveedor);
        DatosConexionIntegracion datos = aDatos(config);
        Totales totales = new Totales();
        for (String etapa : etapas) {
            ResultadoMigracionContable resultado = invocar(adaptador, datos, etapa, solicitud);
            procesarResultado(migracion, proveedor, etapa, resultado, totales, reintentar);
        }
        migracion.setExitosos(totales.exitosos);
        migracion.setOmitidos(totales.omitidos);
        migracion.setErrores(totales.errores);
        migracion.setTotal(totales.total());
        migracion.setEstado(totales.errores > 0 ? "COMPLETADO_CON_ERRORES" : "COMPLETADO");
        migracion.setMensaje(totales.errores > 0
                ? "Migración finalizada con " + totales.errores + " errores."
                : "Migración finalizada correctamente.");
        migracion.setUpdatedAt(OffsetDateTime.now());
        migraciones.save(migracion);
        return aDto(migracion);
    }

    private ResultadoMigracionContable invocar(AdaptadorIntegracion adaptador, DatosConexionIntegracion datos,
                                               String etapa, SolicitudMigracionContable solicitud) {
        return switch (etapa) {
            case "PLAN_CUENTAS" -> adaptador.migrarPlanCuentas(datos, solicitud);
            case "SALDO_INICIAL" -> adaptador.migrarSaldos(datos, solicitud);
            case "ASIENTO" -> adaptador.migrarAsientos(datos, solicitud);
            case "TERCERO" -> adaptador.migrarTerceros(datos, solicitud);
            default -> ResultadoMigracionContable.fallo(etapa, "Etapa de migración no soportada: " + etapa);
        };
    }

    private void procesarResultado(MigracionContableJpa migracion, ProveedorIntegracion proveedor, String etapa,
                                   ResultadoMigracionContable resultado, Totales totales, boolean reintentar) {
        if (resultado == null) {
            registrarFallo(migracion, proveedor, etapa, "El adaptador no devolvió resultado.", totales);
            return;
        }
        if (!resultado.exitoso() && resultado.items().isEmpty()) {
            registrarFallo(migracion, proveedor, etapa, resultado.mensaje(), totales);
            return;
        }
        for (Map<String, String> item : resultado.items()) {
            Map<String, String> registroItem = item == null ? Map.of() : item;
            String externalId = externalId(registroItem);
            Optional<MigracionContableDetalleJpa> existente = detalles
                    .findByEmpresaIdAndProveedorAndTipoAndExternalId(
                            migracion.getEmpresaId(), proveedor.name(), etapa, externalId);
            if (existente.isPresent()) {
                MigracionContableDetalleJpa detalle = existente.get();
                if (reintentar && "ERROR".equals(detalle.getEstado())) {
                    actualizar(detalle, registroItem);
                    detalles.save(detalle);
                    contar(detalle, totales);
                } else {
                    totales.omitidos++;
                }
                continue;
            }
            MigracionContableDetalleJpa detalle = nuevoDetalle(migracion, proveedor, etapa, registroItem);
            detalles.save(detalle);
            contar(detalle, totales);
        }
    }

    private void registrarFallo(MigracionContableJpa migracion, ProveedorIntegracion proveedor, String etapa,
                                String mensaje, Totales totales) {
        MigracionContableDetalleJpa detalle = new MigracionContableDetalleJpa();
        detalle.setMigracionId(migracion.getId());
        detalle.setEmpresaId(migracion.getEmpresaId());
        detalle.setProveedor(proveedor.name());
        detalle.setTipo(etapa);
        detalle.setExternalId("ERROR|" + etapa + "|" + sha256(String.valueOf(mensaje)).substring(0, 12));
        detalle.setEstado("ERROR");
        detalle.setError(recortar(mensaje));
        detalle.setPayload("{}");
        detalles.save(detalle);
        totales.errores++;
    }

    private MigracionContableDetalleJpa nuevoDetalle(MigracionContableJpa migracion, ProveedorIntegracion proveedor,
                                                     String etapa, Map<String, String> item) {
        MigracionContableDetalleJpa detalle = new MigracionContableDetalleJpa();
        detalle.setMigracionId(migracion.getId());
        detalle.setEmpresaId(migracion.getEmpresaId());
        detalle.setProveedor(proveedor.name());
        detalle.setTipo(etapa);
        detalle.setExternalId(externalId(item));
        actualizar(detalle, item);
        return detalle;
    }

    private void actualizar(MigracionContableDetalleJpa detalle, Map<String, String> item) {
        detalle.setPayload(serializar(item));
        String estado = item.get("estado");
        detalle.setEstado(estado == null || estado.isBlank() ? "EXITOSO" : estado.toUpperCase(Locale.ROOT));
        String error = item.get("error");
        detalle.setError(error == null || error.isBlank() ? null : recortar(error));
    }

    private static void contar(MigracionContableDetalleJpa detalle, Totales totales) {
        if ("ERROR".equals(detalle.getEstado())) {
            totales.errores++;
        } else {
            totales.exitosos++;
        }
    }

    private static String externalId(Map<String, String> item) {
        String valor = item.get("externalId");
        if (valor == null || valor.isBlank()) {
            valor = item.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce("", (a, b) -> a + "|" + b);
        }
        return recortar(valor, 300);
    }

    private String serializar(Map<String, String> item) {
        try {
            return mapper.writeValueAsString(item);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private MigracionContableDto aDto(MigracionContableJpa migracion) {
        return new MigracionContableDto(migracion.getId(), migracion.getProveedor(),
                leerEtapas(migracion.getEtapas()), migracion.getEstado(), migracion.getExitosos(),
                migracion.getOmitidos(), migracion.getErrores(), migracion.getTotal(),
                migracion.getMensaje(), migracion.getCreatedAt());
    }

    private MigracionContableDetalleDto aDto(MigracionContableDetalleJpa detalle) {
        return new MigracionContableDetalleDto(detalle.getId(), detalle.getTipo(), detalle.getExternalId(),
                detalle.getEstado(), detalle.getError(), detalle.getPayload(), detalle.getCreatedAt());
    }

    private static List<String> etapas(SolicitudMigracionContable solicitud) {
        if (solicitud == null || solicitud.etapas() == null || solicitud.etapas().isEmpty()) {
            return ETAPAS_POR_DEFECTO;
        }
        List<String> etapas = new ArrayList<>();
        for (String etapa : solicitud.etapas()) {
            if (etapa != null && !etapa.isBlank()) {
                String valor = etapa.trim().toUpperCase(Locale.ROOT);
                if (!etapas.contains(valor)) {
                    etapas.add(valor);
                }
            }
        }
        return etapas.isEmpty() ? ETAPAS_POR_DEFECTO : etapas;
    }

    private String escribirEtapas(List<String> etapas) {
        try {
            return mapper.writeValueAsString(etapas);
        } catch (Exception ex) {
            return String.join(",", etapas);
        }
    }

    private List<String> leerEtapas(String valor) {
        if (valor == null || valor.isBlank()) {
            return ETAPAS_POR_DEFECTO;
        }
        try {
            List<String> etapas = mapper.readValue(valor, new TypeReference<List<String>>() { });
            return etapas == null || etapas.isEmpty() ? ETAPAS_POR_DEFECTO : etapas;
        } catch (Exception ex) {
            return List.of(valor.split(","));
        }
    }

    private IntegracionConfigJpa configRequerida(Long empresaId, ProveedorIntegracion proveedor) {
        return configuraciones.findByEmpresaIdAndProveedor(empresaId, proveedor.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integración no configurada"));
    }

    private MigracionContableJpa migracionRequerida(Long empresaId, ProveedorIntegracion proveedor, Long id) {
        return migraciones.findByIdAndEmpresaIdAndProveedor(id, empresaId, proveedor.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Migración no encontrada"));
    }

    private DatosConexionIntegracion aDatos(IntegracionConfigJpa config) {
        return new DatosConexionIntegracion(
                config.getEmpresa() == null ? null : config.getEmpresa().getId(),
                ProveedorIntegracion.valueOf(config.getProveedor()),
                leerMapa(config.getCredenciales()),
                leerMapa(config.getConfiguracion()),
                config.isActivo());
    }

    private Map<String, String> leerMapa(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> mapa = mapper.readValue(json, new TypeReference<Map<String, String>>() { });
            return mapa == null ? new LinkedHashMap<>() : mapa;
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private static String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static String recortar(String valor) {
        return recortar(valor, 1000);
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return "";
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static final class Totales {
        private int exitosos;
        private int omitidos;
        private int errores;

        private int total() {
            return exitosos + omitidos + errores;
        }
    }
}
