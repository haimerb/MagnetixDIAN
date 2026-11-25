package com.magnetixdian.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.CapacidadIntegracion;
import com.magnetixdian.domain.integracion.ClienteIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.EventoIntegracion;
import com.magnetixdian.domain.integracion.FacturaIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionEventoJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionEventoRepository;
import com.magnetixdian.interfaces.dto.IntegracionConfigDto;
import com.magnetixdian.interfaces.dto.IntegracionConfigRequest;
import com.magnetixdian.interfaces.dto.IntegracionEventoDto;
import com.magnetixdian.interfaces.dto.IntegracionSincronizacionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioIntegraciones {

    private static final Logger log = LoggerFactory.getLogger(ServicioIntegraciones.class);
    private static final int MAX_INTENTOS = 5;
    private static final int[] BACKOFF_MINUTOS = {1, 5, 15, 60, 240};

    private final IntegracionConfigRepository configuraciones;
    private final IntegracionEventoRepository eventos;
    private final EmpresaRepository empresas;
    private final RegistroIntegraciones registro;
    private final AuditoriaService auditoria;
    private final ObjectMapper mapper;
    private final boolean exigirFirma;

    @Autowired(required = false)
    private CifradorAes cifrador;

    public ServicioIntegraciones(
            IntegracionConfigRepository configuraciones,
            IntegracionEventoRepository eventos,
            EmpresaRepository empresas,
            RegistroIntegraciones registro,
            AuditoriaService auditoria,
            ObjectMapper mapper,
            @Value("${integraciones.exigir-firma-webhook:true}") boolean exigirFirma) {
        this.configuraciones = configuraciones;
        this.eventos = eventos;
        this.empresas = empresas;
        this.registro = registro;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.exigirFirma = exigirFirma;
    }

    public List<IntegracionConfigDto> listar(Long empresaId) {
        return Arrays.stream(ProveedorIntegracion.values())
                .map(proveedor -> aDto(proveedor,
                        configuraciones.findByEmpresaIdAndProveedor(empresaId, proveedor.name()).orElse(null),
                        null))
                .toList();
    }

    public IntegracionConfigDto guardar(Long empresaId, ProveedorIntegracion proveedor, IntegracionConfigRequest solicitud) {
        if (!registro.existe(proveedor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Integración no disponible: " + proveedor);
        }
        EmpresaJpa empresa = empresaRepo(empresaId);
        IntegracionConfigJpa config = configuraciones.findByEmpresaIdAndProveedor(empresaId, proveedor.name())
                .orElseGet(IntegracionConfigJpa::new);
        String tokenPlano = null;
        if (config.getWebhookTokenHash() == null || config.getWebhookTokenHash().isBlank()) {
            tokenPlano = generarToken();
            config.setWebhookTokenHash(sha256(tokenPlano));
            config.setWebhookTokenPrefijo(tokenPlano.substring(0, 8));
        }
        config.setEmpresa(empresa);
        config.setProveedor(proveedor.name());
        config.setActivo(solicitud.activo());
        try {
            config.setCredenciales(proteger(mapper.writeValueAsString(solicitud.credenciales() == null
                    ? Map.of() : solicitud.credenciales())));
            config.setConfiguracion(mapper.writeValueAsString(solicitud.configuracion() == null
                    ? Map.of() : solicitud.configuracion()));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuración inválida", ex);
        }
        DatosConexionIntegracion datos = aDatos(config);
        config.setEstado(registro.obtener(proveedor).configurada(datos) ? "CONECTADO" : "DESCONFIGURADO");
        config.setUltimoError(null);
        config.setUpdatedAt(OffsetDateTime.now());
        configuraciones.save(config);
        auditoria.registrar(AuditoriaService.usuarioActual(), "CONFIGURAR_INTEGRACION", "integracion",
                config.getId(), proveedor.name());
        return aDto(proveedor, config, tokenPlano);
    }

    public ResultadoIntegracion probar(Long empresaId, ProveedorIntegracion proveedor) {
        IntegracionConfigJpa config = configRequerida(empresaId, proveedor);
        DatosConexionIntegracion datos = aDatos(config);
        ResultadoIntegracion resultado;
        if (!config.isActivo() || !registro.obtener(proveedor).configurada(datos)) {
            resultado = ResultadoIntegracion.fallo("Integración desactivada o sin credenciales.");
        } else {
            resultado = registro.obtener(proveedor).probarConexion(datos);
        }
        config.setEstado(resultado.exitoso() ? "CONECTADO" : "ERROR");
        config.setUltimoError(resultado.exitoso() ? null : recortar(resultado.mensaje()));
        config.setUpdatedAt(OffsetDateTime.now());
        configuraciones.save(config);
        auditoria.registrar(AuditoriaService.usuarioActual(), "PROBAR_INTEGRACION", "integracion",
                config.getId(), proveedor + " -> " + resultado.estado());
        return resultado;
    }

    public ResultadoIntegracion sincronizar(Long empresaId, ProveedorIntegracion proveedor,
                                            IntegracionSincronizacionRequest solicitud) {
        IntegracionConfigJpa config = configRequerida(empresaId, proveedor);
        DatosConexionIntegracion datos = aDatos(config);
        String tipo = solicitud.tipo() == null ? "" : solicitud.tipo().trim().toUpperCase(Locale.ROOT);
        Map<String, Object> valores = solicitud.datos() == null ? Map.of() : solicitud.datos();
        ResultadoIntegracion resultado;
        try {
            switch (tipo) {
                case "CLIENTE" -> resultado = registro.obtener(proveedor).sincronizarCliente(datos,
                        mapper.convertValue(valores, ClienteIntegracion.class));
                case "PRODUCTO" -> resultado = registro.obtener(proveedor).sincronizarProducto(datos,
                        mapper.convertValue(valores, com.magnetixdian.domain.integracion.ProductoIntegracion.class));
                case "FACTURA" -> resultado = registro.obtener(proveedor).enviarFactura(datos,
                        mapper.convertValue(valores, FacturaIntegracion.class));
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tipo de sincronización no soportado: " + tipo);
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de sincronización inválidos", ex);
        }
        registrarEvento(config, tipo, valores, resultado);
        auditoria.registrar(AuditoriaService.usuarioActual(), "SINCRONIZAR_INTEGRACION", "integracion",
                config.getId(), proveedor + " / " + tipo + " -> " + resultado.estado());
        return resultado;
    }

    public IntegracionEventoDto webhook(String proveedorSlug, String token, String payload,
                                        Map<String, String> headers) {
        ProveedorIntegracion proveedor = ProveedorIntegracion.desde(proveedorSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proveedor desconocido"));
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de webhook ausente");
        }
        IntegracionConfigJpa config = configuraciones.findByWebhookTokenHash(sha256(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de webhook inválido"));
        if (!config.isActivo() || !proveedor.name().equals(config.getProveedor())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de webhook inválido para el proveedor");
        }
        EventoIntegracion evento = new EventoIntegracion(null, "WEBHOOK", payload == null ? "" : payload, headers);
        if (exigirFirma && !registro.obtener(proveedor).verificarFirma(aDatos(config), evento)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de webhook inválida");
        }
        String idExterno = sha256(proveedor + "|" + config.getEmpresa().getId() + "|" + payload);
        IntegracionEventoJpa existente = eventos.findByEmpresaIdAndProveedorAndExternalIdAndTipo(
                config.getEmpresa().getId(), proveedor.name(), idExterno, "WEBHOOK").orElse(null);
        if (existente != null) {
            return aEventoDto(existente);
        }
        IntegracionEventoJpa eventoJpa = newEvento(config, "WEBHOOK", idExterno, payload);
        eventos.save(eventoJpa);
        despachar(eventoJpa.getId());
        return aEventoDto(eventos.findById(eventoJpa.getId()).orElseThrow());
    }

    public List<IntegracionEventoDto> eventos(Long empresaId) {
        return eventos.findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 50))
                .stream().map(this::aEventoDto).toList();
    }

    public IntegracionEventoDto reintentar(Long empresaId, Long eventoId) {
        IntegracionEventoJpa evento = eventos.findByIdAndEmpresaId(eventoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        despachar(evento.getId());
        return aEventoDto(eventos.findById(evento.getId()).orElseThrow());
    }

    @Scheduled(fixedDelayString = "${integraciones.reintentos.intervalo-ms:60000}")
    public void procesarPendientes() {
        List<IntegracionEventoJpa> pendientes = eventos.findByEstadoAndProximoIntentoBefore(
                "PENDIENTE", OffsetDateTime.now(), PageRequest.of(0, 20));
        for (IntegracionEventoJpa evento : pendientes) {
            try {
                despachar(evento.getId());
            } catch (RuntimeException ex) {
                log.warn("No se pudo reintentar el evento de integración {}: {}", evento.getId(), ex.getMessage());
            }
        }
    }

    private void registrarEvento(IntegracionConfigJpa config, String tipo, Map<String, Object> valores,
                                 ResultadoIntegracion resultado) {
        String payload;
        try {
            payload = mapper.writeValueAsString(valores);
        } catch (Exception ex) {
            payload = "{}";
        }
        String externalId = resultado.idExterno() == null ? UUID.randomUUID().toString() : resultado.idExterno();
        IntegracionEventoJpa evento = newEvento(config, tipo, externalId, payload);
        evento.setIntentos(1);
        if (resultado.exitoso()) {
            evento.setEstado("EXITOSO");
            evento.setUltimoError(null);
        } else {
            aplicarFallo(evento, resultado.mensaje());
        }
        eventos.save(evento);
    }

    private void despachar(Long eventoId) {
        IntegracionEventoJpa evento = eventos.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        IntegracionConfigJpa config = configuraciones.findByEmpresaIdAndProveedor(
                evento.getEmpresa().getId(), evento.getProveedor()).orElse(null);
        if (config == null) {
            aplicarFallo(evento, "La integración fue eliminada.");
            eventos.save(evento);
            return;
        }
        ProveedorIntegracion proveedor = ProveedorIntegracion.valueOf(evento.getProveedor());
        EventoIntegracion datosEvento = new EventoIntegracion(evento.getExternalId(), evento.getTipo(),
                evento.getPayload(), Map.of());
        ResultadoIntegracion resultado;
        try {
            resultado = registro.obtener(proveedor).procesarWebhook(aDatos(config), datosEvento);
        } catch (RuntimeException ex) {
            resultado = ResultadoIntegracion.fallo(recortar(ex.getMessage()));
        }
        evento.setIntentos(evento.getIntentos() + 1);
        if (resultado.exitoso()) {
            evento.setEstado("EXITOSO");
            evento.setUltimoError(null);
            evento.setProximoIntento(null);
        } else {
            aplicarFallo(evento, resultado.mensaje());
        }
        evento.setUpdatedAt(OffsetDateTime.now());
        eventos.save(evento);
        auditoria.registrar(AuditoriaService.usuarioActual(), "DESPACHAR_INTEGRACION", "integracion",
                evento.getId(), evento.getProveedor() + " -> " + evento.getEstado());
    }

    private void aplicarFallo(IntegracionEventoJpa evento, String mensaje) {
        evento.setUltimoError(recortar(mensaje));
        if (evento.getIntentos() >= MAX_INTENTOS) {
            evento.setEstado("ERROR");
            evento.setProximoIntento(null);
        } else {
            evento.setEstado("PENDIENTE");
            int indice = Math.min(Math.max(evento.getIntentos() - 1, 0), BACKOFF_MINUTOS.length - 1);
            evento.setProximoIntento(OffsetDateTime.now().plusMinutes(BACKOFF_MINUTOS[indice]));
        }
    }

    private IntegracionEventoJpa newEvento(IntegracionConfigJpa config, String tipo, String externalId, String payload) {
        IntegracionEventoJpa evento = new IntegracionEventoJpa();
        evento.setEmpresa(config.getEmpresa());
        evento.setProveedor(config.getProveedor());
        evento.setTipo(tipo);
        evento.setExternalId(externalId);
        evento.setPayload(payload);
        evento.setEstado("PENDIENTE");
        evento.setIntentos(0);
        evento.setProximoIntento(OffsetDateTime.now());
        return evento;
    }

    private IntegracionConfigJpa configRequerida(Long empresaId, ProveedorIntegracion proveedor) {
        return configuraciones.findByEmpresaIdAndProveedor(empresaId, proveedor.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integración no configurada"));
    }

    private EmpresaJpa empresaRepo(Long empresaId) {
        return empresas.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    }

    private DatosConexionIntegracion aDatos(IntegracionConfigJpa config) {
        return new DatosConexionIntegracion(
                config.getEmpresa().getId(),
                ProveedorIntegracion.valueOf(config.getProveedor()),
                leerCredenciales(config),
                leerMapa(config.getConfiguracion()),
                config.isActivo());
    }

    private Map<String, String> leerMapa(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> mapa = mapper.readValue(json, new TypeReference<Map<String, String>>() { });
            return mapa == null ? Map.of() : mapa;
        } catch (Exception ex) {
            log.warn("No se pudo leer configuración de integración: {}", ex.getMessage());
            return Map.of();
        }
    }

    private IntegracionConfigDto aDto(ProveedorIntegracion proveedor, IntegracionConfigJpa config, String tokenPlano) {
        List<CapacidadIntegracion> capacidades = registro.capacidades(proveedor);
        if (config == null) {
            return new IntegracionConfigDto(null, proveedor.name(), proveedor.slug(), false, "DESCONFIGURADO",
                    false, capacidades, null, null, tokenPlano);
        }
        boolean configurada = registro.existe(proveedor)
                && registro.obtener(proveedor).configurada(aDatos(config));
        return new IntegracionConfigDto(config.getId(), proveedor.name(), proveedor.slug(), config.isActivo(),
                config.getEstado(), configurada, capacidades, config.getUltimoError(),
                config.getUltimaSincronizacion(), tokenPlano);
    }

    private IntegracionEventoDto aEventoDto(IntegracionEventoJpa evento) {
        return new IntegracionEventoDto(evento.getId(), evento.getProveedor(), evento.getTipo(), evento.getEstado(),
                evento.getIntentos(), evento.getProximoIntento(), evento.getUltimoError(), evento.getCreatedAt());
    }

    private String proteger(String json) {
        return cifrador == null ? json : cifrador.cifrar(json);
    }

    private Map<String, String> leerCredenciales(IntegracionConfigJpa config) {
        String valor = config.getCredenciales();
        if (valor == null || valor.isBlank()) {
            return Map.of();
        }
        boolean cifrado = cifrador != null && cifrador.estaCifrado(valor);
        String json = cifrado ? cifrador.descifrar(valor) : valor;
        try {
            Map<String, String> mapa = mapper.readValue(json, new TypeReference<Map<String, String>>() { });
            if (mapa == null) {
                return Map.of();
            }
            if (cifrador != null && !cifrado) {
                config.setCredenciales(cifrador.cifrar(json));
                configuraciones.save(config);
            }
            return mapa;
        } catch (Exception ex) {
            log.warn("No se pudieron leer credenciales de integración: {}", ex.getMessage());
            return Map.of();
        }
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
        if (valor == null) {
            return null;
        }
        return valor.length() <= 1000 ? valor : valor.substring(0, 1000);
    }
}
