package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioHabilitacionDianWizard;
import com.magnetixdian.interfaces.dto.AuditoriaDianDto;
import com.magnetixdian.interfaces.dto.DianHabilitacionDto;
import com.magnetixdian.interfaces.dto.DocumentoTestSetDianDto;
import com.magnetixdian.interfaces.dto.HabilitacionDianEmpresaDto;
import com.magnetixdian.interfaces.dto.ResultadoTestSetDianDto;
import com.magnetixdian.interfaces.dto.SolicitudDocumentosTestDianDto;
import com.magnetixdian.interfaces.dto.SolicitudSwitchoverDianDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/dian")
public class HabilitacionDianController {

    private final ServicioHabilitacionDianWizard servicio;
    private final AutorizacionEmpresa autorizacion;

    public HabilitacionDianController(ServicioHabilitacionDianWizard servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/habilitacion")
    public ResponseEntity<HabilitacionDianEmpresaDto> habilitacion(@PathVariable Long empresaId,
                                                                   Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId));
    }

    @PutMapping("/habilitacion")
    public ResponseEntity<HabilitacionDianEmpresaDto> guardar(@PathVariable Long empresaId,
                                                              @RequestBody DianHabilitacionDto solicitud,
                                                              Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.guardar(empresaId, solicitud));
    }

    @GetMapping("/test-set/documentos")
    public ResponseEntity<List<DocumentoTestSetDianDto>> documentos(@PathVariable Long empresaId,
                                                                    Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.documentos(empresaId));
    }

    @PostMapping("/test-set/documentos")
    public ResponseEntity<ResultadoTestSetDianDto> enviarDocumentos(@PathVariable Long empresaId,
                                                                    @RequestBody SolicitudDocumentosTestDianDto solicitud,
                                                                    Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.enviar(empresaId, solicitud));
    }

    @GetMapping("/test-set/{testSetId}")
    public ResponseEntity<ResultadoTestSetDianDto> estadoTestSet(@PathVariable Long empresaId,
                                                                 @PathVariable String testSetId,
                                                                 Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.consultar(empresaId, testSetId));
    }

    @PostMapping("/switchover")
    public ResponseEntity<HabilitacionDianEmpresaDto> switchover(@PathVariable Long empresaId,
                                                                @RequestBody SolicitudSwitchoverDianDto solicitud,
                                                                Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.switchover(empresaId, solicitud));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<AuditoriaDianDto>> auditoria(
            @PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId).auditoria());
    }
}
