package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EventoAuditoriaJpa;
import com.magnetixdian.infrastructure.persistence.EventoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditoriaPorEmpresaTest {

    private EventoAuditoriaRepository repository;
    private AuditoriaService servicio;

    @BeforeEach
    void setUp() {
        repository = mock(EventoAuditoriaRepository.class);
        servicio = new AuditoriaService(repository);
    }

    @Test
    void registraElEventoConLaEmpresaIndicada() {
        servicio.registrarEnEmpresa(7L, "admin", "FACTURA_ENTREGADA_WHATSAPP", "factura", 10L, "detalle");

        ArgumentCaptor<EventoAuditoriaJpa> captor = ArgumentCaptor.forClass(EventoAuditoriaJpa.class);
        verify(repository).save(captor.capture());
        EventoAuditoriaJpa evento = captor.getValue();
        assertThat(evento.getEmpresaId()).isEqualTo(7L);
        assertThat(evento.getUsuario()).isEqualTo("admin");
        assertThat(evento.getAccion()).isEqualTo("FACTURA_ENTREGADA_WHATSAPP");
        assertThat(evento.getEntidad()).isEqualTo("factura");
        assertThat(evento.getEntidadId()).isEqualTo("10");
        assertThat(evento.getDetalle()).isEqualTo("detalle");
    }

    @Test
    void registrarSinEmpresaDelegaConEmpresaNula() {
        servicio.registrar("admin", "LOGIN", "usuario", 3L, "ok");

        ArgumentCaptor<EventoAuditoriaJpa> captor = ArgumentCaptor.forClass(EventoAuditoriaJpa.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isNull();
        assertThat(captor.getValue().getAccion()).isEqualTo("LOGIN");
    }

    @Test
    void unFalloDeAuditoriaNoInterrumpeLaOperacion() {
        when(repository.save(any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatCode(() -> servicio.registrarEnEmpresa(7L, "admin", "ACCION", "factura", 10L, "detalle"))
                .doesNotThrowAnyException();
    }
}
