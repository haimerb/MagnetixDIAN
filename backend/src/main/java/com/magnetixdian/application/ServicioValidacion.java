package com.magnetixdian.application;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.rule.CatalogoConceptos;
import com.magnetixdian.domain.rule.ContextoValidacion;
import com.magnetixdian.domain.rule.RuleEngine;
import com.magnetixdian.domain.uvt.UvtManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquesta la validación de registros de información exógena contra el
 * motor de reglas y los parámetros de la vigencia (UVT + catálogo).
 */
@Service
public class ServicioValidacion {

    private final RuleEngine ruleEngine;
    private final UvtManager uvtManager;
    private final CatalogoConceptos catalogo;

    public ServicioValidacion(RuleEngine ruleEngine, UvtManager uvtManager, CatalogoConceptos catalogo) {
        this.ruleEngine = ruleEngine;
        this.uvtManager = uvtManager;
        this.catalogo = catalogo;
    }

    /**
     * Valida una operación contra la vigencia del año gravable dado.
     *
     * @param operacion registro a validar
     * @param anioGravable año gravable (determina UVT y catálogo)
     * @param cuantiaMenor monto de cuantía menor (null → usa 3 UVT)
     * @return resultados de todas las reglas aplicadas
     */
    public List<ResultadoRegla> validar(OperacionDatos operacion, Integer anioGravable, BigDecimal cuantiaMenor) {
        BigDecimal uvt = uvtManager.uvtParaAnio(anioGravable);
        ContextoValidacion contexto = new ContextoValidacion(
                operacion, uvt, catalogo.conceptosFormato1001(), cuantiaMenor);
        return ruleEngine.validar(contexto);
    }

    /**
     * Valida una lista de operaciones y agrupa el resumen por severidad.
     */
    public ResumenValidacion validarLote(List<OperacionDatos> operaciones, Integer anioGravable) {
        int errores = 0;
        int advertencias = 0;
        int infos = 0;

        for (OperacionDatos operacion : operaciones) {
            for (ResultadoRegla resultado : validar(operacion, anioGravable, null)) {
                if (resultado.valida() || resultado.severidad() == null) {
                    continue;
                }
                switch (resultado.severidad()) {
                    case ERROR -> errores++;
                    case ADVERTENCIA -> advertencias++;
                    case INFO -> infos++;
                }
            }
        }
        return new ResumenValidacion(errores, advertencias, infos);
    }

    public record ResumenValidacion(int errores, int advertencias, int infos) {
        public int total() {
            return errores + advertencias + infos;
        }
    }
}