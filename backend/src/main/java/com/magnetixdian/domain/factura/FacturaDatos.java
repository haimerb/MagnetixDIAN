package com.magnetixdian.domain.factura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Agregado con toda la información necesaria para calcular el CUFE/CUDE y
 * generar el documento UBL 2.1. Es independiente de la capa de persistencia.
 */
public record FacturaDatos(
        TipoDocumento tipo,
        String numeroCompleto,
        String prefijo,
        long numero,
        LocalDate fechaEmision,
        LocalTime horaEmision,
        LocalDate fechaVencimiento,
        int ambiente,
        String moneda,
        String formaPago,
        String medioPago,
        String tipoOperacion,
        DatosEmisor emisor,
        DatosAdquirente adquirente,
        List<LineaFactura> lineas,
        List<ImpuestoFactura> impuestos,
        BigDecimal subtotal,
        BigDecimal totalImpuestos,
        BigDecimal totalRetenciones,
        BigDecimal total,
        BigDecimal totalPagar,
        String observaciones,
        String cufeReferencia,
        String numeroReferencia,
        // Parámetros de control (DianExtensions / CUFE)
        String softwareId,
        String softwarePin,
        String softwareSecurityCode,
        String testSetId,
        String resolucionNumero,
        LocalDate resolucionFecha,
        LocalDate rangoVigenciaDesde,
        LocalDate rangoVigenciaHasta,
        long rangoDesde,
        long rangoHasta) {

    public String nitEmisor() {
        return emisor.numeroIdentificacion();
    }

    public String nitAdquirente() {
        return adquirente.numeroDocumento();
    }

    public BigDecimal valorSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }
}
