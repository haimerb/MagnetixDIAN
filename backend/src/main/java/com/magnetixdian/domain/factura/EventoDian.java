package com.magnetixdian.domain.factura;

import java.util.Optional;

/**
 * Eventos DIAN que el receptor (o la DIAN) puede registrar sobre una factura
 * electrónica de venta, conforme al Anexo Técnico v1.9.
 */
public enum EventoDian {

    ACUSE_RECIBO("030", "Acuse de recibo de la factura electrónica"),
    RECLAMO("031", "Reclamo sobre el contenido de la factura"),
    RECIBO_BIEN("032", "Recibo del bien o prestación del servicio"),
    ACEPTACION_EXPRESA("033", "Aceptación expresa de la factura"),
    ACEPTACION_TACITA("034", "Aceptación tácita de la factura");

    private final String codigo;
    private final String descripcion;

    EventoDian(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static Optional<EventoDian> porCodigo(String codigo) {
        for (EventoDian e : values()) {
            if (e.codigo.equals(codigo)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
