package com.magnetixdian.contabilidad;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudLineaAsientoDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioAsientosNumeracionTest {

    @Test
    void crearBloqueaPeriodoAntesDeNumerar() {
        AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
        PeriodoContableRepository periodoRepository = mock(PeriodoContableRepository.class);
        ServicioPlanCuentas servicioPlanCuentas = mock(ServicioPlanCuentas.class);
        EntityManager entityManager = mock(EntityManager.class);

        PeriodoContableJpa periodo = new PeriodoContableJpa();
        periodo.setId(7L);
        periodo.setEmpresaId(1L);
        periodo.setAnio(2026);
        periodo.setMes(3);
        periodo.setEstado("ABIERTO");
        when(periodoRepository.findVigentePorFecha(1L, LocalDate.of(2026, 3, 15)))
                .thenReturn(Optional.of(periodo));
        when(asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                1L, "MANUAL", 99L, "MANUAL")).thenReturn(Optional.empty());
        when(asientoRepository.findByEmpresaIdAndPeriodoIdOrderByNumeroAsc(1L, 7L)).thenReturn(List.of());
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(servicioPlanCuentas.asegurar(1L)).thenReturn(Map.of(
                "110505", cuenta("110505", "ACTIVO", "DEUDORA"),
                "413505", cuenta("413505", "INGRESO", "ACREEDORA")));

        ServicioAsientos servicio = new ServicioAsientos(asientoRepository, periodoRepository,
                servicioPlanCuentas);
        servicio.setEntityManager(entityManager);

        AsientoContableDto asiento = servicio.crear(1L, new SolicitudAsientoDto(
                LocalDate.of(2026, 3, 15), "Venta", "MANUAL",
                List.of(linea("110505", "100.00", null), linea("413505", null, "100.00")),
                "MANUAL", 99L));

        verify(entityManager).find(PeriodoContableJpa.class, 7L, LockModeType.PESSIMISTIC_WRITE);
        assertEquals("AS-1-202603-0001", asiento.numero());
    }

    private static CuentaContableJpa cuenta(String codigo, String tipo, String naturaleza) {
        CuentaContableJpa cuenta = new CuentaContableJpa();
        cuenta.setCodigo(codigo);
        cuenta.setNombre(codigo);
        cuenta.setTipo(tipo);
        cuenta.setNaturaleza(naturaleza);
        return cuenta;
    }

    private static SolicitudLineaAsientoDto linea(String codigo, String debito, String credito) {
        return new SolicitudLineaAsientoDto(codigo, codigo,
                debito == null ? BigDecimal.ZERO : new BigDecimal(debito),
                credito == null ? BigDecimal.ZERO : new BigDecimal(credito),
                null, null, BigDecimal.ZERO, null);
    }
}
