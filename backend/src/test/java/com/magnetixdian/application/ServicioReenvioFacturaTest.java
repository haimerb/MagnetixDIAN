package com.magnetixdian.application;

import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.interfaces.dto.ReenvioFacturaDto;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioReenvioFacturaTest {

    private FacturaRepository facturaRepository;
    private ServicioFacturacion facturacion;
    private NotificadorEmail notificador;
    private AuditoriaService auditoria;
    private ServicioReenvioFactura servicio;

    @BeforeEach
    void setUp() {
        facturaRepository = mock(FacturaRepository.class);
        facturacion = mock(ServicioFacturacion.class);
        notificador = mock(NotificadorEmail.class);
        auditoria = mock(AuditoriaService.class);
        servicio = new ServicioReenvioFactura(facturaRepository, facturacion, notificador, auditoria);
    }

    private void prepararFactura(Long empresaId) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(empresaId);
        when(empresa.getRazonSocial()).thenReturn("MAGNETIXDIAN SAS");
        FacturaJpa factura = mock(FacturaJpa.class);
        when(factura.getEmpresa()).thenReturn(empresa);
        when(factura.getNumeroCompleto()).thenReturn("FE 1 1 1001");
        when(factura.getCufe()).thenReturn("abc123");
        when(factura.getXmlFirmado()).thenReturn("<Invoice/>");
        when(facturaRepository.findById(10L)).thenReturn(Optional.of(factura));
    }

    @Test
    void reenviaFacturaConAdjuntosYRegistraAuditoria() throws MessagingException {
        when(notificador.habilitado()).thenReturn(true);
        when(facturacion.pdf(1L, 10L)).thenReturn("pdf".getBytes());
        prepararFactura(1L);

        ReenvioFacturaDto resultado = servicio.reenviar(1L, 10L, " cliente@example.com ");

        assertTrue(resultado.enviado());
        assertEquals("cliente@example.com", resultado.destinatario());
        verify(notificador).enviarFactura(eq("cliente@example.com"), anyString(), anyString(),
                eq("factura-FE 1 1 1001.xml"), any(byte[].class),
                eq("factura-FE 1 1 1001.pdf"), any(byte[].class));
        verify(auditoria).registrar(any(), eq("FACTURA_REENVIADA"), eq("factura"), eq(10L), anyString());
    }

    @Test
    void rechazaCorreoInvalido() throws MessagingException {
        when(notificador.habilitado()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> servicio.reenviar(1L, 10L, "no-es-correo"));
        verify(notificador, never()).enviarFactura(anyString(), anyString(), anyString(),
                anyString(), any(byte[].class), anyString(), any(byte[].class));
    }

    @Test
    void rechazaSiElCorreoNoEstaConfigurado() {
        when(notificador.habilitado()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> servicio.reenviar(1L, 10L, "cliente@example.com"));
        verify(facturaRepository, never()).findById(any());
    }

    @Test
    void rechazaFacturaDeOtraEmpresa() {
        when(notificador.habilitado()).thenReturn(true);
        prepararFactura(2L);
        assertThrows(IllegalArgumentException.class, () -> servicio.reenviar(1L, 10L, "cliente@example.com"));
        verify(facturacion, never()).pdf(any(), any());
    }

    @Test
    void transformaFalloDeEnvioEnConflicto() throws MessagingException {
        when(notificador.habilitado()).thenReturn(true);
        when(facturacion.pdf(1L, 10L)).thenReturn("pdf".getBytes());
        prepararFactura(1L);
        org.mockito.Mockito.doThrow(new MessagingException("SMTP caído"))
                .when(notificador).enviarFactura(anyString(), anyString(), anyString(),
                        anyString(), any(byte[].class), anyString(), any(byte[].class));

        assertThrows(IllegalStateException.class, () -> servicio.reenviar(1L, 10L, "cliente@example.com"));
        verify(auditoria, never()).registrar(any(), anyString(), anyString(), any(), anyString());
    }
}
