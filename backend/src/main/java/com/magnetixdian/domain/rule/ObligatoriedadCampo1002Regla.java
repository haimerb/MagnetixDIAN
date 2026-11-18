package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla: OBLIGATORIEDAD_CAMPO_1002 — presencia y formato de los campos
 * obligatorios del registro del formato 1002 (créditos, descuentos,
 * devoluciones y notas de ajuste): tipo documento, NIT, concepto y al menos
 * un valor de movimiento o nota crédito.
 */
@Component
public class ObligatoriedadCampo1002Regla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "OBLIGATORIEDAD_CAMPO_1002";
    }

    @Override
    public String nombre() {
        return "Campos obligatorios del registro 1002";
    }

    @Override
    public boolean aplicaA(String formato) {
        return "1002".equals(formato);
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        List<ResultadoRegla> resultados = new ArrayList<>();

        validarObligatorio(resultados, "tipoDocumento", op.tipoDocumento(), op.linea());
        validarObligatorio(resultados, "numeroIdentificacion", op.numeroIdentificacion(), op.linea());
        validarObligatorio(resultados, "concepto", op.concepto(), op.linea());

        if (op.valorPago() == null && op.valorNc() == null) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "El registro 1002 requiere un valor de movimiento o de nota crédito.",
                    op.linea(), "valorPago", "valorPago o valorNc", "vacío"));
        }

        validarNoNegativo(resultados, "valorPago", op.valorPago(), op.linea());
        validarNoNegativo(resultados, "valorNc", op.valorNc(), op.linea());

        return resultados;
    }

    private void validarObligatorio(List<ResultadoRegla> resultados, String campo, Object valor, Integer linea) {
        if (valor == null || (valor instanceof String s && s.isBlank())) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "Campo obligatorio '" + campo + "' ausente o vacío.",
                    linea, campo, "Valor no vacío", "vacío"));
        }
    }

    private void validarNoNegativo(List<ResultadoRegla> resultados, String campo, BigDecimal valor, Integer linea) {
        if (valor != null && valor.signum() < 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "El campo '" + campo + "' no puede ser negativo.",
                    linea, campo, "≥ 0", valor.toString()));
        }
    }
}
