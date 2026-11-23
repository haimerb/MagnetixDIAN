package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioGeneracionExogena;
import com.magnetixdian.interfaces.dto.ConceptoExogenaDto;
import com.magnetixdian.interfaces.dto.FormatoExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoValidacionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudValidacionExogenaDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/exogena")
public class ExogenaController {

    private final ServicioGeneracionExogena servicio;
    private final AutorizacionEmpresa autorizacion;

    public ExogenaController(ServicioGeneracionExogena servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/formatos")
    public ResponseEntity<List<FormatoExogenaDto>> formatos(@PathVariable Long empresaId,
                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.formatosOficiales().stream()
                .map(f -> new FormatoExogenaDto(
                        f.codigo(), f.descripcion(), f.estado(), f.activo(),
                        f.version(), f.xsd(), f.validacion(), f.generadorXml(), f.encabezados()))
                .toList());
    }

    @GetMapping("/formatos/{formato}/conceptos")
    public ResponseEntity<List<ConceptoExogenaDto>> conceptos(@PathVariable Long empresaId,
                                                              @PathVariable String formato,
                                                              Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.conceptos(formato));
    }

    @PostMapping("/{formato}/generar")
    public ResponseEntity<ResultadoGeneracionExogenaDto> generar(@PathVariable Long empresaId,
                                                                 @PathVariable String formato,
                                                                 @RequestBody SolicitudGeneracionExogenaDto solicitud,
                                                                 Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        SolicitudGeneracionExogenaDto completa = new SolicitudGeneracionExogenaDto(
                formato,
                solicitud.anioGravable(),
                solicitud.numeroEnvio(),
                solicitud.fechaEnvio(),
                solicitud.fechaInicial(),
                solicitud.fechaFinal(),
                solicitud.concepto(),
                solicitud.encabezados(),
                solicitud.filas());
        ResultadoGeneracionExogenaDto resultado = servicio.generar(empresaId, completa);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=exogena-" + formato + ".xml")
                .body(resultado);
    }

    @PostMapping("/{formato}/validar-xml")
    public ResponseEntity<ResultadoValidacionExogenaDto> validarXml(@PathVariable Long empresaId,
                                                                    @PathVariable String formato,
                                                                    @RequestBody SolicitudValidacionExogenaDto solicitud,
                                                                    Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        String xml = solicitud == null ? null : solicitud.xml();
        return ResponseEntity.ok(servicio.validarXml(formato, xml));
    }
}
