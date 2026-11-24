package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioExogena;
import com.magnetixdian.application.ServicioGeneracionExogena;
import com.magnetixdian.interfaces.dto.ConceptoExogenaDto;
import com.magnetixdian.interfaces.dto.DianConceptoDto;
import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import com.magnetixdian.interfaces.dto.ExogenaValidacionDto;
import com.magnetixdian.interfaces.dto.FormatoExogenaDto;
import com.magnetixdian.interfaces.dto.FormatoTributarioDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import com.magnetixdian.interfaces.dto.ResultadoCatalogoExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoExogenaGenerarDto;
import com.magnetixdian.interfaces.dto.ResultadoGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoValidacionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudCatalogoExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudExogenaGenerarDto;
import com.magnetixdian.interfaces.dto.SolicitudGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudValidacionExogenaDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/exogena")
public class ExogenaController {

    private final ServicioGeneracionExogena servicio;
    private final AutorizacionEmpresa autorizacion;
    private ServicioExogena servicioExogena;

    public ExogenaController(ServicioGeneracionExogena servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @Autowired(required = false)
    public void setServicioExogena(ServicioExogena servicioExogena) {
        this.servicioExogena = servicioExogena;
    }

    @GetMapping("/formatos-tributarios")
    public ResponseEntity<List<FormatoTributarioDto>> formatosTributarios(@PathVariable Long empresaId,
                                                                         Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.formatos());
    }

    @GetMapping("/conceptos")
    public ResponseEntity<List<DianConceptoDto>> conceptosTributarios(
            @PathVariable Long empresaId,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "formato", required = false) String formato,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.conceptos(ano, formato));
    }

    @PostMapping("/catalogo")
    public ResponseEntity<ResultadoCatalogoExogenaDto> cargarCatalogo(
            @PathVariable Long empresaId,
            @RequestBody SolicitudCatalogoExogenaDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.cargarCatalogo(empresaId, solicitud));
    }

    @PostMapping("/clasificar-desde-asientos")
    public ResponseEntity<List<MovimientoTributarioDto>> clasificarDesdeAsientos(
            @PathVariable Long empresaId,
            @RequestParam("ano") Integer ano,
            @RequestBody List<MovimientoTributarioDto> movimientos,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.clasificarDesdeAsientos(empresaId, ano, movimientos));
    }

    @PostMapping("/generar")
    public ResponseEntity<ResultadoExogenaGenerarDto> generarConMotor(
            @PathVariable Long empresaId,
            @RequestBody SolicitudExogenaGenerarDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.generar(empresaId, solicitud));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoTributarioDto>> movimientos(
            @PathVariable Long empresaId,
            @RequestParam("ano") Integer ano,
            @RequestParam(value = "formato", required = false) String formato,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.movimientos(empresaId, ano, formato));
    }

    @GetMapping("/renglones")
    public ResponseEntity<List<ExogenaRenglonDto>> renglones(
            @PathVariable Long empresaId,
            @RequestParam("ano") Integer ano,
            @RequestParam("formato") String formato,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.renglonesDesdeBase(empresaId, ano, formato));
    }

    @GetMapping("/validaciones")
    public ResponseEntity<List<ExogenaValidacionDto>> validaciones(
            @PathVariable Long empresaId,
            @RequestParam("ano") Integer ano,
            @RequestParam("formato") String formato,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioExogena.validaciones(empresaId, ano, formato));
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel(
            @PathVariable Long empresaId,
            @RequestParam("ano") Integer ano,
            @RequestParam("formato") String formato,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=exogena-" + formato + "-" + ano + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(servicioExogena.excel(empresaId, ano, formato));
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
