package com.magnetixdian.dian;

import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ClienteDianSoapWsdlTest {

    @Test
    void usaVersionDosPuntoUnoCuandoNoHayConfiguracion() {
        assertEquals("2.1", cliente("2.1", "").versionPorDefecto());
    }

    @Test
    void respetaVersionExplicita() {
        assertEquals("1.8", cliente("1.8", "").versionPorDefecto());
    }

    @Test
    void aceptaConfiguracionDeUrlOficial() {
        assertEquals("2.1", cliente("2.1", "https://catalogo-vpfe.dian.gov.co/WcfDianCustomerServices.wsdl")
                .versionPorDefecto());
    }

    private ClienteDianSoap cliente(String version, String urlOficial) {
        return new ClienteDianSoap(
                "https://vpfe.dian.gov.co/WcfDianCustomerServices.svc",
                "https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc",
                version,
                urlOficial,
                1000,
                1000,
                mock(CifradorAes.class));
    }
}
