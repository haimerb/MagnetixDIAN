package com.magnetixdian.interfaces.dto;

import java.util.List;

/**
 * Resumen de una importación masiva de facturas.
 */
public record ResultadoImportacionDto(
        int facturasEmitidas,
        int facturasRechazadas,
        int lineasImportadas,
        List<String> errores) {
}
