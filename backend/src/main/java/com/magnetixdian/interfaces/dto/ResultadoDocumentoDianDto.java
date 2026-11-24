package com.magnetixdian.interfaces.dto;

public record ResultadoDocumentoDianDto(
        boolean exitoso,
        Long id,
        String numeroCompleto,
        String cufe,
        String estado,
        String mensaje) {

    public static ResultadoDocumentoDianDto desde(ResultadoEmisionDto resultado) {
        return new ResultadoDocumentoDianDto(
                resultado.exitoso(),
                resultado.id(),
                resultado.numeroCompleto(),
                resultado.cufe(),
                resultado.estado(),
                resultado.mensaje());
    }
}
