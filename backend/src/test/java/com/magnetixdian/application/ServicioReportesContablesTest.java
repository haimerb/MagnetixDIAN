package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.BalanceComprobacionDto;
import com.magnetixdian.interfaces.dto.EstadoFinancieroDto;
import com.magnetixdian.interfaces.dto.FlujoEfectivoDto;
import com.magnetixdian.interfaces.dto.MovimientoContableDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.SaldoCuentaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioReportesContablesTest {

    @Mock
    private AsientoContableRepository asientoRepository;
    @Mock
    private ServicioPlanCuentas servicioPlanCuentas;

    private ServicioReportesContables servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioReportesContables(asientoRepository, servicioPlanCuentas);
    }

    @Test
    void libroDiarioAplicaFiltrosYPaginacion() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));

        PaginaDto<AsientoContableDto> resultado = servicio.libroDiario(1L, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), 7L, null, 0, 1);

        assertEquals(1, resultado.total());
        assertEquals(1, resultado.contenido().size());
        assertEquals("AS-1-202603-0001", resultado.contenido().get(0).numero());
        assertEquals(1, resultado.contenido().get(0).lineas().size());
    }

    @Test
    void libroMayorCalculaSaldoSegunNaturaleza() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));

        List<MovimientoContableDto> clientes = servicio.libroMayor(1L, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), "130505", null, null, 0, 20).contenido();
        List<MovimientoContableDto> proveedores = servicio.libroMayor(1L, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), "220505", null, null, 0, 20).contenido();

        assertEquals(1, clientes.size());
        assertEquals(new BigDecimal("1190.00"), clientes.get(0).saldo());
        assertEquals(new BigDecimal("190.00"), proveedores.get(0).saldo());
    }

    @Test
    void balanceComprobacionCalculaSaldosInicialesYMovimientos() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());

        BalanceComprobacionDto resultado = servicio.balanceComprobacion(1L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        SaldoCuentaDto caja = cuenta(resultado, "110505");
        SaldoCuentaDto clientes = cuenta(resultado, "130505");
        assertEquals(new BigDecimal("500.00"), caja.saldoInicial());
        assertEquals(new BigDecimal("200.00"), caja.credito());
        assertEquals(new BigDecimal("300.00"), caja.saldoFinal());
        assertEquals(new BigDecimal("1190.00"), clientes.debito());
        assertEquals(new BigDecimal("1390.00"), resultado.totalDebito());
        assertEquals(new BigDecimal("1390.00"), resultado.totalCredito());
    }

    @Test
    void balanceGeneralAgrupaActivosPasivosYPatrimonio() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());

        EstadoFinancieroDto resultado = servicio.balanceGeneral(1L, LocalDate.of(2026, 3, 31));

        assertEquals("Balance general", resultado.titulo());
        assertEquals(3, resultado.grupos().size());
        assertEquals("Activos", resultado.grupos().get(0).nombre());
        assertEquals(new BigDecimal("1490.00"), resultado.total());
    }

    @Test
    void estadoResultadosCalculaUtilidad() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());

        EstadoFinancieroDto resultado = servicio.estadoResultados(1L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(new BigDecimal("1000.00"), resultado.grupos().get(0).total());
        assertEquals(new BigDecimal("200.00"), resultado.grupos().get(1).total());
        assertEquals(new BigDecimal("800.00"), resultado.total());
    }

    @Test
    void flujoEfectivoCalculaEntradasSalidasYNeto() {
        when(asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asiento2(), asiento1(), asiento0()));

        FlujoEfectivoDto resultado = servicio.flujoEfectivo(1L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(BigDecimal.ZERO.setScale(2), resultado.entradas());
        assertEquals(new BigDecimal("200.00"), resultado.salidas());
        assertEquals(new BigDecimal("-200.00"), resultado.neto());
        assertEquals(1, resultado.movimientos().size());
    }

    private static SaldoCuentaDto cuenta(BalanceComprobacionDto balance, String codigo) {
        return balance.cuentas().stream()
                .filter(fila -> codigo.equals(fila.codigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró la cuenta " + codigo));
    }

    private static Map<String, CuentaContableJpa> cuentas() {
        Map<String, CuentaContableJpa> cuentas = new LinkedHashMap<>();
        cuentas.put("110505", cuenta("110505", "Caja", "ACTIVO", "DEUDORA"));
        cuentas.put("130505", cuenta("130505", "Clientes", "ACTIVO", "DEUDORA"));
        cuentas.put("220505", cuenta("220505", "Proveedores", "PASIVO", "ACREEDORA"));
        cuentas.put("320505", cuenta("320505", "Patrimonio", "PATRIMONIO", "ACREEDORA"));
        cuentas.put("420505", cuenta("420505", "Ventas", "INGRESO", "ACREEDORA"));
        cuentas.put("610505", cuenta("610505", "Gastos", "EGRESO", "DEUDORA"));
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

    private static AsientoContableJpa asiento0() {
        Map<String, CuentaContableJpa> cuentas = cuentas();
        AsientoContableJpa asiento = asiento(1L, "AS-1-202602-0001", LocalDate.of(2026, 2, 28));
        asiento.agregarLinea(linea(1, cuentas.get("110505"), "500", "0", null, null));
        asiento.agregarLinea(linea(2, cuentas.get("320505"), "0", "500", null, null));
        return asiento;
    }

    private static AsientoContableJpa asiento1() {
        Map<String, CuentaContableJpa> cuentas = cuentas();
        AsientoContableJpa asiento = asiento(2L, "AS-1-202603-0001", LocalDate.of(2026, 3, 10));
        asiento.agregarLinea(linea(1, cuentas.get("130505"), "1190", "0", 7L, 9L));
        asiento.agregarLinea(linea(2, cuentas.get("420505"), "0", "1000", null, null));
        asiento.agregarLinea(linea(3, cuentas.get("220505"), "0", "190", null, null));
        return asiento;
    }

    private static AsientoContableJpa asiento2() {
        Map<String, CuentaContableJpa> cuentas = cuentas();
        AsientoContableJpa asiento = asiento(3L, "AS-1-202603-0002", LocalDate.of(2026, 3, 20));
        asiento.agregarLinea(linea(1, cuentas.get("610505"), "200", "0", null, null));
        asiento.agregarLinea(linea(2, cuentas.get("110505"), "0", "200", null, null));
        return asiento;
    }

    private static AsientoContableJpa asiento(Long id, String numero, LocalDate fecha) {
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(id);
        asiento.setEmpresaId(1L);
        asiento.setNumero(numero);
        asiento.setFecha(fecha);
        asiento.setDescripcion("Asiento de prueba");
        asiento.setTipoEvento("MANUAL");
        asiento.setEstado("CONTABILIZADO");
        asiento.setTotalDebito(BigDecimal.ZERO);
        asiento.setTotalCredito(BigDecimal.ZERO);
        return asiento;
    }

    private static AsientoContableLineaJpa linea(int numeroLinea, CuentaContableJpa cuenta,
                                                 String debito, String credito,
                                                 Long centroCostoId, Long terceroId) {
        AsientoContableLineaJpa linea = new AsientoContableLineaJpa();
        linea.setNumeroLinea(numeroLinea);
        linea.setCuentaContable(cuenta);
        linea.setDescripcion(cuenta.getNombre());
        linea.setDebito(new BigDecimal(debito));
        linea.setCredito(new BigDecimal(credito));
        linea.setCentroCostoId(centroCostoId);
        linea.setTerceroId(terceroId);
        return linea;
    }
}
