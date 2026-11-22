package com.magnetixdian.domain.contabilidad;

import com.magnetixdian.interfaces.dto.DocumentoRadianDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class MotorAsientos {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private MotorAsientos() {
    }

    public record Linea(String codigoCuenta,
                        String descripcion,
                        BigDecimal debito,
                        BigDecimal credito,
                        Long centroCostoId,
                        Long terceroId,
                        BigDecimal baseImponible,
                        String impuestoCodigo) {
    }

    public record Resultado(List<Linea> lineas, BigDecimal totalDebito, BigDecimal totalCredito) {
    }

    public static Linea linea(String codigoCuenta, BigDecimal debito, BigDecimal credito) {
        return new Linea(codigoCuenta, PlanCuentas.nombre(codigoCuenta), escala(debito), escala(credito),
                null, null, CERO, null);
    }

    public static Linea linea(String codigoCuenta, BigDecimal debito, BigDecimal credito, String descripcion) {
        return new Linea(codigoCuenta, descripcion, escala(debito), escala(credito),
                null, null, CERO, null);
    }

    public static Resultado desdeDocumentoRadian(DocumentoRadianDto documento) {
        GeneradorAsientoRadian.Asiento generado = GeneradorAsientoRadian.generar(documento);
        List<Linea> lineas = generado.lineas().stream()
                .map(linea -> new Linea(linea.codigoCuenta(), linea.descripcion(),
                        linea.debito(), linea.credito(), null, null, CERO, null))
                .toList();
        return validar(lineas);
    }

    public static Resultado invertir(Resultado original, String descripcion) {
        List<Linea> invertidas = original.lineas().stream()
                .map(linea -> new Linea(linea.codigoCuenta(), descripcion,
                        linea.credito(), linea.debito(), linea.centroCostoId(), linea.terceroId(),
                        linea.baseImponible(), linea.impuestoCodigo()))
                .toList();
        return validar(invertidas);
    }

    public static Resultado validar(List<Linea> lineas) {
        if (lineas == null || lineas.size() < 2) {
            throw new IllegalArgumentException("El asiento debe tener al menos dos líneas.");
        }
        BigDecimal totalDebito = CERO;
        BigDecimal totalCredito = CERO;
        List<Linea> normalizadas = new ArrayList<>();
        for (Linea linea : lineas) {
            if (linea == null || linea.codigoCuenta() == null || linea.codigoCuenta().isBlank()) {
                throw new IllegalArgumentException("Todas las líneas deben tener cuenta contable.");
            }
            BigDecimal debito = escala(linea.debito());
            BigDecimal credito = escala(linea.credito());
            if (debito.signum() < 0 || credito.signum() < 0) {
                throw new IllegalArgumentException("Los valores de débito y crédito no pueden ser negativos.");
            }
            if (debito.signum() == 0 && credito.signum() == 0) {
                throw new IllegalArgumentException("Todas las líneas deben tener débito o crédito.");
            }
            if (debito.signum() > 0 && credito.signum() > 0) {
                throw new IllegalArgumentException("Una línea no puede tener débito y crédito a la vez.");
            }
            totalDebito = totalDebito.add(debito);
            totalCredito = totalCredito.add(credito);
            normalizadas.add(new Linea(linea.codigoCuenta(), linea.descripcion(), debito, credito,
                    linea.centroCostoId(), linea.terceroId(), escala(linea.baseImponible()),
                    linea.impuestoCodigo()));
        }
        if (totalDebito.compareTo(totalCredito) != 0) {
            throw new IllegalArgumentException("El asiento debe estar balanceado.");
        }
        return new Resultado(List.copyOf(normalizadas), totalDebito, totalCredito);
    }

    private static BigDecimal escala(BigDecimal valor) {
        if (valor == null) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
