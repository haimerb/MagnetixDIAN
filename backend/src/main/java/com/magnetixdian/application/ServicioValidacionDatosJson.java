package com.magnetixdian.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.exogena.FormatoExogena;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.interfaces.dto.ValidacionProcesoDto;
import com.magnetixdian.interfaces.dto.ValidacionProcesoDto.DetalleErrorDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ServicioValidacionDatosJson {

    private final MedioMagneticoRepository medioRepo;
    private final MedioMagneticoDatosJsonRepository datosJsonRepo;
    private final FormatoExogenaRegistry formatos;
    private final ObjectMapper objectMapper;

    public ServicioValidacionDatosJson(MedioMagneticoRepository medioRepo,
                                       MedioMagneticoDatosJsonRepository datosJsonRepo,
                                       FormatoExogenaRegistry formatos,
                                       ObjectMapper objectMapper) {
        this.medioRepo = medioRepo;
        this.datosJsonRepo = datosJsonRepo;
        this.formatos = formatos;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ValidacionProcesoDto validar(Long medioId) {
        OffsetDateTime inicio = OffsetDateTime.now();
        MedioMagneticoJpa medio = medioRepo.findById(medioId)
                .orElseThrow(() -> new IllegalArgumentException("Medio magnético no encontrado: " + medioId));
        List<DetalleErrorDto> detalles = new ArrayList<>();

        FormatoExogena formato = null;
        if (formatos.existe(medio.getFormato())) {
            formato = formatos.obtener(medio.getFormato());
            if (!formato.activo()) {
                detalles.add(error(null, "formato", "ACTIVO", formato.estado(),
                        "El formato no está activo; no es susceptible de validación estructural."));
            }
        } else {
            detalles.add(error(null, "formato", "FORMATO_REGISTRADO", medio.getFormato(),
                    "El formato del registro no existe en el catálogo de exógena."));
        }

        String contenido = datosJsonRepo.leerDatosJson(medioId);
        JsonNode raiz = null;
        if (contenido == null || contenido.isBlank()) {
            detalles.add(error(null, "datos_json", "JSON", null,
                    "El registro no tiene datos_json; use la carga clásica para este medio."));
        } else {
            try {
                raiz = objectMapper.readTree(contenido);
            } catch (Exception e) {
                detalles.add(error(null, "datos_json", "JSON_VÁLIDO", null,
                        "No se pudo interpretar el contenido JSON del registro."));
            }
        }

        int registrosValidados = 0;
        if (raiz != null) {
            if (!raiz.isObject()) {
                detalles.add(error(null, "datos_json", "OBJETO", null,
                        "El contenido JSON debe ser un objeto."));
            } else {
                validarAnio(raiz, medio, detalles);
                registrosValidados = validarFilas(raiz, detalles);
                validarEncabezados(raiz, formato, detalles);
            }
        }

        int errores = 0;
        int advertencias = 0;
        for (DetalleErrorDto detalle : detalles) {
            if ("ERROR".equals(detalle.severidad())) errores++;
            if ("ADVERTENCIA".equals(detalle.severidad())) advertencias++;
        }

        return new ValidacionProcesoDto(
                null,
                medioId,
                "COMPLETADA",
                errores,
                advertencias,
                registrosValidados,
                inicio,
                OffsetDateTime.now(),
                detalles);
    }

    private void validarAnio(JsonNode raiz, MedioMagneticoJpa medio, List<DetalleErrorDto> detalles) {
        JsonNode anio = raiz.get("anioGravable");
        if (anio == null || !anio.isNumber() || anio.asInt() != medio.getAnioGravable()) {
            detalles.add(error(null, "anioGravable", String.valueOf(medio.getAnioGravable()),
                    anio == null ? null : anio.asText(),
                    "El año del JSON no coincide con el año gravable del registro."));
        }
    }

    private int validarFilas(JsonNode raiz, List<DetalleErrorDto> detalles) {
        JsonNode filas = raiz.get("filas");
        if (filas == null || !filas.isArray() || filas.isEmpty()) {
            detalles.add(error(null, "filas", "AL_MENOS_UNA", null,
                    "No hay filas de datos para validar."));
            return 0;
        }

        int total = filas.size();
        for (int i = 0; i < total; i++) {
            JsonNode fila = filas.get(i);
            if (!fila.isObject() || !tieneValores(fila)) {
                detalles.add(error(i + 1, "fila", "VALOR_NO_VACÍO", null,
                        "La fila no contiene valores no vacíos."));
            }
        }
        return total;
    }

    private void validarEncabezados(JsonNode raiz, FormatoExogena formato,
                                    List<DetalleErrorDto> detalles) {
        JsonNode encabezados = raiz.get("encabezados");
        Set<String> presentes = new HashSet<>();
        if (encabezados != null && encabezados.isArray()) {
            Iterator<JsonNode> it = encabezados.elements();
            while (it.hasNext()) {
                presentes.add(normalizar(it.next().asText()));
            }
        } else {
            detalles.add(error(1, "encabezados", "LISTA", null,
                    "No se encontró la lista de encabezados del workbook."));
        }

        if (formato == null || !formato.activo() || formato.encabezados().isEmpty()) return;
        for (String esperado : formato.encabezados()) {
            if (!presentes.contains(normalizar(esperado))) {
                detalles.add(error(1, "encabezados", esperado, null,
                        "Falta el encabezado requerido por el catálogo del formato."));
            }
        }
    }

    private boolean tieneValores(JsonNode fila) {
        Iterator<JsonNode> valores = fila.elements();
        while (valores.hasNext()) {
            String valor = valores.next().asText();
            if (valor != null && !valor.isBlank()) return true;
        }
        return false;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private DetalleErrorDto error(Integer linea, String campo, String esperado,
                                  String encontrado, String mensaje) {
        return new DetalleErrorDto(linea, campo, esperado, encontrado, mensaje, "ERROR");
    }
}
