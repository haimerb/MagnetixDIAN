package com.magnetixdian.exogena;

import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
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
    void catalogoUsaDescripcionesYEstadoDelRegistroOficial() {
        List<FormatoTributarioDto> formatos = catalogo.todos();

        assertEquals(11, formatos.size());
        assertEquals(4, formatos.stream().filter(FormatoTributarioDto::activo).count());
        assertEquals(FormatoTributarioCatalogo.ESTADO_ACTIVO, catalogo.obtener("1001").estado());
        assertEquals(FormatoTributarioCatalogo.ESTADO_REGISTRADO, catalogo.obtener("1003").estado());
        assertFalse(catalogo.activo("1003"));
        assertTrue(formatos.stream().noneMatch(FormatoTributarioDto::prevalidacionOficial));
    }

    @Test
    void formatosOficialesPriorizadosEstanActivosConDescripcionCorrecta() {
        assertEquals("Declaración informativa individual de precios de transferencia",
                catalogo.obtener("1125").descripcion());
        assertEquals("Información de la compra de bonos electrónicos",
                catalogo.obtener("2278").descripcion());
        assertEquals("Información de beneficiarios efectivos",
                catalogo.obtener("5253").descripcion());

        for (String codigo : FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS) {
            FormatoTributarioDto formato = catalogo.obtener(codigo);
            assertTrue(formato.activo(), codigo);
            assertEquals(FormatoTributarioCatalogo.ESTADO_ACTIVO, formato.estado(), codigo);
            assertEquals("/xsd/dian/2025/" + codigo + ".xsd", formato.xsd(), codigo);
        }
    }

    @Test
    void rutaXsdPorAnioYFormato() {
        assertEquals("/xsd/exogena/2026/1001.xsd", catalogo.rutaXsd(2026, "1001"));
        assertEquals("/xsd/dian/2025/5253.xsd", catalogo.rutaXsd(2026, "5253"));
    }

    @Test
    void formatoNoSoportadoFalla() {
        assertThrows(IllegalArgumentException.class, () -> catalogo.validar("9999"));
    }
}
