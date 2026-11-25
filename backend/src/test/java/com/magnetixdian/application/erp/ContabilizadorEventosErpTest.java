package com.magnetixdian.application.erp;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.domain.erp.evento.PedidoAprobadoEvent;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilizadorEventosErpTest {

    @Mock
    private AsientoContableRepository asientoRepository;
    @Mock
    private PeriodoContableRepository periodoRepository;
    @Mock
    private ServicioPlanCuentas servicioPlanCuentas;

    private ContabilizadorEventosErp contabilizador;

    @BeforeEach
    void setUp() {
        ServicioAsientos servicioAsientos =
                new ServicioAsientos(asientoRepository, periodoRepository, servicioPlanCuentas);
        contabilizador = new ContabilizadorEventosErp(servicioAsientos);
    }

    @Test
    void contabilizaPedidoAprobadoConLineasDeVenta() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, ContabilizadorEventosErp.ORIGEN_PEDIDO, 77L,
                ContabilizadorEventosErp.TIPO_EVENTO_VENTA)).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, LocalDate.of(2026, 3, 15)))
                .thenReturn(Optional.of(periodo()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(40L);
            return asiento;
        });

        contabilizador.contabilizar(evento());

        ArgumentCaptor<AsientoContableJpa> captor = ArgumentCaptor.forClass(AsientoContableJpa.class);
        verify(asientoRepository).saveAndFlush(captor.capture());
        AsientoContableJpa guardado = captor.getValue();
        assertEquals(1L, guardado.getEmpresaId());
        assertEquals("VENTA", guardado.getTipoEvento());
        assertEquals("ERP_PEDIDO", guardado.getOrigen());
        assertEquals(77L, guardado.getOrigenId());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalDebito());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalCredito());
        assertEquals(3, guardado.getLineas().size());
        assertEquals("130505", guardado.getLineas().get(0).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1190.00"), guardado.getLineas().get(0).getDebito());
        assertEquals("413505", guardado.getLineas().get(1).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1000.00"), guardado.getLineas().get(1).getCredito());
        assertEquals("240805", guardado.getLineas().get(2).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("190.00"), guardado.getLineas().get(2).getCredito());
        assertEquals("IVA", guardado.getLineas().get(2).getImpuestoCodigo());
    }

    @Test
    void pedidoAprobadoDuplicadoNoCreaSegundoAsiento() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, ContabilizadorEventosErp.ORIGEN_PEDIDO, 77L,
                ContabilizadorEventosErp.TIPO_EVENTO_VENTA)).thenReturn(Optional.of(asientoPrevio()));

        contabilizador.contabilizar(evento());
        contabilizador.contabilizar(evento());

        verify(asientoRepository, times(2)).findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, ContabilizadorEventosErp.ORIGEN_PEDIDO, 77L,
                ContabilizadorEventosErp.TIPO_EVENTO_VENTA);
        verify(periodoRepository, never()).findVigentePorFecha(any(), any());
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
    }

    private static PedidoAprobadoEvent evento() {
        return new PedidoAprobadoEvent(1L, 77L, "PED-0042", "CLI-1", "Cliente Prueba",
                LocalDate.of(2026, 3, 15), new BigDecimal("1000"), new BigDecimal("190"),
                new BigDecimal("1190"), OffsetDateTime.parse("2026-03-15T10:00:00Z"));
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
        cuentas.put("130505", cuenta("130505", "Clientes", "ACTIVO", "DEUDORA"));
        cuentas.put("413505", cuenta("413505", "Ventas", "INGRESO", "ACREEDORA"));
        cuentas.put("240805", cuenta("240805", "IVA generado", "PASIVO", "ACREEDORA"));
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

    private static AsientoContableJpa asientoPrevio() {
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(9L);
        asiento.setEmpresaId(1L);
        asiento.setNumero("AS-1-202603-0009");
        asiento.setFecha(LocalDate.of(2026, 3, 15));
        asiento.setDescripcion("Asiento previo");
        asiento.setTipoEvento("VENTA");
        asiento.setEstado("CONTABILIZADO");
        asiento.setPeriodoId(2L);
        asiento.setOrigen("ERP_PEDIDO");
        asiento.setOrigenId(77L);
        asiento.setTotalDebito(new BigDecimal("1190.00"));
        asiento.setTotalCredito(new BigDecimal("1190.00"));
        return asiento;
    }
}
