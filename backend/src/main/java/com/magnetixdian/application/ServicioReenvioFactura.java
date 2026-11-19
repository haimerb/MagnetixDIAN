package com.magnetixdian.application;

import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.interfaces.dto.ReenvioFacturaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Reenvío manual de una factura ya emitida (XML firmado + PDF) por correo.
 * El envío automático al emitir vive en {@link ServicioFacturacion}; este
 * servicio cubre el caso de que el adquirente no recibiera el mensaje.
 */
@Service
public class ServicioReenvioFactura {

    private static final Logger log = LoggerFactory.getLogger(ServicioReenvioFactura.class);
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final FacturaRepository facturaRepository;
    private final ServicioFacturacion facturacion;
    private final NotificadorEmail notificador;
    private final AuditoriaService auditoria;

    public ServicioReenvioFactura(FacturaRepository facturaRepository, ServicioFacturacion facturacion,
                                  NotificadorEmail notificador, AuditoriaService auditoria) {
        this.facturaRepository = facturaRepository;
        this.facturacion = facturacion;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    @Transactional
    public ReenvioFacturaDto reenviar(Long empresaId, Long facturaId, String destinatario) {
        if (destinatario == null || !CORREO.matcher(destinatario.trim()).matches()) {
            throw new IllegalArgumentException("El correo electrónico no es válido");
        }
        if (!notificador.habilitado()) {
            throw new IllegalStateException("El envío de correos no está configurado en este servidor");
        }

        FacturaJpa factura = facturaRepository.findById(facturaId)
                .filter(f -> f.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        String numero = factura.getNumeroCompleto();
        String cufe = factura.getCufe() != null ? factura.getCufe() : factura.getCude();
        EmpresaJpa empresa = factura.getEmpresa();
        String correo = destinatario.trim();

        try {
            byte[] pdf = facturacion.pdf(empresaId, facturaId);
            String xml = factura.getXmlFirmado();
            String asunto = "Factura electrónica " + numero + " - " + empresa.getRazonSocial();
            String cuerpo = "Estimado(a) adquirente,\n\n"
                    + "Adjuntamos nuevamente la factura electrónica " + numero + " emitida por "
                    + empresa.getRazonSocial() + ".\n\nCUFE: " + cufe
                    + "\n\nEste mensaje fue generado automáticamente.";
            notificador.enviarFactura(correo, asunto, cuerpo,
                    "factura-" + numero + ".xml",
                    xml == null ? new byte[0] : xml.getBytes(StandardCharsets.UTF_8),
                    "factura-" + numero + ".pdf", pdf);
        } catch (Exception ex) {
            log.warn("No se pudo reenviar por correo la factura {}: {}", numero, ex.getMessage());
            throw new IllegalStateException("No se pudo enviar el correo: " + ex.getMessage());
        }

        auditoria.registrar(AuditoriaService.usuarioActual(), "FACTURA_REENVIADA", "factura", facturaId,
                numero + " → " + correo);
        return new ReenvioFacturaDto(true, correo);
    }
}
