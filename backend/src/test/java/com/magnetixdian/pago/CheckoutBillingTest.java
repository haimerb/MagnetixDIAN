package com.magnetixdian.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.PasarelasPago;
import com.magnetixdian.application.pago.CheckoutRequestDto;
import com.magnetixdian.application.pago.CheckoutResponseDto;
import com.magnetixdian.application.pago.ServicioCheckoutBilling;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.PasarelaWompi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckoutBillingTest {

    private BillingCheckoutRepository checkoutRepo;
    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private PlanRepository planRepo;
    private PasarelasPago pasarelas;
    private ServicioCheckoutBilling servicio;

    @BeforeEach
    void setUp() {
        checkoutRepo = mock(BillingCheckoutRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        planRepo = mock(PlanRepository.class);
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

        pasarelas = new PasarelasPago(List.of(pasarelaWompi("pub-test", "integrity-test")));
        servicio = new ServicioCheckoutBilling(checkoutRepo, empresaRepo, usuarioRepo, planRepo,
                pasarelas, "http://frontend.test");
    }

    @Test
    void checkoutCreaReferenciaYQuedaPendienteConPasarelaReal() {
        CheckoutResponseDto respuesta = servicio.iniciar(new CheckoutRequestDto("PRO", 1L,
                "admin@magnetix.test", "WOMPI"));

        assertNotNull(respuesta.referencia());
        assertTrue(respuesta.referencia().startsWith("WOMPI-"));
        assertNotNull(respuesta.checkoutUrl());
        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals(new BigDecimal("99000"), respuesta.monto());
        assertEquals("COP", respuesta.moneda());
    }

    @Test
    void checkoutRechazaPasarelaSimulada() {
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "SIMULADO");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.iniciar(solicitud));

        assertTrue(error.getMessage().contains("SIMULADO"));
    }

    @Test
    void checkoutSinCredencialesDePasarelaDevuelveErrorExplicito() {
        ServicioCheckoutBilling sinCredenciales = new ServicioCheckoutBilling(checkoutRepo, empresaRepo,
                usuarioRepo, planRepo, new PasarelasPago(List.of(pasarelaWompi("", ""))), "http://frontend.test");
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "WOMPI");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> sinCredenciales.iniciar(solicitud));

        assertTrue(error.getMessage().contains("no está configurada"));
        assertTrue(pasarelas.obtener("WOMPI").configurada());
    }

    private static PasarelaWompi pasarelaWompi(String publicKey, String integritySecret) {
        return new PasarelaWompi(publicKey, integritySecret, "events-test",
                "https://checkout.test/p/", "", "https://api.test", 300, false, new ObjectMapper());
    }
}
