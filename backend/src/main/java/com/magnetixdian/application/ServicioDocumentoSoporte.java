package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.AdquirenteRequest;
import com.magnetixdian.interfaces.dto.DocumentoSoporteRequest;
import com.magnetixdian.interfaces.dto.ResultadoDocumentoDianDto;
import com.magnetixdian.interfaces.dto.SolicitudEmisionDianDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioDocumentoSoporte {

    private final ServicioFacturacion facturacion;

    public ServicioDocumentoSoporte(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public ResultadoDocumentoDianDto emitir(Long empresaId, Long usuarioId, DocumentoSoporteRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El documento soporte es obligatorio.");
        }
        if (solicitud.proveedorNumeroDocumento() == null || solicitud.proveedorNumeroDocumento().isBlank()) {
            throw new IllegalArgumentException("La identificación del proveedor es obligatoria.");
        }
        if (solicitud.concepto() == null || solicitud.concepto().isBlank()) {
            throw new IllegalArgumentException("El concepto del documento soporte es obligatorio.");
        }
        AdquirenteRequest proveedor = new AdquirenteRequest(
                solicitud.proveedorTipoDocumento() == null || solicitud.proveedorTipoDocumento().isBlank()
                        ? "31" : solicitud.proveedorTipoDocumento(),
                solicitud.proveedorNumeroDocumento().trim(),
                null,
                solicitud.proveedorNombre(),
                solicitud.proveedorEmail(),
                null,
                null,
                "ORDINARIO",
                "R-99-PN");
        SolicitudEmisionDianDto emision = new SolicitudEmisionDianDto(
                "05",
                proveedor,
                solicitud.lineas(),
                solicitud.retenciones() == null ? List.of() : solicitud.retenciones(),
                solicitud.concepto().trim(),
                solicitud.formaPago(),
                solicitud.medioPago(),
                "10",
                null,
                null,
                null,
                solicitud.concepto().trim(),
                null);
        return ResultadoDocumentoDianDto.desde(facturacion.emitirSolicitud(empresaId, usuarioId, emision));
    }
}
