package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.dian.ArchivoListadoDian;
import com.magnetixdian.domain.dian.OrigenListadoDian;
import com.magnetixdian.domain.dian.ProveedorListadosDian;
import com.magnetixdian.domain.dian.SolicitudListadoDian;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.ResultadoTraerDianDto;
import com.magnetixdian.interfaces.dto.SolicitudTraerDianTokenDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ServicioTraerDian {

    private static final String PROVEEDOR_TOKEN = "DIAN";

    private final List<ProveedorListadosDian> proveedores;
    private final ServicioDocumentosRadian documentos;
    private final IntegracionConfigRepository configuraciones;
    private final EmpresaRepository empresas;
    private final AuditoriaService auditoria;
    private final ObjectMapper mapper;

    public ServicioTraerDian(List<ProveedorListadosDian> proveedores,
                             ServicioDocumentosRadian documentos,
                             IntegracionConfigRepository configuraciones,
                             EmpresaRepository empresas,
                             AuditoriaService auditoria,
                             ObjectMapper mapper) {
        this.proveedores = proveedores;
        this.documentos = documentos;
        this.configuraciones = configuraciones;
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.mapper = mapper;
    }

    public ResultadoTraerDianDto porToken(Long empresaId, SolicitudTraerDianTokenDto solicitud) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria.");
        }
        validarRango(solicitud.fechaDesde(), solicitud.fechaHasta());
        String token = solicitud.token() == null ? "" : solicitud.token().trim();
        if (token.isEmpty()) {
            token = tokenGuardado(empresaId);
        }
        if (token.isEmpty()) {
            throw new IllegalArgumentException("El token de la DIAN es obligatorio.");
        }
        if (solicitud.guardarToken() == null || solicitud.guardarToken()) {
            guardarToken(empresaId, token);
        }
        SolicitudListadoDian lista = new SolicitudListadoDian(token, null, null, solicitud.direccion(),
                solicitud.fechaDesde(), solicitud.fechaHasta(), solicitud.grupo(), solicitud.nitTercero());
        return importar(empresaId, lista, OrigenListadoDian.TOKEN);
    }

    public ResultadoTraerDianDto porArchivo(Long empresaId, byte[] contenido, String nombreArchivo,
                                            String direccion, LocalDate fechaDesde, LocalDate fechaHasta,
                                            String grupo, String nitTercero) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        validarRango(fechaDesde, fechaHasta);
        SolicitudListadoDian lista = new SolicitudListadoDian(null, contenido, nombreArchivo, direccion,
                fechaDesde, fechaHasta, grupo, nitTercero);
        return importar(empresaId, lista, OrigenListadoDian.ARCHIVO);
    }

    private ResultadoTraerDianDto importar(Long empresaId, SolicitudListadoDian solicitud,
                                           OrigenListadoDian origen) {
        ProveedorListadosDian proveedor = proveedores.stream()
                .filter(item -> item.origen() == origen && item.soporta(solicitud))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se pudo preparar la consulta a la DIAN."));
        try {
            ArchivoListadoDian archivo = proveedor.obtener(solicitud);
            List<DocumentoRadianDto> registros = documentos.importar(empresaId, solicitud.direccion(),
                    archivo.contenido(), archivo.nombreArchivo());
            if (origen == OrigenListadoDian.TOKEN) {
                marcarExito(empresaId);
            }
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "TRAER_DIAN", "dian",
                    empresaId, origen.name() + " / " + registros.size() + " documentos");
            return new ResultadoTraerDianDto(origen.name(), archivo.nombreArchivo(), registros.size(),
                    origen == OrigenListadoDian.TOKEN
                            ? "Documentos traídos de la DIAN por token o enlace."
                            : "Documentos importados desde el archivo exportado por la DIAN.", registros);
        } catch (RuntimeException ex) {
            if (origen == OrigenListadoDian.TOKEN) {
                marcarError(empresaId, ex.getMessage());
            }
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "TRAER_DIAN_ERROR", "dian",
                    empresaId, origen.name() + " / " + recortar(ex.getMessage()));
            throw ex;
        }
    }

    private void validarRango(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new IllegalArgumentException("El rango de fechas es obligatorio.");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser mayor a la fecha final.");
        }
    }

    private String tokenGuardado(Long empresaId) {
        IntegracionConfigJpa config = configuraciones.findByEmpresaIdAndProveedor(empresaId, PROVEEDOR_TOKEN)
                .orElse(null);
        if (config == null || config.getCredenciales() == null || config.getCredenciales().isBlank()) {
            return "";
        }
        try {
            Map<String, Object> credenciales = mapper.readValue(config.getCredenciales(), Map.class);
            Object token = credenciales.get("token");
            return token == null ? "" : token.toString().trim();
        } catch (IOException ex) {
            throw new IllegalArgumentException("La credencial DIAN guardada no es válida.", ex);
        }
    }

    private void guardarToken(Long empresaId, String token) {
        EmpresaJpa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        IntegracionConfigJpa config = configuraciones.findByEmpresaIdAndProveedor(empresaId, PROVEEDOR_TOKEN)
                .orElseGet(IntegracionConfigJpa::new);
        config.setEmpresa(empresa);
        config.setProveedor(PROVEEDOR_TOKEN);
        config.setActivo(true);
        try {
            config.setCredenciales(mapper.writeValueAsString(Map.of("token", token)));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo guardar la credencial DIAN.", ex);
        }
        config.setEstado("CONECTADO");
        config.setUltimoError(null);
        config.setUpdatedAt(OffsetDateTime.now());
        configuraciones.save(config);
    }

    private void marcarExito(Long empresaId) {
        configuraciones.findByEmpresaIdAndProveedor(empresaId, PROVEEDOR_TOKEN).ifPresent(config -> {
            config.setEstado("CONECTADO");
            config.setUltimoError(null);
            config.setUltimaSincronizacion(OffsetDateTime.now());
            config.setUpdatedAt(OffsetDateTime.now());
            configuraciones.save(config);
        });
    }

    private void marcarError(Long empresaId, String mensaje) {
        configuraciones.findByEmpresaIdAndProveedor(empresaId, PROVEEDOR_TOKEN).ifPresent(config -> {
            config.setEstado("ERROR");
            config.setUltimoError(recortar(mensaje));
            config.setUpdatedAt(OffsetDateTime.now());
            configuraciones.save(config);
        });
    }

    private static String recortar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Error al traer documentos de la DIAN.";
        }
        return mensaje.length() <= 1000 ? mensaje : mensaje.substring(0, 1000);
    }
}
