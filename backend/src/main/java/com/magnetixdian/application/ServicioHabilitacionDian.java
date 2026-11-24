package com.magnetixdian.application;

import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDianResolver;
import com.magnetixdian.domain.dian.RespuestaDian;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.interfaces.dto.DianHabilitacionDto;
import com.magnetixdian.interfaces.dto.ResultadoHabilitacionDianDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ServicioHabilitacionDian {

    private static final int AMBIENTE_PRUEBAS = 2;
    private static final int AMBIENTE_PRODUCCION = 1;
    private static final int TOTAL_DOCUMENTOS_TEST_SET = 10;

    private final EmpresaFeConfigRepository configRepository;
    private final ClienteDianSoap cliente;
    private final ModoOperacionDianResolver modoResolver;
    private final ServicioConfigFe servicioConfigFe;
    private final AuditoriaService auditoria;
    private final boolean failFast;

    public ServicioHabilitacionDian(EmpresaFeConfigRepository configRepository, ClienteDianSoap cliente,
                                    ModoOperacionDianResolver modoResolver, ServicioConfigFe servicioConfigFe,
                                    AuditoriaService auditoria,
                                    @Value("${magnetixdian.seguridad.fail-fast:false}") boolean failFast) {
        this.configRepository = configRepository;
        this.cliente = cliente;
        this.modoResolver = modoResolver;
        this.servicioConfigFe = servicioConfigFe;
        this.auditoria = auditoria;
        this.failFast = failFast;
    }

    @Transactional(readOnly = true)
    public ResultadoHabilitacionDianDto validar(Long empresaId, DianHabilitacionDto solicitud) {
        ModoOperacionDian modo = validarModo(solicitud);
        EmpresaFeConfigJpa config = config(empresaId);
        validarRequisitos(config, solicitud, modo, true);
        validarSeguridad(config, AMBIENTE_PRUEBAS);
        return new ResultadoHabilitacionDianDto(modo, AMBIENTE_PRUEBAS, null, null, "CONFIGURACION_VALIDADA",
                null, null, null, List.of(), false);
    }

    @Transactional
    public ResultadoHabilitacionDianDto enviarLotePruebas(Long empresaId, DianHabilitacionDto solicitud,
                                                          List<byte[]> documentos, String version) {
        ModoOperacionDian modo = validarModo(solicitud);
        EmpresaFeConfigJpa config = config(empresaId);
        validarRequisitos(config, solicitud, modo, true);
        validarSeguridad(config, AMBIENTE_PRUEBAS);
        validarLote(documentos);
        byte[] contenido = comprimir(documentos);
        RespuestaDian respuesta = cliente.sendTestSetAsync(contenido,
                "test-set-" + solicitud.testSetId().trim() + ".zip", solicitud.testSetId(),
                AMBIENTE_PRUEBAS, version(version), config);
        auditar(empresaId, "TEST_SET_ENVIADO", "testSetId=" + solicitud.testSetId()
                + ", trackId=" + nvl(respuesta.trackId()) + ", statusCode=" + nvl(respuesta.statusCode()));
        if (respuesta.aprobada()) {
            auditar(empresaId, "TEST_SET_APROBADO", "trackId=" + nvl(respuesta.trackId()));
        }
        return ResultadoHabilitacionDianDto.desde(modo, AMBIENTE_PRUEBAS, respuesta);
    }

    @Transactional(readOnly = true)
    public ResultadoHabilitacionDianDto consultarEstado(Long empresaId, DianHabilitacionDto solicitud,
                                                        String trackId, int ambiente, String version) {
        EmpresaFeConfigJpa config = config(empresaId);
        validarSeguridad(config, ambiente);
        RespuestaDian respuesta = cliente.getStatus(trackId, ambiente, version(version), config);
        if (respuesta.aprobada()) {
            auditar(empresaId, "TEST_SET_APROBADO", "trackId=" + nvl(trackId) + ", ambiente=" + ambiente);
        }
        ModoOperacionDian modo = solicitud == null ? null : validarModo(solicitud);
        return ResultadoHabilitacionDianDto.desde(modo, ambiente, respuesta);
    }

    @Transactional
    public ResultadoHabilitacionDianDto switchoverProduccion(Long empresaId, DianHabilitacionDto solicitud,
                                                              String trackId, String version) {
        ModoOperacionDian modo = validarModo(solicitud);
        EmpresaFeConfigJpa config = config(empresaId);
        validarRequisitos(config, solicitud, modo, false);
        validarSeguridad(config, AMBIENTE_PRODUCCION);
        if (trackId == null || trackId.isBlank()) {
            throw new IllegalArgumentException("El trackId del lote de pruebas aprobado es obligatorio");
        }
        RespuestaDian respuesta = cliente.getStatusByTrackId(trackId, AMBIENTE_PRODUCCION, version(version), config);
        if (!respuesta.aprobada()) {
            throw new IllegalStateException("La DIAN no aprobó el lote de pruebas; no se permite switchover a producción");
        }
        servicioConfigFe.activarProduccion(empresaId, trackId);
        auditar(empresaId, "SWITCHOVER_PRODUCCION", "trackId=" + trackId + ", modo=" + modo
                + ", statusCode=" + respuesta.statusCode());
        return ResultadoHabilitacionDianDto.desde(modo, AMBIENTE_PRODUCCION, respuesta);
    }

    private ModoOperacionDian validarModo(DianHabilitacionDto solicitud) {
        if (solicitud == null || solicitud.modo() == null) {
            throw new IllegalArgumentException("El modo DIAN es obligatorio");
        }
        return modoResolver.resolver(solicitud.modo().name());
    }

    private void validarRequisitos(EmpresaFeConfigJpa config, DianHabilitacionDto solicitud,
                                   ModoOperacionDian modo, boolean requiereTestSet) {
        if (config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()) {
            throw new IllegalArgumentException("Debe cargar el certificado DIAN antes de continuar");
        }
        if (config.getCertificadoPassword() == null || config.getCertificadoPassword().isBlank()) {
            throw new IllegalArgumentException("Debe configurar el PIN del certificado DIAN");
        }
        String resolucion = valor(solicitud == null ? null : solicitud.resolucionNumero(), config.getResolucionNumero());
        if (resolucion == null || resolucion.isBlank()) {
            throw new IllegalArgumentException("El número de resolución DIAN es obligatorio");
        }
        LocalDate fechaResolucion = solicitud != null && solicitud.resolucionFecha() != null
                ? solicitud.resolucionFecha() : config.getResolucionFecha();
        if (fechaResolucion == null) {
            throw new IllegalArgumentException("La fecha de la resolución DIAN es obligatoria");
        }
        String prefijo = valor(solicitud == null ? null : solicitud.prefijo(), config.getPrefijo());
        if (prefijo == null || prefijo.isBlank()) {
            throw new IllegalArgumentException("El prefijo de la numeración es obligatorio");
        }
        Long desde = solicitud != null && solicitud.rangoDesde() != null
                ? solicitud.rangoDesde().longValue() : config.getRangoDesde();
        Long hasta = solicitud != null && solicitud.rangoHasta() != null
                ? solicitud.rangoHasta().longValue() : config.getRangoHasta();
        if (desde == null || hasta == null || desde <= 0 || hasta < desde) {
            throw new IllegalArgumentException("El rango de numeración DIAN no es válido");
        }
        if (modoResolver.requiereHabilitacionProveedorTecnologico(modo)
                && (config.getSoftwareId() == null || config.getSoftwareId().isBlank())) {
            throw new IllegalArgumentException("El modo " + modo + " requiere softwareId habilitado");
        }
        if (requiereTestSet && (solicitud == null || solicitud.testSetId() == null
                || solicitud.testSetId().isBlank())) {
            throw new IllegalArgumentException("El testSetId es obligatorio para el set de pruebas");
        }
    }

    private void validarSeguridad(EmpresaFeConfigJpa config, int ambiente) {
        if (!failFast || ambiente != AMBIENTE_PRODUCCION) {
            return;
        }
        if ("SIMULADO".equalsIgnoreCase(config.getCanal())) {
            throw new IllegalStateException("Canal SIMULADO bloqueado en producción por SEGURIDAD_FAIL_FAST");
        }
        if (config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()
                || config.getCertificadoPassword() == null || config.getCertificadoPassword().isBlank()) {
            throw new IllegalStateException("Certificado DIAN obligatorio en producción con SEGURIDAD_FAIL_FAST");
        }
    }

    private void validarLote(List<byte[]> documentos) {
        if (documentos == null || documentos.size() != TOTAL_DOCUMENTOS_TEST_SET) {
            throw new IllegalArgumentException("El set de pruebas debe tener 8 facturas, 1 nota débito y 1 nota crédito");
        }
        int facturas = 0;
        int notasDebito = 0;
        int notasCredito = 0;
        for (byte[] documento : documentos) {
            if (documento == null || documento.length == 0) {
                throw new IllegalArgumentException("El set de pruebas contiene un XML vacío");
            }
            String xml = new String(documento, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            if (xml.contains("<invoice")) {
                facturas++;
            } else if (xml.contains("<debitnote")) {
                notasDebito++;
            } else if (xml.contains("<creditnote")) {
                notasCredito++;
            }
        }
        if (facturas != 8 || notasDebito != 1 || notasCredito != 1) {
            throw new IllegalArgumentException("El set de pruebas debe tener 8 facturas, 1 nota débito y 1 nota crédito");
        }
    }

    private byte[] comprimir(List<byte[]> documentos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (int i = 0; i < documentos.size(); i++) {
                String nombre = i < 8 ? "factura-" + (i + 1) + ".xml"
                        : i == 8 ? "nota-debito.xml" : "nota-credito.xml";
                zip.putNextEntry(new ZipEntry(nombre));
                zip.write(documentos.get(i));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo empaquetar el set de pruebas", e);
        }
        return salida.toByteArray();
    }

    private EmpresaFeConfigJpa config(Long empresaId) {
        return configRepository.findConEmpresa(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("La empresa no tiene configuración DIAN"));
    }

    private void auditar(Long empresaId, String accion, String detalle) {
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), accion, "empresa", empresaId, detalle);
    }

    private String version(String version) {
        return version == null || version.isBlank() ? cliente.versionPorDefecto() : version.trim();
    }

    private static String valor(String solicitado, String configurado) {
        return solicitado == null || solicitado.isBlank() ? configurado : solicitado;
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
