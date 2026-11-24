package com.magnetixdian.dian;

import com.magnetixdian.domain.dian.CatalogoWsdlDian;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoWsdlDianTest {

    @Test
    void resuelveRecursosVersionadosParaAmbosAmbientes() {
        for (String version : new String[]{"1.8", "2.1"}) {
            for (int ambiente : new int[]{1, 2}) {
                assertEquals("wsdl/dian/" + version + "/" + ambiente + "/WcfDianCustomerServices.wsdl",
                        CatalogoWsdlDian.ruta(version, ambiente));
                CatalogoWsdlDian.validarRecurso(version, ambiente);
            }
        }
    }

    @Test
    void rechazaVersionNoSoportada() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoWsdlDian.ruta("2026", 1));
    }

    @Test
    void parseaRespuestaAprobada() {
        String cuerpo = """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                  <s:Body>
                    <SendBillSyncResponse>
                      <TrackId>TRACK-1</TrackId>
                      <StatusCode>00</StatusCode>
                      <StatusMessage>Aceptado</StatusMessage>
                      <Cufe>CUFE-1</Cufe>
                      <XmlHash>HASH-1</XmlHash>
                      <Timestamp>2026-11-23T10:00:00Z</Timestamp>
                    </SendBillSyncResponse>
                  </s:Body>
                </s:Envelope>
                """;
        var respuesta = ClienteDianSoap.parsearRespuesta(cuerpo);
        assertTrue(respuesta.aprobada());
        assertEquals("TRACK-1", respuesta.trackId());
        assertEquals("CUFE-1", respuesta.cufe());
        assertEquals("HASH-1", respuesta.xmlHash());
        assertTrue(respuesta.errores().isEmpty());
    }

    @Test
    void parseaRechazoConErrores() {
        String cuerpo = """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                  <s:Body>
                    <GetStatusResponse>
                      <TrackId>TRACK-2</TrackId>
                      <StatusCode>01</StatusCode>
                      <StatusMessage>Rechazado</StatusMessage>
                      <Error><Code>E-1</Code><Message>Documento inválido</Message></Error>
                    </GetStatusResponse>
                  </s:Body>
                </s:Envelope>
                """;
        var respuesta = ClienteDianSoap.parsearRespuesta(cuerpo);
        assertFalse(respuesta.aprobada());
        assertEquals("E-1", respuesta.errores().get(0).codigo());
        assertEquals("Documento inválido", respuesta.errores().get(0).mensaje());
    }

    @Test
    void parseaFaultSoap() {
        String cuerpo = """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                  <s:Body>
                    <s:Fault>
                      <faultcode>s:Client</faultcode>
                      <faultstring>Certificado inválido</faultstring>
                    </s:Fault>
                  </s:Body>
                </s:Envelope>
                """;
        var respuesta = ClienteDianSoap.parsearRespuesta(cuerpo);
        assertFalse(respuesta.aprobada());
        assertEquals("s:Client", respuesta.statusCode());
        assertEquals("Certificado inválido", respuesta.statusMessage());
    }
}
