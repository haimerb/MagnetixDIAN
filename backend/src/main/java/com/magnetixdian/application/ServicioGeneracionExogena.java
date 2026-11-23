package com.magnetixdian.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.magnetixdian.domain.exogena.CabeceraExogena;
import com.magnetixdian.domain.exogena.DocumentoExogena;
import com.magnetixdian.domain.exogena.FormatoExogena;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.domain.exogena.MapeadorExogenaOficial;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.infrastructure.xml.GeneradorXmlExogenaOficial;
import com.magnetixdian.infrastructure.xml.ValidadorXsdOficialDian2025;
import com.magnetixdian.interfaces.dto.ConceptoExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoValidacionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudGeneracionExogenaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ServicioGeneracionExogena {

    public static final String VALIDACION_XSD_LOCAL = "XSD local DIAN 2025 (no prevalidación oficial)";

    private final FormatoExogenaRegistry formatos;
    private final MapeadorExogenaOficial mapeador;
    private final GeneradorXmlExogenaOficial generador;
    private final ValidadorXsdOficialDian2025 validador;
    private final ObjectMapper objectMapper;
    private final MedioMagneticoRepository medioRepo;
    private final MedioMagneticoDatosJsonRepository datosJsonRepo;
    private final AuditoriaService auditoria;

    public ServicioGeneracionExogena(FormatoExogenaRegistry formatos,
                                     MapeadorExogenaOficial mapeador,
                                     GeneradorXmlExogenaOficial generador,
                                     ValidadorXsdOficialDian2025 validador,
                                     ObjectMapper objectMapper,
                                     MedioMagneticoRepository medioRepo,
                                     MedioMagneticoDatosJsonRepository datosJsonRepo,
                                     AuditoriaService auditoria) {
        this.formatos = formatos;
        this.mapeador = mapeador;
        this.generador = generador;
        this.validador = validador;
        this.objectMapper = objectMapper;
        this.medioRepo = medioRepo;
        this.datosJsonRepo = datosJsonRepo;
        this.auditoria = auditoria;
    }

    public List<FormatoExogena> formatosOficiales() {
        return FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS.stream()
                .map(formatos::obtener)
                .toList();
    }

    public List<ConceptoExogenaDto> conceptos(String formato) {
        FormatoExogena oficial = validarOficial(formatos.validarActivo(formato));
        return mapeador.conceptos(oficial.codigo()).stream()
                .map(c -> new ConceptoExogenaDto(c.formato(), c.codigo(), c.descripcion(), c.codCpt()))
                .toList();
    }

    @Transactional
    public ResultadoGeneracionExogenaDto generar(Long empresaId, SolicitudGeneracionExogenaDto solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Solicitud de generación vacía");
        }
        FormatoExogena oficial = validarOficial(formatos.validarActivo(solicitud.formato()));
        int anio = solicitud.anioGravable() == null ? LocalDate.now().getYear() : solicitud.anioGravable();
        ObjectNode datos = objectMapper.createObjectNode();
        datos.put("anioGravable", anio);
        datos.set("filas", objectMapper.valueToTree(
                solicitud.filas() == null ? List.of() : solicitud.filas()));
        DocumentoExogena documento = mapeador.mapear(
                oficial.codigo(), datos, cabecera(oficial, solicitud, anio));
        String xml = generador.generar(documento);
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "GENERAR_EXOGENA",
                "FormatoExogena", oficial.codigo(), "XML generado con " + documento.cantReg() + " registros");
        return new ResultadoGeneracionExogenaDto(
                oficial.codigo(), oficial.version(), oficial.descripcion(), documento.cantReg(),
                xml, VALIDACION_XSD_LOCAL, false);
    }

    @Transactional
    public String generarDesdeMedio(Long medioMagneticoId) {
        MedioMagneticoJpa medio = medioRepo.findById(medioMagneticoId)
                .orElseThrow(() -> new IllegalArgumentException("Medio magnético no encontrado: " + medioMagneticoId));
        FormatoExogena oficial = validarOficial(formatos.validarActivo(medio.getFormato()));
        String json = datosJsonRepo.leerDatosJson(medioMagneticoId);
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("El medio magnético no tiene datos JSON para generar el XML");
        }
        JsonNode datos;
        try {
            datos = objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Los datos JSON del medio magnético no son válidos", ex);
        }
        int anio = medio.getAnioGravable();
        CabeceraExogena cabecera = new CabeceraExogena(
                oficial.codigo(), oficial.version(), anio, 1,
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                anio + "-01-01", anio + "-12-31", null);
        DocumentoExogena documento = mapeador.mapear(oficial.codigo(), datos, cabecera);
        String xml = generador.generar(documento);
        medio.setEstado("XML_GENERADO");
        medio.touch();
        medioRepo.save(medio);
        auditoria.registrarEnEmpresa(medio.getEmpresa().getId(), AuditoriaService.usuarioActual(),
                "GENERAR_EXOGENA", "MedioMagnetico", medioMagneticoId,
                "XML " + oficial.codigo() + " generado con " + documento.cantReg() + " registros");
        return xml;
    }

    public ResultadoValidacionExogenaDto validarXml(String formato, String xml) {
        FormatoExogena oficial = validarOficial(formatos.validarActivo(formato));
        try {
            validador.validar(xml, oficial.codigo());
            return new ResultadoValidacionExogenaDto(
                    oficial.codigo(), true, "XML válido contra XSD local DIAN 2025", false);
        } catch (IllegalArgumentException ex) {
            return new ResultadoValidacionExogenaDto(oficial.codigo(), false, ex.getMessage(), false);
        }
    }

    private FormatoExogena validarOficial(FormatoExogena formato) {
        if (!FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS.contains(formato.codigo())) {
            throw new IllegalArgumentException("Formato no gestionado por el generador exógena oficial: "
                    + formato.codigo());
        }
        return formato;
    }

    private CabeceraExogena cabecera(FormatoExogena formato, SolicitudGeneracionExogenaDto solicitud, int anio) {
        int numeroEnvio = solicitud.numeroEnvio() == null ? 1 : solicitud.numeroEnvio();
        return new CabeceraExogena(
                formato.codigo(),
                formato.version(),
                anio,
                numeroEnvio,
                fechaHora(solicitud.fechaEnvio()),
                fecha(solicitud.fechaInicial(), anio + "-01-01"),
                fecha(solicitud.fechaFinal(), anio + "-12-31"),
                solicitud.concepto());
    }

    private String fechaHora(String valor) {
        if (valor == null || valor.isBlank()) {
            return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        try {
            return OffsetDateTime.parse(valor, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeException ex) {
            try {
                return LocalDateTime.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeException otro) {
                throw new IllegalArgumentException("fechaEnvio debe ser una fecha ISO-8601 válida", otro);
            }
        }
    }

    private String fecha(String valor, String valorPorDefecto) {
        if (valor == null || valor.isBlank()) {
            return valorPorDefecto;
        }
        try {
            return LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Las fechas deben tener formato YYYY-MM-DD", ex);
        }
    }
}
