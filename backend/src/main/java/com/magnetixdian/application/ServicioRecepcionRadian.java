package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.magnetixdian.domain.radian.DireccionDocumento;
import com.magnetixdian.domain.radian.EstadoCausacion;
import com.magnetixdian.domain.radian.TipoDocumentoRadian;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianImportDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianRecepcionDto;
import com.magnetixdian.interfaces.dto.ResultadoRecepcionRadianDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ServicioRecepcionRadian {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ServicioDocumentosRadian documentos;
    private final ServicioCausacionRadian causacion;
    private final DocumentoRadianRepository repository;

    public ServicioRecepcionRadian(ServicioDocumentosRadian documentos,
                                   ServicioCausacionRadian causacion,
                                   DocumentoRadianRepository repository) {
        this.documentos = documentos;
        this.causacion = causacion;
        this.repository = repository;
    }

    @Transactional
    public ResultadoRecepcionRadianDto recibir(Long empresaId, DocumentoRadianRecepcionDto solicitud) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (solicitud == null || solicitud.cufeCude() == null || solicitud.cufeCude().isBlank()) {
            throw new IllegalArgumentException("El CUFE/CUDE del documento es obligatorio.");
        }
        String direccion = DireccionDocumento.desde(
                solicitud.direccion() == null || solicitud.direccion().isBlank()
                        ? DireccionDocumento.RECIBIDO.name() : solicitud.direccion()).name();
        TipoDocumentoRadian tipo = TipoDocumentoRadian.desde(normalizarTipo(solicitud.tipoDocumento()));
        String cufe = solicitud.cufeCude().trim();

        Optional<DocumentoRadianJpa> duplicado = repository.findByEmpresaIdAndDireccionAndCufeCude(
                empresaId, direccion, cufe);
        if (duplicado.isEmpty()) {
            String otraDireccion = DireccionDocumento.RECIBIDO.name().equals(direccion)
                    ? DireccionDocumento.EMITIDO.name() : DireccionDocumento.RECIBIDO.name();
            duplicado = repository.findByEmpresaIdAndDireccionAndCufeCude(empresaId, otraDireccion, cufe);
        }
        if (duplicado.isPresent()) {
            boolean causado = !EstadoCausacion.PENDIENTE.equals(duplicado.get().getEstadoCausacion());
            return new ResultadoRecepcionRadianDto(duplicado.get().getId(), tipo.codigo(), cufe, true, causado,
                    "El documento ya existe en RADIAN.");
        }

        DocumentoRadianImportDto importacion = new DocumentoRadianImportDto(
                empresaId,
                direccion,
                tipo.codigo(),
                cufe,
                solicitud.consecutivo(),
                solicitud.nitContraparte(),
                solicitud.razonSocialContraparte(),
                solicitud.fecha(),
                solicitud.subtotal(),
                solicitud.impuestos(),
                solicitud.retenciones(),
                solicitud.total(),
                solicitud.estadoRadian() == null || solicitud.estadoRadian().isBlank()
                        ? "RECIBIDO" : solicitud.estadoRadian(),
                List.of(),
                List.of());
        byte[] contenido;
        try {
            contenido = MAPPER.writeValueAsString(importacion).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar la recepción RADIAN.", ex);
        }
        List<DocumentoRadianDto> importados = documentos.importar(empresaId, direccion, contenido, "recepcion.json");
        DocumentoRadianDto importado = importados.stream()
                .filter(item -> cufe.equals(item.cufeCude()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo registrar la recepción RADIAN."));
        causacion.causar(empresaId, importado.id(), new SolicitudCausacionDto(LocalDate.now(),
                "Causación automática por recepción RADIAN"));
        return new ResultadoRecepcionRadianDto(importado.id(), tipo.codigo(), cufe, false, true,
                "Documento recibido y causado.");
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return TipoDocumentoRadian.FACTURA.codigo();
        }
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return switch (normalizado) {
            case "01", "FACTURA", "FACTURAVENTA" -> TipoDocumentoRadian.FACTURA.codigo();
            case "05", "SOPORTE", "DOCUMENTOSOPORTE" -> TipoDocumentoRadian.DOCUMENTO_SOPORTE.codigo();
            case "31", "NOMINA" -> TipoDocumentoRadian.NOMINA.codigo();
            case "91", "NC", "NOTACREDITO" -> TipoDocumentoRadian.NOTA_CREDITO.codigo();
            case "92", "ND", "NOTADEBITO" -> TipoDocumentoRadian.NOTA_DEBITO.codigo();
            default -> throw new IllegalArgumentException("Tipo de documento RADIAN no soportado: " + tipo);
        };
    }
}
