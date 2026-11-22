package com.magnetixdian.domain.contabilidad;

import com.magnetixdian.interfaces.dto.DocumentoRadianDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class GeneradorAsientoRadian {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private GeneradorAsientoRadian() {
    }

    public record Linea(String codigoCuenta, String descripcion, BigDecimal debito, BigDecimal credito) {
    }

    public record Asiento(List<Linea> lineas, BigDecimal totalDebito, BigDecimal totalCredito) {
    }

    public static Asiento generar(DocumentoRadianDto documento) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento RADIAN es obligatorio.");
        }
        BigDecimal subtotal = escala(documento.subtotal());
        BigDecimal impuestos = escala(documento.impuestos());
        BigDecimal retenciones = escala(documento.retenciones());
        BigDecimal total = escala(documento.total());

        if (retenciones.compareTo(total) > 0) {
            throw new IllegalArgumentException("Las retenciones no pueden superar el total del documento.");
        }
        if (subtotal.add(impuestos).compareTo(total) != 0) {
            throw new IllegalArgumentException("El total debe ser igual a subtotal más impuestos.");
        }

        String direccion = documento.direccion() == null ? "" : documento.direccion().toUpperCase();
        String tipo = documento.tipoDocumento();
        List<Linea> lineas = new ArrayList<>();

        if ("05".equals(tipo)) {
            agregar(lineas, PlanCuentas.COMPRAS, subtotal, CERO);
            agregar(lineas, PlanCuentas.IVA_DESCONTABLE, impuestos, CERO);
            agregar(lineas, PlanCuentas.RETENCION_FUENTE, CERO, retenciones);
            agregar(lineas, PlanCuentas.PROVEEDORES, CERO, total.subtract(retenciones));
        } else if ("EMITIDO".equals(direccion)) {
            if ("91".equals(tipo)) {
                agregar(lineas, PlanCuentas.DEVOLUCIONES_VENTAS, subtotal, CERO);
                agregar(lineas, PlanCuentas.IVA_GENERADO, impuestos, CERO);
                agregar(lineas, PlanCuentas.CLIENTES, retenciones, CERO);
                agregar(lineas, PlanCuentas.RETENCIONES, CERO, retenciones);
                agregar(lineas, PlanCuentas.CLIENTES, CERO, total);
            } else {
                agregar(lineas, PlanCuentas.CLIENTES, total, CERO);
                agregar(lineas, PlanCuentas.RETENCIONES, retenciones, CERO);
                agregar(lineas, PlanCuentas.VENTAS, CERO, subtotal);
                agregar(lineas, PlanCuentas.IVA_GENERADO, CERO, impuestos);
                agregar(lineas, PlanCuentas.CLIENTES, CERO, retenciones);
            }
        } else {
            if ("91".equals(tipo)) {
                agregar(lineas, PlanCuentas.RETENCION_FUENTE, retenciones, CERO);
                agregar(lineas, PlanCuentas.PROVEEDORES, total.subtract(retenciones), CERO);
                agregar(lineas, PlanCuentas.DEVOLUCIONES_COMPRAS, CERO, subtotal);
                agregar(lineas, PlanCuentas.IVA_DESCONTABLE, CERO, impuestos);
            } else {
                agregar(lineas, PlanCuentas.COMPRAS, subtotal, CERO);
                agregar(lineas, PlanCuentas.IVA_DESCONTABLE, impuestos, CERO);
                agregar(lineas, PlanCuentas.RETENCION_FUENTE, CERO, retenciones);
                agregar(lineas, PlanCuentas.PROVEEDORES, CERO, total.subtract(retenciones));
            }
        }

        BigDecimal totalDebito = CERO;
        BigDecimal totalCredito = CERO;
        for (Linea linea : lineas) {
            totalDebito = totalDebito.add(linea.debito());
            totalCredito = totalCredito.add(linea.credito());
        }
        if (totalDebito.compareTo(totalCredito) != 0) {
            throw new IllegalStateException("El asiento RADIAN generado no está balanceado.");
        }
        return new Asiento(lineas, totalDebito, totalCredito);
    }

    private static void agregar(List<Linea> lineas, String codigoCuenta, BigDecimal debito, BigDecimal credito) {
        BigDecimal debitoEscalado = escala(debito);
        BigDecimal creditoEscalado = escala(credito);
        if (debitoEscalado.compareTo(CERO) == 0 && creditoEscalado.compareTo(CERO) == 0) {
            return;
        }
        lineas.add(new Linea(codigoCuenta, PlanCuentas.nombre(codigoCuenta),
                debitoEscalado, creditoEscalado));
    }

    private static BigDecimal escala(BigDecimal valor) {
        if (valor == null) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
