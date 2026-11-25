package com.magnetixdian.facturacion;

import com.magnetixdian.application.ServicioDocumentoSoporte;
import com.magnetixdian.application.ServicioFacturacion;
import com.magnetixdian.application.ServicioNominaElectronica;
import com.magnetixdian.application.ServicioNotasDian;
import com.magnetixdian.interfaces.dto.DocumentoSoporteRequest;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.LineaFacturaRequest;
import com.magnetixdian.interfaces.dto.NominaElectronicaDto;
import com.magnetixdian.interfaces.dto.NotaDianRequest;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import com.magnetixdian.interfaces.dto.SolicitudEmisionDianDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentosDianServiciosTest {

    @Mock
    private ServicioFacturacion facturacion;

    private ServicioNotasDian notas;
    private ServicioDocumentoSoporte soporte;
    private ServicioNominaElectronica nomina;

    @BeforeEach
    void setUp() {
        notas = new ServicioNotasDian(facturacion);
        soporte = new ServicioDocumentoSoporte(facturacion);
        nomina = new ServicioNominaElectronica(facturacion);
    }

    @Test
    void notaCreditoUsaReferenciaYMotivo() {
        stubResultado();
        NotaDianRequest solicitud = new NotaDianRequest(null, lineas(), "FE-001", "CUFE-001",
                "Descuento", "1", "10");

        notas.emitir(1L, 7L, "91", solicitud);

        SolicitudEmisionDianDto capturada = capturar();
        assertEquals("91", capturada.tipoDocumento());
        assertEquals("CUFE-001", capturada.cufeReferencia());
        assertEquals("FE-001", capturada.numeroReferencia());
        assertEquals("Descuento", capturada.motivo());
        assertEquals("Descuento", capturada.observaciones());
    }

    @Test
    void notaDebitoAceptaAlias() {
        stubResultado();
        notas.emitir(1L, 7L, "nota-debito", new NotaDianRequest(null, lineas(), "FE-001", "CUFE-001",
                "Intereses", null, null));

        assertEquals("92", capturar().tipoDocumento());
    }

    @Test
    void notaSinReferenciaEsRechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> notas.emitir(1L, 7L, "91", new NotaDianRequest(null, lineas(), "FE-001", null,
                        "Motivo", null, null)));
    }

    @Test
    void documentoDeSoporteUsaTipo05YProveedor() {
        stubResultado();
        DocumentoSoporteRequest solicitud = new DocumentoSoporteRequest("31", "900999999", "Proveedor SA",
                "proveedor@test.com", "Honorarios", lineas(), List.of(), "1", "10");

        soporte.emitir(1L, 7L, solicitud);

        SolicitudEmisionDianDto capturada = capturar();
        assertEquals("05", capturada.tipoDocumento());
        assertEquals("900999999", capturada.adquirente().numeroDocumento());
        assertEquals("Proveedor SA", capturada.adquirente().nombre());
        assertEquals("Honorarios", capturada.motivo());
    }

    @Test
    void nominaUsaTipo31YConstruyeLineasYDeducciones() {
        stubResultado();
        NominaElectronicaDto nominaDto = new NominaElectronicaDto("13", "12345678", "Ana Pérez", "2026-03",
                null, List.of(new NominaElectronicaDto.ConceptoDto("SAL", "Salario", new BigDecimal("1000.00"))),
                List.of(new NominaElectronicaDto.ConceptoDto("AFP", "Aportes", new BigDecimal("100.00"))));

        nomina.emitir(1L, 7L, nominaDto);

        SolicitudEmisionDianDto capturada = capturar();
        assertEquals("31", capturada.tipoDocumento());
        assertEquals("12345678", capturada.adquirente().numeroDocumento());
        assertEquals("SAL", capturada.lineas().get(0).codigoProducto());
        assertEquals("05", capturada.retenciones().get(0).codigo());
        assertEquals(new BigDecimal("100.00"), capturada.retenciones().get(0).valor());
        assertEquals(nominaDto, capturada.nomina());
    }

    @Test
    void solicitudDesdeFacturaConservaPedidoId() {
        FacturaRequest request = new FacturaRequest("01", null, List.of(), null, null, null, null,
                null, null, null, 42L);

        assertEquals(42L, SolicitudEmisionDianDto.desde(request).pedidoId());
    }

    private void stubResultado() {
        when(facturacion.emitirSolicitud(eq(1L), eq(7L), any())).thenReturn(resultado());
    }

    private SolicitudEmisionDianDto capturar() {
        ArgumentCaptor<SolicitudEmisionDianDto> captor = ArgumentCaptor.forClass(SolicitudEmisionDianDto.class);
        verify(facturacion).emitirSolicitud(eq(1L), eq(7L), captor.capture());
        return captor.getValue();
    }

    private List<LineaFacturaRequest> lineas() {
        return List.of(new LineaFacturaRequest("SERV", "Servicio", BigDecimal.ONE, "94",
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    private ResultadoEmisionDto resultado() {
        return new ResultadoEmisionDto(true, 1L, "FE-001", "CUFE-001", "ACEPTADO", null, "OK", List.of());
    }
}
