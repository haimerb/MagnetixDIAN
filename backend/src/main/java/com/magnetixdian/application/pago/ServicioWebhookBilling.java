package com.magnetixdian.application.pago;

import com.magnetixdian.domain.pago.PagoAprobadoEvent;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventJpa;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventRepository;
import com.magnetixdian.infrastructure.pago.LectorEventoPagoWebhook;
import com.magnetixdian.interfaces.dto.PagoWebhookEventDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class ServicioWebhookBilling {

    private static final int LONGITUD_MENSAJE = 300;
    private static final int LONGITUD_ESTADO = 20;

    private final ValidadorFirmaWebhook validador;
    private final LectorEventoPagoWebhook lector;
    private final BillingCheckoutRepository checkoutRepo;
    private final BillingWebhookEventRepository eventoRepo;
    private final RegistradorEventoWebhook registrador;
    private final EmpresaRepository empresaRepo;
    private final ServicioEntitlement entregas;
    private final ServicioComprobante comprobantes;
    private final ApplicationEventPublisher publicador;

    public ServicioWebhookBilling(ValidadorFirmaWebhook validador,
                                  LectorEventoPagoWebhook lector,
                                  BillingCheckoutRepository checkoutRepo,
                                  BillingWebhookEventRepository eventoRepo,
                                  RegistradorEventoWebhook registrador,
                                  EmpresaRepository empresaRepo,
                                  ServicioEntitlement entregas,
                                  ServicioComprobante comprobantes,
                                  ApplicationEventPublisher publicador) {
        this.validador = validador;
        this.lector = lector;
        this.checkoutRepo = checkoutRepo;
        this.eventoRepo = eventoRepo;
        this.registrador = registrador;
        this.empresaRepo = empresaRepo;
        this.entregas = entregas;
        this.comprobantes = comprobantes;
        this.publicador = publicador;
    }

    @Transactional
    public WebhookResultDto procesar(String proveedor, String payload, Map<String, String> headers) {
        ProveedorPago proveedorEnum = ProveedorPago.desde(proveedor);
        validador.verificar(proveedorEnum, payload, headers);
        PagoWebhookEventDto evento = lector.leer(proveedorEnum, payload);
        if (evento.eventId() == null || evento.eventId().isBlank()) {
            throw new IllegalArgumentException("El evento del webhook no tiene identificador.");
        }
        if (evento.referencia() == null || evento.referencia().isBlank()) {
            throw new IllegalArgumentException("El evento del webhook no tiene referencia.");
        }
        BillingCheckoutJpa checkout = checkoutRepo.findByReferencia(evento.referencia())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Referencia de pago no encontrada: " + evento.referencia()));
        BigDecimal esperado = checkout.getMonto();
        BigDecimal monto = evento.monto() == null ? esperado : evento.monto();
        if (monto.compareTo(esperado) < 0) {
            throw new IllegalArgumentException("El monto notificado es menor al monto esperado.");
        }
        String estadoEvento = evento.estado() == null ? "PENDIENTE" : evento.estado();
        boolean aprobado = "APPROVED".equalsIgnoreCase(estadoEvento) || "PAID".equalsIgnoreCase(estadoEvento);

        BillingWebhookEventJpa registrado = nuevoEvento(proveedorEnum, evento, estadoEvento, monto, headers, payload);
        try {
            registrador.registrar(registrado);
        } catch (DataIntegrityViolationException duplicado) {
            return eventoRepo.findByProveedorAndEventId(proveedorEnum.name(), evento.eventId())
                    .map(eventoPrevio -> aResultado(eventoPrevio, true))
                    .orElseThrow(() -> new IllegalStateException(
                            "El evento duplicado no pudo ser consultado.", duplicado));
        }

        String resultado;
        String numeroComprobante = null;
        String mensaje;
        if (aprobado) {
            if (checkout.getEmpresaId() == null) {
                throw new IllegalArgumentException("El checkout aprobado no tiene empresa asociada.");
            }
            EmpresaJpa empresa = empresaRepo.findById(checkout.getEmpresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa del pago no encontrada."));
            entregas.activar(checkout.getEmpresaId(), checkout.getPlanCode(), proveedorEnum.name(),
                    checkout.getReferencia());
            ComprobantePagoDto comprobante = comprobantes.generar(checkout.getReferencia(), empresa.getId(),
                    empresa.getNit(), checkout.getPlanCode(), monto, checkout.getMoneda());
            numeroComprobante = comprobante.numero();
            publicador.publishEvent(new PagoAprobadoEvent(empresa.getId(), checkout.getPlanCode(),
                    checkout.getReferencia(), evento.eventId()));
            resultado = "APROBADO";
            mensaje = "Pago aprobado, suscripción activada y comprobante generado.";
        } else {
            resultado = "RECHAZADO".equalsIgnoreCase(estadoEvento) ? "RECHAZADO" : "PENDIENTE";
            mensaje = "La pasarela no aprobó el pago.";
        }
        checkout.setEstado(resultado);
        checkout.setUpdatedAt(OffsetDateTime.now());
        checkoutRepo.save(checkout);

        registrado.setResultado(resultado);
        registrado.setComprobanteNumero(numeroComprobante);
        registrado.setMensaje(limitar(mensaje, LONGITUD_MENSAJE));
        eventoRepo.save(registrado);
        return new WebhookResultDto(resultado, evento.referencia(), numeroComprobante, false, mensaje);
    }

    private BillingWebhookEventJpa nuevoEvento(ProveedorPago proveedor, PagoWebhookEventDto evento,
                                               String estadoEvento, BigDecimal monto,
                                               Map<String, String> headers, String payload) {
        BillingWebhookEventJpa registrado = new BillingWebhookEventJpa();
        registrado.setProveedor(proveedor.name());
        registrado.setEventId(evento.eventId());
        registrado.setReferencia(evento.referencia());
        registrado.setEstado(limitar(estadoEvento, LONGITUD_ESTADO));
        registrado.setMonto(monto);
        registrado.setMoneda(evento.moneda());
        registrado.setResultado("PENDIENTE");
        registrado.setFirmado(validador.firmaVerificada(proveedor, payload, headers));
        registrado.setMensaje("Evento en procesamiento.");
        return registrado;
    }

    private static String limitar(String valor, int longitud) {
        return valor.length() > longitud ? valor.substring(0, longitud) : valor;
    }

    private static WebhookResultDto aResultado(BillingWebhookEventJpa evento, boolean duplicado) {
        String mensaje = duplicado ? "Evento ya procesado anteriormente." : evento.getMensaje();
        return new WebhookResultDto(evento.getResultado(), evento.getReferencia(),
                evento.getComprobanteNumero(), duplicado, mensaje);
    }
}
