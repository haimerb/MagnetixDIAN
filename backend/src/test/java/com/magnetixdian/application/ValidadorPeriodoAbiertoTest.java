package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorPeriodoAbiertoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 15);

    @Mock
    private PeriodoContableRepository periodoRepository;

    @Test
    void permiteMovimientoEnPeriodoAbierto() {
        when(periodoRepository.findVigentePorFecha(1L, FECHA)).thenReturn(Optional.of(periodo("ABIERTO")));

        ValidadorPeriodoAbierto validador = new ValidadorPeriodoAbierto(periodoRepository);

        assertDoesNotThrow(() -> validador.verificar(1L, FECHA));
    }

    @Test
    void bloqueaMovimientoEnPeriodoCerrado() {
        when(periodoRepository.findVigentePorFecha(1L, FECHA)).thenReturn(Optional.of(periodo("CERRADO")));

        ValidadorPeriodoAbierto validador = new ValidadorPeriodoAbierto(periodoRepository);

        assertThrows(IllegalArgumentException.class, () -> validador.verificar(1L, FECHA));
    }

    @Test
    void permiteMovimientoSinPeriodoConfigurado() {
        when(periodoRepository.findVigentePorFecha(1L, FECHA)).thenReturn(Optional.empty());

        ValidadorPeriodoAbierto validador = new ValidadorPeriodoAbierto(periodoRepository);

        assertDoesNotThrow(() -> validador.verificar(1L, FECHA));
    }

    private static PeriodoContableJpa periodo(String estado) {
        PeriodoContableJpa periodo = new PeriodoContableJpa();
        periodo.setAnio(2026);
        periodo.setMes(3);
        periodo.setEstado(estado);
        return periodo;
    }
}
