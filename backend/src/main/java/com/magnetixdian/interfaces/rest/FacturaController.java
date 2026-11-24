package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.application.ImportadorFacturas;
import com.magnetixdian.application.ServicioDocumentoSoporte;
import com.magnetixdian.application.ServicioFacturacion;
import com.magnetixdian.application.ServicioNominaElectronica;
import com.magnetixdian.application.ServicioNotasDian;
import com.magnetixdian.application.ServicioRecepcionRadian;
import com.magnetixdian.infrastructure.persistence.CatalogoDianJpa;
import com.magnetixdian.infrastructure.persistence.CatalogoDianRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianRecepcionDto;
import com.magnetixdian.interfaces.dto.DocumentoSoporteRequest;
import com.magnetixdian.interfaces.dto.ErrorValidacionDto;
import com.magnetixdian.interfaces.dto.FacturaDetalleDto;
import com.magnetixdian.interfaces.dto.FacturaEventoDto;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.FacturaResumenDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import com.magnetixdian.interfaces.dto.NominaElectronicaDto;
import com.magnetixdian.interfaces.dto.NotaDianRequest;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.RegistrarEventoRequest;
import com.magnetixdian.interfaces.dto.ResultadoDocumentoDianDto;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionDto;
import com.magnetixdian.interfaces.dto.ResultadoRecepcionRadianDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
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
    private final ImportadorFacturas importador;
    private final AuthService authService;
    private final CatalogoDianRepository catalogoRepository;
    private final AutorizacionEmpresa autorizacion;
    private final ServicioNotasDian notas;
    private final ServicioDocumentoSoporte documentosSoporte;
    private final ServicioNominaElectronica nominaElectronica;
    private final ServicioRecepcionRadian recepcionRadian;

    public FacturaController(ServicioFacturacion servicio, ImportadorFacturas importador, AuthService authService,
                             CatalogoDianRepository catalogoRepository,
                             AutorizacionEmpresa autorizacion) {
        this(servicio, importador, authService, catalogoRepository, autorizacion, null, null, null, null);
    }

    @Autowired
    public FacturaController(ServicioFacturacion servicio, ImportadorFacturas importador, AuthService authService,
                             CatalogoDianRepository catalogoRepository, AutorizacionEmpresa autorizacion,
                             ServicioNotasDian notas, ServicioDocumentoSoporte documentosSoporte,
                             ServicioNominaElectronica nominaElectronica,
                             ServicioRecepcionRadian recepcionRadian) {
        this.servicio = servicio;
        this.importador = importador;
        this.authService = authService;
        this.catalogoRepository = catalogoRepository;
        this.autorizacion = autorizacion;
        this.notas = notas;
        this.documentosSoporte = documentosSoporte;
        this.nominaElectronica = nominaElectronica;
        this.recepcionRadian = recepcionRadian;
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

    @PostMapping("/{empresaId}/notas/{tipo}")
    public ResponseEntity<ResultadoDocumentoDianDto> emitirNota(@PathVariable Long empresaId,
                                                                @PathVariable String tipo,
                                                                @RequestBody NotaDianRequest request,
                                                                Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        verificar(notas, "ServicioNotasDian");
        Long usuarioId = authService.perfil(authentication.getName()).id();
        return ResponseEntity.ok(notas.emitir(empresaId, usuarioId, tipo, request));
    }

    @PostMapping("/{empresaId}/documentos-soporte")
    public ResponseEntity<ResultadoDocumentoDianDto> emitirDocumentoSoporte(
            @PathVariable Long empresaId,
            @RequestBody DocumentoSoporteRequest request,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        verificar(documentosSoporte, "ServicioDocumentoSoporte");
        Long usuarioId = authService.perfil(authentication.getName()).id();
        return ResponseEntity.ok(documentosSoporte.emitir(empresaId, usuarioId, request));
    }

    @PostMapping("/{empresaId}/nomina-electronica")
    public ResponseEntity<ResultadoDocumentoDianDto> emitirNomina(@PathVariable Long empresaId,
                                                                  @RequestBody NominaElectronicaDto request,
                                                                  Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        verificar(nominaElectronica, "ServicioNominaElectronica");
        Long usuarioId = authService.perfil(authentication.getName()).id();
        return ResponseEntity.ok(nominaElectronica.emitir(empresaId, usuarioId, request));
    }

    @PostMapping("/{empresaId}/radian/recepcion")
    public ResponseEntity<ResultadoRecepcionRadianDto> recibirRadian(
            @PathVariable Long empresaId,
            @RequestBody DocumentoRadianRecepcionDto request,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        verificar(recepcionRadian, "ServicioRecepcionRadian");
        return ResponseEntity.ok(recepcionRadian.recibir(empresaId, request));
    }

    @GetMapping("/{empresaId}/movimientos-exogena")
    public ResponseEntity<List<MovimientoTributarioDto>> movimientosExogena(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer ano,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.movimientosParaExogena(empresaId, ano));
    }

    @PostMapping(value = "/{empresaId}/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoImportacionDto> importar(@PathVariable Long empresaId,
                                                            @RequestParam("archivo") MultipartFile archivo,
                                                            Authentication authentication) throws IOException {
        autorizacion.verificar(empresaId, authentication);
        Long usuarioId = authService.perfil(authentication.getName()).id();
        return ResponseEntity.ok(importador.importar(empresaId, usuarioId, archivo.getBytes(),
                archivo.getOriginalFilename()));
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

    private void verificar(Object servicio, String nombre) {
        if (servicio == null) {
            throw new IllegalStateException(nombre + " no disponible.");
        }
    }
}
