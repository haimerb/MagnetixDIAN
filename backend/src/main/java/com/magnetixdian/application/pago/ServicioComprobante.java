package com.magnetixdian.application.pago;

import com.magnetixdian.infrastructure.pago.BillingComprobanteJpa;
import com.magnetixdian.infrastructure.pago.BillingComprobanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ServicioComprobante {

    private static final BigDecimal TASA_IMPUESTO = new BigDecimal("0.19");

    private final BillingComprobanteRepository repositorio;

    public ServicioComprobante(BillingComprobanteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public ComprobantePagoDto generar(String referencia, Long empresaId, String nit, String planCode,
                                      BigDecimal monto, String moneda) {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La referencia del comprobante es obligatoria.");
        }
        if (nit == null || nit.isBlank()) {
            throw new IllegalArgumentException("El NIT de la empresa es obligatorio.");
        }
        if (monto == null || monto.signum() < 0) {
            throw new IllegalArgumentException("El monto del comprobante no es válido.");
        }
        long consecutivo = repositorio.siguienteNumero();
        BillingComprobanteJpa comprobante = new BillingComprobanteJpa();
        comprobante.setNumero(String.format("CP-%06d", consecutivo));
        comprobante.setReferencia(referencia);
        comprobante.setEmpresaId(empresaId);
        comprobante.setNit(nit);
        comprobante.setPlanCode(planCode);
        comprobante.setMonto(monto.setScale(2, RoundingMode.HALF_UP));
        comprobante.setImpuestos(impuestos(monto));
        comprobante.setMoneda(moneda == null || moneda.isBlank() ? "COP" : moneda.toUpperCase());
        comprobante.setEstado("PAGADO");
        comprobante.setPdfUrl("/api/pagos/comprobantes/" + comprobante.getNumero() + "/pdf");
        return aDto(repositorio.save(comprobante));
    }

    @Transactional(readOnly = true)
    public ComprobantePagoDto obtener(String numero) {
        return aDto(repositorio.findByNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado: " + numero)));
    }

    @Transactional(readOnly = true)
    public Optional<ComprobantePagoDto> porReferencia(String referencia) {
        return repositorio.findByReferencia(referencia).map(c -> aDto(c));
    }

    private static BigDecimal impuestos(BigDecimal monto) {
        BigDecimal base = monto.divide(BigDecimal.ONE.add(TASA_IMPUESTO), 2, RoundingMode.HALF_UP);
        return monto.subtract(base).setScale(2, RoundingMode.HALF_UP);
    }

    private static ComprobantePagoDto aDto(BillingComprobanteJpa c) {
        LocalDate fecha = c.getCreatedAt() == null ? LocalDate.now() : c.getCreatedAt().toLocalDate();
        return new ComprobantePagoDto(c.getNumero(), fecha, c.getNit(), c.getPlanCode(), c.getMonto(),
                c.getImpuestos(), c.getEstado(), c.getPdfUrl());
    }
}
