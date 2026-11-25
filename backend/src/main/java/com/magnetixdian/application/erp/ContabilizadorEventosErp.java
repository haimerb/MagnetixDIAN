package com.magnetixdian.application.erp;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.domain.contabilidad.PlanCuentas;
import com.magnetixdian.domain.erp.evento.PedidoAprobadoEvent;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudLineaAsientoDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class ContabilizadorEventosErp {

    public static final String ORIGEN_PEDIDO = "ERP_PEDIDO";
    public static final String TIPO_EVENTO_VENTA = "VENTA";

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ServicioAsientos servicioAsientos;

    public ContabilizadorEventosErp(ServicioAsientos servicioAsientos) {
        this.servicioAsientos = servicioAsientos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void contabilizar(PedidoAprobadoEvent evento) {
        if (evento == null || evento.empresaId() == null || evento.pedidoId() == null) {
            return;
        }
        LocalDate fecha = evento.fecha() != null ? evento.fecha()
                : evento.ocurridoEn() != null ? evento.ocurridoEn().toLocalDate() : LocalDate.now();
        BigDecimal total = escala(evento.total());
        BigDecimal impuestos = escala(evento.impuestos());
        BigDecimal subtotal = escala(total.subtract(impuestos));
        String cliente = evento.clienteNombre() == null || evento.clienteNombre().isBlank()
                ? "Cliente " + evento.clienteId()
                : evento.clienteNombre();
        String numero = evento.numero() == null || evento.numero().isBlank()
                ? String.valueOf(evento.pedidoId())
                : evento.numero();
        String descripcion = "Contabilización pedido " + numero + " - " + cliente;
        List<SolicitudLineaAsientoDto> lineas = List.of(
                new SolicitudLineaAsientoDto(PlanCuentas.CLIENTES, cliente, total, CERO,
                        null, null, CERO, null),
                new SolicitudLineaAsientoDto(PlanCuentas.VENTAS, "Venta pedido " + numero,
                        CERO, subtotal, null, null, subtotal, null),
                new SolicitudLineaAsientoDto(PlanCuentas.IVA_GENERADO, "IVA pedido " + numero,
                        CERO, impuestos, null, null, subtotal, "IVA"));
        servicioAsientos.crear(evento.empresaId(), new SolicitudAsientoDto(
                fecha, descripcion, TIPO_EVENTO_VENTA, lineas, ORIGEN_PEDIDO, evento.pedidoId()));
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
