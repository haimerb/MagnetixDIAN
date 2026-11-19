package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioReenvioFactura;
import com.magnetixdian.interfaces.dto.ReenviarFacturaRequest;
import com.magnetixdian.interfaces.dto.ReenvioFacturaDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reenvío manual del XML y PDF de una factura al correo indicado.
 */
@RestController
@RequestMapping("/api/facturacion")
public class ReenvioFacturaController {

    private final ServicioReenvioFactura servicio;
    private final AutorizacionEmpresa autorizacion;

    public ReenvioFacturaController(ServicioReenvioFactura servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/{empresaId}/{id}/reenviar")
    public ResponseEntity<ReenvioFacturaDto> reenviar(@PathVariable Long empresaId,
                                                      @PathVariable Long id,
                                                      @RequestBody ReenviarFacturaRequest request,
                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.reenviar(empresaId, id, request.destinatario()));
    }
}
