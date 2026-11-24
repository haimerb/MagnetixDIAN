package com.magnetixdian.interfaces.dto;

public record DatosDeclaranteDto(
        String nit,
        String digitoVerificacion,
        String razonSocial,
        String tipoDocumento,
        String municipio,
        String pais,
        String regimen,
        String regimenNiif) {
}
