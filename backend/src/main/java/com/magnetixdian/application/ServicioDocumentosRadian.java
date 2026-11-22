package com.magnetixdian.application;

import com.magnetixdian.domain.radian.DireccionDocumento;
import com.magnetixdian.domain.radian.EstadoCausacion;
import com.magnetixdian.domain.radian.LectorDocumentosRadian;
import com.magnetixdian.domain.radian.TipoDocumentoRadian;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianLineaJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianImportDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianImpuestoDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianLineaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServicioDocumentosRadian {

    public static final String TIPO_DOCUMENTO = "RADIAN";
    public static final String TIPO_EVENTO_CAUSACION = "CAUSACION";

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static volatile DocumentoAsientoRepository repositorioVinculos;

    private final DocumentoRadianRepository repository;
    private final DocumentoAsientoRepository documentoAsientoRepository;

    public ServicioDocumentosRadian(DocumentoRadianRepository repository) {
        this(repository, null);
    }

    @Autowired
    public ServicioDocumentosRadian(DocumentoRadianRepository repository,
                                    DocumentoAsientoRepository documentoAsientoRepository) {
        this.repository = repository;
        this.documentoAsientoRepository = documentoAsientoRepository;
        repositorioVinculos = documentoAsientoRepository;
    }

    public List<DocumentoRadianDto> importar(Long empresaId, String direccion,
                                             byte[] contenido, String nombreArchivo) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        List<DocumentoRadianImportDto> documentos = LectorDocumentosRadian.parsear(contenido, nombreArchivo);
        if (documentos.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene documentos RADIAN.");
        }
        List<DocumentoRadianDto> resultado = new ArrayList<>();
        for (DocumentoRadianImportDto item : documentos) {
            String direccionResuelta = DireccionDocumento.desde(
                    direccion != null ? direccion : item.direccion()).name();
            validar(item, direccionResuelta);
            Optional<DocumentoRadianJpa> existente = repository.findByEmpresaIdAndDireccionAndCufeCude(
                    empresaId, direccionResuelta, item.cufeCude().trim());
            if (existente.isPresent()) {
                actualizar(existente.get(), direccionResuelta, item);
                resultado.add(mapear(existente.get(), documentoAsientoRepository));
                continue;
            }
            DocumentoRadianJpa entidad = construir(empresaId, direccionResuelta, item);
            DocumentoRadianJpa guardado = repository.save(entidad);
            resultado.add(mapear(guardado == null ? entidad : guardado, documentoAsientoRepository));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<DocumentoRadianDto> listar(Long empresaId, String direccion, String tipoDocumento,
                                           String estadoCausacion, LocalDate fechaDesde, LocalDate fechaHasta) {
        return repository.buscar(empresaId, direccion, tipoDocumento, estadoCausacion, fechaDesde, fechaHasta)
                .stream().map(entidad -> mapear(entidad, documentoAsientoRepository)).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoRadianJpa obtener(Long empresaId, Long documentoId) {
        return repository.findByIdAndEmpresaId(documentoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Documento RADIAN no encontrado."));
    }

    public static DocumentoRadianDto mapear(DocumentoRadianJpa entidad) {
        return mapear(entidad, repositorioVinculos);
    }

    public static DocumentoRadianDto mapear(DocumentoRadianJpa entidad,
                                            DocumentoAsientoRepository vinculos) {
        List<DocumentoRadianLineaDto> lineas = entidad.getLineas().stream()
                .map(linea -> new DocumentoRadianLineaDto(
                        linea.getNumeroLinea(),
                        linea.getCodigoProducto(),
                        linea.getDescripcion(),
                        linea.getCantidad(),
                        linea.getValorUnitario(),
                        linea.getSubtotal(),
                        linea.getImpuestos(),
                        linea.getTotal()))
                .toList();
        List<DocumentoRadianImpuestoDto> impuestos = entidad.getImpuestosDetalle().stream()
                .map(impuesto -> new DocumentoRadianImpuestoDto(
                        impuesto.getTipo(),
                        impuesto.getNombre(),
                        impuesto.getBase(),
                        impuesto.getPorcentaje(),
                        impuesto.getValor()))
                .toList();
        Optional<DocumentoAsientoJpa> vinculo = resolverVinculo(entidad, vinculos);
        String estadoContabilizacion = vinculo.isPresent() ? "CONTABILIZADO" : "PENDIENTE";
        Long asientoContableId = vinculo.map(v -> v.getAsiento() == null ? null : v.getAsiento().getId())
                .orElse(null);
        return new DocumentoRadianDto(
                entidad.getId(),
                entidad.getEmpresaId(),
                entidad.getDireccion(),
                entidad.getTipoDocumento(),
                entidad.getCufeCude(),
                entidad.getConsecutivo(),
                entidad.getNitContraparte(),
                entidad.getRazonSocialContraparte(),
                entidad.getFecha(),
                entidad.getSubtotal(),
                entidad.getImpuestos(),
                entidad.getRetenciones(),
                entidad.getTotal(),
                entidad.getEstadoRadian(),
                entidad.getEstadoCausacion(),
                estadoContabilizacion,
                asientoContableId,
                entidad.getCreatedAt(),
                lineas,
                impuestos);
    }

    private static Optional<DocumentoAsientoJpa> resolverVinculo(DocumentoRadianJpa entidad,
                                                                DocumentoAsientoRepository vinculos) {
        if (vinculos == null || entidad.getId() == null) {
            return Optional.empty();
        }
        Optional<DocumentoAsientoJpa> vinculo = vinculos.findFirstByDocumentoRadianIdOrderByIdDesc(entidad.getId());
        return vinculo.isPresent() ? vinculo : vinculos.findByDocumentoRadianId(entidad.getId());
    }

    private static void validar(DocumentoRadianImportDto item, String direccion) {
        if (item == null) {
            throw new IllegalArgumentException("El documento RADIAN es obligatorio.");
        }
        if (item.cufeCude() == null || item.cufeCude().isBlank()) {
            throw new IllegalArgumentException("El CUFE/CUDE es obligatorio.");
        }
        if (item.consecutivo() == null || item.consecutivo().isBlank()) {
            throw new IllegalArgumentException("El consecutivo es obligatorio.");
        }
        if (item.fecha() == null) {
            throw new IllegalArgumentException("La fecha del documento es obligatoria.");
        }
        TipoDocumentoRadian.desde(item.tipoDocumento());
        DireccionDocumento.desde(direccion);
    }

    private static DocumentoRadianJpa construir(Long empresaId, String direccion,
                                                DocumentoRadianImportDto item) {
        DocumentoRadianJpa entidad = new DocumentoRadianJpa();
        entidad.setEmpresaId(empresaId);
        actualizar(entidad, direccion, item);
        entidad.setEstadoCausacion(EstadoCausacion.PENDIENTE);
        return entidad;
    }

    private static void actualizar(DocumentoRadianJpa entidad, String direccion,
                                   DocumentoRadianImportDto item) {
        entidad.setDireccion(direccion);
        entidad.setTipoDocumento(item.tipoDocumento().trim());
        entidad.setCufeCude(item.cufeCude().trim());
        entidad.setConsecutivo(item.consecutivo().trim());
        entidad.setNitContraparte(item.nitContraparte());
        entidad.setRazonSocialContraparte(item.razonSocialContraparte());
        entidad.setFecha(item.fecha());
        entidad.setSubtotal(escala(item.subtotal()));
        entidad.setImpuestos(escala(item.impuestos()));
        entidad.setRetenciones(escala(item.retenciones()));
        entidad.setTotal(escala(item.total()));
        String estadoRadian = item.estadoRadian() == null || item.estadoRadian().isBlank()
                ? "PENDIENTE" : item.estadoRadian().trim();
        entidad.setEstadoRadian(estadoRadian);

        if (item.lineas() != null) {
            entidad.getLineas().clear();
            int numero = 1;
            for (DocumentoRadianLineaDto linea : item.lineas()) {
                DocumentoRadianLineaJpa entidadLinea = new DocumentoRadianLineaJpa();
                entidadLinea.setNumeroLinea(linea.numeroLinea() == null ? numero : linea.numeroLinea());
                entidadLinea.setCodigoProducto(linea.codigoProducto());
                entidadLinea.setDescripcion(linea.descripcion());
                entidadLinea.setCantidad(linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad());
                entidadLinea.setValorUnitario(escala(linea.valorUnitario()));
                entidadLinea.setSubtotal(escala(linea.subtotal()));
                entidadLinea.setImpuestos(escala(linea.impuestos()));
                entidadLinea.setTotal(escala(linea.total()));
                entidad.agregarLinea(entidadLinea);
                numero++;
            }
        }
        if (item.impuestosDetalle() != null) {
            entidad.getImpuestosDetalle().clear();
            for (DocumentoRadianImpuestoDto impuesto : item.impuestosDetalle()) {
                DocumentoRadianImpuestoJpa entidadImpuesto = new DocumentoRadianImpuestoJpa();
                entidadImpuesto.setTipo(impuesto.tipo());
                entidadImpuesto.setNombre(impuesto.nombre());
                entidadImpuesto.setBase(escala(impuesto.base()));
                entidadImpuesto.setPorcentaje(impuesto.porcentaje() == null
                        ? BigDecimal.ZERO : impuesto.porcentaje());
                entidadImpuesto.setValor(escala(impuesto.valor()));
                entidad.agregarImpuesto(entidadImpuesto);
            }
        }
    }

    private static BigDecimal escala(BigDecimal valor) {
        if (valor == null) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
