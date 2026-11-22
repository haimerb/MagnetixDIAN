package com.magnetixdian.domain.contabilidad;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlanCuentas {

    public static final String CLIENTES = "130505";
    public static final String PROVEEDORES = "220505";
    public static final String VENTAS = "413505";
    public static final String DEVOLUCIONES_VENTAS = "413515";
    public static final String COMPRAS = "519505";
    public static final String DEVOLUCIONES_COMPRAS = "529515";
    public static final String IVA_GENERADO = "240805";
    public static final String IVA_DESCONTABLE = "135515";
    public static final String RETENCIONES = "135520";
    public static final String RETENCION_FUENTE = "236505";
    public static final String CAJA = "110505";
    public static final String AJUSTES = "399999";

    private static final Map<String, String> NOMBRES = new LinkedHashMap<>();

    static {
        NOMBRES.put(CLIENTES, "Clientes");
        NOMBRES.put(PROVEEDORES, "Proveedores");
        NOMBRES.put(VENTAS, "Ventas");
        NOMBRES.put(DEVOLUCIONES_VENTAS, "Devoluciones ventas");
        NOMBRES.put(COMPRAS, "Compras");
        NOMBRES.put(DEVOLUCIONES_COMPRAS, "Devoluciones compras");
        NOMBRES.put(IVA_GENERADO, "IVA generado");
        NOMBRES.put(IVA_DESCONTABLE, "IVA descontable");
        NOMBRES.put(RETENCIONES, "Retenciones");
        NOMBRES.put(RETENCION_FUENTE, "Retención en la fuente");
        NOMBRES.put(CAJA, "Caja");
        NOMBRES.put(AJUSTES, "Ajustes");
    }

    private PlanCuentas() {
    }

    public static String nombre(String codigo) {
        return NOMBRES.getOrDefault(codigo, codigo);
    }
}
