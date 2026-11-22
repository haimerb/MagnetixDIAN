package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioTraerDian;
import com.magnetixdian.interfaces.dto.ResultadoTraerDianDto;
import com.magnetixdian.interfaces.dto.SolicitudTraerDianTokenDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/empresas/{empresaId}/dian/documentos")
public class TraerDianController {

    private final ServicioTraerDian servicio;
    private final AutorizacionEmpresa autorizacion;

    public TraerDianController(ServicioTraerDian servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultadoTraerDianDto> porToken(
            @PathVariable Long empresaId,
            @RequestBody SolicitudTraerDianTokenDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.porToken(empresaId, solicitud));
    }

    @PostMapping(value = "/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoTraerDianDto> porArchivo(
            @PathVariable Long empresaId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "direccion", required = false) String direccion,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "grupo", required = false) String grupo,
            @RequestParam(value = "nitTercero", required = false) String nitTercero,
            Authentication authentication) throws IOException {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.porArchivo(empresaId, archivo.getBytes(), archivo.getOriginalFilename(),
                direccion, fechaDesde, fechaHasta, grupo, nitTercero));
    }
}
