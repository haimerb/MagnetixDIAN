package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CausacionRadianJpa;
import com.magnetixdian.infrastructure.persistence.CausacionRadianRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.ResultadoCausacionDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCausacionRadianTest {

    @Mock
    private DocumentoRadianRepository documentoRepository;
    @Mock
    private CausacionRadianRepository causacionRepository;

    private ServicioCausacionRadian servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioCausacionRadian(documentoRepository, causacionRepository);
    }

    @Test
    void causarDocumentoNuevo() {
        DocumentoRadianJpa documento = documento("1190.00", "50.00");
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(documento));
        when(causacionRepository.findByDocumentoRadianId(5L)).thenReturn(Optional.empty());
        when(causacionRepository.saveAndFlush(any(CausacionRadianJpa.class))).thenAnswer(invocation -> {
            CausacionRadianJpa causacion = invocation.getArgument(0);
            causacion.setId(9L);
            return causacion;
        });

        ResultadoCausacionDto resultado = servicio.causar(1L, 5L,
                new SolicitudCausacionDto(LocalDate.of(2026, 3, 1), "Causación prueba"));

        assertEquals(9L, resultado.causacionId());
        assertEquals(LocalDate.of(2026, 3, 1), resultado.fechaCausacion());
        assertEquals(new BigDecimal("1190.00"), resultado.total());
        assertEquals("CAUSADA", documento.getEstadoCausacion());
        verify(documentoRepository).save(documento);
    }

    @Test
    void causarSinSolicitudUsaFechaDeHoyYSinObservaciones() {
        DocumentoRadianJpa documento = documento("1190.00", "50.00");
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(documento));
        when(causacionRepository.findByDocumentoRadianId(5L)).thenReturn(Optional.empty());
        when(causacionRepository.saveAndFlush(any(CausacionRadianJpa.class))).thenAnswer(invocation -> {
            CausacionRadianJpa causacion = invocation.getArgument(0);
            causacion.setId(9L);
            return causacion;
        });

        ResultadoCausacionDto resultado = servicio.causar(1L, 5L, null);

        ArgumentCaptor<CausacionRadianJpa> captor = ArgumentCaptor.forClass(CausacionRadianJpa.class);
        verify(causacionRepository).saveAndFlush(captor.capture());
        CausacionRadianJpa guardada = captor.getValue();
        assertEquals(LocalDate.now(), guardada.getFechaCausacion());
        assertEquals(LocalDate.now(), resultado.fechaCausacion());
        assertEquals(null, guardada.getObservaciones());
    }

    @Test
    void causarConFechaNulaUsaFechaDeHoy() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(documento("1190.00", "50.00")));
        when(causacionRepository.findByDocumentoRadianId(5L)).thenReturn(Optional.empty());
        when(causacionRepository.saveAndFlush(any(CausacionRadianJpa.class))).thenAnswer(invocation -> {
            CausacionRadianJpa causacion = invocation.getArgument(0);
            causacion.setId(9L);
            return causacion;
        });

        ResultadoCausacionDto resultado = servicio.causar(1L, 5L,
                new SolicitudCausacionDto(null, null));

        assertEquals(LocalDate.now(), resultado.fechaCausacion());
    }

    @Test
    void causarDosVecesEsIdempotente() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(documento("1190.00", "50.00")));
        when(causacionRepository.findByDocumentoRadianId(5L))
                .thenReturn(Optional.of(causacion(9L)));

        ResultadoCausacionDto resultado = servicio.causar(1L, 5L,
                new SolicitudCausacionDto(LocalDate.of(2026, 3, 1), null));

        assertEquals(9L, resultado.causacionId());
        verify(causacionRepository, never()).saveAndFlush(any(CausacionRadianJpa.class));
    }

    @Test
    void retencionesSuperioresAlTotalFallan() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(documento("100.00", "150.00")));
        when(causacionRepository.findByDocumentoRadianId(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> servicio.causar(1L, 5L,
                new SolicitudCausacionDto(LocalDate.of(2026, 3, 1), null)));
        verify(causacionRepository, never()).saveAndFlush(any(CausacionRadianJpa.class));
    }

    @Test
    void documentoInexistenteFalla() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> servicio.causar(1L, 5L, null));
    }

    private static DocumentoRadianJpa documento(String total, String retenciones) {
        DocumentoRadianJpa entidad = new DocumentoRadianJpa();
        entidad.setId(5L);
        entidad.setEmpresaId(1L);
        entidad.setDireccion("EMITIDO");
        entidad.setTipoDocumento("01");
        entidad.setCufeCude("CUFE-5");
        entidad.setConsecutivo("F-005");
        entidad.setFecha(LocalDate.of(2026, 2, 20));
        entidad.setSubtotal(new BigDecimal("1000.00"));
        entidad.setImpuestos(new BigDecimal("190.00"));
        entidad.setRetenciones(new BigDecimal(retenciones));
        entidad.setTotal(new BigDecimal(total));
        entidad.setEstadoCausacion("PENDIENTE");
        return entidad;
    }

    private static CausacionRadianJpa causacion(Long id) {
        CausacionRadianJpa causacion = new CausacionRadianJpa();
        causacion.setId(id);
        causacion.setDocumentoRadianId(5L);
        causacion.setEmpresaId(1L);
        causacion.setFechaCausacion(LocalDate.of(2026, 3, 1));
        causacion.setTotal(new BigDecimal("1190.00"));
        causacion.setEstado("CAUSADA");
        return causacion;
    }
}
