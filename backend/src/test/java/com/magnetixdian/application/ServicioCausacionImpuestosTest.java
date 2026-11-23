package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoRepository;
import com.magnetixdian.interfaces.dto.CausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudPagoImpuestoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCausacionImpuestosTest {

    @Mock
    private ImpuestoCausacionRepository causacionRepository;
    @Mock
    private ImpuestoPagoRepository pagoRepository;
    @Mock
    private ServicioTarifasImpuesto servicioTarifas;
    @Mock
    private ServicioResponsabilidadesFiscales servicioResponsabilidades;

    private ServicioCausacionImpuestos servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioCausacionImpuestos(
                causacionRepository, pagoRepository, servicioTarifas, servicioResponsabilidades);
    }

    @Test
    void causaImpuestoConTarifaVigente() {
        when(causacionRepository.findByEmpresaIdAndOrigenAndOrigenIdAndImpuestoCodigo(
                1L, "FACTURA", 5L, "01")).thenReturn(Optional.empty());
        when(servicioResponsabilidades.esResponsable(
                1L, "01", LocalDate.of(2026, 3, 15), null)).thenReturn(true);
        when(servicioTarifas.calcular("01", 2026, null, new BigDecimal("1000000.00")))
                .thenReturn(new ResultadoCalculoImpuestoDto(
                        "01", "IVA", 2026, null,
                        new BigDecimal("1000000.00"), new BigDecimal("52374.00"),
                        BigDecimal.ZERO, null, new BigDecimal("1000000.00"),
                        new BigDecimal("0.1900"), true, new BigDecimal("190000.00")));
        when(pagoRepository.sumarPorCausacion(any())).thenReturn(BigDecimal.ZERO);
        when(causacionRepository.save(any(ImpuestoCausacionJpa.class))).thenAnswer(invocacion -> {
            ImpuestoCausacionJpa causacion = invocacion.getArgument(0);
            causacion.setId(10L);
            return causacion;
        });

        CausacionImpuestoDto resultado = servicio.causar(1L,
                new SolicitudCausacionImpuestoDto(
                        "01", "FACTURA", 5L, null, LocalDate.of(2026, 3, 15),
                        null, new BigDecimal("1000000.00"), "Causación de prueba"));

        assertEquals(10L, resultado.id());
        assertEquals("2026-03", resultado.periodo());
        assertEquals(0, new BigDecimal("190000.00").compareTo(resultado.valor()));
        assertEquals("CAUSADO", resultado.estado());
    }

    @Test
    void rechazaCausacionSinResponsabilidadFiscal() {
        when(causacionRepository.findByEmpresaIdAndOrigenAndOrigenIdAndImpuestoCodigo(
                1L, "FACTURA", 5L, "01")).thenReturn(Optional.empty());
        when(servicioResponsabilidades.esResponsable(
                1L, "01", LocalDate.of(2026, 3, 15), null)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> servicio.causar(1L,
                new SolicitudCausacionImpuestoDto(
                        "01", "FACTURA", 5L, null, LocalDate.of(2026, 3, 15),
                        null, new BigDecimal("1000000.00"), null)));
    }

    @Test
    void marcaCausacionComoPagadaAlCompletarElPago() {
        ImpuestoCausacionJpa causacion = new ImpuestoCausacionJpa();
        causacion.setId(11L);
        causacion.setEmpresaId(1L);
        causacion.setImpuestoCodigo("01");
        causacion.setOrigen("FACTURA");
        causacion.setOrigenId(5L);
        causacion.setPeriodo("2026-03");
        causacion.setFecha(LocalDate.of(2026, 3, 15));
        causacion.setBaseImponible(new BigDecimal("1000000.00"));
        causacion.setTarifa(new BigDecimal("0.1900"));
        causacion.setValor(new BigDecimal("100.00"));
        causacion.setEstado("CAUSADO");

        when(causacionRepository.findById(11L)).thenReturn(Optional.of(causacion));
        when(pagoRepository.sumarPorCausacion(11L)).thenReturn(new BigDecimal("60.00"));
        when(pagoRepository.save(any(ImpuestoPagoJpa.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrarPago(1L, 11L, new SolicitudPagoImpuestoDto(
                LocalDate.of(2026, 3, 20), new BigDecimal("40.00"), "COMP-1"));

        verify(causacionRepository).save(argThat(registro -> "PAGADO".equals(registro.getEstado())));
    }
}
