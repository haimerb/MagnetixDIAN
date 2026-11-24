package com.magnetixdian.facturacion;

import com.magnetixdian.application.ServicioCausacionRadian;
import com.magnetixdian.application.ServicioDocumentosRadian;
import com.magnetixdian.application.ServicioRecepcionRadian;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianRecepcionDto;
import com.magnetixdian.interfaces.dto.ResultadoCausacionDto;
import com.magnetixdian.interfaces.dto.ResultadoRecepcionRadianDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioRecepcionRadianTest {

    @Mock
    private ServicioDocumentosRadian documentos;
    @Mock
    private ServicioCausacionRadian causacion;
    @Mock
    private DocumentoRadianRepository repository;

    private ServicioRecepcionRadian servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioRecepcionRadian(documentos, causacion, repository);
    }

    @Test
    void recibirDocumentoNuevoImportaYCausa() {
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-R-001"))
                .thenReturn(Optional.empty());
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "EMITIDO", "CUFE-R-001"))
                .thenReturn(Optional.empty());
        when(documentos.importar(eq(1L), eq("RECIBIDO"), any(byte[].class), eq("recepcion.json")))
                .thenReturn(List.of(documentoImportado()));
        when(causacion.causar(eq(1L), eq(5L), any())).thenReturn(new ResultadoCausacionDto(
                5L, 9L, "CAUSADA", LocalDate.of(2026, 3, 1), new BigDecimal("1190.00"), "OK"));

        ResultadoRecepcionRadianDto resultado = servicio.recibir(1L, solicitud());

        assertFalse(resultado.duplicado());
        assertTrue(resultado.causado());
        assertEquals(5L, resultado.documentoId());
        assertEquals("01", resultado.tipoDocumento());
        assertEquals("CUFE-R-001", resultado.cufeCude());
        verify(causacion).causar(eq(1L), eq(5L), any());
    }

    @Test
    void recibirDuplicadoCausadoNoImportaNiCausa() {
        DocumentoRadianJpa duplicado = new DocumentoRadianJpa();
        duplicado.setId(5L);
        duplicado.setEstadoCausacion("CAUSADA");
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-R-001"))
                .thenReturn(Optional.of(duplicado));

        ResultadoRecepcionRadianDto resultado = servicio.recibir(1L, solicitud());

        assertTrue(resultado.duplicado());
        assertTrue(resultado.causado());
        verify(documentos, never()).importar(any(), anyString(), any(), anyString());
        verify(causacion, never()).causar(any(), any(), any());
    }

    @Test
    void recibirDuplicadoPendienteNoSeMarcacausado() {
        DocumentoRadianJpa duplicado = new DocumentoRadianJpa();
        duplicado.setId(5L);
        duplicado.setEstadoCausacion("PENDIENTE");
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-R-001"))
                .thenReturn(Optional.of(duplicado));

        ResultadoRecepcionRadianDto resultado = servicio.recibir(1L, solicitud());

        assertTrue(resultado.duplicado());
        assertFalse(resultado.causado());
    }

    @Test
    void recibirSinCufeEsRechazado() {
        assertThrows(IllegalArgumentException.class, () -> servicio.recibir(1L, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.recibir(1L,
                new DocumentoRadianRecepcionDto("RECIBIDO", "01", "  ", null, null, null,
                        LocalDate.of(2026, 3, 1), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.TEN, null)));
    }

    @Test
    void recibirNormalizaAliasDeTipo() {
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-R-001"))
                .thenReturn(Optional.empty());
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "EMITIDO", "CUFE-R-001"))
                .thenReturn(Optional.empty());
        when(documentos.importar(eq(1L), eq("RECIBIDO"), any(byte[].class), eq("recepcion.json")))
                .thenReturn(List.of(documentoImportado()));
        when(causacion.causar(eq(1L), eq(5L), any())).thenReturn(new ResultadoCausacionDto(
                5L, 9L, "CAUSADA", LocalDate.of(2026, 3, 1), new BigDecimal("1190.00"), "OK"));

        DocumentoRadianRecepcionDto solicitud = new DocumentoRadianRecepcionDto("RECIBIDO", "nomina",
                "CUFE-R-001", "N-001", "900123456", "MAGNETIXDIAN SAS", LocalDate.of(2026, 3, 1),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), null);
        servicio.recibir(1L, solicitud);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(documentos).importar(eq(1L), eq("RECIBIDO"), captor.capture(), eq("recepcion.json"));
        assertTrue(new String(captor.getValue()).contains("\"31\""));
    }

    private DocumentoRadianRecepcionDto solicitud() {
        return new DocumentoRadianRecepcionDto("RECIBIDO", "01", "CUFE-R-001", "F-001",
                "900123456", "MAGNETIXDIAN SAS", LocalDate.of(2026, 3, 1),
                new BigDecimal("1000.00"), new BigDecimal("190.00"), BigDecimal.ZERO,
                new BigDecimal("1190.00"), null);
    }

    private DocumentoRadianDto documentoImportado() {
        return new DocumentoRadianDto(5L, 1L, "RECIBIDO", "01", "CUFE-R-001", "F-001",
                "900123456", "MAGNETIXDIAN SAS", LocalDate.of(2026, 3, 1),
                new BigDecimal("1000.00"), new BigDecimal("190.00"), BigDecimal.ZERO,
                new BigDecimal("1190.00"), "RECIBIDO", "PENDIENTE", null, null, null, List.of(), List.of());
    }
}
