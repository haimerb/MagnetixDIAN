package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.interfaces.dto.ValidacionProcesoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioValidacionDatosJsonTest {

    private static final Long MEDIO_ID = 10L;

    @Mock
    private MedioMagneticoRepository medioRepo;
    @Mock
    private MedioMagneticoDatosJsonRepository datosJsonRepo;
    @Mock
    private MedioMagneticoJpa medio;

    private ServicioValidacionDatosJson servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioValidacionDatosJson(
                medioRepo, datosJsonRepo, new FormatoExogenaRegistry(), new ObjectMapper());
        when(medioRepo.findById(MEDIO_ID)).thenReturn(Optional.of(medio));
    }

    @Test
    void validaEstructuraCompleta() {
        when(medio.getFormato()).thenReturn("1001");
        when(medio.getAnioGravable()).thenReturn(2025);
        when(datosJsonRepo.leerDatosJson(MEDIO_ID)).thenReturn("""
                {
                  "anioGravable": 2025,
                  "hoja": "1001",
                  "encabezados": ["TIPO", "NIT", "DV", "CONCEPTO", "VALOR_PAGO", "RET_RENTA",
                    "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC",
                    "VALOR_GASTO", "VALOR_COSTO", "NOTA_CREDITO"],
                  "filas": [{"NIT": "900123456"}, {"NIT": "900123457"}]
                }
                """);

        ValidacionProcesoDto resultado = servicio.validar(MEDIO_ID);

        assertEquals(0, resultado.erroresTotal());
        assertEquals(2, resultado.registrosValidados());
        assertTrue(resultado.errores().isEmpty());
    }

    @Test
    void reportaEncabezadosYFilaInvalida() {
        when(medio.getFormato()).thenReturn("1001");
        when(medio.getAnioGravable()).thenReturn(2025);
        when(datosJsonRepo.leerDatosJson(MEDIO_ID)).thenReturn("""
                {"anioGravable": 2024, "encabezados": ["NIT"], "filas": [{}]}
                """);

        ValidacionProcesoDto resultado = servicio.validar(MEDIO_ID);

        assertTrue(resultado.erroresTotal() > 1);
        assertEquals(1, resultado.registrosValidados());
        assertTrue(resultado.errores().stream().anyMatch(e -> "fila".equals(e.campo())));
        assertTrue(resultado.errores().stream().anyMatch(e -> "encabezados".equals(e.campo())));
    }

    @Test
    void reportaAusenciaDeDatosJsonSinFallar() {
        when(medio.getFormato()).thenReturn("1001");
        when(datosJsonRepo.leerDatosJson(MEDIO_ID)).thenReturn(null);

        ValidacionProcesoDto resultado = servicio.validar(MEDIO_ID);

        assertEquals(1, resultado.erroresTotal());
        assertEquals("datos_json", resultado.errores().get(0).campo());
    }

    @Test
    void reportaFormatoInactivo() {
        when(medio.getFormato()).thenReturn("1003");
        when(medio.getAnioGravable()).thenReturn(2025);
        when(datosJsonRepo.leerDatosJson(MEDIO_ID)).thenReturn("""
                {"anioGravable": 2025, "encabezados": [], "filas": [{"NIT": "900123456"}]}
                """);

        ValidacionProcesoDto resultado = servicio.validar(MEDIO_ID);

        assertTrue(resultado.errores().stream().anyMatch(e -> "formato".equals(e.campo())));
    }
}
