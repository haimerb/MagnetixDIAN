package com.magnetixdian.application;

import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.ConsultorPagoRemoto;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PagoJpa;
import com.magnetixdian.infrastructure.persistence.PagoRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.interfaces.dto.CheckoutRequest;
import com.magnetixdian.interfaces.dto.PagoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquesta el pago de una suscripción: inicia el checkout en la pasarela
 * elegida y, al confirmarse el pago, activa el plan.
 */
@Service
public class ServicioPagos {

    private final PagoRepository pagoRepo;
    private final PlanRepository planRepo;
    private final EmpresaRepository empresaRepo;
    private final PasarelasPago pasarelas;
    private final ServicioSuscripciones suscripciones;
    private final AuditoriaService auditoria;
    private final String frontendUrl;

    public ServicioPagos(PagoRepository pagoRepo,
                         PlanRepository planRepo,
                         EmpresaRepository empresaRepo,
                         PasarelasPago pasarelas,
                         ServicioSuscripciones suscripciones,
                         AuditoriaService auditoria,
                         @Value("${magnetixdian.app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.pagoRepo = pagoRepo;
        this.planRepo = planRepo;
        this.empresaRepo = empresaRepo;
        this.pasarelas = pasarelas;
        this.suscripciones = suscripciones;
        this.auditoria = auditoria;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(readOnly = true)
    public List<PagoDto> listar(Long empresaId) {
        return pagoRepo.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public PagoDto obtener(Long empresaId, String referencia) {
        return aDto(buscar(empresaId, referencia));
    }

    @Transactional
    public PagoDto reconciliar(Long empresaId, String referencia) {
        PagoJpa pago = buscar(empresaId, referencia);
        if (!"PENDIENTE".equalsIgnoreCase(pago.getEstado())) {
            return aDto(pago);
        }
        ConsultorPagoRemoto consultor = pasarelas.consultor(pago.getProveedor());
        if (!consultor.configurado()) {
            throw new IllegalStateException("La pasarela " + pago.getProveedor()
                    + " no está configurada para consultar pagos.");
        }
        EventoPago evento = consultor.consultar(referencia)
                .orElseThrow(() -> new IllegalStateException("La pasarela aún no registra este pago."));
        if (evento.monto() == null || evento.monto().compareTo(pago.getMonto()) != 0) {
            throw new IllegalStateException("El monto reportado por la pasarela no coincide con el pago.");
        }
        if (evento.aprobado()) {
            aplicarPlan(pago);
        } else if ("RECHAZADO".equalsIgnoreCase(evento.estado())) {
            pago.setEstado("RECHAZADO");
        } else {
            return aDto(pago);
        }
        pago.setUpdatedAt(OffsetDateTime.now());
        pagoRepo.save(pago);
        auditoria.registrar(AuditoriaService.usuarioActual(), "PAGO_RECONCILIADO", "pago", pago.getId(),
                pago.getProveedor() + " " + evento.estado());
        return aDto(pago);
    }

    private PagoJpa buscar(Long empresaId, String referencia) {
        return pagoRepo.findByReferencia(referencia)
                .filter(p -> p.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
    }

    @Transactional
    public PagoDto iniciar(Long empresaId, String planCodigo, String proveedor) {
        return iniciar(empresaId, new CheckoutRequest(planCodigo, proveedor));
    }

    @Transactional
    public PagoDto iniciar(Long empresaId, CheckoutRequest request) {
        String planCodigo = request.planCodigo();
        var empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        PlanJpa plan = planRepo.findByCodigoIgnoreCase(planCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + planCodigo));
        PasarelaPago pasarela = pasarelas.obtener(request.proveedor());
        if (!pasarela.configurada()) {
            throw new IllegalStateException("La pasarela " + pasarela.proveedor() + " no está configurada.");
        }
        MetodoPago metodo = request.metodo() == null ? MetodoPago.CARD : request.metodo();
        validarMetodo(pasarela.proveedor(), metodo, request.aceptaTerminos());

        PagoJpa pago = new PagoJpa();
        pago.setEmpresa(empresa);
        pago.setPlanCodigo(plan.getCodigo());
        pago.setProveedor(pasarela.proveedor());
        pago.setMetodo(metodo.name());
        pago.setReferencia(pasarela.proveedor() + "-" + UUID.randomUUID());
        pago.setMonto(plan.getPrecioMensual());
        pago.setMoneda(plan.getMoneda());
        pago.setEstado("PENDIENTE");
        pagoRepo.save(pago);

        SolicitudPago solicitud = new SolicitudPago(empresaId, pago.getId(), pago.getReferencia(),
                plan.getCodigo(), plan.getPrecioMensual(), plan.getMoneda(),
                "Plan " + plan.getNombre(), frontendUrl + "/suscripcion", metodo, request.celular(),
                request.financialInstitutionId(), request.paymentType(), request.documentType(),
                request.documentNumber(), request.payerName(), request.payerEmail(), request.payerPhone(),
                request.descripcion());
        CheckoutPago checkout = pasarela.crearCheckout(solicitud);
        pago.setUrlCheckout(checkout.url());
        if (checkout.confirmadoInmediato()) {
            aplicarPlan(pago);
        }
        pago.setUpdatedAt(OffsetDateTime.now());
        pagoRepo.save(pago);

        auditoria.registrar(AuditoriaService.usuarioActual(), "PAGO_INICIADO", "pago", pago.getId(),
                pago.getProveedor() + " " + pago.getPlanCodigo());
        return aDto(pago);
    }

    @Transactional
    public void procesarWebhook(String proveedor, String payload, Map<String, String> headers) {
        PasarelaPago pasarela = pasarelas.obtener(proveedor);
        EventoPago evento = pasarela.procesarWebhook(payload, headers).orElse(null);
        if (evento == null) {
            return;
        }
        PagoJpa pago = pagoRepo.findByReferencia(evento.referencia()).orElse(null);
        if (pago == null) {
            return;
        }
        if (evento.monto() == null || evento.monto().compareTo(pago.getMonto()) != 0) {
            auditoria.registrar(AuditoriaService.usuarioActual(), "PAGO_RECHAZADO_MONTO", "pago", pago.getId(),
                    evento.monto() + " != " + pago.getMonto());
            return;
        }
        if (evento.aprobado() && !"APROBADO".equals(pago.getEstado())) {
            aplicarPlan(pago);
        } else if ("RECHAZADO".equals(evento.estado())) {
            pago.setEstado("RECHAZADO");
        }
        pago.setUpdatedAt(OffsetDateTime.now());
        pagoRepo.save(pago);
        auditoria.registrar(AuditoriaService.usuarioActual(), "PAGO_WEBHOOK", "pago", pago.getId(),
                proveedor + " " + evento.estado());
    }

    private static void validarMetodo(String proveedor, MetodoPago metodo, boolean aceptaTerminos) {
        if ((metodo == MetodoPago.BREB_QR || metodo == MetodoPago.DAVIPLATA)
                && !ProveedorPago.WOMPI.name().equalsIgnoreCase(proveedor)) {
            throw new IllegalArgumentException("El método " + metodo + " requiere la pasarela WOMPI.");
        }
        if ((metodo == MetodoPago.BREB_QR || metodo == MetodoPago.DAVIPLATA) && !aceptaTerminos) {
            throw new IllegalArgumentException(
                    "Debe aceptar los términos y el tratamiento de datos personales de Wompi.");
        }
    }

    private void aplicarPlan(PagoJpa pago) {
        suscripciones.cambiarPlan(pago.getEmpresa().getId(), pago.getPlanCodigo(), pago.getProveedor());
        pago.setEstado("APROBADO");
    }

    private PagoDto aDto(PagoJpa p) {
        return new PagoDto(p.getId(), p.getReferencia(), p.getProveedor(), p.getPlanCodigo(),
                p.getMonto(), p.getMoneda(), p.getEstado(), p.getUrlCheckout());
    }
}
