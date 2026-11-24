package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoTributarioDto(
        Long empresaId,
        Integer ano,
        Long asientoId,
        Long asientoLineaId,
        Long terceroId,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String razonSocial,
        String municipio,
        String pais,
        String cuentaCodigo,
        String conceptoCodigo,
        Long conceptoId,
        BigDecimal valor,
        LocalDate fecha,
        String origenTipoDocumento,
        String origenNumeroDocumento,
        String origen) {

    public MovimientoTributarioDto(Long empresaId, Long terceroId, String concepto,
                                   BigDecimal valor, LocalDate fecha, String tipoDocumento) {
        this(empresaId, fecha == null ? null : fecha.getYear(), null, null, terceroId,
                tipoDocumento, null, null, null, null, null, null, concepto, null,
                valor, fecha, tipoDocumento, null, "DIAN");
    }

    public String concepto() {
        return conceptoCodigo;
    }

    public static MovimientoTributarioDto deAsiento(Long empresaId, Long asientoId, Long asientoLineaId,
                                                    Long terceroId, String tipoDocumento, String numeroDocumento,
                                                    String razonSocial, String cuentaCodigo, String conceptoCodigo,
                                                    BigDecimal valor, LocalDate fecha, String origenTipoDocumento,
                                                    String origenNumeroDocumento, String origen) {
        return new MovimientoTributarioDto(empresaId, fecha == null ? null : fecha.getYear(), asientoId,
                asientoLineaId, terceroId, tipoDocumento, numeroDocumento, null, razonSocial, null, "CO",
                cuentaCodigo, conceptoCodigo, null, valor, fecha, origenTipoDocumento, origenNumeroDocumento, origen);
    }
}
