package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import com.magnetixdian.infrastructure.importacion.MapperColumnasInteligente;
import com.magnetixdian.infrastructure.persistence.ImportacionStagingJpa;
import com.magnetixdian.infrastructure.persistence.ImportacionStagingRepository;
import com.magnetixdian.infrastructure.persistence.MapeoArchivoJpa;
import com.magnetixdian.infrastructure.persistence.MapeoArchivoRepository;
import com.magnetixdian.interfaces.dto.ConfirmacionImportacionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionUniversalDto;
import com.magnetixdian.interfaces.dto.VistaPreviaArchivoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioMapeoColumnasTest {

    private static final String CSV =
            "NIT;Razon Social;Consecutivo;Descripcion;Cantidad;Valor Unitario\n"
                    + "900123456;Cliente Uno;F-1;Servicio uno;2;1000\n";

    private final MapeoArchivoRepository mapeos = mock(MapeoArchivoRepository.class);
    private final ImportacionStagingRepository staging = mock(ImportacionStagingRepository.class);
    private final ServicioFacturacion facturacion = mock(ServicioFacturacion.class);
    private final ImportadorFacturas importador = new ImportadorFacturas(facturacion);
    private final MapperColumnasInteligente mapperColumnas = new MapperColumnasInteligente();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(mapeos.findByEmpresaIdAndFormatoDetectadoAndCabeceraOrigen(
                anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(staging.findByEmpresaIdAndArchivoIdAndTipo(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(staging.findByEmpresaIdAndArchivoIdAndTipoAndExternalId(
                anyLong(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void vistaPreviaDetectaCabecerasYSugerenciasSinEmitir() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "clientes.csv", "text/csv",
                CSV.getBytes(StandardCharsets.UTF_8));

        VistaPreviaArchivoDto vista = servicio().vistaPrevia(1L, archivo);

        assertEquals(6, vista.cabeceras().size());
        assertEquals(1, vista.filasEjemplo().size());
        assertEquals(6, vista.sugerencias().size());
        assertTrue(vista.confianza() > 0.9);
        assertFalse(vista.contenidoBase64().isBlank());
        assertTrue(vista.sugerencias().stream().anyMatch(s -> "valor_unitario".equals(s.campo())));
        verify(staging).save(any(ImportacionStagingJpa.class));
    }

    @Test
    void confirmarPreparaFilasEnStagingSinEmitirFacturas() {
        byte[] contenido = CSV.getBytes(StandardCharsets.UTF_8);
        ConfirmacionImportacionDto solicitud = new ConfirmacionImportacionDto("archivo-1", "clientes.csv",
                "GENERICO", Base64.getEncoder().encodeToString(contenido), null);

        ResultadoImportacionUniversalDto resultado = servicio().confirmar(1L, solicitud);

        assertEquals("STAGED", resultado.estado());
        assertEquals(0, resultado.errores());
        assertEquals(1, resultado.filasImportadas());
        assertEquals(1, resultado.filasLeidas());
        verify(staging).save(any(ImportacionStagingJpa.class));
        verify(mapeos, atLeastOnce()).save(any(MapeoArchivoJpa.class));
        verifyNoFacturacion();
    }

    @Test
    void confirmarRepetidoNoDuplicaStaging() {
        byte[] contenido = CSV.getBytes(StandardCharsets.UTF_8);
        ImportacionStagingJpa existente = new ImportacionStagingJpa();
        existente.setTipo("FACTURA");
        existente.setEstado("PENDIENTE");
        when(staging.findByEmpresaIdAndArchivoIdAndTipoAndExternalId(
                1L, "archivo-1", "FACTURA", "archivo-1|1")).thenReturn(Optional.of(existente));
        ConfirmacionImportacionDto solicitud = new ConfirmacionImportacionDto("archivo-1", "clientes.csv",
                "GENERICO", Base64.getEncoder().encodeToString(contenido), null);

        ResultadoImportacionUniversalDto resultado = servicio().confirmar(1L, solicitud);

        assertEquals("STAGED", resultado.estado());
        assertEquals(0, resultado.filasImportadas());
        assertEquals(1, resultado.filasOmitidas());
        verify(staging, never()).save(any(ImportacionStagingJpa.class));
        verifyNoFacturacion();
    }

    @Test
    void mapeosDevuelveRegistrosPersistidosPorEmpresa() {
        MapeoArchivoJpa entidad = new MapeoArchivoJpa();
        entidad.setEmpresaId(1L);
        entidad.setFormatoDetectado("GENERICO");
        entidad.setCabeceraOrigen("NIT");
        entidad.setCampo("nit");
        entidad.setIndice(0);
        entidad.setConfianza(1.0);
        when(mapeos.findByEmpresaIdOrderByFormatoDetectadoAscCabeceraOrigenAsc(1L))
                .thenReturn(List.of(entidad));

        List<MapeoColumnaDto> resultado = servicio().mapeos(1L);

        assertEquals(1, resultado.size());
        assertEquals("nit", resultado.get(0).campo());
        assertEquals("NIT", resultado.get(0).cabeceraOrigen());
    }

    private void verifyNoFacturacion() {
        verify(facturacion, never()).emitir(anyLong(), anyLong(), any());
    }

    private ServicioMapeoColumnas servicio() {
        return new ServicioMapeoColumnas(mapeos, staging, importador, mapperColumnas, mapper);
    }
}
