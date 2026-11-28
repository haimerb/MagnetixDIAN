package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ValorMovimiento1002Regla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "VALOR_MOVIMIENTO_1002";
    }

    @Override
    public String nombre() {
        return "Valor de movimiento del registro 1002";
    }

    @Override
    public boolean aplicaA(String formato) {
        return "1002".equals(formato);
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        BigDecimal movimiento = op.valorPago();
        BigDecimal notaCredito = op.valorNc();

        if (movimiento == null && notaCredito == null) {
            return List.of();
        }
        if (esPositivo(movimiento) || esPositivo(notaCredito)) {
            return List.of();
        }

        return List.of(ResultadoRegla.error(
                codigo(),
                Severidad.ERROR,
                "El registro 1002 requiere un valor de movimiento o de nota crédito mayor que cero.",
                op.linea(),
                "valorPago",
                "> 0",
                "movimiento=" + movimiento + ", notaCredito=" + notaCredito));
    }

    private boolean esPositivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
