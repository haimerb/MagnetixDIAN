package com.magnetixdian.contabilidad;

import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import com.magnetixdian.interfaces.dto.RegimenContableDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioPlanCuentasContableTest {

    @Test
    void regimenPorDefectoEsNiifPymes() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("NIIF_PYMES");
        ServicioPlanCuentas servicio = new ServicioPlanCuentas(mock(CuentaContableRepository.class));
        servicio.setJdbc(jdbc);

        RegimenContableDto regimen = servicio.regimen(1L);

        assertEquals("NIIF_PYMES", regimen.regimenNiif());
        assertEquals("NIIF_PYMES", regimen.versionPuc());
    }

    @Test
    void cambiarRegimenAceptaAliasFullYActualizaEmpresa() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("NIIF_FULL");
        ServicioPlanCuentas servicio = new ServicioPlanCuentas(mock(CuentaContableRepository.class));
        servicio.setJdbc(jdbc);

        RegimenContableDto regimen = servicio.cambiarRegimen(1L, "FULL");

        assertEquals("NIIF_FULL", regimen.regimenNiif());
        assertEquals("NIIF_FULL", regimen.versionPuc());
        verify(jdbc).update(anyString(), eq("NIIF_FULL"), eq(1L));
    }

    @Test
    void regimenNoSoportadoFalla() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("NIIF_OTRO");
        ServicioPlanCuentas servicio = new ServicioPlanCuentas(mock(CuentaContableRepository.class));
        servicio.setJdbc(jdbc);

        assertThrows(IllegalArgumentException.class, () -> servicio.regimen(1L));
    }
}
