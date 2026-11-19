package com.magnetixdian.domain.cufe;

import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CufeCalculatorTest {

    private static FacturaDatos factura(int ambiente) {
        DatosEmisor emisor = new DatosEmisor("31", "900123456", 8, "Emisor", "O-13", "O-13",
                null, null, null, null, false);
        DatosAdquirente adq = new DatosAdquirente("31", "830053812", 4, "Cliente", null,
                null, null, null, null);
        LineaFactura linea = LineaFactura.of(1, "Servicio", BigDecimal.ONE, new BigDecimal("100000"),
                new BigDecimal("19"), BigDecimal.ZERO, "94", "80101500");
        List<ImpuestoFactura> impuestos = List.of(new ImpuestoFactura(CufeCalculator.IVA, "IVA",
                new BigDecimal("100000"), new BigDecimal("19"), new BigDecimal("19000")));
        return new FacturaDatos(TipoDocumento.FACTURA_VENTA, "FV-1", "FV", 1L,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 30, 0), null, ambiente, "COP", "1", "10",
                null, emisor, adq, List.of(linea), impuestos, new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                new BigDecimal("119000.00"), null, null, null, "sw-1", "12345", "abc", null,
                "18764000523766", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31), 1L, 10000L);
    }

    @Test
    void cufeEsDeterministaYHex() {
        String c1 = CufeCalculator.calcular(factura(2));
        String c2 = CufeCalculator.calcular(factura(2));
        assertEquals(c1, c2);
        assertEquals(96, c1.length());
        assertTrue(c1.matches("[0-9a-f]{96}"));
    }

    @Test
    void cufeCambiaConElAmbiente() {
        assertNotEquals(CufeCalculator.calcular(factura(1)), CufeCalculator.calcular(factura(2)));
    }

    @Test
    void cudeDeEventoCoincideConEjemploDelAnexo() {
        String cude = CufeCalculator.calcularEvento("1", LocalDate.of(2019, 4, 30),
                LocalTime.of(19, 48, 50), "99998888", "800197268", "030", "FE123", "01", "11111");
        assertEquals("0d91ba25b01f5e7dbda870a11b274501d3a62a73e91932c473c86c93f12a142a"
                + "2ac45876efcde3e679024a01c0be41f9", cude);
    }

    @Test
    void codigoSeguridadSoftwareEsHex() {
        String c = CufeCalculator.softwareSecurityCode("sw-1", "12345", "FV-1");
        assertEquals(96, c.length());
        assertTrue(c.matches("[0-9a-f]{96}"));
    }
}
