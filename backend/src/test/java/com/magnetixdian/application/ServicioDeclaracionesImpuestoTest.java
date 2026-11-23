package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CalendarioTributarioJpa;
import com.magnetixdian.infrastructure.persistence.CalendarioTributarioRepository;
import com.magnetixdian.infrastructure.persistence.DeclaracionImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.DeclaracionImpuestoRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoRepository;
import com.magnetixdian.interfaces.dto.CalendarioTributarioDto;
import com.magnetixdian.interfaces.dto.DeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudDeclaracionImpuestoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioDeclaracionesImpuestoTest {

    @Mock
    private DeclaracionImpuestoRepository declaracionRepository;
    @Mock
    private ImpuestoCausacionRepository causacionRepository;
    @Mock
    private ImpuestoPagoRepository pagoRepository;
    @Mock
    private CalendarioTributarioRepository calendarioRepository;

    private ServicioDeclaracionesImpuesto servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioDeclaracionesImpuesto(
                declaracionRepository, causacionRepository, pagoRepository, calendarioRepository);
    }

    @Test
    void generarBorradorCalculaSaldosYUsaFechaDelCalendario() {
        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);
        when(causacionRepository.findByEmpresaIdAndImpuestoCodigoAndFechaBetweenOrderByFechaAscIdAsc(
                1L, "IVA", desde, hasta)).thenReturn(List.of(
                causacion("CAUSADO", "190000.00"),
                causacion("ANULADO", "50000.00")));
        when(pagoRepository.sumarPorEmpresaImpuestoPeriodo(1L, "IVA", desde, hasta))
                .thenReturn(new BigDecimal("40000.00"));
        when(calendarioRepository.findFirstByImpuestoCodigoAndAnioGravableAndPeriodo(
                "IVA", 2026, "2026-03")).thenReturn(Optional.of(calendario(LocalDate.of(2026, 4, 15))));
        when(declaracionRepository.findByEmpresaIdAndImpuestoCodigoAndAnioGravableAndPeriodo(
                1L, "IVA", 2026, "2026-03")).thenReturn(Optional.empty());
        when(declaracionRepository.save(any(DeclaracionImpuestoJpa.class))).thenAnswer(invocacion -> {
            DeclaracionImpuestoJpa declaracion = invocacion.getArgument(0);
            ReflectionTestUtils.setField(declaracion, "id", 7L);
            return declaracion;
        });

        DeclaracionImpuestoDto resultado = servicio.generarBorrador(1L,
                new SolicitudDeclaracionImpuestoDto(" iva ", null, "  ", desde, hasta));

        assertEquals(7L, resultado.id());
        assertEquals("IVA", resultado.impuestoCodigo());
        assertEquals(2026, resultado.anioGravable());
        assertEquals("2026-03", resultado.periodo());
        assertEquals(0, new BigDecimal("190000.00").compareTo(resultado.totalCausado()));
        assertEquals(0, new BigDecimal("40000.00").compareTo(resultado.totalPagado()));
        assertEquals(0, new BigDecimal("150000.00").compareTo(resultado.saldo()));
        assertEquals(LocalDate.of(2026, 4, 15), resultado.fechaLimite());
        assertEquals("BORRADOR", resultado.estado());
    }

    @Test
    void generarBorradorUsaFechaLimitePorDefecto() {
        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);
        when(causacionRepository.findByEmpresaIdAndImpuestoCodigoAndFechaBetweenOrderByFechaAscIdAsc(
                1L, "01", desde, hasta)).thenReturn(List.of());
        when(pagoRepository.sumarPorEmpresaImpuestoPeriodo(1L, "01", desde, hasta))
                .thenReturn(BigDecimal.ZERO);
        when(calendarioRepository.findFirstByImpuestoCodigoAndAnioGravableAndPeriodo(
                "01", 2026, "2026-03")).thenReturn(Optional.empty());
        when(declaracionRepository.findByEmpresaIdAndImpuestoCodigoAndAnioGravableAndPeriodo(
                1L, "01", 2026, "2026-03")).thenReturn(Optional.empty());
        when(declaracionRepository.save(any(DeclaracionImpuestoJpa.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DeclaracionImpuestoDto resultado = servicio.generarBorrador(1L,
                new SolicitudDeclaracionImpuestoDto("01", 2026, "2026-03", desde, hasta));

        assertEquals(LocalDate.of(2026, 4, 20), resultado.fechaLimite());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.saldo()));
    }

    @Test
    void generarBorradorRechazaDeclaracionPresentada() {
        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);
        when(causacionRepository.findByEmpresaIdAndImpuestoCodigoAndFechaBetweenOrderByFechaAscIdAsc(
                1L, "IVA", desde, hasta)).thenReturn(List.of());
        when(pagoRepository.sumarPorEmpresaImpuestoPeriodo(1L, "IVA", desde, hasta))
                .thenReturn(BigDecimal.ZERO);
        when(calendarioRepository.findFirstByImpuestoCodigoAndAnioGravableAndPeriodo(
                "IVA", 2026, "2026-03")).thenReturn(Optional.empty());
        when(declaracionRepository.findByEmpresaIdAndImpuestoCodigoAndAnioGravableAndPeriodo(
                1L, "IVA", 2026, "2026-03"))
                .thenReturn(Optional.of(declaracion("PRESENTADA")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.generarBorrador(1L,
                        new SolicitudDeclaracionImpuestoDto("IVA", 2026, "2026-03", desde, hasta)));

        assertEquals("La declaración ya fue presentada y no puede recalcularse.", error.getMessage());
        verify(declaracionRepository, never()).save(any());
    }

    @Test
    void generarBorradorRechazaRangoDeFechasInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarBorrador(1L,
                new SolicitudDeclaracionImpuestoDto(
                        "IVA", 2026, "2026-03", LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1))));
    }

    @Test
    void marcarPresentadaActualizaEstado() {
        DeclaracionImpuestoJpa declaracion = declaracion("BORRADOR");
        when(declaracionRepository.findById(7L)).thenReturn(Optional.of(declaracion));
        when(declaracionRepository.save(any(DeclaracionImpuestoJpa.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DeclaracionImpuestoDto resultado = servicio.marcarPresentada(1L, 7L);

        assertEquals("PRESENTADA", resultado.estado());
        assertEquals(1L, resultado.empresaId());
    }

    @Test
    void marcarPresentadaRechazaDeclaracionDeOtraEmpresa() {
        DeclaracionImpuestoJpa declaracion = declaracion("BORRADOR");
        declaracion.setEmpresaId(2L);
        when(declaracionRepository.findById(7L)).thenReturn(Optional.of(declaracion));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.marcarPresentada(1L, 7L));

        assertEquals("Declaración no encontrada.", error.getMessage());
        verify(declaracionRepository, never()).save(any());
    }

    @Test
    void listarFiltraPorAnio() {
        when(declaracionRepository.findByEmpresaIdAndAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(1L, 2026))
                .thenReturn(List.of(declaracion("BORRADOR")));

        List<DeclaracionImpuestoDto> resultado = servicio.listar(1L, 2026);

        assertEquals(1, resultado.size());
        assertEquals("IVA", resultado.get(0).impuestoCodigo());
    }

    @Test
    void listarCalendarioMapeaRegistros() {
        when(calendarioRepository.findByAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(2026))
                .thenReturn(List.of(calendario(LocalDate.of(2026, 4, 20))));

        List<CalendarioTributarioDto> resultado = servicio.listarCalendario(2026);

        assertEquals(1, resultado.size());
        assertEquals("IVA", resultado.get(0).impuestoCodigo());
        assertEquals("MENSUAL", resultado.get(0).periodicidad());
        assertEquals(LocalDate.of(2026, 4, 20), resultado.get(0).fechaLimite());
    }

    private static DeclaracionImpuestoJpa declaracion(String estado) {
        DeclaracionImpuestoJpa declaracion = new DeclaracionImpuestoJpa();
        ReflectionTestUtils.setField(declaracion, "id", 7L);
        declaracion.setEmpresaId(1L);
        declaracion.setImpuestoCodigo("IVA");
        declaracion.setAnioGravable(2026);
        declaracion.setPeriodo("2026-03");
        declaracion.setFechaDesde(LocalDate.of(2026, 3, 1));
        declaracion.setFechaHasta(LocalDate.of(2026, 3, 31));
        declaracion.setFechaLimite(LocalDate.of(2026, 4, 20));
        declaracion.setEstado(estado);
        declaracion.setTotalCausado(BigDecimal.ZERO);
        declaracion.setTotalPagado(BigDecimal.ZERO);
        declaracion.setSaldo(BigDecimal.ZERO);
        return declaracion;
    }

    private static ImpuestoCausacionJpa causacion(String estado, String valor) {
        ImpuestoCausacionJpa causacion = new ImpuestoCausacionJpa();
        causacion.setEstado(estado);
        causacion.setValor(new BigDecimal(valor));
        return causacion;
    }

    private static CalendarioTributarioJpa calendario(LocalDate fechaLimite) {
        CalendarioTributarioJpa calendario = new CalendarioTributarioJpa();
        ReflectionTestUtils.setField(calendario, "id", 4L);
        ReflectionTestUtils.setField(calendario, "impuestoCodigo", "IVA");
        ReflectionTestUtils.setField(calendario, "anioGravable", 2026);
        ReflectionTestUtils.setField(calendario, "periodicidad", "MENSUAL");
        ReflectionTestUtils.setField(calendario, "periodo", "2026-03");
        ReflectionTestUtils.setField(calendario, "fechaLimite", fechaLimite);
        return calendario;
    }
}
