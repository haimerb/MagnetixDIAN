package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioCausacionRadian;
import com.magnetixdian.application.ServicioContabilizacion;
import com.magnetixdian.application.ServicioDocumentosRadian;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.ResultadoCausacionDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/radian")
public class RadianController {

    private final ServicioDocumentosRadian servicioDocumentos;
    private final ServicioCausacionRadian servicioCausacion;
    private final ServicioContabilizacion servicioContabilizacion;
    private final AutorizacionEmpresa autorizacion;

    public RadianController(ServicioDocumentosRadian servicioDocumentos,
                            ServicioCausacionRadian servicioCausacion,
                            ServicioContabilizacion servicioContabilizacion,
                            AutorizacionEmpresa autorizacion) {
        this.servicioDocumentos = servicioDocumentos;
        this.servicioCausacion = servicioCausacion;
        this.servicioContabilizacion = servicioContabilizacion;
        this.autorizacion = autorizacion;
    }

    @PostMapping(value = "/documentos/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentoRadianDto>> importar(
            @PathVariable Long empresaId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "direccion", required = false) String direccion,
            Authentication authentication) throws IOException {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioDocumentos.importar(
                empresaId, direccion, archivo.getBytes(), archivo.getOriginalFilename()));
    }

    @GetMapping("/documentos")
    public ResponseEntity<List<DocumentoRadianDto>> listar(
            @PathVariable Long empresaId,
            @RequestParam(value = "direccion", required = false) String direccion,
            @RequestParam(value = "tipoDocumento", required = false) String tipoDocumento,
            @RequestParam(value = "estadoCausacion", required = false) String estadoCausacion,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioDocumentos.listar(
                empresaId, direccion, tipoDocumento, estadoCausacion, fechaDesde, fechaHasta));
    }

    @PostMapping("/documentos/{id}/causar")
    public ResponseEntity<ResultadoCausacionDto> causar(
            @PathVariable Long empresaId,
            @PathVariable Long id,
            @RequestBody(required = false) SolicitudCausacionDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCausacion.causar(empresaId, id, solicitud));
    }

    @PostMapping("/documentos/{id}/contabilizar")
    public ResponseEntity<AsientoContableDto> contabilizar(
            @PathVariable Long empresaId,
            @PathVariable Long id,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioContabilizacion.contabilizar(empresaId, id));
    }
}
