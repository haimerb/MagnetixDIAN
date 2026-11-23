package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.ResponsabilidadFiscalJpa;
import com.magnetixdian.infrastructure.persistence.ResponsabilidadFiscalRepository;
import com.magnetixdian.interfaces.dto.ResponsabilidadFiscalDto;
import com.magnetixdian.interfaces.dto.SolicitudResponsabilidadFiscalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioResponsabilidadesFiscalesTest {

    @Mock
    private ResponsabilidadFiscalRepository repository;

    private ServicioResponsabilidadesFiscales servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioResponsabilidadesFiscales(repository);
    }

    @Test
    void registrarNormalizaCodigos() {
        when(repository.save(any(ResponsabilidadFiscalJpa.class))).thenAnswer(invocacion -> {
            ResponsabilidadFiscalJpa responsabilidad = invocacion.getArgument(0);
            ReflectionTestUtils.setField(responsabilidad, "id", 5L);
            return responsabilidad;
        });

        ResponsabilidadFiscalDto resultado = servicio.registrar(1L,
                new SolicitudResponsabilidadFiscalDto(
                        9L, " iva ", " responsable iva ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        assertEquals(5L, resultado.id());
        assertEquals(1L, resultado.empresaId());
        assertEquals("IVA", resultado.impuestoCodigo());
        assertEquals("RESPONSABLE IVA", resultado.codigo());

        ArgumentCaptor<ResponsabilidadFiscalJpa> captor = ArgumentCaptor.forClass(ResponsabilidadFiscalJpa.class);
        verify(repository).save(captor.capture());
        assertEquals(9L, captor.getValue().getTerceroId());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().getDesde());
        assertEquals(LocalDate.of(2026, 12, 31), captor.getValue().getHasta());
    }

    @Test
    void registrarRechazaDatosIncompletos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(1L,
                new SolicitudResponsabilidadFiscalDto(
                        9L, " ", "IVA", LocalDate.of(2026, 1, 1), null)));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(1L,
                new SolicitudResponsabilidadFiscalDto(
                        9L, "IVA", " ", LocalDate.of(2026, 1, 1), null)));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(1L,
                new SolicitudResponsabilidadFiscalDto(9L, "IVA", "IVA", null, null)));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(1L,
                new SolicitudResponsabilidadFiscalDto(
                        9L, "IVA", "IVA", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1))));
    }

    @Test
    void esResponsableRespetaVigenciaYTercero() {
        when(repository.findByEmpresaIdAndImpuestoCodigo(1L, "IVA")).thenReturn(List.of(
                responsabilidad(null, "IVA", "GENERAL",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
                responsabilidad(9L, "IVA", "ESPECIFICA",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                responsabilidad(null, "IVA", "VENCIDA",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));

        assertTrue(servicio.esResponsable(1L, " iva ", LocalDate.of(2026, 3, 15), 9L));
        assertTrue(servicio.esResponsable(1L, "IVA", LocalDate.of(2026, 3, 15), 77L));
        assertFalse(servicio.esResponsable(1L, "IVA", LocalDate.of(2026, 7, 1), 9L));
        assertFalse(servicio.esResponsable(1L, "IVA", LocalDate.of(2027, 1, 1), 9L));
    }

    @Test
    void listarDelegadaEnRepositorio() {
        when(repository.findByEmpresaIdOrderByImpuestoCodigoAscDesdeDesc(1L))
                .thenReturn(List.of(responsabilidad(9L, "IVA", "IVA",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))));

        List<ResponsabilidadFiscalDto> resultado = servicio.listar(1L);

        assertEquals(1, resultado.size());
        assertEquals("IVA", resultado.get(0).codigo());
    }

    private static ResponsabilidadFiscalJpa responsabilidad(Long terceroId, String impuestoCodigo,
                                                             String codigo, LocalDate desde, LocalDate hasta) {
        ResponsabilidadFiscalJpa responsabilidad = new ResponsabilidadFiscalJpa();
        responsabilidad.setEmpresaId(1L);
        responsabilidad.setTerceroId(terceroId);
        responsabilidad.setImpuestoCodigo(impuestoCodigo);
        responsabilidad.setCodigo(codigo);
        responsabilidad.setDesde(desde);
        responsabilidad.setHasta(hasta);
        return responsabilidad;
    }
}
