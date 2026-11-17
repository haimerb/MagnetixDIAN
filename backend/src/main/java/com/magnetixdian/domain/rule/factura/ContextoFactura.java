package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;

/**
 * Contexto de validación de un documento electrónico: datos del documento y
 * configuración DIAN de la empresa emisora.
 */
public record ContextoFactura(FacturaDatos factura, EmpresaFeConfigJpa config) {
}
