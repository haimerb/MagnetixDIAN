package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioGeneracionAutomaticaTest {

    private MedioMagneticoRepository medioRepo;
    private ServicioMedioMagnetico servicioMedio;
    private ServicioGeneracionAutomatica servicio;

    @BeforeEach
    void setUp() {
        medioRepo = mock(MedioMagneticoRepository.class);
        servicioMedio = mock(ServicioMedioMagnetico.class);
        servicio = new ServicioGeneracionAutomatica(medioRepo, servicioMedio);
    }

    private MedioMagneticoJpa medio(long id) {
        MedioMagneticoJpa m = mock(MedioMagneticoJpa.class);
        when(m.getId()).thenReturn(id);
        return m;
    }

    @Test
    void sinMediosValidadosNoHaceNada() {
        when(medioRepo.findByEstadoIn(List.of("VALIDADO"))).thenReturn(List.of());
        assertEquals(0, servicio.generarPendientes());
        verify(servicioMedio, never()).generarXml(anyLong());
    }

    @Test
    void generaXmlDeTodosLosValidados() {
        MedioMagneticoJpa m1 = medio(1);
        MedioMagneticoJpa m2 = medio(2);
        when(medioRepo.findByEstadoIn(List.of("VALIDADO"))).thenReturn(List.of(m1, m2));
        assertEquals(2, servicio.generarPendientes());
        verify(servicioMedio).generarXml(1L);
        verify(servicioMedio).generarXml(2L);
    }

    @Test
    void unFalloNoDetieneElLote() {
        MedioMagneticoJpa m1 = medio(1);
        MedioMagneticoJpa m2 = medio(2);
        when(medioRepo.findByEstadoIn(List.of("VALIDADO"))).thenReturn(List.of(m1, m2));
        doThrow(new IllegalStateException("no validado")).when(servicioMedio).generarXml(1L);
        assertEquals(1, servicio.generarPendientes());
        verify(servicioMedio, times(1)).generarXml(1L);
        verify(servicioMedio, times(1)).generarXml(2L);
    }
}
