package com.magnetixdian.contabilidad;

import com.magnetixdian.application.ServicioImpuestos;
import com.magnetixdian.interfaces.dto.CatalogoImpuestoDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ServicioImpuestosTest {

    private final ServicioImpuestos servicio = new ServicioImpuestos(mock(JdbcTemplate.class));

    @Test
    void normalizaAliasTributarios() {
        assertEquals("01", servicio.normalizar("IVA"));
        assertEquals("01", servicio.normalizar("impuesto_valor_agregado"));
        assertEquals("05", servicio.normalizar("RETEFUENTE"));
        assertEquals("06", servicio.normalizar("RETE_IVA"));
        assertEquals("07", servicio.normalizar("RETEICA"));
        assertEquals("01", servicio.normalizar("01"));
    }

    @Test
    void catalogoIncluyeCuatroImpuestos() {
        List<CatalogoImpuestoDto> catalogo = servicio.catalogo();

        assertEquals(4, catalogo.size());
        assertTrue(catalogo.stream().anyMatch(item -> "05".equals(item.codigo())));
    }

    @Test
    void rangosInvalidosFalanAntesDeConsultar() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.causacionDesdeAsientos(1L, LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 2, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.declaracionDesdeAsientos(1L, 2026, "03/2026"));
    }

    @Test
    void codigoDesconocidoFalla() {
        assertThrows(IllegalArgumentException.class, () -> servicio.normalizar("IC"));
        assertThrows(IllegalArgumentException.class, () -> servicio.normalizar(" "));
    }
}
