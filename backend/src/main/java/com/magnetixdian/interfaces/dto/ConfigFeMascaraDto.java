package com.magnetixdian.interfaces.dto;

public record ConfigFeMascaraDto(
        Integer ambiente,
        String canal,
        String certificadoNombre,
        String certificadoMascara,
        boolean tieneCertificado,
        boolean tienePacKey,
        String versionWsdl) {
}
