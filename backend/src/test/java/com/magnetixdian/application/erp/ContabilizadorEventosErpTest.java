package com.magnetixdian.application.erp;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.application.ServicioAsientosPendientes;
import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.domain.erp.evento.CobroClienteEvent;
import com.magnetixdian.domain.erp.evento.CompraRecibidaEvent;
import com.magnetixdian.domain.erp.evento.PagoProveedorEvent;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import com.magnetixdian.interfaces.dto.FacturaEmitidaEvent;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilizadorEventosErpTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 15);

    @Mock
    private AsientoContableRepository asientoRepository;
    @Mock
    private PeriodoContableRepository periodoRepository;
    @Mock
    private ServicioPlanCuentas servicioPlanCuentas;
    @Mock
    private ServicioAsientosPendientes pendientes;

    private ContabilizadorEventosErp contabilizador;

    @BeforeEach
    void setUp() {
        ServicioAsientos servicioAsientos =
                new ServicioAsientos(asientoRepository, periodoRepository, servicioPlanCuentas);
        contabilizador = new ContabilizadorEventosErp(servicioAsientos, pendientes);
    }

    @Test
    void contabilizaFacturaEmitidaConRetenciones() {
        prepararGuardado(ContabilizadorEventosErp.ORIGEN_FACTURA, 77L,
                ContabilizadorEventosErp.TIPO_VENTA);

        contabilizador.contabilizarFactura(factura("ACEPTADO"));

        AsientoContableJpa guardado = capturarAsiento();
        assertEquals("VENTA", guardado.getTipoEvento());
        assertEquals(ContabilizadorEventosErp.ORIGEN_FACTURA, guardado.getOrigen());
        assertEquals(77L, guardado.getOrigenId());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalDebito());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalCredito());
        assertEquals(4, guardado.getLineas().size());
        assertEquals("130505", guardado.getLineas().get(0).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1140.00"), guardado.getLineas().get(0).getDebito());
        assertEquals("413505", guardado.getLineas().get(1).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1000.00"), guardado.getLineas().get(1).getCredito());
        assertEquals("240805", guardado.getLineas().get(2).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("190.00"), guardado.getLineas().get(2).getCredito());
        assertEquals("135520", guardado.getLineas().get(3).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("50.00"), guardado.getLineas().get(3).getDebito());
    }

    @Test
    void facturaRechazadaNoContabiliza() {
        contabilizador.contabilizarFactura(factura("RECHAZADA"));
        verifyNoInteractions(asientoRepository, periodoRepository, servicioPlanCuentas);
    }

    @Test
    void contabilizaCompraRecibidaConIvaDescontable() {
        prepararGuardado(ContabilizadorEventosErp.ORIGEN_COMPRA, 12L,
                ContabilizadorEventosErp.TIPO_COMPRA);

        contabilizador.contabilizarCompra(compra());

        AsientoContableJpa guardado = capturarAsiento();
        assertEquals("COMPRA", guardado.getTipoEvento());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalDebito());
        assertEquals(new BigDecimal("1190.00"), guardado.getTotalCredito());
        assertEquals(3, guardado.getLineas().size());
        assertEquals("519505", guardado.getLineas().get(0).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1000.00"), guardado.getLineas().get(0).getDebito());
        assertEquals("135515", guardado.getLineas().get(1).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("190.00"), guardado.getLineas().get(1).getDebito());
        assertEquals("220505", guardado.getLineas().get(2).getCuentaContable().getCodigo());
        assertEquals(new BigDecimal("1190.00"), guardado.getLineas().get(2).getCredito());
    }

    @Test
    void contabilizaPagoProveedor() {
        prepararGuardado(ContabilizadorEventosErp.ORIGEN_PAGO, 31L,
                ContabilizadorEventosErp.TIPO_PAGO);

        contabilizador.contabilizarPagoProveedor(pago());

        AsientoContableJpa guardado = capturarAsiento();
        assertEquals("PAGO", guardado.getTipoEvento());
        assertEquals(new BigDecimal("500.00"), guardado.getTotalDebito());
        assertEquals(new BigDecimal("500.00"), guardado.getTotalCredito());
        assertEquals("220505", guardado.getLineas().get(0).getCuentaContable().getCodigo());
        assertEquals("110505", guardado.getLineas().get(1).getCuentaContable().getCodigo());
    }

    @Test
    void contabilizaCobroCliente() {
        prepararGuardado(ContabilizadorEventosErp.ORIGEN_COBRO, 44L,
                ContabilizadorEventosErp.TIPO_COBRO);

        contabilizador.contabilizarCobroCliente(cobro());

        AsientoContableJpa guardado = capturarAsiento();
        assertEquals("COBRO", guardado.getTipoEvento());
        assertEquals(new BigDecimal("300.00"), guardado.getTotalDebito());
        assertEquals(new BigDecimal("300.00"), guardado.getTotalCredito());
        assertEquals("110505", guardado.getLineas().get(0).getCuentaContable().getCodigo());
        assertEquals("130505", guardado.getLineas().get(1).getCuentaContable().getCodigo());
    }

    @Test
    void eventoDuplicadoNoCreaSegundoAsiento() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, ContabilizadorEventosErp.ORIGEN_PAGO, 31L,
                ContabilizadorEventosErp.TIPO_PAGO)).thenReturn(Optional.of(asientoPrevio()));

        contabilizador.contabilizarPagoProveedor(pago());
        contabilizador.contabilizarPagoProveedor(pago());

        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
        verify(periodoRepository, never()).findVigentePorFecha(any(), any());
        verify(servicioPlanCuentas, never()).asegurar(any());
    }

    @Test
    void falloDeContabilizacionRegistraOutbox() {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, ContabilizadorEventosErp.ORIGEN_FACTURA, 77L,
                ContabilizadorEventosErp.TIPO_VENTA)).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, FECHA)).thenReturn(Optional.of(periodo()));
        when(servicioPlanCuentas.asegurar(1L)).thenThrow(new IllegalStateException("BD caída"));

        contabilizador.contabilizarFactura(factura("ACEPTADO"));

        verify(pendientes).registrar(eq(1L), any(SolicitudAsientoDto.class), contains("BD caída"));
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
    }

    private void prepararGuardado(String origen, Long origenId, String tipoEvento) {
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, origen, origenId, tipoEvento)).thenReturn(Optional.empty());
        when(periodoRepository.findVigentePorFecha(1L, FECHA)).thenReturn(Optional.of(periodo()));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(cuentas());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(40L);
            return asiento;
        });
    }

    private AsientoContableJpa capturarAsiento() {
        ArgumentCaptor<AsientoContableJpa> captor = ArgumentCaptor.forClass(AsientoContableJpa.class);
        verify(asientoRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static FacturaEmitidaEvent factura(String estado) {
        return new FacturaEmitidaEvent(1L, 77L, "FE-0042", "01", "cufe-77", null, "track-77",
                "https://qr", estado, FECHA, "900123456", 5L,
                new BigDecimal("1000"), new BigDecimal("190"), new BigDecimal("50"),
                new BigDecimal("1190"), new BigDecimal("1140"), null);
    }

    private static CompraRecibidaEvent compra() {
        return new CompraRecibidaEvent(1L, 12L, 13L, "OC-0007", "900654321", "Proveedor Prueba",
                "BODEGA-1", FECHA, new BigDecimal("1190"), new BigDecimal("1000"),
                new BigDecimal("190"), BigDecimal.ZERO,
                OffsetDateTime.parse("2026-03-15T10:00:00Z"));
    }

    private static PagoProveedorEvent pago() {
        return new PagoProveedorEvent(1L, 31L, 12L, 9L, "900654321", "Proveedor Prueba",
                FECHA, new BigDecimal("500"), "TR-500",
                OffsetDateTime.parse("2026-03-15T11:00:00Z"));
    }

    private static CobroClienteEvent cobro() {
        return new CobroClienteEvent(1L, 44L, 77L, 9L, "900123456", "Cliente Prueba",
                FECHA, new BigDecimal("300"), "TR-300",
                OffsetDateTime.parse("2026-03-15T12:00:00Z"));
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
        cuentas.put("130505", cuenta("130505", "Clientes", "ACTIVO", "DEUDORA"));
        cuentas.put("135515", cuenta("135515", "IVA descontable", "ACTIVO", "DEUDORA"));
        cuentas.put("135520", cuenta("135520", "Retenciones", "ACTIVO", "DEUDORA"));
        cuentas.put("220505", cuenta("220505", "Proveedores", "PASIVO", "ACREEDORA"));
        cuentas.put("236505", cuenta("236505", "Retención en la fuente", "PASIVO", "ACREEDORA"));
        cuentas.put("240805", cuenta("240805", "IVA generado", "PASIVO", "ACREEDORA"));
        cuentas.put("413505", cuenta("413505", "Ventas", "INGRESO", "ACREEDORA"));
        cuentas.put("519505", cuenta("519505", "Compras", "EGRESO", "DEUDORA"));
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
        asiento.setFecha(FECHA);
        asiento.setDescripcion("Asiento previo");
        asiento.setTipoEvento("PAGO");
        asiento.setEstado("CONTABILIZADO");
        asiento.setPeriodoId(2L);
        asiento.setOrigen(ContabilizadorEventosErp.ORIGEN_PAGO);
        asiento.setOrigenId(31L);
        asiento.setTotalDebito(new BigDecimal("500.00"));
        asiento.setTotalCredito(new BigDecimal("500.00"));
        return asiento;
    }
}
