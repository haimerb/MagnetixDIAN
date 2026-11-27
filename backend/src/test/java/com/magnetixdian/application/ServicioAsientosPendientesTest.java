package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.infrastructure.persistence.AsientoPendienteJpa;
import com.magnetixdian.infrastructure.persistence.AsientoPendienteRepository;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudLineaAsientoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAsientosPendientesTest {

    @Mock
    private AsientoPendienteRepository repository;
    @Mock
    private ServicioAsientos servicioAsientos;

    private ServicioAsientosPendientes servicio;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        servicio = new ServicioAsientosPendientes(repository, servicioAsientos, objectMapper);
    }

    @Test
    void registraOutboxUnaSolaVez() {
        when(repository.existsByEmpresaIdAndOrigenAndOrigenIdAndTipoEventoAndEstadoIn(
                eq(1L), eq("FACTURA"), eq(77L), eq("VENTA"), any()))
                .thenReturn(true);

        servicio.registrar(1L, solicitud(), "error previo");

        verifyNoInteractions(servicioAsientos);
        verify(repository, times(0)).save(any());
    }

    @Test
    void procesaOutboxYMarcaProcesado() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AsientoPendienteJpa pendiente = pendiente(solicitud());

        boolean procesado = servicio.procesar(pendiente);

        assertTrue(procesado);
        assertEquals(ServicioAsientosPendientes.PROCESADO, pendiente.getEstado());
        assertEquals(0, pendiente.getIntentos());
        verify(servicioAsientos).crear(eq(1L), any(SolicitudAsientoDto.class));
    }

    @Test
    void marcaFallidoDespuesDeCincoIntentos() throws Exception {
        doThrow(new IllegalStateException("BD caída")).when(servicioAsientos).crear(any(), any());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AsientoPendienteJpa pendiente = pendiente(solicitud());

        for (int i = 0; i < 5; i++) {
            assertFalse(servicio.procesar(pendiente));
        }

        assertEquals(ServicioAsientosPendientes.FALLIDO, pendiente.getEstado());
        assertEquals(5, pendiente.getIntentos());
        assertEquals("BD caída", pendiente.getUltimoError());
        verify(repository, times(5)).save(any());
    }

    @Test
    void guardaSolicitudSerializadaAlRegistrar() {
        when(repository.existsByEmpresaIdAndOrigenAndOrigenIdAndTipoEventoAndEstadoIn(
                eq(1L), eq("FACTURA"), eq(77L), eq("VENTA"), any()))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        servicio.registrar(1L, solicitud(), "BD caída");

        ArgumentCaptor<AsientoPendienteJpa> captor = ArgumentCaptor.forClass(AsientoPendienteJpa.class);
        verify(repository).save(captor.capture());
        AsientoPendienteJpa guardado = captor.getValue();
        assertEquals("FACTURA", guardado.getOrigen());
        assertEquals(77L, guardado.getOrigenId());
        assertEquals(ServicioAsientosPendientes.PENDIENTE, guardado.getEstado());
        assertEquals("BD caída", guardado.getUltimoError());
        assertTrue(guardado.getPayload().contains("VENTA"));
    }

    private AsientoPendienteJpa pendiente(SolicitudAsientoDto solicitud) throws Exception {
        AsientoPendienteJpa pendiente = new AsientoPendienteJpa();
        pendiente.setEmpresaId(1L);
        pendiente.setOrigen(solicitud.origen());
        pendiente.setOrigenId(solicitud.origenId());
        pendiente.setTipoEvento(solicitud.tipoEvento());
        pendiente.setFecha(solicitud.fecha());
        pendiente.setPayload(objectMapper.writeValueAsString(solicitud));
        pendiente.setEstado(ServicioAsientosPendientes.PENDIENTE);
        return pendiente;
    }

    private static SolicitudAsientoDto solicitud() {
        List<SolicitudLineaAsientoDto> lineas = List.of(
                new SolicitudLineaAsientoDto("130505", "Cliente", new BigDecimal("1000.00"),
                        BigDecimal.ZERO, null, null, new BigDecimal("1000.00"), null),
                new SolicitudLineaAsientoDto("413505", "Venta", BigDecimal.ZERO,
                        new BigDecimal("1000.00"), null, null, new BigDecimal("1000.00"), null));
        return new SolicitudAsientoDto(LocalDate.of(2026, 3, 15), "Factura 1", "VENTA",
                lineas, "FACTURA", 77L);
    }
}
