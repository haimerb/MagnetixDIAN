package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.domain.exogena.MapeadorExogenaOficial;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoDatosJsonRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.infrastructure.xml.GeneradorXmlExogenaOficial;
import com.magnetixdian.infrastructure.xml.ValidadorXsdOficialDian2025;
import com.magnetixdian.interfaces.dto.ResultadoGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudGeneracionExogenaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioGeneracionExogenaTest {

    @Mock
    private MedioMagneticoRepository medioRepo;
    @Mock
    private MedioMagneticoDatosJsonRepository datosJsonRepo;
    @Mock
    private AuditoriaService auditoria;

    private ServicioGeneracionExogena servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioGeneracionExogena(
                new FormatoExogenaRegistry(),
                new MapeadorExogenaOficial(),
                new GeneradorXmlExogenaOficial(new ValidadorXsdOficialDian2025()),
                new ValidadorXsdOficialDian2025(),
                new ObjectMapper(),
                medioRepo,
                datosJsonRepo,
                auditoria);
    }

    @Test
    void generarXmlValidoYRegistraAuditoria() {
        ResultadoGeneracionExogenaDto resultado = servicio.generar(1L, solicitud("1125"));

        assertEquals("1125", resultado.formato());
        assertEquals("13", resultado.version());
        assertEquals(1, resultado.registros());
        assertFalse(resultado.prevalidacionOficial());
        assertTrue(resultado.xml().contains("<Formato>1125</Formato>"));
        verify(auditoria).registrarEnEmpresa(eq(1L), nullable(String.class), eq("GENERAR_EXOGENA"),
                eq("FormatoExogena"), eq("1125"), anyString());
    }

    @Test
    void rechazaFormatoLegacyEnGeneradorOficial() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.generar(1L, solicitud("1001")));

        assertTrue(error.getMessage().contains("oficial"));
    }

    @Test
    void generarDesdeMedioActualizaEstado() {
        MedioMagneticoJpa medio = mock(MedioMagneticoJpa.class);
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(medioRepo.findById(9L)).thenReturn(Optional.of(medio));
        when(medio.getFormato()).thenReturn("2278");
        when(medio.getAnioGravable()).thenReturn(2025);
        when(medio.getEmpresa()).thenReturn(empresa);
        when(empresa.getId()).thenReturn(1L);
        when(datosJsonRepo.leerDatosJson(9L)).thenReturn("""
                {
                  "anioGravable": 2025,
                  "filas": [
                    {
                      "TIPO_DOCUMENTO_BENEFICIARIO": "31",
                      "NIT": "900123456",
                      "PAIS": "1",
                      "CLASE_BONO": "1",
                      "VALOR_TOTAL": "250000"
                    }
                  ]
                }
                """);

        String xml = servicio.generarDesdeMedio(9L);

        assertTrue(xml.contains("<Formato>2278</Formato>"));
        verify(medio).setEstado("XML_GENERADO");
        verify(medio).touch();
        verify(medioRepo).save(medio);
    }

    private SolicitudGeneracionExogenaDto solicitud(String formato) {
        return new SolicitudGeneracionExogenaDto(
                formato,
                2025,
                1,
                "2025-01-15T10:00:00Z",
                "2025-01-01",
                "2025-12-31",
                null,
                List.of("TIPO_DOCUMENTO", "NIT", "TIPVINC", "TOP", "PAIS", "CIUDAD", "VALOR_PAGO",
                        "REEST", "ACCOST"),
                List.of(Map.of(
                        "TIPO_DOCUMENTO", "31",
                        "NIT", "900123456",
                        "TIPVINC", "1",
                        "TOP", "1",
                        "PAIS", "1",
                        "CIUDAD", "BOGOTA",
                        "VALOR_PAGO", "1000000",
                        "REEST", "NO",
                        "ACCOST", "NO")));
    }
}
