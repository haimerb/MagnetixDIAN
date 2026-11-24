package com.magnetixdian.pago;

import com.magnetixdian.application.PasarelasPago;
import com.magnetixdian.application.pago.CheckoutRequestDto;
import com.magnetixdian.application.pago.CheckoutResponseDto;
import com.magnetixdian.application.pago.ServicioCheckoutBilling;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.PasarelaSimulada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckoutBillingTest {

    @Test
    void checkoutCreaReferenciaYQuedaPendiente() {
        BillingCheckoutRepository checkoutRepo = mock(BillingCheckoutRepository.class);
        EmpresaRepository empresaRepo = mock(EmpresaRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        PlanRepository planRepo = mock(PlanRepository.class);
        when(checkoutRepo.save(any(BillingCheckoutJpa.class))).thenAnswer(i -> i.getArgument(0));

        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(empresa));

        PlanJpa plan = mock(PlanJpa.class);
        when(plan.getCodigo()).thenReturn("PRO");
        when(plan.getNombre()).thenReturn("Profesional");
        when(plan.getPrecioMensual()).thenReturn(new BigDecimal("99000"));
        when(plan.getMoneda()).thenReturn("COP");
        when(planRepo.findByCodigoIgnoreCase("PRO")).thenReturn(Optional.of(plan));

        ServicioCheckoutBilling servicio = new ServicioCheckoutBilling(checkoutRepo, empresaRepo, usuarioRepo,
                planRepo, new PasarelasPago(List.of(new PasarelaSimulada())),
                new ValidadorFirmaWebhook("", "", false, "test"), "http://frontend.test");

        CheckoutResponseDto respuesta = servicio.iniciar(new CheckoutRequestDto("PRO", 1L,
                "admin@magnetix.test", "SIMULADO"));

        assertNotNull(respuesta.referencia());
        assertTrue(respuesta.referencia().startsWith("SIMULADO-"));
        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals(new BigDecimal("99000"), respuesta.monto());
        assertEquals("COP", respuesta.moneda());
    }
}
