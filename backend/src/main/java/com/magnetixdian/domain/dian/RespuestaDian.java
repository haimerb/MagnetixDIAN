package com.magnetixdian.domain.dian;

import java.util.List;

public record RespuestaDian(
        String trackId,
        String statusCode,
        String statusMessage,
        String cufe,
        String xmlHash,
        String timestamp,
        List<ErrorDian> errores) {

    public static RespuestaDian error(String mensaje) {
        return new RespuestaDian(null, "ERROR", mensaje, null, null, null,
                List.of(new ErrorDian("ERROR", mensaje)));
    }

    public boolean aprobada() {
        return "00".equals(statusCode) && (errores == null || errores.isEmpty());
    }

    public List<ErrorDian> errores() {
        return errores == null ? List.of() : List.copyOf(errores);
    }
}
