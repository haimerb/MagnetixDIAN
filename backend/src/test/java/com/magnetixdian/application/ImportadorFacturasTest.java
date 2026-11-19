package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportadorFacturasTest {

    private final ServicioFacturacion facturacion = mock(ServicioFacturacion.class);
    private final ImportadorFacturas importador = new ImportadorFacturas(facturacion);

    private static byte[] csv(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void agrupaFilasPorGrupoYEmiteUnaFacturaPorGrupo() {
        when(facturacion.emitir(anyLong(), anyLong(), any()))
                .thenReturn(new ResultadoEmisionDto(true, 1L, "F-1", "CUFE", "ACEPTADO", null, "ok", List.of()));

        String contenido = "grupo;adquirente_numero_documento;adquirente_nombre;descripcion;cantidad;valor_unitario;iva_pct\n"
                + "A;900123456;Cliente Uno;Servicio uno;2;1000;19\n"
                + "A;900123456;Cliente Uno;Servicio dos;1;500;19\n"
                + "B;800999888;Cliente Dos;Producto;3;2500;0\n";

        ResultadoImportacionDto resultado = importador.importar(1L, 7L, csv(contenido), "facturas.csv");

        assertThat(resultado.facturasEmitidas()).isEqualTo(2);
        assertThat(resultado.facturasRechazadas()).isZero();
        assertThat(resultado.lineasImportadas()).isEqualTo(3);

        ArgumentCaptor<FacturaRequest> captor = ArgumentCaptor.forClass(FacturaRequest.class);
        verify(facturacion, org.mockito.Mockito.times(2)).emitir(anyLong(), anyLong(), captor.capture());
        assertThat(captor.getAllValues().get(0).lineas()).hasSize(2);
        assertThat(captor.getAllValues().get(1).lineas()).hasSize(1);
        assertThat(captor.getAllValues().get(0).adquirente().numeroDocumento()).isEqualTo("900123456");
    }

    @Test
    void reportaColumnasFaltantes() {
        ResultadoImportacionDto resultado = importador.importar(1L, 7L,
                csv("grupo;descripcion\nA;Servicio\n"), "facturas.csv");

        assertThat(resultado.facturasEmitidas()).isZero();
        assertThat(resultado.errores()).anyMatch(e -> e.contains("adquirente_numero_documento"));
    }

    @Test
    void reportaGruposRechazados() {
        when(facturacion.emitir(anyLong(), anyLong(), any()))
                .thenReturn(new ResultadoEmisionDto(false, null, null, null, "RECHAZADO", null,
                        "El documento tiene errores de validación", List.of()));

        String contenido = "grupo;adquirente_numero_documento;adquirente_nombre;descripcion;cantidad;valor_unitario\n"
                + "A;900123456;Cliente Uno;Servicio;1;1000\n";

        ResultadoImportacionDto resultado = importador.importar(1L, 7L, csv(contenido), "facturas.csv");

        assertThat(resultado.facturasRechazadas()).isEqualTo(1);
        assertThat(resultado.errores()).anyMatch(e -> e.startsWith("Grupo A:"));
    }
}
