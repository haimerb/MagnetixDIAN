package com.magnetixdian.domain.dian;

import java.util.regex.Pattern;

public final class CatalogoWsdlDian {

    public static final String VERSION_POR_DEFECTO = "1.8";
    public static final String NOMBRE_ARCHIVO = "WcfDianCustomerServices.wsdl";

    private static final Pattern VERSION = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");

    private CatalogoWsdlDian() {
    }

    public static String ruta(String version, int ambiente) {
        String versionNormalizada = validarVersion(version);
        AmbienteDian.desde(ambiente);
        return "wsdl/dian/" + versionNormalizada + "/" + ambiente + "/" + NOMBRE_ARCHIVO;
    }

    public static String recurso(String version, int ambiente) {
        return "classpath:" + ruta(version, ambiente);
    }

    public static void validarRecurso(String version, int ambiente) {
        String ruta = ruta(version, ambiente);
        if (CatalogoWsdlDian.class.getClassLoader().getResource(ruta) == null) {
            throw new IllegalStateException("No hay WSDL DIAN para la versión " + version
                    + " y ambiente " + ambiente + ". Ruta esperada: " + recurso(version, ambiente));
        }
    }

    private static String validarVersion(String version) {
        if (version == null || !VERSION.matcher(version.trim()).matches()) {
            throw new IllegalArgumentException("Versión de WSDL DIAN no soportada: " + version);
        }
        return version.trim();
    }
}
