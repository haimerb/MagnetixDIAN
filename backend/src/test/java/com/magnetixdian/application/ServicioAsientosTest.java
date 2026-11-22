package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAsientosTest {

    @Mock
    private AsientoContableRepository asientoRepository;
    @Mock
    private PeriodoContableRepository periodoRepository;
    @Mock
    private ServicioPlanCuentas servicioPlanCuentas;

    private ServicioAsientos servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioAsientos(asientoRepository, periodoRepository, servicioPlanCuentas);
    }

    @Test
    void crearGuardaAsientoBalanceadoConPeriodoAbierto() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "MANUAL", 10L, "MANUAL")).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, LocalDate.of(2026, 3, 15)))
                .thenReturn(Optional.of(periodo()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(30L);
            return asiento;
        });

        AsientoContableDto resultado = servicio.crear(1L, new SolicitudAsientoDto(
                LocalDate.of(2026, 3, 15), "Ajuste manual", "MANUAL",
                List.of(
                        new SolicitudLineaAsientoDto("110505", "Caja", new BigDecimal("100"), BigDecimal.ZERO,
                                null, null, null, null),
                        new SolicitudLineaAsientoDto("420505", "Venta", BigDecimal.ZERO, new BigDecimal("100"),
                                null, null, null, null)),
                "MANUAL", 10L));

        assertEquals(30L, resultado.id());
        assertEquals("AS-1-202603-0001", resultado.numero());
        assertEquals(new BigDecimal("100.00"), resultado.totalDebito());
        ArgumentCaptor<AsientoContableJpa> captor = ArgumentCaptor.forClass(AsientoContableJpa.class);
        verify(asientoRepository).saveAndFlush(captor.capture());
        AsientoContableJpa guardado = captor.getValue();
        assertEquals("MANUAL", guardado.getOrigen());
        assertEquals(10L, guardado.getOrigenId());
        assertEquals(2L, guardado.getPeriodoId());
        assertEquals(2, guardado.getLineas().size());
        assertEquals("Caja", guardado.getLineas().get(0).getDescripcion());
    }

    @Test
    void crearRechazaPeriodoCerrado() {
        PeriodoContableJpa cerrado = periodo();
        cerrado.setEstado("CERRADO");
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "MANUAL", 10L, "MANUAL")).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, LocalDate.of(2026, 3, 15)))
                .thenReturn(Optional.of(cerrado));

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, nuevaSolicitud()));
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
    }

    @Test
    void crearEsIdempotentePorOrigen() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "MANUAL", 10L, "MANUAL")).thenReturn(Optional.of(asientoConLineas()));

        AsientoContableDto resultado = servicio.crear(1L, nuevaSolicitud());

        assertEquals(9L, resultado.id());
        verify(periodoRepository, never()).findVigentePorFecha(any(), any());
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
    }

    @Test
    void reversarInvierteAsientoYLoMarcaAnulado() {
        AsientoContableJpa original = asientoConLineas();
        when(asientoRepository.findById(9L)).thenReturn(Optional.of(original));
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "REVERSION", 9L, "REVERSION")).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, LocalDate.of(2026, 3, 15)))
                .thenReturn(Optional.of(periodo()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(31L);
            return asiento;
        });

        AsientoContableDto resultado = servicio.reversar(1L, 9L);

        assertEquals(31L, resultado.id());
        assertEquals("REVERSION", resultado.tipoEvento());
        ArgumentCaptor<AsientoContableJpa> captor = ArgumentCaptor.forClass(AsientoContableJpa.class);
        verify(asientoRepository).saveAndFlush(captor.capture());
        assertEquals(9L, captor.getValue().getOrigenId());
        assertEquals(9L, captor.getValue().getReversaDeId());
        assertEquals("ANULADO", original.getEstado());
        verify(asientoRepository).save(original);
    }

    @Test
    void cerrarPeriodoGeneraAsientoDeCierreBalanceado() {
        PeriodoContableJpa periodo = periodo();
        when(periodoRepository.findById(2L)).thenReturn(Optional.of(periodo));
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "CIERRE", 2L, "CIERRE")).thenReturn(Optional.empty());
        when(asientoRepository.findByEmpresaIdAndPeriodoIdOrderByNumeroAsc(1L, 2L))
                .thenReturn(List.of(asientoConMovimientos()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(32L);
            return asiento;
        });

        AsientoContableDto resultado = servicio.cerrarPeriodo(1L, 2L);

        assertEquals(32L, resultado.id());
        assertEquals("CIERRE", resultado.tipoEvento());
        assertEquals(resultado.totalDebito(), resultado.totalCredito());
        assertEquals("CERRADO", periodo.getEstado());
        verify(periodoRepository).save(periodo);
    }

    private static SolicitudAsientoDto nuevaSolicitud() {
        return new SolicitudAsientoDto(LocalDate.of(2026, 3, 15), "Ajuste manual", "MANUAL",
                List.of(
                        new SolicitudLineaAsientoDto("110505", "Caja", new BigDecimal("100"), BigDecimal.ZERO,
                                null, null, null, null),
                        new SolicitudLineaAsientoDto("420505", "Venta", BigDecimal.ZERO, new BigDecimal("100"),
                                null, null, null, null)),
                "MANUAL", 10L);
    }

    private static PeriodoContableJpa periodo() {
        PeriodoContableJpa periodo = new PeriodoContableJpa();
        periodo.setId(2L);
        periodo.setEmpresaId(1L);
        periodo.setAnio(2026);
        periodo.setMes(3);
        periodo.setFechaDesde(LocalDate.of(2026, 3, 1));
        periodo.setFechaHasta(LocalDate.of(2026, 3, 31));
        periodo.setEstado("ABIERTO");
        return periodo;
    }

    private static Map<String, CuentaContableJpa> cuentas() {
        Map<String, CuentaContableJpa> cuentas = new LinkedHashMap<>();
        cuentas.put("110505", cuenta("110505", "Caja", "ACTIVO", "DEUDORA"));
        cuentas.put("420505", cuenta("420505", "Ventas", "INGRESO", "ACREEDORA"));
        cuentas.put("610505", cuenta("610505", "Gastos", "EGRESO", "DEUDORA"));
        cuentas.put("399999", cuenta("399999", "Cierre", "EGRESO", "DEUDORA"));
        cuentas.put("320505", cuenta("320505", "Utilidades", "PATRIMONIO", "ACREEDORA"));
        return cuentas;
    }

    private static CuentaContableJpa cuenta(String codigo, String nombre, String tipo, String naturaleza) {
        CuentaContableJpa cuenta = new CuentaContableJpa();
        cuenta.setCodigo(codigo);
        cuenta.setNombre(nombre);
        cuenta.setTipo(tipo);
        cuenta.setNaturaleza(naturaleza);
        return cuenta;
    }

    private static AsientoContableJpa asientoConLineas() {
        Map<String, CuentaContableJpa> cuentas = cuentas();
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(9L);
        asiento.setEmpresaId(1L);
        asiento.setNumero("AS-1-202603-0009");
        asiento.setFecha(LocalDate.of(2026, 3, 15));
        asiento.setDescripcion("Asiento previo");
        asiento.setTipoEvento("MANUAL");
        asiento.setEstado("CONTABILIZADO");
        asiento.setPeriodoId(2L);
        asiento.setOrigen("MANUAL");
        asiento.setOrigenId(10L);
        asiento.setTotalDebito(new BigDecimal("100.00"));
        asiento.setTotalCredito(new BigDecimal("100.00"));
        asiento.agregarLinea(linea(1, cuentas.get("110505"), new BigDecimal("100"), BigDecimal.ZERO));
        asiento.agregarLinea(linea(2, cuentas.get("420505"), BigDecimal.ZERO, new BigDecimal("100")));
        return asiento;
    }

    private static AsientoContableJpa asientoConMovimientos() {
        Map<String, CuentaContableJpa> cuentas = cuentas();
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(8L);
        asiento.setEmpresaId(1L);
        asiento.setNumero("AS-1-202603-0008");
        asiento.setFecha(LocalDate.of(2026, 3, 14));
        asiento.setTipoEvento("RADIAN");
        asiento.setEstado("CONTABILIZADO");
        asiento.setPeriodoId(2L);
        asiento.setTotalDebito(new BigDecimal("1390.00"));
        asiento.setTotalCredito(new BigDecimal("1390.00"));
        asiento.agregarLinea(linea(1, cuentas.get("110505"), new BigDecimal("1390"), BigDecimal.ZERO));
        asiento.agregarLinea(linea(2, cuentas.get("420505"), BigDecimal.ZERO, new BigDecimal("1190")));
        asiento.agregarLinea(linea(3, cuentas.get("610505"), new BigDecimal("200"), BigDecimal.ZERO));
        return asiento;
    }

    private static AsientoContableLineaJpa linea(int numero, CuentaContableJpa cuenta,
                                                 BigDecimal debito, BigDecimal credito) {
        AsientoContableLineaJpa linea = new AsientoContableLineaJpa();
        linea.setNumeroLinea(numero);
        linea.setCuentaContable(cuenta);
        linea.setDescripcion(cuenta.getNombre());
        linea.setDebito(debito);
        linea.setCredito(credito);
        return linea;
    }
}
