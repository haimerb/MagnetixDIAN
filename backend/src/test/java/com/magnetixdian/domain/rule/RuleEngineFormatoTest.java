package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineFormatoTest {

    private final RuleEngine engine = new RuleEngine(List.of(
            new ObligatoriedadCampoRegla(),
            new ObligatoriedadCampo1002Regla(),
            new ValorMovimiento1002Regla(),
            new ConsistenciaValores1002Regla(),
            new ClaveTercero1002Regla(),
            new ConceptoValidoRegla()));

    private ContextoValidacion contexto(String formato) {
        OperacionDatos op = new OperacionDatos(1, "NIT", "900123456", 8, "7001",
                new BigDecimal("500000"), null, null, null, null,
                null, null, null, null, null, false);
        return new ContextoValidacion(op, new BigDecimal("52374"), Map.of("7001", "x"), null, formato);
    }

    @Test
    void formato1002OmiteReglasDel1001() {
        List<ResultadoRegla> r = engine.validar(contexto("1002"));
        assertEquals(1, r.size());
        assertEquals("CONCEPTO_VALIDO", r.get(0).codigoRegla());
    }

    @Test
    void formato1001EjecutaSusReglas() {
        OperacionDatos op = new OperacionDatos(1, "NIT", "900123456", 8, "7001",
                null, null, null, null, null, null, null, null, null, null, false);
        ContextoValidacion ctx = new ContextoValidacion(op, new BigDecimal("52374"), Map.of("7001", "x"), null, "1001");
        List<ResultadoRegla> r = engine.validar(ctx);
        assertTrue(r.stream().anyMatch(x -> "OBLIGATORIEDAD_CAMPO".equals(x.codigoRegla())));
        assertTrue(r.stream().noneMatch(x -> "OBLIGATORIEDAD_CAMPO_1002".equals(x.codigoRegla())));
    }

    @Test
    void conceptoInvalidoIndicaElFormatoEnElMensaje() {
        OperacionDatos op = new OperacionDatos(1, "NIT", "900123456", 8, "9999",
                new BigDecimal("500000"), null, null, null, null,
                null, null, null, null, null, false);
        ContextoValidacion ctx = new ContextoValidacion(op, new BigDecimal("52374"), Map.of(), null, "1002");
        List<ResultadoRegla> r = engine.validar(ctx);
        assertTrue(r.stream().anyMatch(x -> x.mensaje() != null && x.mensaje().contains("formato 1002")));
    }
}
