package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import com.magnetixdian.infrastructure.excel.LectorFilasImportacion;
import com.magnetixdian.infrastructure.importacion.MapperColumnasInteligente;
import com.magnetixdian.infrastructure.persistence.ImportacionStagingJpa;
import com.magnetixdian.infrastructure.persistence.ImportacionStagingRepository;
import com.magnetixdian.infrastructure.persistence.MapeoArchivoJpa;
import com.magnetixdian.infrastructure.persistence.MapeoArchivoRepository;
import com.magnetixdian.interfaces.dto.ConfirmacionImportacionDto;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.ResultadoImportacionUniversalDto;
import com.magnetixdian.interfaces.dto.VistaPreviaArchivoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServicioMapeoColumnas {

    private static final String TIPO_ARCHIVO = "ARCHIVO";
    private static final String TIPO_FACTURA = "FACTURA";
    private static final int MAX_FILAS_EJEMPLO = 5;

    private final MapeoArchivoRepository mapeos;
    private final ImportacionStagingRepository staging;
    private final ImportadorFacturas importador;
    private final MapperColumnasInteligente mapperColumnas;
    private final ObjectMapper mapper;

    public ServicioMapeoColumnas(MapeoArchivoRepository mapeos,
                                 ImportacionStagingRepository staging,
                                 ImportadorFacturas importador,
                                 MapperColumnasInteligente mapperColumnas,
                                 ObjectMapper mapper) {
        this.mapeos = mapeos;
        this.staging = staging;
        this.importador = importador;
        this.mapperColumnas = mapperColumnas;
        this.mapper = mapper;
    }

    @Transactional
    public VistaPreviaArchivoDto vistaPrevia(Long empresaId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "importacion" : archivo.getOriginalFilename();
        byte[] contenido = leer(archivo);
        List<List<String>> filas = LectorFilasImportacion.leer(contenido, nombre);
        List<String> cabeceras = filas.isEmpty() ? List.of() : new ArrayList<>(filas.get(0));
        List<List<String>> ejemplos = filas.size() > 1
                ? List.copyOf(filas.subList(1, Math.min(filas.size(), MAX_FILAS_EJEMPLO + 1)))
                : List.of();
        String formato = mapperColumnas.detectarFormato(nombre, cabeceras);
        List<MapeoColumnaDto> sugerencias = mapperColumnas.sugerir(cabeceras, formato);
        String archivoId = UUID.randomUUID().toString();
        guardarArchivo(empresaId, archivoId, contenido);
        return new VistaPreviaArchivoDto(archivoId, nombre, formato, cabeceras, ejemplos, sugerencias,
                confianza(sugerencias), Math.max(0, filas.size() - 1), Base64.getEncoder().encodeToString(contenido));
    }

    @Transactional
    public ResultadoImportacionUniversalDto confirmar(Long empresaId, ConfirmacionImportacionDto solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La confirmación es obligatoria.");
        }
        String archivoId = solicitud.archivoId() == null || solicitud.archivoId().isBlank()
                ? UUID.randomUUID().toString() : solicitud.archivoId();
        String nombre = solicitud.nombreArchivo() == null || solicitud.nombreArchivo().isBlank()
                ? "importacion" : solicitud.nombreArchivo();
        byte[] contenido = contenido(empresaId, archivoId, solicitud);
        List<List<String>> filas = LectorFilasImportacion.leer(contenido, nombre);
        List<String> cabeceras = filas.isEmpty() ? List.of() : filas.get(0);
        int filasLeidas = Math.max(0, filas.size() - 1);
        String formato = solicitud.formatoDetectado() == null || solicitud.formatoDetectado().isBlank()
                ? mapperColumnas.detectarFormato(nombre, cabeceras) : solicitud.formatoDetectado();
        List<MapeoColumnaDto> columnas = solicitud.columnas() == null || solicitud.columnas().isEmpty()
                ? mapperColumnas.sugerir(cabeceras, formato) : solicitud.columnas();
        persistirMapeos(empresaId, formato, columnas);

        Map<String, Integer> mapeo = mapperColumnas.aCamposImportador(columnas);
        List<String> mensajes = new ArrayList<>();
        List<FacturaRequest> preparadas;
        try {
            preparadas = importador.preparar(contenido, nombre, mapeo);
        } catch (RuntimeException ex) {
            mensajes.add(ex.getMessage() == null ? "No fue posible preparar la importación." : ex.getMessage());
            return new ResultadoImportacionUniversalDto(archivoId, "ERROR", filasLeidas, 0, 0, 1, mensajes);
        }

        int duplicadas = 0;
        int guardadas = 0;
        for (int i = 0; i < preparadas.size(); i++) {
            int fila = i + 1;
            String externalId = archivoId + "|" + fila;
            Optional<ImportacionStagingJpa> existente = staging
                    .findByEmpresaIdAndArchivoIdAndTipoAndExternalId(empresaId, archivoId, TIPO_FACTURA, externalId);
            if (existente.isPresent()) {
                duplicadas++;
                continue;
            }
            ImportacionStagingJpa etapa = new ImportacionStagingJpa();
            etapa.setEmpresaId(empresaId);
            etapa.setArchivoId(archivoId);
            etapa.setTipo(TIPO_FACTURA);
            etapa.setExternalId(externalId);
            etapa.setFila(fila);
            etapa.setEstado("PENDIENTE");
            etapa.setPayload(serializar(preparadas.get(i)));
            staging.save(etapa);
            guardadas++;
        }
        int lineas = preparadas.stream().mapToInt(factura -> factura.lineas() == null ? 0 : factura.lineas().size()).sum();
        int omitidas = Math.max(0, filasLeidas - lineas) + duplicadas;
        if (guardadas == 0) {
            mensajes.add("No hay filas nuevas para preparar; la importación ya fue confirmada.");
        }
        return new ResultadoImportacionUniversalDto(archivoId, "STAGED", filasLeidas, guardadas, omitidas, 0, mensajes);
    }

    public List<MapeoColumnaDto> mapeos(Long empresaId) {
        return mapeos.findByEmpresaIdOrderByFormatoDetectadoAscCabeceraOrigenAsc(empresaId)
                .stream()
                .map(mapeo -> new MapeoColumnaDto(mapeo.getCampo(), mapeo.getCabeceraOrigen(),
                        mapeo.getIndice(), mapeo.getConfianza(), mapeo.getFormatoDetectado()))
                .toList();
    }

    private void guardarArchivo(Long empresaId, String archivoId, byte[] contenido) {
        if (staging.findByEmpresaIdAndArchivoIdAndTipo(empresaId, archivoId, TIPO_ARCHIVO).isPresent()) {
            return;
        }
        ImportacionStagingJpa archivo = new ImportacionStagingJpa();
        archivo.setEmpresaId(empresaId);
        archivo.setArchivoId(archivoId);
        archivo.setTipo(TIPO_ARCHIVO);
        archivo.setExternalId(archivoId);
        archivo.setFila(0);
        archivo.setEstado("PREVIO");
        archivo.setPayload(Base64.getEncoder().encodeToString(contenido));
        staging.save(archivo);
    }

    private void persistirMapeos(Long empresaId, String formato, List<MapeoColumnaDto> columnas) {
        if (columnas == null) {
            return;
        }
        for (MapeoColumnaDto columna : columnas) {
            if (columna == null || columna.cabeceraOrigen() == null || columna.cabeceraOrigen().isBlank()) {
                continue;
            }
            MapeoArchivoJpa entidad = mapeos.findByEmpresaIdAndFormatoDetectadoAndCabeceraOrigen(
                            empresaId, formato, columna.cabeceraOrigen())
                    .orElseGet(MapeoArchivoJpa::new);
            entidad.setEmpresaId(empresaId);
            entidad.setFormatoDetectado(formato);
            entidad.setCabeceraOrigen(columna.cabeceraOrigen());
            entidad.setCampo(columna.campo());
            entidad.setIndice(columna.indice());
            entidad.setConfianza(columna.confianza());
            mapeos.save(entidad);
        }
    }

    private byte[] contenido(Long empresaId, String archivoId, ConfirmacionImportacionDto solicitud) {
        if (solicitud.contenidoBase64() != null && !solicitud.contenidoBase64().isBlank()) {
            try {
                return Base64.getDecoder().decode(solicitud.contenidoBase64().trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("El contenidoBase64 no es válido.", ex);
            }
        }
        return staging.findByEmpresaIdAndArchivoIdAndTipo(empresaId, archivoId, TIPO_ARCHIVO)
                .map(ImportacionStagingJpa::getPayload)
                .filter(payload -> payload != null && !payload.isBlank())
                .map(payload -> Base64.getDecoder().decode(payload.trim()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Se requiere contenidoBase64 o una vista previa almacenada para el archivo."));
    }

    private String serializar(FacturaRequest factura) {
        try {
            return mapper.writeValueAsString(factura);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo preparar la factura para staging.", ex);
        }
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo cargado.", ex);
        }
    }

    private static double confianza(List<MapeoColumnaDto> sugerencias) {
        if (sugerencias.isEmpty()) {
            return 0;
        }
        double suma = sugerencias.stream().mapToDouble(MapeoColumnaDto::confianza).sum();
        return Math.round((suma / sugerencias.size()) * 100) / 100.0;
    }
}
