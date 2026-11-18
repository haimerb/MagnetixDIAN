package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Utilidad de pruebas para construir documentos electrónicos con valores válidos
 * por defecto; los campos se sobrescriben directamente en cada caso.
 */
final class TestFacturas {

    static final DatosEmisor EMISOR = new DatosEmisor(
            "31", "900123456", NitDigitoVerificacionRegla.calcularDv("900123456"),
            "MAGNETIXDIAN SAS", "O-13", "O-13", "Calle 1 # 1-1", "Bogota", "Cundinamarca",
            "emisor@x.co", false);

    static final DatosAdquirente ADQUIRENTE = new DatosAdquirente(
            "31", "830053812", NitDigitoVerificacionRegla.calcularDv("830053812"),
            "Cliente SAS", "cliente@x.co", "Calle 2 # 2-2", "Bogota", "O-13", "O-13");

    private TestFacturas() {
    }

    static LineaFactura lineaValida() {
        return LineaFactura.of(1, "Servicio", BigDecimal.ONE, new BigDecimal("100000"),
                new BigDecimal("19"), BigDecimal.ZERO, "94", "80101500");
    }

    static Builder base() {
        return new Builder();
    }

    static final class Builder {
        TipoDocumento tipo = TipoDocumento.FACTURA_VENTA;
        String prefijo = "FV";
        long numero = 1L;
        LocalDate fechaEmision = LocalDate.of(2026, 1, 15);
        DatosEmisor emisor = EMISOR;
        DatosAdquirente adquirente = ADQUIRENTE;
        List<LineaFactura> lineas = List.of(lineaValida());
        BigDecimal subtotal = new BigDecimal("100000.00");
        BigDecimal totalImpuestos = new BigDecimal("19000.00");
        BigDecimal totalRetenciones = BigDecimal.ZERO;
        BigDecimal total = new BigDecimal("119000.00");
        BigDecimal totalPagar = new BigDecimal("119000.00");
        String resolucionNumero = "18764000523766";
        LocalDate rangoVigenciaDesde = LocalDate.of(2025, 1, 1);
        LocalDate rangoVigenciaHasta = LocalDate.of(2026, 12, 31);
        long rangoDesde = 1L;
        long rangoHasta = 10000L;
        String cufeReferencia = null;
        String numeroReferencia = null;

        FacturaDatos build() {
            return new FacturaDatos(
                    tipo, prefijo + "-" + numero, prefijo, numero, fechaEmision, LocalTime.NOON,
                    null, 2, "COP", "1", "10", null, emisor, adquirente, lineas,
                    List.<ImpuestoFactura>of(), subtotal, totalImpuestos, totalRetenciones, total,
                    totalPagar, null, cufeReferencia, numeroReferencia, "sw-1", "12345", "abc123", null,
                    resolucionNumero, LocalDate.of(2025, 1, 1), rangoVigenciaDesde,
                    rangoVigenciaHasta, rangoDesde, rangoHasta);
        }
    }
}
