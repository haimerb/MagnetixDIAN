package com.magnetixdian.application.pago;

import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.application.PasarelasPago;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ServicioCheckoutBilling {

    private final BillingCheckoutRepository checkoutRepo;
    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final PlanRepository planRepo;
    private final PasarelasPago pasarelas;
    private final String frontendUrl;

    public ServicioCheckoutBilling(BillingCheckoutRepository checkoutRepo,
                                   EmpresaRepository empresaRepo,
                                   UsuarioRepository usuarioRepo,
                                   PlanRepository planRepo,
                                   PasarelasPago pasarelas,
                                   @Value("${magnetixdian.app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.checkoutRepo = checkoutRepo;
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.planRepo = planRepo;
        this.pasarelas = pasarelas;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public CheckoutResponseDto iniciar(CheckoutRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de checkout es obligatoria.");
        }
        if (request.planCode() == null || request.planCode().isBlank()) {
            throw new IllegalArgumentException("Debe indicar el plan.");
        }
        if (request.correo() == null || request.correo().isBlank()) {
            throw new IllegalArgumentException("Debe indicar el correo del pagador.");
        }
        ProveedorPago proveedor = ProveedorPago.desde(request.proveedor());
        if (proveedor == ProveedorPago.SIMULADO) {
            throw new IllegalArgumentException(
                    "El checkout público requiere WOMPI o MERCADOPAGO; SIMULADO no genera webhook firmado.");
        }
        MetodoPago metodo = request.metodo() == null ? MetodoPago.CARD : request.metodo();
        validarMetodo(proveedor, metodo, request.aceptaTerminos());
        EmpresaJpa empresa = resolverEmpresa(request.empresaId(), request.correo());
        String planCodigo = request.planCode().trim();
        PlanJpa plan = planRepo.findByCodigoIgnoreCase(planCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + planCodigo));
        PasarelaPago pasarela = pasarelas.obtener(proveedor.name());
        if (!pasarela.configurada()) {
            throw new IllegalStateException(
                    "La pasarela " + proveedor + " no está configurada. Configure sus credenciales para cobrar.");
        }
        String referencia = proveedor.name() + "-" + UUID.randomUUID();
        BillingCheckoutJpa checkout = new BillingCheckoutJpa();
        checkout.setReferencia(referencia);
        checkout.setEmpresaId(empresa.getId());
        checkout.setPlanCode(plan.getCodigo());
        checkout.setCorreo(request.correo().trim());
        checkout.setProveedor(proveedor.name());
        checkout.setMonto(plan.getPrecioMensual());
        checkout.setMoneda(plan.getMoneda() == null ? "COP" : plan.getMoneda().toUpperCase(Locale.ROOT));
        checkout.setEstado("PENDIENTE");
        checkoutRepo.save(checkout);

        String descripcion = request.descripcion() == null || request.descripcion().isBlank()
                ? "Plan " + plan.getNombre() : request.descripcion().trim();
        String correoPagador = request.payerEmail() == null || request.payerEmail().isBlank()
                ? request.correo().trim() : request.payerEmail().trim();
        SolicitudPago solicitud = new SolicitudPago(empresa.getId(), checkout.getId(), referencia,
                plan.getCodigo(), plan.getPrecioMensual(), checkout.getMoneda(), "Plan " + plan.getNombre(),
                frontendUrl + "/suscripcion", metodo, request.celular(), request.financialInstitutionId(),
                request.paymentType(), request.documentType(), request.documentNumber(), request.payerName(),
                correoPagador, request.payerPhone(), descripcion);
        CheckoutPago resultado = pasarela.crearCheckout(solicitud);
        checkout.setCheckoutUrl(resultado.url());
        checkout.setUpdatedAt(OffsetDateTime.now());
        checkoutRepo.save(checkout);
        return new CheckoutResponseDto(resultado.url(), referencia, checkout.getMonto(), checkout.getMoneda(),
                "PENDIENTE", metodo.name(), resultado.tipo(), resultado.qrImage(), resultado.qrId(),
                resultado.transaccionId());
    }

    private static void validarMetodo(ProveedorPago proveedor, MetodoPago metodo, boolean aceptaTerminos) {
        if ((metodo == MetodoPago.BREB_QR || metodo == MetodoPago.DAVIPLATA)
                && proveedor != ProveedorPago.WOMPI) {
            throw new IllegalArgumentException("El método " + metodo + " requiere la pasarela WOMPI.");
        }
        if ((metodo == MetodoPago.BREB_QR || metodo == MetodoPago.DAVIPLATA) && !aceptaTerminos) {
            throw new IllegalArgumentException(
                    "Debe aceptar los términos y el tratamiento de datos personales de Wompi.");
        }
    }

    private EmpresaJpa resolverEmpresa(Long empresaId, String correo) {
        if (empresaId != null) {
            return empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        }
        return usuarioRepo.findByEmailIgnoreCase(correo.trim())
                .map(UsuarioJpa::getEmpresa)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró una empresa asociada al correo " + correo));
    }
}
