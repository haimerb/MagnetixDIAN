package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regla: NIT_DIGITO_VERIFICACION — dígito de verificación del NIT.
 *
 * <p>Algoritmo DIAN (módulo 11) aplicado sobre el NIT sin DV. El peso para
 * cada dígito se toma de la secuencia cíclica 3,7,13,17,19,23,29,37,41,43,47.
 * El DV es el residuo de 11 menos (suma mod 11), con casos especiales:
 * residuo 0 → DV 0; residuo 1 → DV 1 y se añade un dígito de control.</p>
 */
@Component
public class NitDigitoVerificacionRegla implements ReglaValidacion {

    private static final int[] PESOS = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 61, 67, 71};

    @Override
    public String codigo() {
        return "NIT_DIGITO_VERIFICACION";
    }

    @Override
    public String nombre() {
        return "Validación dígito de verificación NIT (módulo 11)";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        String nit = op.numeroIdentificacion();
        Integer dv = op.dv();

        if (nit == null || nit.isBlank()) {
            return List.of();
        }

        Integer dvEsperado = calcularDv(nit);
        if (dvEsperado == null || dvEsperado.equals(dv)) {
            return List.of(ResultadoRegla.ok(codigo()));
        }

        return List.of(ResultadoRegla.error(
                codigo(),
                Severidad.ERROR,
                "Dígito de verificación del NIT " + nit + " no corresponde (esperado " + dvEsperado + ").",
                op.linea(),
                "dv",
                String.valueOf(dvEsperado),
                String.valueOf(dv)));
    }

    /**
     * Calcula el dígito de verificación de un NIT según el algoritmo DIAN.
     *
     * @param nit NIT sin DV (solo dígitos)
     * @return DV calculado; {@code null} si el NIT no es numérico o vacío
     */
    public static Integer calcularDv(String nit) {
        if (nit == null || nit.isBlank() || !nit.matches("\\d+")) {
            return null;
        }
        int suma = 0;
        int largo = nit.length();
        for (int i = 0; i < largo; i++) {
            int digito = Character.getNumericValue(nit.charAt(largo - 1 - i));
            suma += digito * PESOS[i % PESOS.length];
        }
        int residuo = suma % 11;
        if (residuo == 0) {
            return 0;
        }
        int dv = 11 - residuo;
        return dv == 11 ? 0 : dv;
    }
}