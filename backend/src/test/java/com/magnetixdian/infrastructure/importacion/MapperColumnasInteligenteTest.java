package com.magnetixdian.infrastructure.importacion;

import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperColumnasInteligenteTest {

    private final MapperColumnasInteligente mapper = new MapperColumnasInteligente();

    @Test
    void normalizaSinAcentosNiPuntuacion() {
        assertEquals("razonsocial", mapper.normalizar("  Razón-Social  "));
        assertEquals("valorunitario", mapper.normalizar("Valor Unitario"));
    }

    @Test
    void aliasExactoObtieneConfianzaUno() {
        assertEquals(1.0, mapper.puntuar("NIT", "nit"));
        assertEquals(1.0, mapper.puntuar("Razón Social", "razon_social"));
    }

    @Test
    void prefijoYContencionObtienenConfianza08Y06() {
        assertEquals(0.8, mapper.puntuar("NIT Adquirente", "nit"));
        assertEquals(0.6, mapper.puntuar("X Documento Y", "nit"));
    }

    @Test
    void solapamientoDeTokensObtieneConfianza04() {
        assertEquals(0.4, mapper.puntuar("Razon Comercial Social", "razon_social"));
    }

    @Test
    void cabecerasSinAliasSeDescartan() {
        List<MapeoColumnaDto> sugerencias = mapper.sugerir(List.of("XYZ", "NIT"), "GENERICO");

        assertEquals(1, sugerencias.size());
        assertEquals("nit", sugerencias.get(0).campo());
        assertEquals(1, sugerencias.get(0).indice());
    }

    @Test
    void detectaFormatoPorCodigoDeHoja() {
        assertEquals("1001", mapper.detectarFormato("Reporte 1001 Clientes", List.of("NIT")));
    }

    @Test
    void detectaFormatoDesdeCabecerasSinPlantillas() {
        String formato = mapper.detectarFormato("Export", List.of("NIT", "Razon Social", "Total"));

        assertTrue(formato.startsWith("FORMATO-"));
        assertEquals(formato, mapper.detectarFormato("Otro nombre", List.of("NIT", "Razon Social", "Total")));
    }

    @Test
    void convierteCamposCanonicosACamposDelImportador() {
        List<MapeoColumnaDto> columnas = List.of(
                new MapeoColumnaDto("razon_social", "Razon Social", 1, 1.0, "GENERICO"),
                new MapeoColumnaDto("nit", "NIT", 0, 1.0, "GENERICO"),
                new MapeoColumnaDto("numero", "Consecutivo", 2, 1.0, "GENERICO"),
                new MapeoColumnaDto("cantidad", "Cantidad", 3, 1.0, "GENERICO"));

        Map<String, Integer> resultado = mapper.aCamposImportador(columnas);

        assertEquals(0, resultado.get("adquirente_numero_documento"));
        assertEquals(1, resultado.get("adquirente_nombre"));
        assertEquals(2, resultado.get("grupo"));
        assertEquals(3, resultado.get("cantidad"));
    }
}
