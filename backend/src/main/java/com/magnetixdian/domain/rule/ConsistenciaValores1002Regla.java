package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConsistenciaValores1002Regla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "CONSISTENCIA_VALORES_1002";
    }

    @Override
    public String nombre() {
        return "Consistencia de valores del registro 1002";
    }

    @Override
    public boolean aplicaA(String formato) {
        return "1002".equals(formato);
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        BigDecimal base = base(op.valorPago(), op.valorNc());
        if (base == null) {
            return List.of();
        }

        List<ResultadoRegla> resultados = new ArrayList<>();
        validarNoSupera(resultados, "retencionRenta", op.retencionRenta(), base, op.linea());
        validarNoSupera(resultados, "retencionIva", op.retencionIva(), base, op.linea());
        validarNoSupera(resultados, "retencionIca", op.retencionIca(), base, op.linea());
        validarNoSupera(resultados, "retencionTimbre", op.retencionTimbre(), base, op.linea());

        BigDecimal total = BigDecimal.ZERO
                .add(nuloACero(op.retencionRenta()))
                .add(nuloACero(op.retencionIva()))
                .add(nuloACero(op.retencionIca()))
                .add(nuloACero(op.retencionTimbre()));

        if (total.compareTo(base) > 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(),
                    Severidad.ERROR,
                    "La suma de retenciones (" + total + ") supera el valor base 1002.",
                    op.linea(),
                    "retenciones",
                    "≤ valor base 1002",
                    total.toString()));
        }

        return resultados;
    }

    private BigDecimal base(BigDecimal movimiento, BigDecimal notaCredito) {
        if (movimiento != null && movimiento.signum() > 0) {
            return movimiento;
        }
        if (notaCredito != null && notaCredito.signum() > 0) {
            return notaCredito;
        }
        return null;
    }

    private BigDecimal nuloACero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private void validarNoSupera(List<ResultadoRegla> resultados, String campo,
                                 BigDecimal retencion, BigDecimal base, Integer linea) {
        if (retencion == null) {
            return;
        }
        if (retencion.signum() < 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(),
                    Severidad.ERROR,
                    "La retención '" + campo + "' no puede ser negativa.",
                    linea,
                    campo,
                    "≥ 0",
                    retencion.toString()));
            return;
        }
        if (retencion.compareTo(base) > 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(),
                    Severidad.ERROR,
                    "La retención '" + campo + "' (" + retencion + ") supera el valor base 1002.",
                    linea,
                    campo,
                    "≤ valor base 1002",
                    retencion.toString()));
        }
    }
}
