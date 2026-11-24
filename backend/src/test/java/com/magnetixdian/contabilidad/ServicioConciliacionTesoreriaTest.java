package com.magnetixdian.contabilidad;

import com.magnetixdian.application.ServicioConciliacion;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.ConciliacionRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.MovimientoTesoreriaDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionTesoreriaDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

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

class ServicioConciliacionTesoreriaTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 15);

    private final DocumentoRadianRepository documentoRepository = mock(DocumentoRadianRepository.class);
    private final AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
    private final ConciliacionRepository conciliacionRepository = mock(ConciliacionRepository.class);

    @Test
    void requiereAccesoABaseDeDatos() {
        ServicioConciliacion servicio = new ServicioConciliacion(documentoRepository, asientoRepository,
                conciliacionRepository);

        assertThrows(IllegalStateException.class,
                () -> servicio.conciliarTesoreria(1L, List.of(movimientoBanco())));
    }

    @Test
    void sinMovimientosDevuelveCeros() {
        ServicioConciliacion servicio = new ServicioConciliacion(documentoRepository, asientoRepository,
                conciliacionRepository);
        servicio.setJdbc(mock(JdbcTemplate.class));

        ResultadoConciliacionTesoreriaDto resultado = servicio.conciliarTesoreria(1L, List.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.saldoLibro()));
        assertEquals(0, resultado.coincidencias());
        assertTrue(resultado.lineas().isEmpty());
    }

    @Test
    void marcaCoincidenciaEntreLibroYExtracto() throws SQLException {
        ResultSet filaLibro = mock(ResultSet.class);
        when(filaLibro.getDate("fecha")).thenReturn(java.sql.Date.valueOf(FECHA));
        when(filaLibro.getString("cuenta_codigo")).thenReturn("110505");
        when(filaLibro.getString("documento_tipo")).thenReturn("FACTURA");
        when(filaLibro.getLong("documento_id")).thenReturn(5L);
        when(filaLibro.getString("descripcion")).thenReturn("Pago factura");
        when(filaLibro.getBigDecimal("valor")).thenReturn(new BigDecimal("100.00"));

        ServicioConciliacion servicio = new ServicioConciliacion(documentoRepository, asientoRepository,
                conciliacionRepository);
        servicio.setJdbc(jdbcConLibro(filaLibro));

        ResultadoConciliacionTesoreriaDto resultado = servicio.conciliarTesoreria(1L,
                List.of(movimientoBanco()));

        assertEquals(1, resultado.coincidencias());
        assertEquals(1, resultado.lineas().size());
        assertEquals("COINCIDE", resultado.lineas().get(0).estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.diferencia()));
    }

    private static JdbcTemplate jdbcConLibro(ResultSet filaLibro) {
        return new JdbcTemplate() {
            @Override
            public void query(String sql, RowCallbackHandler callback, Object... args) {
                try {
                    callback.processRow(filaLibro);
                } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };
    }

    private static MovimientoTesoreriaDto movimientoBanco() {
        return new MovimientoTesoreriaDto(FECHA, "110505", "900123456", "FACTURA-5",
                "Pago factura", "DEBITO", new BigDecimal("100.00"));
    }
}
