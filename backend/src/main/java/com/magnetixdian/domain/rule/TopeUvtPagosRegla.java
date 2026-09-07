package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regla: TOPE_UVT_PAGOS — pagos acumulados por debajo de la cuantía menor
 * (3 UVT para el formato 1001) deben reportarse de forma agregada como
 * cuantías menores (NIT genérico 222222222).
 */
@Component
public class TopeUvtPagosRegla implements ReglaValidacion {

    /** Por resolución DIAN, la cuantía menor del formato 1001 es 3 UVT. */
    private static final BigDecimal CUANTIA_MENOR_UVT = new BigDecimal("3");

    @Override
    public String codigo() {
        return "TOPE_UVT_PAGOS";
    }

    @Override
    public String nombre() {
        return "Tope UVT para reporte de pagos (cuantía menor 3 UVT)";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        if (op.valorPago() == null || op.valorPago().signum() <= 0) {
            return List.of();
        }

        BigDecimal tope = contexto.cuantiaMenor() != null
                ? contexto.cuantiaMenor()
                : contexto.uvtAPesos(CUANTIA_MENOR_UVT);

        if (op.cuantiaMenor()) {
            return List.of(ResultadoRegla.ok(codigo()));
        }

        if (op.valorPago().compareTo(tope) < 0) {
            return List.of(ResultadoRegla.error(
                    codigo(), Severidad.ADVERTENCIA,
                    "Pago por " + op.valorPago() + " (menor a cuantía menor de " + tope
                    + ") debe reportarse como cuantía menor con NIT genérico 222222222.",
                    op.linea(), "valorPago", "≥ " + tope, op.valorPago().toString()));
        }

        return List.of(ResultadoRegla.ok(codigo()));
    }
}