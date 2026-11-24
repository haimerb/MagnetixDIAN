package com.magnetixdian.contabilidad;

import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.application.ServicioReportesContables;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.interfaces.dto.ResumenContableDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioReportesContablesSqlTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    private final AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
    private final ServicioPlanCuentas servicioPlanCuentas = mock(ServicioPlanCuentas.class);

    @Test
    void reporteSqlRequiereJdbcTemplate() {
        ServicioReportesContables servicio = new ServicioReportesContables(asientoRepository,
                servicioPlanCuentas);

        assertThrows(IllegalStateException.class,
                () -> servicio.resumenPorCuenta(1L, DESDE, HASTA));
    }

    @Test
    void resumenPorCuentaMapeaSaldoDeDeudora() throws SQLException {
        ResultSet fila = mock(ResultSet.class);
        when(fila.getString("codigo")).thenReturn("413505");
        when(fila.getString("nombre")).thenReturn("Ventas");
        when(fila.getBigDecimal("debito")).thenReturn(BigDecimal.ZERO);
        when(fila.getBigDecimal("credito")).thenReturn(new BigDecimal("250.00"));
        when(fila.getBigDecimal("saldo")).thenReturn(new BigDecimal("250.00"));
        when(fila.getInt("cantidad")).thenReturn(3);

        ServicioReportesContables servicio = new ServicioReportesContables(asientoRepository,
                servicioPlanCuentas);
        servicio.setJdbc(jdbcQueDevuelve(fila, "cc.naturaleza"));

        ResumenContableDto resumen = servicio.resumenPorCuenta(1L, DESDE, HASTA).get(0);

        assertEquals("413505", resumen.codigo());
        assertEquals(0, new BigDecimal("250.00").compareTo(resumen.saldo()));
        assertEquals(3, resumen.cantidadAsientos());
    }

    @Test
    void resumenPorTerceroUsaTerceroContable() throws SQLException {
        ResultSet fila = mock(ResultSet.class);
        when(fila.getString("codigo")).thenReturn("900123456");
        when(fila.getString("nombre")).thenReturn("DEMO SAS");
        when(fila.getBigDecimal("debito")).thenReturn(new BigDecimal("100.00"));
        when(fila.getBigDecimal("credito")).thenReturn(BigDecimal.ZERO);
        when(fila.getBigDecimal("saldo")).thenReturn(new BigDecimal("100.00"));
        when(fila.getInt("cantidad")).thenReturn(1);

        ServicioReportesContables servicio = new ServicioReportesContables(asientoRepository,
                servicioPlanCuentas);
        servicio.setJdbc(jdbcQueDevuelve(fila, "LEFT JOIN tercero_contable"));

        ResumenContableDto resumen = servicio.resumenPorTercero(1L, null, HASTA).get(0);

        assertEquals("900123456", resumen.codigo());
        assertEquals("DEMO SAS", resumen.nombre());
        assertEquals(1, resumen.cantidadAsientos());
    }

    private static JdbcTemplate jdbcQueDevuelve(ResultSet fila, String fragmentoSql) {
        return new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                assertTrue(sql.contains(fragmentoSql));
                assertEquals(1L, args[0]);
                assertEquals(5, args.length);
                try {
                    return List.of(rowMapper.mapRow(fila, 0));
                } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };
    }
}
