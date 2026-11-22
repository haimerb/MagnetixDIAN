package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.infrastructure.excel.ImportadorMultiFormato;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.interfaces.dto.FormatoDetectadoDto;
import com.magnetixdian.interfaces.dto.ImportacionMultiFormatoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioImportacionExogenaTest {

    @Mock
    private ImportadorMultiFormato importador;
    @Mock
    private ServicioMedioMagnetico servicioMedioMagnetico;
    @Mock
    private MedioMagneticoDatosJsonRepository datosJsonRepository;

    private ServicioImportacionExogena servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioImportacionExogena(
                importador, servicioMedioMagnetico, datosJsonRepository, new ObjectMapper());
    }

    @Test
    void soloPersisteFormatosActivos() throws IOException {
        FormatoDetectadoDto activo = new FormatoDetectadoDto(
                "1001", "1001", "ACTIVO", true, 1,
                List.of("NIT"), List.of(Map.of("NIT", "900123456")), null);
        FormatoDetectadoDto inactivo = new FormatoDetectadoDto(
                "1003", "1003", "PENDIENTE_GENERADOR", false, 1,
                List.of("CONCEPTO"), List.of(Map.of("CONCEPTO", "C1003")), null);
        when(importador.importar(any(MultipartFile.class))).thenReturn(
                new ImportacionMultiFormatoDto(null, List.of(activo, inactivo), List.of(), 0));

        MedioMagneticoJpa medio = mock(MedioMagneticoJpa.class);
        when(medio.getId()).thenReturn(10L);
        when(servicioMedioMagnetico.crearOReusar(1L, "1001", 2025)).thenReturn(medio);

        ImportacionMultiFormatoDto resultado =
                servicio.importar(1L, 2025, mock(MultipartFile.class));

        assertEquals(1, resultado.formatosPersistidos());
        assertEquals(10L, resultado.formatos().get(0).medioMagneticoId());
        verify(datosJsonRepository).actualizarDatosJson(eq(10L), anyString());
        verify(servicioMedioMagnetico, never()).crearOReusar(eq(1L), eq("1003"), anyInt());
    }
}
