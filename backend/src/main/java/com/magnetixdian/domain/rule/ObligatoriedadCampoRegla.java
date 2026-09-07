package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla: OBLIGATORIEDAD_CAMPO — presencia y formato de campos obligatorios
 * del registro 1001 (tipo documento, NIT, concepto, valor del pago).
 */
@Component
public class ObligatoriedadCampoRegla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "OBLIGATORIEDAD_CAMPO";
    }

    @Override
    public String nombre() {
        return "Campos obligatorios del registro";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        List<ResultadoRegla> resultados = new ArrayList<>();

        validarObligatorio(resultados, "tipoDocumento", op.tipoDocumento(), op.linea());
        validarObligatorio(resultados, "numeroIdentificacion", op.numeroIdentificacion(), op.linea());
        validarObligatorio(resultados, "concepto", op.concepto(), op.linea());
        validarObligatorio(resultados, "valorPago", op.valorPago(), op.linea());

        if (op.valorPago() != null && op.valorPago().signum() < 0) {
            resultados.add(ResultadoRegla.error(
                    codigo(), Severidad.ERROR,
                    "El valor del pago no puede ser negativo.",
                    op.linea(), "valorPago", "≥ 0", op.valorPago().toString()));
        }

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
}