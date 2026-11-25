package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.NotaDianRequest;
import com.magnetixdian.interfaces.dto.ResultadoDocumentoDianDto;
import com.magnetixdian.interfaces.dto.SolicitudEmisionDianDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ServicioNotasDian {

    private final ServicioFacturacion facturacion;

    public ServicioNotasDian(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public ResultadoDocumentoDianDto emitir(Long empresaId, Long usuarioId, String tipo, NotaDianRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La nota es obligatoria.");
        }
        if (solicitud.cufeFactura() == null || solicitud.cufeFactura().isBlank()) {
            throw new IllegalArgumentException("El CUFE de la factura referenciada es obligatorio.");
        }
        if (solicitud.numeroFactura() == null || solicitud.numeroFactura().isBlank()) {
            throw new IllegalArgumentException("El número de la factura referenciada es obligatorio.");
        }
        if (solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            throw new IllegalArgumentException("El motivo de la nota es obligatorio.");
        }
        String codigo = normalizarTipo(tipo);
        SolicitudEmisionDianDto emision = new SolicitudEmisionDianDto(
                codigo,
                solicitud.adquirente(),
                solicitud.lineas(),
                List.of(),
                solicitud.motivo(),
                solicitud.formaPago(),
                solicitud.medioPago(),
                "10",
                null,
                solicitud.cufeFactura().trim(),
                solicitud.numeroFactura().trim(),
                solicitud.motivo().trim(),
                null,
                null);
        return ResultadoDocumentoDianDto.desde(facturacion.emitirSolicitud(empresaId, usuarioId, emision));
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de nota es obligatorio.");
        }
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return switch (normalizado) {
            case "91", "NC", "NOTACREDITO", "CREDITNOTE" -> "91";
            case "92", "ND", "NOTADEBITO", "DEBITNOTE" -> "92";
            default -> throw new IllegalArgumentException("Tipo de nota no soportado: " + tipo);
        };
    }
}
