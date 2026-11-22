package com.magnetixdian.application;

import com.magnetixdian.domain.notificacion.NotificadorWhatsApp;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppRequest;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ServicioEntregaFactura {

    public static final String ACCION_ENTREGA = "FACTURA_ENTREGADA_WHATSAPP";

    private final FacturaRepository facturaRepository;
    private final ServicioBranding branding;
    private final NotificadorWhatsApp notificador;
    private final AuditoriaService auditoria;

    public ServicioEntregaFactura(FacturaRepository facturaRepository, ServicioBranding branding,
                                  NotificadorWhatsApp notificador, AuditoriaService auditoria) {
        this.facturaRepository = facturaRepository;
        this.branding = branding;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public EntregaWhatsAppResponse entregar(Long empresaId, Long facturaId, EntregaWhatsAppRequest request) {
        return entregar(empresaId, facturaId, request, AuditoriaService.usuarioActual());
    }

    @Transactional(readOnly = true)
    public EntregaWhatsAppResponse entregar(Long empresaId, Long facturaId, EntregaWhatsAppRequest request,
                                            String usuario) {
        FacturaJpa factura = facturaRepository.findById(facturaId)
                .filter(f -> f.getEmpresa() != null && empresaId != null
                        && empresaId.equals(f.getEmpresa().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        BrandingDto marca = branding.obtener(empresaId);
        String destino = request != null && request.numero() != null && !request.numero().isBlank()
                ? request.numero() : marca.whatsappNumber();
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("Número de WhatsApp no configurado para la empresa.");
        }
        if (!notificador.numeroValido(destino)) {
            throw new IllegalArgumentException("Número de WhatsApp inválido.");
        }

        String plantilla = request != null && request.mensaje() != null && !request.mensaje().isBlank()
                ? request.mensaje() : marca.whatsappTemplate();
        String mensaje = aplicarPlantilla(plantilla, factura, marca);
        String numero = notificador.normalizarNumero(destino);
        String enlace = notificador.construirEnlace(numero, mensaje);

        auditoria.registrarEnEmpresa(empresaId, usuario, ACCION_ENTREGA, "factura", facturaId,
                "numero=" + numero + " · enlace=" + enlace);
        return new EntregaWhatsAppResponse(empresaId, facturaId, numero, enlace, mensaje, true);
    }

    private String aplicarPlantilla(String plantilla, FacturaJpa factura, BrandingDto marca) {
        String texto = plantilla == null || plantilla.isBlank() ? ServicioBranding.PLANTILLA_WHATSAPP : plantilla;
        String numeroFactura = factura.getNumeroCompleto() == null ? "" : factura.getNumeroCompleto();
        BigDecimal total = factura.getTotalPagar() == null ? BigDecimal.ZERO : factura.getTotalPagar();
        String totalTexto = total.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String empresa = marca.displayName() == null ? "" : marca.displayName();

        texto = texto.replace("{{numero}}", "{numero}")
                .replace("{{total}}", "{total}")
                .replace("{{empresa}}", "{empresa}")
                .replace("{numero}", numeroFactura)
                .replace("{factura}", numeroFactura)
                .replace("{total}", totalTexto)
                .replace("{empresa}", empresa);
        return texto;
    }
}
