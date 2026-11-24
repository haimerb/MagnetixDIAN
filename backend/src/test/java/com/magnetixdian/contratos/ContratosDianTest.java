package com.magnetixdian.contratos;

import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDianResolver;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratosDianTest {

    private final ModoOperacionDianResolver resolver = new ModoOperacionDianResolver();

    @Test
    void resolverEsComponenteSpring() {
        assertTrue(ModoOperacionDianResolver.class.isAnnotationPresent(Component.class));
    }

    @Test
    void propioInternoEsTenantInternoSinTercerosNiProveedorTecnologico() {
        ModoOperacionDian modo = ModoOperacionDian.PROPIO_INTERNO;

        assertTrue(resolver.esTenantInterno(modo));
        assertFalse(resolver.permiteFacturarATerceros(modo));
        assertFalse(resolver.requiereHabilitacionProveedorTecnologico(modo));
        assertFalse(resolver.esAdquirido(modo));
        assertEquals(modo, resolver.resolver("PROPIO_INTERNO"));
    }

    @Test
    void adquiridoClienteEsElModoPorDefectoDelCliente() {
        ModoOperacionDian modo = ModoOperacionDian.ADQUIRIDO_CLIENTE;

        assertEquals(modo, resolver.modoPorDefectoParaCliente());
        assertFalse(resolver.esTenantInterno(modo));
        assertFalse(resolver.permiteFacturarATerceros(modo));
        assertFalse(resolver.requiereHabilitacionProveedorTecnologico(modo));
        assertTrue(resolver.esAdquirido(modo));
        assertEquals(modo, resolver.resolver("ADQUIRIDO_CLIENTE"));
    }

    @Test
    void ptMagnetixdianPermiteFacturarATercerosYRequiereHabilitacion() {
        ModoOperacionDian modo = ModoOperacionDian.PT_MAGNETIXDIAN;

        assertFalse(resolver.esTenantInterno(modo));
        assertTrue(resolver.permiteFacturarATerceros(modo));
        assertTrue(resolver.requiereHabilitacionProveedorTecnologico(modo));
        assertFalse(resolver.esAdquirido(modo));
        assertEquals(modo, resolver.resolver("PT_MAGNETIXDIAN"));
    }

    @Test
    void ptExternoPermiteFacturarATercerosYRequiereHabilitacion() {
        ModoOperacionDian modo = ModoOperacionDian.PT_EXTERNO;

        assertFalse(resolver.esTenantInterno(modo));
        assertTrue(resolver.permiteFacturarATerceros(modo));
        assertTrue(resolver.requiereHabilitacionProveedorTecnologico(modo));
        assertFalse(resolver.esAdquirido(modo));
        assertEquals(modo, resolver.resolver("PT_EXTERNO"));
    }

    @Test
    void resolverRechazaValoresFueraDelContrato() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver("INVALIDO"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver("propio_interno"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver(" PROPIO_INTERNO"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver(null));
    }

    @Test
    void politicasRechazanModoNulo() {
        assertThrows(IllegalArgumentException.class, () -> resolver.esTenantInterno(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.permiteFacturarATerceros(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.requiereHabilitacionProveedorTecnologico(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.esAdquirido(null));
    }
}
