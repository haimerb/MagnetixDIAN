package com.magnetixdian.application;

import com.magnetixdian.domain.notificacion.NotificadorWhatsApp;
import com.magnetixdian.infrastructure.notification.NotificadorWhatsAppClickToChat;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppRequest;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioEntregaFacturaTest {

    private FacturaRepository facturaRepository;
    private ServicioBranding branding;
    private NotificadorWhatsApp notificador;
    private AuditoriaService auditoria;
    private ServicioEntregaFactura servicio;

    @BeforeEach
    void setUp() {
        facturaRepository = mock(FacturaRepository.class);
        branding = mock(ServicioBranding.class);
        notificador = new NotificadorWhatsAppClickToChat("https://wa.me");
        auditoria = mock(AuditoriaService.class);
        servicio = new ServicioEntregaFactura(facturaRepository, branding, notificador, auditoria);
    }

    private BrandingDto marca(String numero, String plantilla) {
        return new BrandingDto(1L, "mi-empresa", "Mi Empresa", null, "#0E5C55", "#D98A2B", "Roboto",
                null, numero, plantilla, "#0E5C55", "#FFFFFF");
    }

    private void factura(Long empresaId) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(empresaId);
        FacturaJpa factura = mock(FacturaJpa.class);
        when(factura.getEmpresa()).thenReturn(empresa);
        when(factura.getNumeroCompleto()).thenReturn("FE 1 1 1001");
        when(factura.getTotalPagar()).thenReturn(new BigDecimal("1234500.00"));
        when(facturaRepository.findById(10L)).thenReturn(Optional.of(factura));
    }

    @Test
    void construyeEnlaceWaMeYAuditaLaEntrega() {
        factura(1L);
        when(branding.obtener(1L)).thenReturn(marca("573001234567", "Su factura {numero} por {total}"));

        EntregaWhatsAppResponse respuesta = servicio.entregar(1L, 10L,
                new EntregaWhatsAppRequest(" +57 300 123 4567 ", null), "admin");

        assertThat(respuesta.numero()).isEqualTo("+573001234567".replace("+", ""));
        assertThat(respuesta.enlace()).startsWith("https://wa.me/573001234567?text=");
        String texto = URLDecoder.decode(respuesta.enlace().substring(respuesta.enlace().indexOf("?text=") + 6),
                StandardCharsets.UTF_8);
        assertThat(texto).isEqualTo("Su factura FE 1 1 1001 por 1234500.00");
        assertThat(respuesta.enviado()).isTrue();
        verify(auditoria).registrarEnEmpresa(eq(1L), eq("admin"),
                eq(ServicioEntregaFactura.ACCION_ENTREGA), eq("factura"), eq(10L), anyString());
    }

    @Test
    void usaNumeroYPlantillaDeLaMarcaCuandoLaPeticionNoLosTrae() {
        factura(1L);
        when(branding.obtener(1L)).thenReturn(marca("3001234567", "Factura {{numero}} total {{total}}"));

        EntregaWhatsAppResponse respuesta = servicio.entregar(1L, 10L, new EntregaWhatsAppRequest(null, null));

        assertThat(respuesta.numero()).isEqualTo("3001234567");
        assertThat(respuesta.mensaje()).isEqualTo("Factura FE 1 1 1001 total 1234500.00");
        assertThat(respuesta.enlace()).startsWith("https://wa.me/3001234567?text=");
    }

    @Test
    void rechazaNumerosInvalidosSinAuditar() {
        factura(1L);
        when(branding.obtener(1L)).thenReturn(marca("573001234567", null));

        assertThatThrownBy(() -> servicio.entregar(1L, 10L, new EntregaWhatsAppRequest("12345", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.entregar(1L, 10L, new EntregaWhatsAppRequest("30abc4567", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(auditoria, never()).registrarEnEmpresa(any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void rechazaFacturaDeOtraEmpresa() {
        factura(2L);

        assertThatThrownBy(() -> servicio.entregar(1L, 10L, new EntregaWhatsAppRequest("3001234567", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(auditoria, never()).registrarEnEmpresa(any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void validaLongitudColombianaYConstruyeEnlaceSeguro() {
        assertThat(notificador.numeroValido("+57 300 123 4567")).isTrue();
        assertThat(notificador.numeroValido("300123")).isFalse();
        assertThat(notificador.numeroValido("5730012345678901")).isFalse();

        String enlace = notificador.construirEnlace("300 123 4567", "Factura 1001");
        assertThat(enlace).startsWith("https://wa.me/3001234567?text=");
        assertThat(URLDecoder.decode(enlace.substring(enlace.indexOf("?text=") + 6), StandardCharsets.UTF_8))
                .isEqualTo("Factura 1001");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(branding.obtener(1L)).thenReturn(marca("573001234567", "Hola"));
        factura(1L);
        servicio.entregar(1L, 10L, new EntregaWhatsAppRequest("3001234567", null));
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), anyString(), anyString(), any(), captor.capture());
        assertThat(captor.getValue()).contains("https://wa.me/3001234567");
    }
}
