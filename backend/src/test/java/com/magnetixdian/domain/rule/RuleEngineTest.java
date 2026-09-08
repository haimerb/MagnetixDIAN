package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {

    @Test
    void ejecutaTodasLasReglasRegistradas() {
        ReglaValidacion r1 = regla("R1", "regla uno");
        ReglaValidacion r2 = regla("R2", "regla dos");
        RuleEngine engine = new RuleEngine(List.of(r1, r2));

        ContextoValidacion contexto = contexto();
        List<ResultadoRegla> resultados = engine.validar(contexto);

        assertEquals(2, resultados.size());
    }

    @Test
    void reglasActivasSeOrdenanPorCodigo() {
        RuleEngine engine = new RuleEngine(List.of(
                regla("B", "segunda"),
                regla("A", "primera")));

        List<ReglaValidacion> activas = engine.reglasActivas();
        assertEquals(2, activas.size());
        assertEquals("A", activas.get(0).codigo());
        assertEquals("B", activas.get(1).codigo());
    }

    @Test
    void codigoDuplicadoQuedaConLaUltimaRegla() {
        RuleEngine engine = new RuleEngine(List.of(
                regla("X", "original"),
                regla("X", "reemplazo")));
        assertEquals(1, engine.reglasActivas().size());
        assertEquals("reemplazo", engine.reglasActivas().get(0).nombre());
    }

    private ReglaValidacion regla(String codigo, String nombre) {
        return new ReglaValidacion() {
            @Override
            public String codigo() {
                return codigo;
            }

            @Override
            public String nombre() {
                return nombre;
            }

            @Override
            public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
                return List.of(ResultadoRegla.ok(codigo));
            }
        };
    }

    private ContextoValidacion contexto() {
        com.magnetixdian.domain.model.OperacionDatos op =
                new com.magnetixdian.domain.model.OperacionDatos(
                        1, "NIT", "900123456", 8, "2012",
                        new BigDecimal("3000000"), null, null, null, null,
                        null, null, null, null, null, false);
        return new ContextoValidacion(op, new BigDecimal("52374"), java.util.Map.of(), null);
    }
}