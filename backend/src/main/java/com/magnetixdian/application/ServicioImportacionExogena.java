package com.magnetixdian.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.infrastructure.excel.ImportadorMultiFormato;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.interfaces.dto.FormatoDetectadoDto;
import com.magnetixdian.interfaces.dto.ImportacionMultiFormatoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServicioImportacionExogena {

    private final ImportadorMultiFormato importador;
    private final ServicioMedioMagnetico servicioMedioMagnetico;
    private final MedioMagneticoDatosJsonRepository datosJsonRepository;
    private final ObjectMapper objectMapper;

    public ServicioImportacionExogena(ImportadorMultiFormato importador,
                                      ServicioMedioMagnetico servicioMedioMagnetico,
                                      MedioMagneticoDatosJsonRepository datosJsonRepository,
                                      ObjectMapper objectMapper) {
        this.importador = importador;
        this.servicioMedioMagnetico = servicioMedioMagnetico;
        this.datosJsonRepository = datosJsonRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportacionMultiFormatoDto importar(Long empresaId, Integer anioGravable,
                                               MultipartFile archivo) throws IOException {
        ImportacionMultiFormatoDto leido = importador.importar(archivo);
        List<FormatoDetectadoDto> formatos = new ArrayList<>();
        int persistidos = 0;

        for (FormatoDetectadoDto detectado : leido.formatos()) {
            if (!detectado.activo()) {
                formatos.add(detectado);
                continue;
            }
            Long medioId = servicioMedioMagnetico
                    .crearOReusar(empresaId, detectado.codigo(), anioGravable)
                    .getId();
            datosJsonRepository.actualizarDatosJson(medioId, construirJson(anioGravable, detectado));
            formatos.add(new FormatoDetectadoDto(
                    detectado.codigo(), detectado.hoja(), detectado.estado(), true,
                    detectado.filas(), detectado.encabezados(), detectado.datos(), medioId));
            persistidos++;
        }

        return new ImportacionMultiFormatoDto(
                anioGravable, formatos, leido.hojasOmitidas(), persistidos);
    }

    private String construirJson(Integer anioGravable, FormatoDetectadoDto detectado) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("anioGravable", anioGravable);
        contenido.put("hoja", detectado.hoja());
        contenido.put("encabezados", detectado.encabezados());
        contenido.put("filas", detectado.datos());
        try {
            return objectMapper.writeValueAsString(contenido);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar los datos del formato " + detectado.codigo(), e);
        }
    }
}
