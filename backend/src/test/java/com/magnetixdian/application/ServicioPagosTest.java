package com.magnetixdian.application;

import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.infrastructure.pago.PasarelaSimulada;
import com.magnetixdian.infrastructure.pago.PasarelaWompi;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PagoJpa;
import com.magnetixdian.infrastructure.persistence.PagoRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.interfaces.dto.PagoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioPagosTest {

    private PagoRepository pagoRepo;
    private PlanRepository planRepo;
    private EmpresaRepository empresaRepo;
    private ServicioSuscripciones suscripciones;
    private ServicioPagos servicio;

    @BeforeEach
    void setUp() {
        pagoRepo = mock(PagoRepository.class);
        planRepo = mock(PlanRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        suscripciones = mock(ServicioSuscripciones.class);
        AuditoriaService auditoria = mock(AuditoriaService.class);

        PasarelaPago simulada = new PasarelaSimulada();
        PasarelaPago wompi = new PasarelaWompi("", "", "", "https://checkout.wompi.co/p/", new ObjectMapper());
        PasarelasPago pasarelas = new PasarelasPago(List.of(simulada, wompi));

        servicio = new ServicioPagos(pagoRepo, planRepo, empresaRepo, pasarelas, suscripciones, auditoria,
                "http://localhost:5173");
    }

    private void prepararPlan() {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(empresa));
        PlanJpa plan = mock(PlanJpa.class);
        when(plan.getCodigo()).thenReturn("PRO");
        when(plan.getNombre()).thenReturn("Profesional");
        when(plan.getPrecioMensual()).thenReturn(new BigDecimal("99000"));
        when(plan.getMoneda()).thenReturn("COP");
        when(planRepo.findByCodigoIgnoreCase("PRO")).thenReturn(Optional.of(plan));
        when(pagoRepo.save(any(PagoJpa.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void checkoutSimuladoActivaElPlanDeInmediato() {
        prepararPlan();

        PagoDto dto = servicio.iniciar(1L, "PRO", "SIMULADO");

        assertEquals("APROBADO", dto.estado());
        assertNull(dto.url());
        verify(suscripciones).cambiarPlan(1L, "PRO", "SIMULADO");
    }

    @Test
    void pasarelaSinCredencialesNoPermiteCheckout() {
        prepararPlan();

        assertThrows(IllegalStateException.class, () -> servicio.iniciar(1L, "PRO", "WOMPI"));
        verify(suscripciones, never()).cambiarPlan(any(), any(), any());
    }

    @Test
    void pasarelaDesconocidaLanzaError() {
        prepararPlan();
        assertThrows(IllegalArgumentException.class, () -> servicio.iniciar(1L, "PRO", "PAYPAL"));
    }

    @Test
    void webhookDeSimuladaNoHaceNada() {
        servicio.procesarWebhook("SIMULADO", "{}", java.util.Map.of());
        verify(pagoRepo, never()).save(any());
    }
}
