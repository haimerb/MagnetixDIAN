package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.evento.DepreciacionCalculadaEvent;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpActivoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpActivoRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpDepreciacionJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpDepreciacionRepository;
import com.magnetixdian.interfaces.dto.erp.ActivoDto;
import com.magnetixdian.interfaces.dto.erp.ActivoRequestDto;
import com.magnetixdian.interfaces.dto.erp.DepreciacionDto;
import com.magnetixdian.interfaces.dto.erp.DepreciacionRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

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
class ServicioActivosTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 15);

    @Mock
    private ErpActivoRepository activos;
    @Mock
    private ErpDepreciacionRepository depreciaciones;
    @Mock
    private PublicadorEventosErp publicador;

    private ServicioActivos servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioActivos(activos, depreciaciones, publicador);
    }

    @Test
    void darAltaInicializaValorLibros() {
        when(activos.save(any(ErpActivoJpa.class))).thenAnswer(invocation -> {
            ErpActivoJpa activo = invocation.getArgument(0);
            ReflectionTestUtils.setField(activo, "id", 1L);
            return activo;
        });

        ActivoDto dto = servicio.darAlta(1L, new ActivoRequestDto("ACT-0001", "Computador",
                "Equipo de oficina", FECHA, new BigDecimal("1000"), new BigDecimal("100"), 12, null));

        assertEquals("ACTIVO", dto.estado());
        assertEquals(new BigDecimal("1000.00"), dto.valorLibros());
        assertEquals(new BigDecimal("0.00"), dto.depreciacionAcumulada());
    }

    @Test
    void darAltaRechazaCodigoDuplicado() {
        when(activos.existsByEmpresaIdAndCodigo(1L, "ACT-0001")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.darAlta(1L, new ActivoRequestDto("ACT-0001", "Computador",
                        "Equipo de oficina", FECHA, new BigDecimal("1000"), new BigDecimal("100"), 12, null)));

        assertEquals("Ya existe un activo con el código ACT-0001 para la empresa", error.getMessage());
        verify(activos, never()).save(any(ErpActivoJpa.class));
    }

    @Test
    void darAltaConvierteConflictoDeUnicidadEnSolicitudInvalida() {
        when(activos.save(any(ErpActivoJpa.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate", new RuntimeException("duplicate key value violates unique constraint \"uq_erp_activo_empresa_codigo\"")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.darAlta(1L, new ActivoRequestDto("ACT-0001", "Computador",
                        "Equipo de oficina", FECHA, new BigDecimal("1000"), new BigDecimal("100"), 12, null)));

        assertEquals("Ya existe un activo con el código ACT-0001 para la empresa", error.getMessage());
    }

    @Test
    void calcularDepreciacionPublicaEventoYActualizaActivo() {
        ErpActivoJpa activo = activo();
        when(activos.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(activo));
        when(depreciaciones.findByActivoIdAndPeriodo(1L, "2026-03")).thenReturn(Optional.empty());
        when(depreciaciones.save(any(ErpDepreciacionJpa.class))).thenAnswer(invocation -> {
            ErpDepreciacionJpa depreciacion = invocation.getArgument(0);
            ReflectionTestUtils.setField(depreciacion, "id", 9L);
            return depreciacion;
        });

        DepreciacionDto dto = servicio.calcularDepreciacion(1L, 1L,
                new DepreciacionRequestDto("2026-03", FECHA));

        assertEquals(new BigDecimal("75.00"), dto.valor());
        assertEquals(new BigDecimal("175.00"), dto.depreciacionAcumulada());
        assertEquals(new BigDecimal("825.00"), dto.valorLibros());
        assertEquals(new BigDecimal("175.00"), activo.getDepreciacionAcumulada());
        assertEquals(new BigDecimal("825.00"), activo.getValorLibros());
        ArgumentCaptor<DepreciacionCalculadaEvent> captor = ArgumentCaptor.forClass(DepreciacionCalculadaEvent.class);
        verify(publicador).publicar(captor.capture());
        assertEquals(9L, captor.getValue().depreciacionId());
        assertEquals("ACT-0001", captor.getValue().codigoActivo());
        assertEquals(new BigDecimal("75.00"), captor.getValue().valor());
        verify(activos).save(activo);
    }

    @Test
    void calcularDepreciacionRechazaPeriodoDuplicado() {
        when(activos.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(activo()));
        when(depreciaciones.findByActivoIdAndPeriodo(1L, "2026-03")).thenReturn(Optional.of(depreciacion()));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcularDepreciacion(1L, 1L, new DepreciacionRequestDto("2026-03", FECHA)));

        verify(depreciaciones, never()).save(any(ErpDepreciacionJpa.class));
        verify(publicador, never()).publicar(any());
    }

    private static ErpActivoJpa activo() {
        ErpActivoJpa activo = new ErpActivoJpa();
        activo.setEmpresaId(1L);
        activo.setCodigo("ACT-0001");
        activo.setNombre("Computador");
        activo.setCategoria("Equipo de oficina");
        activo.setFechaAlta(FECHA);
        activo.setValorAdquisicion(new BigDecimal("1000"));
        activo.setValorResidual(new BigDecimal("100"));
        activo.setVidaUtilMeses(12);
        activo.setDepreciacionAcumulada(new BigDecimal("100"));
        activo.setValorLibros(new BigDecimal("900"));
        activo.setEstado("ACTIVO");
        return activo;
    }

    private static ErpDepreciacionJpa depreciacion() {
        ErpDepreciacionJpa depreciacion = new ErpDepreciacionJpa();
        depreciacion.setEmpresaId(1L);
        depreciacion.setActivoId(1L);
        depreciacion.setPeriodo("2026-03");
        depreciacion.setFecha(FECHA);
        depreciacion.setValor(new BigDecimal("75"));
        depreciacion.setDepreciacionAcumulada(new BigDecimal("175"));
        depreciacion.setValorLibros(new BigDecimal("825"));
        depreciacion.setEstado("REGISTRADO");
        return depreciacion;
    }
}
