package com.magnetixdian.facturacion;

import com.magnetixdian.application.ServicioFacturacion;
import com.magnetixdian.infrastructure.persistence.FacturaImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioFacturacionMovimientosTest {

    @Mock
    private FacturaRepository facturaRepository;

    private ServicioFacturacion servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioFacturacion(null, null, facturaRepository, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, List.of());
    }

    @Test
    void movimientosIncluyenFacturasYRetencionesYExcluyenRechazadas() {
        when(facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(facturaAceptada(), facturaRechazada()));

        List<MovimientoTributarioDto> movimientos = servicio.movimientosParaExogena(1L, 2026);

        assertEquals(2, movimientos.size());
        assertTrue(movimientos.stream().anyMatch(m -> "FACTURA".equals(m.concepto())
                && new BigDecimal("1190.00").equals(m.valor())));
        assertTrue(movimientos.stream().anyMatch(m -> "RETENCION".equals(m.concepto())
                && new BigDecimal("50.00").equals(m.valor())));
        assertTrue(movimientos.stream().noneMatch(m -> "FE-RECHAZADA".equals(m.tipoDocumento())));
        assertEquals(LocalDate.of(2026, 3, 1), movimientos.get(0).fecha());
    }

    @Test
    void movimientosConAnoNuloUsanElAnoActual() {
        when(facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(facturaAceptada()));

        List<MovimientoTributarioDto> movimientos = servicio.movimientosParaExogena(1L, null);

        assertEquals(2, movimientos.size());
        assertEquals(java.time.LocalDate.now().getYear(), movimientos.get(0).fecha().getYear());
    }

    private FacturaJpa facturaAceptada() {
        FacturaJpa factura = new FacturaJpa();
        factura.setNumeroCompleto("FE-001");
        factura.setTipoDocumento("01");
        factura.setEstado("ACEPTADO");
        factura.setFechaEmision(LocalDate.of(2026, 3, 1));
        factura.setAdquirenteTipoDoc("13");
        factura.setAdquirenteNumDoc("12345678");
        factura.setTotal(new BigDecimal("1190.00"));
        factura.setTotalImpuestos(new BigDecimal("190.00"));
        factura.setTotalRetenciones(new BigDecimal("50.00"));
        factura.addImpuesto(impuesto("IVA", "19", "190.00"));
        factura.addImpuesto(impuesto("05", "RETENCION", "50.00"));
        return factura;
    }

    private FacturaJpa facturaRechazada() {
        FacturaJpa factura = new FacturaJpa();
        factura.setNumeroCompleto("FE-RECHAZADA");
        factura.setTipoDocumento("01");
        factura.setEstado("RECHAZADA");
        factura.setFechaEmision(LocalDate.of(2026, 3, 2));
        factura.setTotal(new BigDecimal("100.00"));
        return factura;
    }

    private FacturaImpuestoJpa impuesto(String tipo, String codigo, String valor) {
        FacturaImpuestoJpa impuesto = new FacturaImpuestoJpa();
        impuesto.setTipo(tipo);
        impuesto.setNombre(codigo);
        impuesto.setBase(new BigDecimal("1000.00"));
        impuesto.setPorcentaje(new BigDecimal("19.00"));
        impuesto.setValor(new BigDecimal(valor));
        return impuesto;
    }
}
