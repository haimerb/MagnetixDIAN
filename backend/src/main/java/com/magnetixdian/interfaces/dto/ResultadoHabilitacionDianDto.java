package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.dian.ErrorDian;
import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.domain.dian.RespuestaDian;

import java.util.List;

public record ResultadoHabilitacionDianDto(
        ModoOperacionDian modo,
        int ambiente,
        String trackId,
        String statusCode,
        String statusMessage,
        String cufe,
        String xmlHash,
        String timestamp,
        List<ErrorDian> errores,
        boolean aprobado) {

    public static ResultadoHabilitacionDianDto desde(ModoOperacionDian modo, int ambiente, RespuestaDian respuesta) {
        return new ResultadoHabilitacionDianDto(modo, ambiente, respuesta.trackId(), respuesta.statusCode(),
                respuesta.statusMessage(), respuesta.cufe(), respuesta.xmlHash(), respuesta.timestamp(),
                respuesta.errores(), respuesta.aprobada());
    }
}
