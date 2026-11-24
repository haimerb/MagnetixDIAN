package com.magnetixdian.exogena;

import com.magnetixdian.domain.exogena.FormatoTributarioCatalogo;
import com.magnetixdian.interfaces.dto.FormatoTributarioDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatoTributarioCatalogoTest {

    private final FormatoTributarioCatalogo catalogo = new FormatoTributarioCatalogo();

    @Test
    void solo1001EstaActivo() {
        List<FormatoTributarioDto> formatos = catalogo.todos();

        assertEquals(11, formatos.size());
        assertEquals(1, formatos.stream().filter(FormatoTributarioDto::activo).count());
        assertEquals(FormatoTributarioCatalogo.ESTADO_ACTIVO, catalogo.obtener("1001").estado());
        assertEquals(FormatoTributarioCatalogo.ESTADO_REGISTRADO, catalogo.obtener("1003").estado());
        assertTrue(formatos.stream().noneMatch(FormatoTributarioDto::prevalidacionOficial));
    }

    @Test
    void rutaXsdPorAnioYFormato() {
        assertEquals("/xsd/exogena/2026/1001.xsd", catalogo.rutaXsd(2026, "1001"));
        assertFalse(catalogo.activo("5253"));
    }

    @Test
    void formatoNoSoportadoFalla() {
        assertThrows(IllegalArgumentException.class, () -> catalogo.validar("9999"));
    }
}
