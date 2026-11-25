package com.magnetixdian.application;

import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDianResolver;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EventoAuditoriaJpa;
import com.magnetixdian.infrastructure.persistence.EventoAuditoriaRepository;
import com.magnetixdian.infrastructure.xml.GeneradorUblFactura;
import com.magnetixdian.interfaces.dto.AuditoriaDianDto;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import com.magnetixdian.interfaces.dto.DianHabilitacionDto;
import com.magnetixdian.interfaces.dto.DocumentoTestSetDianDto;
import com.magnetixdian.interfaces.dto.HabilitacionDianEmpresaDto;
import com.magnetixdian.interfaces.dto.ResultadoHabilitacionDianDto;
import com.magnetixdian.interfaces.dto.ResultadoTestSetDianDto;
import com.magnetixdian.interfaces.dto.SolicitudDocumentosTestDianDto;
import com.magnetixdian.interfaces.dto.SolicitudSwitchoverDianDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServicioHabilitacionDianWizard {

    public static final String CONFIRMACION_SWITCHOVER = "APRUEBO_SWITCHOVER";

    private static final int AMBIENTE_PRUEBAS = 2;
    private static final Pattern TRACK_ID = Pattern.compile("trackId=([^,]+)");
    private static final Set<String> ACCIONES_RELEVANTES = Set.of(
            "CONFIG_FE_ACTUALIZADA", "TEST_SET_ENVIADO", "TEST_SET_APROBADO", "SWITCHOVER_PRODUCCION");

    private static final List<DocumentoTestSetDianDto> CATALOGO = List.of(
            new DocumentoTestSetDianDto("TEST-01", "01", "Factura electrónica de venta 1", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-02", "01", "Factura electrónica de venta 2", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-03", "01", "Factura electrónica de venta 3", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-04", "01", "Factura electrónica de venta 4", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-05", "01", "Factura electrónica de venta 5", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-06", "01", "Factura electrónica de venta 6", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-07", "01", "Factura electrónica de venta 7", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-08", "01", "Factura electrónica de venta 8", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-09-ND", "02", "Nota débito de factura", "PENDIENTE", null),
            new DocumentoTestSetDianDto("TEST-10-NC", "03", "Nota crédito de factura", "PENDIENTE", null));

    private final ServicioConfigFe servicioConfigFe;
    private final ServicioHabilitacionDian habilitacion;
    private final EmpresaFeConfigRepository configRepository;
    private final EventoAuditoriaRepository auditoriaRepository;
    private final ModoOperacionDianResolver modoResolver;
    private final GeneradorUblFactura generador;
    private final JdbcTemplate jdbc;

    public ServicioHabilitacionDianWizard(ServicioConfigFe servicioConfigFe,
                                          ServicioHabilitacionDian habilitacion,
                                          EmpresaFeConfigRepository configRepository,
                                          EventoAuditoriaRepository auditoriaRepository,
                                          ModoOperacionDianResolver modoResolver,
                                          GeneradorUblFactura generador,
                                          JdbcTemplate jdbc) {
        this.servicioConfigFe = servicioConfigFe;
        this.habilitacion = habilitacion;
        this.configRepository = configRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.modoResolver = modoResolver;
        this.generador = generador;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public HabilitacionDianEmpresaDto obtener(Long empresaId) {
        EmpresaFeConfigJpa config = configuracion(empresaId);
        List<EventoAuditoriaJpa> eventos = eventos(empresaId);
        return new HabilitacionDianEmpresaDto(
                empresaId,
                modo(empresaId).name(),
                estado(config, eventos),
                config.getResolucionNumero(),
                config.getResolucionFecha(),
                config.getPrefijo(),
                entero(config.getRangoDesde()),
                entero(config.getRangoHasta()),
                config.getTestSetId(),
                config.getSoftwareId(),
                config.isNotificarAdquirente(),
                config.getCertificadoNombre(),
                fechaEvento(eventos, "TEST_SET_APROBADO"),
                fechaEvento(eventos, "SWITCHOVER_PRODUCCION"),
                eventos.stream().filter(e -> ACCIONES_RELEVANTES.contains(e.getAccion())).map(this::auditoria).toList());
    }

    @Transactional
    public HabilitacionDianEmpresaDto guardar(Long empresaId, DianHabilitacionDto solicitud) {
        if (solicitud == null || solicitud.modo() == null) {
            throw new IllegalArgumentException("El modo DIAN es obligatorio");
        }
        ModoOperacionDian modo = modoResolver.resolver(solicitud.modo().name());
        EmpresaFeConfigJpa actual = configuracion(empresaId);
        servicioConfigFe.guardar(empresaId, configDesde(actual, solicitud));
        jdbc.update("UPDATE empresa SET modo_operacion_dian = ? WHERE id = ?", modo.name(), empresaId);
        return obtener(empresaId);
    }

    @Transactional(readOnly = true)
    public List<DocumentoTestSetDianDto> documentos(Long empresaId) {
        configuracion(empresaId);
        String estadoDocumento = estadoDocumento(estado(configuracion(empresaId), eventos(empresaId)));
        return CATALOGO.stream()
                .map(d -> new DocumentoTestSetDianDto(d.trackId(), d.tipoDocumento(), d.descripcion(),
                        estadoDocumento, null))
                .toList();
    }

    @Transactional
    public ResultadoTestSetDianDto enviar(Long empresaId, SolicitudDocumentosTestDianDto solicitud) {
        if (solicitud == null || solicitud.documentos() == null || solicitud.documentos().size() != 10) {
            throw new IllegalArgumentException("El set de pruebas debe tener 8 facturas, 1 nota débito y 1 nota crédito");
        }
        EmpresaFeConfigJpa config = configuracion(empresaId);
        String testSetId = config.getTestSetId();
        if (testSetId == null || testSetId.isBlank()) {
            throw new IllegalArgumentException("El testSetId es obligatorio para el set de pruebas");
        }
        List<byte[]> documentos = documentosGenerados(testSetId, solicitud.documentos());
        ResultadoHabilitacionDianDto resultado = habilitacion.enviarLotePruebas(empresaId,
                solicitudDesde(config, modo(empresaId), null), documentos, null);
        return resultadoTestSet(testSetId, resultado, solicitud.documentos());
    }

    @Transactional(readOnly = true)
    public ResultadoTestSetDianDto consultar(Long empresaId, String testSetId) {
        EmpresaFeConfigJpa config = configuracion(empresaId);
        String trackId = trackId(empresaId, config, testSetId);
        ResultadoHabilitacionDianDto resultado = habilitacion.consultarEstado(empresaId,
                solicitudDesde(config, modo(empresaId), null), trackId, AMBIENTE_PRUEBAS, null);
        return resultadoTestSet(testSetId, resultado, documentos(empresaId));
    }

    @Transactional
    public HabilitacionDianEmpresaDto switchover(Long empresaId, SolicitudSwitchoverDianDto solicitud) {
        if (solicitud == null || !CONFIRMACION_SWITCHOVER.equals(solicitud.confirmacion())) {
            throw new IllegalArgumentException("La confirmación explícita del switchover es obligatoria");
        }
        EmpresaFeConfigJpa config = configuracion(empresaId);
        String trackId = trackId(empresaId, config, solicitud.testSetId());
        habilitacion.switchoverProduccion(empresaId,
                solicitudDesde(config, modo(empresaId), solicitud.resolucionNumero()), trackId, null);
        return obtener(empresaId);
    }

    private List<byte[]> documentosGenerados(String testSetId, List<DocumentoTestSetDianDto> documentos) {
        int facturas = 0;
        int notasDebito = 0;
        int notasCredito = 0;
        for (DocumentoTestSetDianDto documento : documentos) {
            switch (categoria(documento.tipoDocumento())) {
                case "FACTURA" -> facturas++;
                case "NOTA_DEBITO" -> notasDebito++;
                default -> notasCredito++;
            }
        }
        if (facturas != 8 || notasDebito != 1 || notasCredito != 1) {
            throw new IllegalArgumentException("El set de pruebas debe tener 8 facturas, 1 nota débito y 1 nota crédito");
        }
        List<byte[]> xmls = new ArrayList<>();
        int secuenciaFactura = 1;
        int secuenciaDebito = 1;
        int secuenciaCredito = 1;
        for (DocumentoTestSetDianDto documento : documentos) {
            switch (categoria(documento.tipoDocumento())) {
                case "FACTURA" -> xmls.add(generador.construirDocumentoPrueba("01", testSetId, secuenciaFactura++));
                case "NOTA_DEBITO" -> xmls.add(generador.construirDocumentoPrueba("92", testSetId, secuenciaDebito++));
                default -> xmls.add(generador.construirDocumentoPrueba("91", testSetId, secuenciaCredito++));
            }
        }
        return xmls;
    }

    private String categoria(String tipo) {
        String normalizado = tipo == null ? "" : tipo.trim();
        return switch (normalizado) {
            case "01", "FACTURA", "FACTURAVENTA" -> "FACTURA";
            case "02", "NOTA_DEBITO", "NOTADEBITO" -> "NOTA_DEBITO";
            case "03", "NOTA_CREDITO", "NOTACREDITO" -> "NOTA_CREDITO";
            default -> throw new IllegalArgumentException("Tipo de documento de prueba no soportado: " + tipo);
        };
    }

    private ResultadoTestSetDianDto resultadoTestSet(String testSetId, ResultadoHabilitacionDianDto resultado,
                                                     List<DocumentoTestSetDianDto> documentos) {
        String estado = estadoResultado(resultado);
        String estadoDocumento = estadoDocumento(estado);
        int total = documentos.size();
        int aprobados = "TEST_SET_APROBADO".equals(estado) ? total : 0;
        int rechazados = "TEST_SET_RECHAZADO".equals(estado) ? total : 0;
        List<DocumentoTestSetDianDto> detalle = documentos.stream()
                .map(d -> new DocumentoTestSetDianDto(d.trackId(), d.tipoDocumento(), d.descripcion(),
                        estadoDocumento, resultado.statusMessage()))
                .toList();
        return new ResultadoTestSetDianDto(testSetId, estado, total, aprobados, rechazados, detalle);
    }

    private String estadoResultado(ResultadoHabilitacionDianDto resultado) {
        if (resultado.aprobado()) {
            return "TEST_SET_APROBADO";
        }
        String codigo = resultado.statusCode();
        if ("PENDIENTE".equalsIgnoreCase(codigo) || "EN_PROCESO".equalsIgnoreCase(codigo)
                || "PENDING".equalsIgnoreCase(codigo)) {
            return "TEST_SET_ENVIADO";
        }
        return "TEST_SET_RECHAZADO";
    }

    private String estado(EmpresaFeConfigJpa config, List<EventoAuditoriaJpa> eventos) {
        if (eventos.stream().anyMatch(e -> "SWITCHOVER_PRODUCCION".equals(e.getAccion()))
                || (config.getAmbiente() == 1 && "DIAN".equalsIgnoreCase(config.getCanal()))) {
            return "PRODUCCION";
        }
        if (eventos.stream().anyMatch(e -> "TEST_SET_APROBADO".equals(e.getAccion()))) {
            return "TEST_SET_APROBADO";
        }
        if (eventos.stream().anyMatch(e -> "TEST_SET_ENVIADO".equals(e.getAccion()))) {
            return "TEST_SET_ENVIADO";
        }
        return "BORRADOR";
    }

    private String estadoDocumento(String estado) {
        return switch (estado) {
            case "TEST_SET_APROBADO", "PRODUCCION" -> "APROBADO";
            case "TEST_SET_ENVIADO" -> "ENVIADO";
            case "TEST_SET_RECHAZADO" -> "RECHAZADO";
            default -> "PENDIENTE";
        };
    }

    private String trackId(Long empresaId, EmpresaFeConfigJpa config, String testSetId) {
        EventoAuditoriaJpa envio = eventos(empresaId).stream()
                .filter(e -> "TEST_SET_ENVIADO".equals(e.getAccion()))
                .findFirst()
                .orElse(null);
        String registrado = envio == null ? null : extraerTrackId(envio.getDetalle());
        if (registrado != null && (testSetId == null || testSetId.isBlank()
                || testSetId.trim().equals(config.getTestSetId()))) {
            return registrado;
        }
        if (testSetId == null || testSetId.isBlank()) {
            throw new IllegalArgumentException("El trackId del lote de pruebas es obligatorio");
        }
        return testSetId.trim();
    }

    private String extraerTrackId(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return null;
        }
        Matcher matcher = TRACK_ID.matcher(detalle);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private OffsetDateTime fechaEvento(List<EventoAuditoriaJpa> eventos, String accion) {
        return eventos.stream()
                .filter(e -> accion.equals(e.getAccion()))
                .findFirst()
                .map(EventoAuditoriaJpa::getCreatedAt)
                .orElse(null);
    }

    private AuditoriaDianDto auditoria(EventoAuditoriaJpa evento) {
        return new AuditoriaDianDto(evento.getId(), evento.getAccion(), evento.getDetalle(),
                evento.getUsuario(), evento.getCreatedAt());
    }

    private List<EventoAuditoriaJpa> eventos(Long empresaId) {
        return auditoriaRepository.findTop50ByEmpresaIdOrderByCreatedAtDesc(empresaId);
    }

    private EmpresaFeConfigJpa configuracion(Long empresaId) {
        return configRepository.findConEmpresa(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("La empresa no tiene configuración DIAN"));
    }

    private ModoOperacionDian modo(Long empresaId) {
        try {
            String valor = jdbc.queryForObject(
                    "SELECT modo_operacion_dian FROM empresa WHERE id = ?", String.class, empresaId);
            return modoResolver.resolver(valor);
        } catch (EmptyResultDataAccessException | IllegalArgumentException e) {
            return modoResolver.modoPorDefectoParaCliente();
        }
    }

    private ConfigFeDto configDesde(EmpresaFeConfigJpa actual, DianHabilitacionDto solicitud) {
        return new ConfigFeDto(
                (int) actual.getAmbiente(),
                valor(solicitud.softwareId(), actual.getSoftwareId()),
                null,
                null,
                valor(solicitud.testSetId(), actual.getTestSetId()),
                valor(solicitud.resolucionNumero(), actual.getResolucionNumero()),
                solicitud.resolucionFecha() == null ? actual.getResolucionFecha() : solicitud.resolucionFecha(),
                valor(solicitud.prefijo(), actual.getPrefijo()),
                solicitud.rangoDesde() == null ? actual.getRangoDesde() : solicitud.rangoDesde().longValue(),
                solicitud.rangoHasta() == null ? actual.getRangoHasta() : solicitud.rangoHasta().longValue(),
                actual.getRangoVigenciaDesde(),
                actual.getRangoVigenciaHasta(),
                actual.getCanal(),
                actual.getPacProveedor(),
                actual.getPacBaseUrl(),
                null,
                actual.getCertificadoNombre(),
                null,
                actual.getCertificadoPath(),
                actual.getResponsabilidadFiscal(),
                actual.getCodigoMunicipio(),
                actual.isActivo(),
                actual.getCertificadoPath() != null && !actual.getCertificadoPath().isBlank(),
                actual.getPacApiKey() != null && !actual.getPacApiKey().isBlank(),
                solicitud.notificarAdquirente());
    }

    private DianHabilitacionDto solicitudDesde(EmpresaFeConfigJpa config, ModoOperacionDian modo,
                                               String resolucionNumero) {
        return new DianHabilitacionDto(
                modo,
                valor(resolucionNumero, config.getResolucionNumero()),
                config.getResolucionFecha(),
                config.getPrefijo(),
                entero(config.getRangoDesde()),
                entero(config.getRangoHasta()),
                config.getTestSetId(),
                config.getSoftwareId(),
                config.isNotificarAdquirente());
    }

    private Integer entero(Long valor) {
        return valor == null ? null : valor.intValue();
    }

    private String valor(String solicitado, String configurado) {
        return solicitado == null || solicitado.isBlank() ? configurado : solicitado.trim();
    }
}
