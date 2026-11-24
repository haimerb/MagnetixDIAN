package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioCausacionImpuestos;
import com.magnetixdian.application.ServicioCertificadosImpuesto;
import com.magnetixdian.application.ServicioDeclaracionesImpuesto;
import com.magnetixdian.application.ServicioImpuestos;
import com.magnetixdian.application.ServicioResponsabilidadesFiscales;
import com.magnetixdian.application.ServicioTarifasImpuesto;
import com.magnetixdian.interfaces.dto.CalendarioTributarioDto;
import com.magnetixdian.interfaces.dto.CatalogoImpuestoDto;
import com.magnetixdian.interfaces.dto.CausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.CertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.DeclaracionAsientoDto;
import com.magnetixdian.interfaces.dto.DeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.PagoImpuestoDto;
import com.magnetixdian.interfaces.dto.ResponsabilidadFiscalDto;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.ResumenImpuestoAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudDeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudPagoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudResponsabilidadFiscalDto;
import com.magnetixdian.interfaces.dto.TarifaImpuestoDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/impuestos")
public class ImpuestosController {

    private final ServicioTarifasImpuesto servicioTarifas;
    private final ServicioResponsabilidadesFiscales servicioResponsabilidades;
    private final ServicioCausacionImpuestos servicioCausacion;
    private final ServicioCertificadosImpuesto servicioCertificados;
    private final ServicioDeclaracionesImpuesto servicioDeclaraciones;
    private final AutorizacionEmpresa autorizacion;
    private ServicioImpuestos servicioImpuestos;

    public ImpuestosController(ServicioTarifasImpuesto servicioTarifas,
                               ServicioResponsabilidadesFiscales servicioResponsabilidades,
                               ServicioCausacionImpuestos servicioCausacion,
                               ServicioCertificadosImpuesto servicioCertificados,
                               ServicioDeclaracionesImpuesto servicioDeclaraciones,
                               AutorizacionEmpresa autorizacion) {
        this.servicioTarifas = servicioTarifas;
        this.servicioResponsabilidades = servicioResponsabilidades;
        this.servicioCausacion = servicioCausacion;
        this.servicioCertificados = servicioCertificados;
        this.servicioDeclaraciones = servicioDeclaraciones;
        this.autorizacion = autorizacion;
    }

    @Autowired(required = false)
    public void setServicioImpuestos(ServicioImpuestos servicioImpuestos) {
        this.servicioImpuestos = servicioImpuestos;
    }

    private String normalizar(String impuestoCodigo) {
        if (servicioImpuestos == null || impuestoCodigo == null) {
            return impuestoCodigo;
        }
        return servicioImpuestos.normalizar(impuestoCodigo);
    }

    @GetMapping("/catalogo")
    public ResponseEntity<List<CatalogoImpuestoDto>> catalogo(@PathVariable Long empresaId,
                                                              Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioImpuestos.catalogo());
    }

    @GetMapping("/tarifas")
    public ResponseEntity<List<TarifaImpuestoDto>> tarifas(@PathVariable Long empresaId,
                                                           @RequestParam(required = false) String impuestoCodigo,
                                                           @RequestParam(required = false) Integer anio,
                                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioTarifas.listar(normalizar(impuestoCodigo), anio));
    }

    @PostMapping("/calcular")
    public ResponseEntity<ResultadoCalculoImpuestoDto> calcular(
            @PathVariable Long empresaId,
            @RequestBody SolicitudCalculoImpuestoDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioTarifas.calcular(
                normalizar(solicitud.impuestoCodigo()), solicitud.anio(),
                solicitud.conceptoCodigo(), solicitud.base()));
    }

    @GetMapping("/responsabilidades")
    public ResponseEntity<List<ResponsabilidadFiscalDto>> responsabilidades(@PathVariable Long empresaId,
                                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioResponsabilidades.listar(empresaId));
    }

    @PostMapping("/responsabilidades")
    public ResponseEntity<ResponsabilidadFiscalDto> registrarResponsabilidad(
            @PathVariable Long empresaId,
            @RequestBody SolicitudResponsabilidadFiscalDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        SolicitudResponsabilidadFiscalDto normalizada = new SolicitudResponsabilidadFiscalDto(
                solicitud.terceroId(), normalizar(solicitud.impuestoCodigo()), solicitud.codigo(),
                solicitud.desde(), solicitud.hasta());
        return ResponseEntity.ok(servicioResponsabilidades.registrar(empresaId, normalizada));
    }

    @PostMapping("/causaciones")
    public ResponseEntity<CausacionImpuestoDto> causar(@PathVariable Long empresaId,
                                                       @RequestBody SolicitudCausacionImpuestoDto solicitud,
                                                       Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        SolicitudCausacionImpuestoDto normalizada = new SolicitudCausacionImpuestoDto(
                normalizar(solicitud.impuestoCodigo()), solicitud.origen(), solicitud.origenId(),
                solicitud.terceroId(), solicitud.fecha(), solicitud.conceptoCodigo(),
                solicitud.baseImponible(), solicitud.observaciones());
        return ResponseEntity.ok(servicioCausacion.causar(empresaId, normalizada));
    }

    @GetMapping("/causaciones/desde-asientos")
    public ResponseEntity<List<ResumenImpuestoAsientoDto>> causacionDesdeAsientos(
            @PathVariable Long empresaId,
            @RequestParam("fechaDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam("fechaHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioImpuestos.causacionDesdeAsientos(empresaId, fechaDesde, fechaHasta));
    }

    @GetMapping("/causaciones")
    public ResponseEntity<List<CausacionImpuestoDto>> causaciones(@PathVariable Long empresaId,
                                                                  @RequestParam(required = false) String periodo,
                                                                  @RequestParam(required = false) Long terceroId,
                                                                  Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        List<CausacionImpuestoDto> resultado = terceroId != null
                ? servicioCausacion.listarPorTercero(empresaId, terceroId)
                : servicioCausacion.listarPorPeriodo(
                        empresaId, periodo == null ? YearMonth.now().toString() : periodo);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/causaciones/{causacionId}/pagos")
    public ResponseEntity<PagoImpuestoDto> registrarPago(@PathVariable Long empresaId,
                                                         @PathVariable Long causacionId,
                                                         @RequestBody SolicitudPagoImpuestoDto solicitud,
                                                         Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCausacion.registrarPago(empresaId, causacionId, solicitud));
    }

    @PostMapping("/causaciones/{causacionId}/anular")
    public ResponseEntity<CausacionImpuestoDto> anularCausacion(@PathVariable Long empresaId,
                                                                @PathVariable Long causacionId,
                                                                Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCausacion.anular(empresaId, causacionId));
    }

    @PostMapping("/certificados")
    public ResponseEntity<CertificadoImpuestoDto> emitirCertificado(
            @PathVariable Long empresaId,
            @RequestBody SolicitudCertificadoImpuestoDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        SolicitudCertificadoImpuestoDto normalizada = new SolicitudCertificadoImpuestoDto(
                solicitud.terceroId(), normalizar(solicitud.impuestoCodigo()), solicitud.anio(),
                solicitud.numero(), solicitud.valor(), solicitud.fechaEmision());
        return ResponseEntity.ok(servicioCertificados.emitir(empresaId, normalizada));
    }

    @GetMapping("/certificados")
    public ResponseEntity<List<CertificadoImpuestoDto>> certificados(@PathVariable Long empresaId,
                                                                     @RequestParam(required = false) Integer anio,
                                                                     Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCertificados.listar(empresaId, anio));
    }

    @PostMapping("/declaraciones")
    public ResponseEntity<DeclaracionImpuestoDto> generarDeclaracion(
            @PathVariable Long empresaId,
            @RequestBody SolicitudDeclaracionImpuestoDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        SolicitudDeclaracionImpuestoDto normalizada = new SolicitudDeclaracionImpuestoDto(
                normalizar(solicitud.impuestoCodigo()), solicitud.anioGravable(), solicitud.periodo(),
                solicitud.fechaDesde(), solicitud.fechaHasta());
        return ResponseEntity.ok(servicioDeclaraciones.generarBorrador(empresaId, normalizada));
    }

    @GetMapping("/declaraciones/desde-asientos")
    public ResponseEntity<DeclaracionAsientoDto> declaracionDesdeAsientos(
            @PathVariable Long empresaId,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "periodo", required = false) String periodo,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioImpuestos.declaracionDesdeAsientos(empresaId, anio, periodo));
    }

    @GetMapping("/declaraciones")
    public ResponseEntity<List<DeclaracionImpuestoDto>> declaraciones(@PathVariable Long empresaId,
                                                                      @RequestParam(required = false) Integer anio,
                                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioDeclaraciones.listar(empresaId, anio));
    }

    @PostMapping("/declaraciones/{declaracionId}/presentar")
    public ResponseEntity<DeclaracionImpuestoDto> presentarDeclaracion(
            @PathVariable Long empresaId,
            @PathVariable Long declaracionId,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioDeclaraciones.marcarPresentada(empresaId, declaracionId));
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioTributarioDto>> calendario(@PathVariable Long empresaId,
                                                                    @RequestParam(required = false) Integer anio,
                                                                    Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioDeclaraciones.listarCalendario(anio));
    }
}
