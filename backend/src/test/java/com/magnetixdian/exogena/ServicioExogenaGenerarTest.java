package com.magnetixdian.exogena;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioExogena;
import com.magnetixdian.domain.exogena.AgregadorExogena;
import com.magnetixdian.domain.exogena.ClasificadorMovimientos;
import com.magnetixdian.domain.exogena.FormatoTributarioCatalogo;
import com.magnetixdian.domain.exogena.GeneradorExcelExogena;
import com.magnetixdian.domain.exogena.GeneradorXml1001Exogena;
import com.magnetixdian.domain.exogena.MapeadorCuentaConcepto;
import com.magnetixdian.domain.exogena.ValidadorExogena;
import com.magnetixdian.domain.exogena.ValidadorXsdExogena;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.interfaces.dto.ResultadoExogenaGenerarDto;
import com.magnetixdian.interfaces.dto.SolicitudExogenaGenerarDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ServicioExogenaGenerarTest {

    private static final int ANO = 2026;
    private static final String FORMATO = FormatoTributarioCatalogo.FORMATO_1001;
    private static final String NIT = "900123456";

    @Test
    void genera1001ConXmlValidadoYExcel() throws Exception {
        ServicioExogena servicio = new ServicioExogena(jdbc(), mock(AuditoriaService.class),
                new FormatoTributarioCatalogo(), new ClasificadorMovimientos(), new MapeadorCuentaConcepto(),
                new ValidadorExogena(), new AgregadorExogena(), new GeneradorExcelExogena(),
                new GeneradorXml1001Exogena(), new ValidadorXsdExogena());

        ResultadoExogenaGenerarDto resultado = servicio.generar(1L,
                new SolicitudExogenaGenerarDto(ANO, FORMATO, true));

        assertEquals(1L, resultado.generacionId());
        assertEquals("GENERADO", resultado.estado());
        assertEquals(1, resultado.totalRenglones());
        assertEquals(0, resultado.totalValidaciones());
        assertFalse(resultado.prevalidacionOficial());
        assertTrue(resultado.xml().contains("prevalidacionOficial=\"false\""));
        assertTrue(resultado.excel().length > 0);
    }

    private static JdbcTemplate jdbc() {
        return new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> tipo, Object... args) {
                if (sql.contains("INSERT INTO exogena_generacion")) {
                    return tipo.cast(1L);
                }
                return tipo.cast("NIIF_PYMES");
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> List<T> queryForList(String sql, Class<T> tipo, Object... args) {
                return (List<T>) List.of("413505");
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
                try {
                    if (sql.contains("FROM dian_concepto")) {
                        return List.of(mapper.mapRow(concepto(), 0));
                    }
                    if (sql.contains("SUM(m.valor)")) {
                        return List.of(mapper.mapRow(renglon(), 0));
                    }
                    return List.of(mapper.mapRow(movimiento(), 0));
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public <T> T query(String sql, ResultSetExtractor<T> extractor, Object... args) {
                try {
                    return extractor.extractData(declarante());
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
                                           ParameterizedPreparedStatementSetter<T> setter) {
                return new int[batchArgs.size()][0];
            }

            @Override
            public int update(String sql, Object... args) {
                return 1;
            }
        };
    }

    private static ResultSet concepto() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        org.mockito.Mockito.when(rs.getLong("id")).thenReturn(10L);
        org.mockito.Mockito.when(rs.getInt("ano")).thenReturn(ANO);
        org.mockito.Mockito.when(rs.getString("formato")).thenReturn(FORMATO);
        org.mockito.Mockito.when(rs.getString("codigo_concepto")).thenReturn("1001");
        org.mockito.Mockito.when(rs.getString("descripcion")).thenReturn("Ingresos operacionales");
        org.mockito.Mockito.when(rs.getString("tipo_tercero")).thenReturn("JURIDICA");
        org.mockito.Mockito.when(rs.getString("tipo_documento")).thenReturn("NIT");
        org.mockito.Mockito.when(rs.getBigDecimal("valor_minimo")).thenReturn(BigDecimal.ZERO);
        org.mockito.Mockito.when(rs.getBoolean("activo")).thenReturn(true);
        return rs;
    }

    private static ResultSet movimiento() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        org.mockito.Mockito.when(rs.getLong("empresa_id")).thenReturn(1L);
        org.mockito.Mockito.when(rs.getInt("ano")).thenReturn(ANO);
        org.mockito.Mockito.when(rs.getObject("asiento_id")).thenReturn(10L);
        org.mockito.Mockito.when(rs.getObject("asiento_linea_id")).thenReturn(11L);
        org.mockito.Mockito.when(rs.getObject("tercero_id")).thenReturn(2L);
        org.mockito.Mockito.when(rs.getString("tipo_documento")).thenReturn("NIT");
        org.mockito.Mockito.when(rs.getString("numero_documento")).thenReturn(NIT);
        org.mockito.Mockito.when(rs.getString("digito_verificacion")).thenReturn(dv());
        org.mockito.Mockito.when(rs.getString("razon_social")).thenReturn("DEMO SAS");
        org.mockito.Mockito.when(rs.getString("municipio")).thenReturn("BOGOTA");
        org.mockito.Mockito.when(rs.getString("pais")).thenReturn("CO");
        org.mockito.Mockito.when(rs.getString("cuenta_codigo")).thenReturn("413505");
        org.mockito.Mockito.when(rs.getString("codigo_concepto")).thenReturn("1001");
        org.mockito.Mockito.when(rs.getObject("concepto_id")).thenReturn(10L);
        org.mockito.Mockito.when(rs.getBigDecimal("valor")).thenReturn(new BigDecimal("150.00"));
        org.mockito.Mockito.when(rs.getDate("fecha"))
                .thenReturn(java.sql.Date.valueOf(LocalDate.of(ANO, 3, 15)));
        org.mockito.Mockito.when(rs.getString("origen_tipo_documento")).thenReturn("FACTURA");
        org.mockito.Mockito.when(rs.getString("origen_numero_documento")).thenReturn("F-1");
        org.mockito.Mockito.when(rs.getString("origen")).thenReturn("FACTURACION");
        return rs;
    }

    private static ResultSet renglon() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        org.mockito.Mockito.when(rs.getInt("ano")).thenReturn(ANO);
        org.mockito.Mockito.when(rs.getString("formato")).thenReturn(FORMATO);
        org.mockito.Mockito.when(rs.getObject("tercero_id")).thenReturn(2L);
        org.mockito.Mockito.when(rs.getString("tipo_documento")).thenReturn("NIT");
        org.mockito.Mockito.when(rs.getString("numero_documento")).thenReturn(NIT);
        org.mockito.Mockito.when(rs.getString("digito_verificacion")).thenReturn(dv());
        org.mockito.Mockito.when(rs.getString("razon_social")).thenReturn("DEMO SAS");
        org.mockito.Mockito.when(rs.getString("municipio")).thenReturn("BOGOTA");
        org.mockito.Mockito.when(rs.getString("pais")).thenReturn("CO");
        org.mockito.Mockito.when(rs.getLong("concepto_id")).thenReturn(10L);
        org.mockito.Mockito.when(rs.getString("codigo_concepto")).thenReturn("1001");
        org.mockito.Mockito.when(rs.getString("descripcion")).thenReturn("Ingresos operacionales");
        org.mockito.Mockito.when(rs.getBigDecimal("valor")).thenReturn(new BigDecimal("150.00"));
        org.mockito.Mockito.when(rs.getInt("cantidad")).thenReturn(1);
        return rs;
    }

    private static ResultSet declarante() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        org.mockito.Mockito.when(rs.getString("nit")).thenReturn(NIT + "-" + dv());
        org.mockito.Mockito.when(rs.getString("razon_social")).thenReturn("DEMO SAS");
        org.mockito.Mockito.when(rs.getString("tipo_documento")).thenReturn("NIT");
        org.mockito.Mockito.when(rs.getString("ciudad")).thenReturn("BOGOTA");
        org.mockito.Mockito.when(rs.getString("regimen")).thenReturn("COMUN");
        return rs;
    }

    private static String dv() {
        return String.valueOf(NitDigitoVerificacionRegla.calcularDv(NIT));
    }
}
