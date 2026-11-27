package com.magnetixdian.application.erp;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.application.ServicioAsientosPendientes;
import com.magnetixdian.domain.contabilidad.PlanCuentas;
import com.magnetixdian.domain.erp.evento.CobroClienteEvent;
import com.magnetixdian.domain.erp.evento.CompraRecibidaEvent;
import com.magnetixdian.domain.erp.evento.PagoProveedorEvent;
import com.magnetixdian.interfaces.dto.FacturaEmitidaEvent;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudLineaAsientoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ContabilizadorEventosErp {

    public static final String ORIGEN_FACTURA = "FACTURA";
    public static final String ORIGEN_COMPRA = "COMPRA";
    public static final String ORIGEN_PAGO = "PAGO";
    public static final String ORIGEN_COBRO = "COBRO";

    public static final String TIPO_VENTA = "VENTA";
    public static final String TIPO_COMPRA = "COMPRA";
    public static final String TIPO_PAGO = "PAGO";
    public static final String TIPO_COBRO = "COBRO";

    private static final Set<String> ESTADOS_RECHAZADOS = Set.of(
            "RECHAZADO", "RECHAZADA", "ANULADO", "ANULADA");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Logger log = LoggerFactory.getLogger(ContabilizadorEventosErp.class);

    private final ServicioAsientos servicioAsientos;
    private final ServicioAsientosPendientes pendientes;

    public ContabilizadorEventosErp(ServicioAsientos servicioAsientos) {
        this(servicioAsientos, null);
    }

    @Autowired
    public ContabilizadorEventosErp(ServicioAsientos servicioAsientos, ServicioAsientosPendientes pendientes) {
        this.servicioAsientos = servicioAsientos;
        this.pendientes = pendientes;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void contabilizarFactura(FacturaEmitidaEvent evento) {
        if (evento == null || evento.empresaId() == null || evento.facturaId() == null
                || rechazado(evento.estado())) {
            return;
        }
        BigDecimal subtotal = escala(evento.subtotal());
        BigDecimal impuestos = escala(evento.impuestos());
        BigDecimal retenciones = escala(evento.retenciones()).max(CERO);
        if (subtotal.add(impuestos).signum() == 0) {
            subtotal = escala(evento.total());
            impuestos = CERO;
        }
        BigDecimal neto = subtotal.add(impuestos).subtract(retenciones);
        if (neto.signum() <= 0) {
            return;
        }
        String numero = texto(evento.numeroCompleto(), String.valueOf(evento.facturaId()));
        String tercero = texto(evento.terceroDocumento(), "Tercero " + evento.facturaId());
        LocalDate fecha = evento.fechaEmision() != null ? evento.fechaEmision() : LocalDate.now();
        List<SolicitudLineaAsientoDto> lineas = new ArrayList<>();
        lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.CLIENTES,
                "Factura " + numero + " - " + tercero, neto, CERO, null, evento.terceroId(), neto, "IVA"));
        lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.VENTAS,
                "Venta factura " + numero, CERO, subtotal, null, null, subtotal, null));
        if (impuestos.signum() > 0) {
            lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.IVA_GENERADO,
                    "IVA factura " + numero, CERO, impuestos, null, null, subtotal, "IVA"));
        }
        if (retenciones.signum() > 0) {
            lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.RETENCIONES,
                    "Retenciones factura " + numero, retenciones, CERO, null, evento.terceroId(), subtotal, "RETENCION"));
        }
        crearAsiento(evento.empresaId(), fecha, "Contabilización factura " + numero,
                TIPO_VENTA, ORIGEN_FACTURA, evento.facturaId(), lineas);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void contabilizarCompra(CompraRecibidaEvent evento) {
        if (evento == null || evento.empresaId() == null || evento.compraId() == null) {
            return;
        }
        BigDecimal subtotal = escala(evento.subtotal());
        BigDecimal impuestos = escala(evento.impuestos());
        BigDecimal retenciones = escala(evento.retenciones()).max(CERO);
        if (subtotal.add(impuestos).signum() == 0) {
            subtotal = escala(evento.total());
            impuestos = CERO;
        }
        BigDecimal bruto = subtotal.add(impuestos);
        BigDecimal neto = bruto.subtract(retenciones);
        if (bruto.signum() <= 0 || neto.signum() <= 0) {
            return;
        }
        String numero = texto(evento.numeroCompra(), String.valueOf(evento.compraId()));
        String proveedor = texto(evento.proveedorNombre(), "Proveedor " + evento.compraId());
        LocalDate fecha = evento.fecha() != null ? evento.fecha() : LocalDate.now();
        List<SolicitudLineaAsientoDto> lineas = new ArrayList<>();
        lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.COMPRAS,
                "Compra " + numero + " - " + proveedor, subtotal, CERO, null, null, subtotal, null));
        if (impuestos.signum() > 0) {
            lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.IVA_DESCONTABLE,
                    "IVA descontable compra " + numero, impuestos, CERO, null, null, subtotal, "IVA"));
        }
        lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.PROVEEDORES,
                "Proveedor compra " + numero, CERO, neto, null, null, bruto, null));
        if (retenciones.signum() > 0) {
            lineas.add(new SolicitudLineaAsientoDto(PlanCuentas.RETENCION_FUENTE,
                    "Retención compra " + numero, CERO, retenciones, null, null, bruto, "RETENCION"));
        }
        crearAsiento(evento.empresaId(), fecha, "Contabilización compra " + numero,
                TIPO_COMPRA, ORIGEN_COMPRA, evento.compraId(), lineas);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void contabilizarPagoProveedor(PagoProveedorEvent evento) {
        if (evento == null || evento.empresaId() == null || evento.pagoId() == null) {
            return;
        }
        BigDecimal valor = escala(evento.valor());
        if (valor.signum() <= 0) {
            return;
        }
        String proveedor = texto(evento.proveedorNombre(), "Proveedor " + evento.proveedorId());
        LocalDate fecha = evento.fecha() != null ? evento.fecha() : LocalDate.now();
        List<SolicitudLineaAsientoDto> lineas = List.of(
                new SolicitudLineaAsientoDto(PlanCuentas.PROVEEDORES,
                        "Pago proveedor " + proveedor, valor, CERO, null, null, valor, null),
                new SolicitudLineaAsientoDto(PlanCuentas.CAJA,
                        "Pago proveedor " + proveedor, CERO, valor, null, null, valor, null));
        crearAsiento(evento.empresaId(), fecha, "Contabilización pago proveedor",
                TIPO_PAGO, ORIGEN_PAGO, evento.pagoId(), lineas);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void contabilizarCobroCliente(CobroClienteEvent evento) {
        if (evento == null || evento.empresaId() == null || evento.cobroId() == null) {
            return;
        }
        BigDecimal valor = escala(evento.valor());
        if (valor.signum() <= 0) {
            return;
        }
        String cliente = texto(evento.clienteNombre(), "Cliente " + evento.clienteId());
        LocalDate fecha = evento.fecha() != null ? evento.fecha() : LocalDate.now();
        List<SolicitudLineaAsientoDto> lineas = List.of(
                new SolicitudLineaAsientoDto(PlanCuentas.CAJA,
                        "Cobro cliente " + cliente, valor, CERO, null, null, valor, null),
                new SolicitudLineaAsientoDto(PlanCuentas.CLIENTES,
                        "Cobro cliente " + cliente, CERO, valor, null, null, valor, null));
        crearAsiento(evento.empresaId(), fecha, "Contabilización cobro cliente",
                TIPO_COBRO, ORIGEN_COBRO, evento.cobroId(), lineas);
    }

    private void crearAsiento(Long empresaId, LocalDate fecha, String descripcion, String tipoEvento,
                              String origen, Long origenId, List<SolicitudLineaAsientoDto> lineas) {
        SolicitudAsientoDto solicitud = new SolicitudAsientoDto(
                fecha, descripcion, tipoEvento, lineas, origen, origenId);
        try {
            servicioAsientos.crear(empresaId, solicitud);
        } catch (RuntimeException ex) {
            log.warn("No se pudo contabilizar {} {}. Se programa reintento: {}",
                    origen, origenId, ex.getMessage());
            if (pendientes != null) {
                pendientes.registrar(empresaId, solicitud, ex.getMessage());
            }
        }
    }

    private static boolean rechazado(String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        return ESTADOS_RECHAZADOS.contains(estado.toUpperCase(Locale.ROOT));
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String texto(String texto, String respaldo) {
        return texto == null || texto.isBlank() ? respaldo : texto;
    }
}
