package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioMapeoColumnas;
import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import com.magnetixdian.interfaces.dto.ConfirmacionImportacionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionUniversalDto;
import com.magnetixdian.interfaces.dto.VistaPreviaArchivoDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportacionUniversalControllerTest {

    private final ServicioMapeoColumnas servicio = mock(ServicioMapeoColumnas.class);
    private final AutorizacionEmpresa autorizacion = mock(AutorizacionEmpresa.class);
    private final Authentication authentication = mock(Authentication.class);
    private final ImportacionUniversalController controller =
            new ImportacionUniversalController(servicio, autorizacion);

    @Test
    void vistaPreviaVerificaEmpresaYDelegates() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv", "text/csv", new byte[0]);
        VistaPreviaArchivoDto esperado = new VistaPreviaArchivoDto("a-1", "datos.csv", "GENERICO",
                List.of("NIT"), List.of(), List.of(), 0, 0, "");
        when(servicio.vistaPrevia(1L, archivo)).thenReturn(esperado);

        VistaPreviaArchivoDto resultado = controller.vistaPrevia(1L, archivo, authentication);

        assertEquals(esperado, resultado);
        verify(autorizacion).verificar(1L, authentication);
    }

    @Test
    void confirmarNoExponeEmisionInmediata() {
        ConfirmacionImportacionDto solicitud = new ConfirmacionImportacionDto("a-1", "datos.csv",
                "GENERICO", "", List.of());
        ResultadoImportacionUniversalDto esperado =
                new ResultadoImportacionUniversalDto("a-1", "STAGED", 1, 1, 0, 0, List.of());
        when(servicio.confirmar(1L, solicitud)).thenReturn(esperado);

        ResultadoImportacionUniversalDto resultado = controller.confirmar(1L, solicitud, authentication);

        assertEquals("STAGED", resultado.estado());
        verify(autorizacion).verificar(1L, authentication);
    }

    @Test
    void mapeosDevuelveListadoDelServicio() {
        List<MapeoColumnaDto> esperado = List.of(
                new MapeoColumnaDto("nit", "NIT", 0, 1.0, "GENERICO"));
        when(servicio.mapeos(eq(1L))).thenReturn(esperado);

        assertEquals(esperado, controller.mapeos(1L, authentication));
        verify(servicio).mapeos(1L);
        verify(autorizacion).verificar(1L, authentication);
    }
}
