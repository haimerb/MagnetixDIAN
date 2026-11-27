package com.magnetixdian.application.pago;

import com.magnetixdian.application.PasarelasPago;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.PagoAprobadoEvent;
import com.magnetixdian.domain.pago.PasarelaPago;
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
import java.util.Optional;

@Service
public class ServicioWebhookBilling {

    private static final int LONGITUD_MENSAJE = 300;
    private static final int LONGITUD_ESTADO = 20;

    private final ValidadorFirmaWebhook validador;
    private final PasarelasPago pasarelas;
    private final LectorEventoPagoWebhook lector;
    private final BillingCheckoutRepository checkoutRepo;
    private final BillingWebhookEventRepository eventoRepo;
    private final RegistradorEventoWebhook registrador;
    private final EmpresaRepository empresaRepo;
    private final ServicioEntitlement entregas;
    private final ServicioComprobante comprobantes;
    private final ApplicationEventPublisher publicador;

    public ServicioWebhookBilling(ValidadorFirmaWebhook validador,
                                  PasarelasPago pasarelas,
                                  LectorEventoPagoWebhook lector,
                                  BillingCheckoutRepository checkoutRepo,
                                  BillingWebhookEventRepository eventoRepo,
                                  RegistradorEventoWebhook registrador,
                                  EmpresaRepository empresaRepo,
                                  ServicioEntitlement entregas,
                                  ServicioComprobante comprobantes,
                                  ApplicationEventPublisher publicador) {
        this.validador = validador;
        this.pasarelas = pasarelas;
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
        PagoWebhookEventDto evento = resolverEventoRemoto(proveedorEnum, payload, headers,
                lector.leer(proveedorEnum, payload));
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

    private PagoWebhookEventDto resolverEventoRemoto(ProveedorPago proveedor, String payload,
                                                     Map<String, String> headers,
                                                     PagoWebhookEventDto leido) {
        if (!requiereResolucion(leido) || !pasarelas.existe(proveedor.name())) {
            return leido;
        }
        Optional<EventoPago> resuelto;
        try {
            resuelto = pasarelas.obtener(proveedor.name()).procesarWebhook(payload, headers);
        } catch (RuntimeException e) {
            return leido;
        }
        if (resuelto.isEmpty()) {
            return leido;
        }
        EventoPago evento = resuelto.get();
        return new PagoWebhookEventDto(leido.proveedor(), eventoId(leido, headers),
                valor(evento.referencia(), leido.referencia()), estado(evento, leido.estado()),
                evento.monto() == null ? leido.monto() : evento.monto(), leido.moneda());
    }

    private static boolean requiereResolucion(PagoWebhookEventDto evento) {
        return esVacio(evento.referencia()) || esVacio(evento.estado())
                || "PENDIENTE".equalsIgnoreCase(evento.estado()) || evento.monto() == null;
    }

    private static String eventoId(PagoWebhookEventDto leido, Map<String, String> headers) {
        return esVacio(leido.eventId()) ? cabecera(headers, "data.id") : leido.eventId();
    }

    private static String valor(String resuelto, String leido) {
        return esVacio(resuelto) ? leido : resuelto;
    }

    private static String estado(EventoPago evento, String leido) {
        if (evento.aprobado()) {
            return "APPROVED";
        }
        if ("RECHAZADO".equalsIgnoreCase(evento.estado())) {
            return "RECHAZADO";
        }
        return esVacio(leido) ? "PENDIENTE" : leido;
    }

    private static String cabecera(Map<String, String> headers, String nombre) {
        if (headers == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(nombre))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
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
