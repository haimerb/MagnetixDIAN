package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla: CONSISTENCIA_VALORES — coherencia aritmética del registro.
 * Las retenciones no pueden superar el valor pagado; los totales deben cuadrar.
 */
@Component
public class ConsistenciaValoresRegla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "CONSISTENCIA_VALORES";
    }

    @Override
    public String nombre() {
        return "Consistencia de valores del registro";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        List<ResultadoRegla> resultados = new ArrayList<>();

        if (op.valorPago() == null || op.valorPago().signum() <= 0) {
            return resultados;
        }

        validarNoSupera(resultados, "retencionRenta", op.retencionRenta(), op.valorPago(), op.linea());
        validarNoSupera(resultados, "retencionIva", op.retencionIva(), op.valorPago(), op.linea());
        validarNoSupera(resultados, "retencionIca", op.retencionIca(), op.valorPago(), op.linea());
        validarNoSupera(resultados, "retencionTimbre", op.retencionTimbre(), op.valorPago(), op.linea());

        var totalRetenciones = java.math.BigDecimal.ZERO
                .add(op.retencionRenta() == null ? java.math.BigDecimal.ZERO : op.retencionRenta())
                .add(op.retencionIva() == null ? java.math.BigDecimal.ZERO : op.retencionIva())
                .add(op.retencionIca() == null ? java.math.BigDecimal.ZERO : op.retencionIca())
                .add(op.retencionTimbre() == null ? java.math.BigDecimal.ZERO : op.retencionTimbre());

        if (totalRetenciones.compareTo(op.valorPago()) > 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "La suma de retenciones (" + totalRetenciones + ") supera el valor del pago.",
                    op.linea(), "retenciones", "≤ valorPago", totalRetenciones.toString()));
        }

        return resultados;
    }

    private void validarNoSupera(List<ResultadoRegla> resultados, String campo,
                                 java.math.BigDecimal retencion, java.math.BigDecimal valorPago, Integer linea) {
        if (retencion == null) {
            return;
        }
        if (retencion.signum() < 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "La retención '" + campo + "' no puede ser negativa.",
                    linea, campo, "≥ 0", retencion.toString()));
            return;
        }
        if (retencion.compareTo(valorPago) > 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "La retención '" + campo + "' (" + retencion + ") supera el valor del pago.",
                    linea, campo, "≤ valorPago", retencion.toString()));
        }
    }
}