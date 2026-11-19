package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.application.ServicioFacturacion;
import com.magnetixdian.infrastructure.persistence.CatalogoDianJpa;
import com.magnetixdian.infrastructure.persistence.CatalogoDianRepository;
import com.magnetixdian.interfaces.dto.ErrorValidacionDto;
import com.magnetixdian.interfaces.dto.FacturaDetalleDto;
import com.magnetixdian.interfaces.dto.FacturaEventoDto;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.FacturaResumenDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.RegistrarEventoRequest;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * API REST de facturación electrónica: validar, emitir, listar, descargar
 * (PDF/XML) y consultar catálogos DIAN.
 */
@RestController
@RequestMapping("/api/facturacion")
public class FacturaController {

    private final ServicioFacturacion servicio;
    private final AuthService authService;
    private final CatalogoDianRepository catalogoRepository;
    private final AutorizacionEmpresa autorizacion;

    public FacturaController(ServicioFacturacion servicio, AuthService authService,
                             CatalogoDianRepository catalogoRepository,
                             AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.authService = authService;
        this.catalogoRepository = catalogoRepository;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/{empresaId}/validar")
    public ResponseEntity<List<ErrorValidacionDto>> validar(@PathVariable Long empresaId,
                                                            @RequestBody FacturaRequest request,
                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.validar(empresaId, request));
    }

    @PostMapping("/{empresaId}/emitir")
    public ResponseEntity<ResultadoEmisionDto> emitir(@PathVariable Long empresaId,
                                                      @RequestBody FacturaRequest request,
                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        Long usuarioId = authService.perfil(authentication.getName()).id();
        return ResponseEntity.ok(servicio.emitir(empresaId, usuarioId, request));
    }

    @GetMapping("/{empresaId}")
    public ResponseEntity<List<FacturaResumenDto>> listar(@PathVariable Long empresaId,
                                                          Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.listar(empresaId));
    }

    @GetMapping("/{empresaId}/paginado")
    public ResponseEntity<PaginaDto<FacturaResumenDto>> listarPaginado(
            @PathVariable Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.listarPaginado(empresaId, page, size));
    }

    @GetMapping("/{empresaId}/export")
    public ResponseEntity<byte[]> exportar(@PathVariable Long empresaId,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        byte[] contenido = servicio.exportarCsv(empresaId, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("facturas-" + empresaId + ".csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    @GetMapping("/{empresaId}/{id}")
    public ResponseEntity<FacturaDetalleDto> detalle(@PathVariable Long empresaId, @PathVariable Long id,
                                                     Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.detalle(empresaId, id));
    }

    @PostMapping("/{empresaId}/{id}/eventos")
    public ResponseEntity<FacturaEventoDto> registrarEvento(@PathVariable Long empresaId, @PathVariable Long id,
                                                            @RequestBody RegistrarEventoRequest request,
                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.registrarEvento(empresaId, id, request.tipoEvento()));
    }

    @GetMapping("/{empresaId}/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long empresaId, @PathVariable Long id,
                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        byte[] contenido = servicio.pdf(empresaId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=factura-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }

    @GetMapping("/{empresaId}/{id}/xml")
    public ResponseEntity<byte[]> xml(@PathVariable Long empresaId, @PathVariable Long id,
                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        String xml = servicio.xmlFirmado(empresaId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=factura-" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/catalogo/{tipo}")
    public ResponseEntity<List<CatalogoDianJpa>> catalogo(@PathVariable String tipo) {
        return ResponseEntity.ok(catalogoRepository.findByTipoAndActivoTrueOrderByCodigo(tipo));
    }
}
