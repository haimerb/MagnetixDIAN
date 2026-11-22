package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConciliacion;
import com.magnetixdian.application.ServicioContabilizacion;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/contabilidad")
public class ContabilidadController {

    private final ServicioContabilizacion servicioContabilizacion;
    private final ServicioConciliacion servicioConciliacion;
    private final AutorizacionEmpresa autorizacion;

    public ContabilidadController(ServicioContabilizacion servicioContabilizacion,
                                  ServicioConciliacion servicioConciliacion,
                                  AutorizacionEmpresa autorizacion) {
        this.servicioContabilizacion = servicioContabilizacion;
        this.servicioConciliacion = servicioConciliacion;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/asientos")
    public ResponseEntity<List<AsientoContableDto>> asientos(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioContabilizacion.listar(empresaId));
    }

    @PostMapping("/conciliacion")
    public ResponseEntity<ResultadoConciliacionDto> conciliar(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "direccion", required = false) String direccion,
            @RequestParam(value = "tipoDocumento", required = false) String tipoDocumento,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioConciliacion.conciliar(
                empresaId, fechaDesde, fechaHasta, direccion, tipoDocumento));
    }
}
