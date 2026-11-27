package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioAsientos;
import com.magnetixdian.application.ServicioCentrosCosto;
import com.magnetixdian.application.ServicioConciliacion;
import com.magnetixdian.application.ServicioContabilizacion;
import com.magnetixdian.application.ServicioPeriodos;
import com.magnetixdian.application.ServicioPlanCuentas;
import com.magnetixdian.application.ServicioReportesContables;
import com.magnetixdian.application.ServicioTerceros;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.BalanceComprobacionDto;
import com.magnetixdian.interfaces.dto.CentroCostoDto;
import com.magnetixdian.interfaces.dto.CuentaContableDto;
import com.magnetixdian.interfaces.dto.EstadoFinancieroDto;
import com.magnetixdian.interfaces.dto.FlujoEfectivoDto;
import com.magnetixdian.interfaces.dto.MovimientoContableDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.PeriodoContableDto;
import com.magnetixdian.interfaces.dto.RegimenContableDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionTesoreriaDto;
import com.magnetixdian.interfaces.dto.ResumenContableDto;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudCentroCostoDto;
import com.magnetixdian.interfaces.dto.SolicitudConciliacionTesoreriaDto;
import com.magnetixdian.interfaces.dto.SolicitudCuentaContableDto;
import com.magnetixdian.interfaces.dto.SolicitudPeriodoDto;
import com.magnetixdian.interfaces.dto.SolicitudTerceroDto;
import com.magnetixdian.interfaces.dto.TerceroContableDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final ServicioAsientos servicioAsientos;
    private final ServicioReportesContables servicioReportes;
    private final AutorizacionEmpresa autorizacion;
    private ServicioPlanCuentas servicioPlanCuentas;
    private ServicioPeriodos servicioPeriodos;
    private ServicioCentrosCosto servicioCentrosCosto;
    private ServicioTerceros servicioTerceros;

    public ContabilidadController(ServicioContabilizacion servicioContabilizacion,
                                  ServicioConciliacion servicioConciliacion,
                                  ServicioAsientos servicioAsientos,
                                  ServicioReportesContables servicioReportes,
                                  AutorizacionEmpresa autorizacion) {
        this.servicioContabilizacion = servicioContabilizacion;
        this.servicioConciliacion = servicioConciliacion;
        this.servicioAsientos = servicioAsientos;
        this.servicioReportes = servicioReportes;
        this.autorizacion = autorizacion;
    }

    @Autowired(required = false)
    public void setServicioPlanCuentas(ServicioPlanCuentas servicioPlanCuentas) {
        this.servicioPlanCuentas = servicioPlanCuentas;
    }

    @Autowired(required = false)
    public void setServicioPeriodos(ServicioPeriodos servicioPeriodos) {
        this.servicioPeriodos = servicioPeriodos;
    }

    @Autowired(required = false)
    public void setServicioCentrosCosto(ServicioCentrosCosto servicioCentrosCosto) {
        this.servicioCentrosCosto = servicioCentrosCosto;
    }

    @Autowired(required = false)
    public void setServicioTerceros(ServicioTerceros servicioTerceros) {
        this.servicioTerceros = servicioTerceros;
    }

    @GetMapping("/puc")
    public ResponseEntity<List<CuentaContableDto>> puc(@PathVariable Long empresaId,
                                                       Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPlanCuentas.listarDto(empresaId));
    }

    @PostMapping("/puc")
    public ResponseEntity<CuentaContableDto> crearCuenta(@PathVariable Long empresaId,
                                                        @RequestBody SolicitudCuentaContableDto solicitud,
                                                        Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPlanCuentas.crear(empresaId, solicitud));
    }

    @GetMapping("/regimen")
    public ResponseEntity<RegimenContableDto> regimen(@PathVariable Long empresaId,
                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPlanCuentas.regimen(empresaId));
    }

    @PutMapping("/regimen")
    public ResponseEntity<RegimenContableDto> cambiarRegimen(@PathVariable Long empresaId,
                                                             @RequestParam("regimenNiif") String regimenNiif,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPlanCuentas.cambiarRegimen(empresaId, regimenNiif));
    }

    @GetMapping("/periodos")
    public ResponseEntity<List<PeriodoContableDto>> periodos(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPeriodos.listar(empresaId));
    }

    @PostMapping("/periodos")
    public ResponseEntity<PeriodoContableDto> crearPeriodo(@PathVariable Long empresaId,
                                                           @RequestBody SolicitudPeriodoDto solicitud,
                                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPeriodos.crear(empresaId, solicitud));
    }

    @PutMapping("/periodos/{periodoId}/cerrar-registro")
    public ResponseEntity<PeriodoContableDto> cerrarRegistroPeriodo(@PathVariable Long empresaId,
                                                                    @PathVariable Long periodoId,
                                                                    Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioPeriodos.cerrar(empresaId, periodoId));
    }

    @GetMapping("/centros-costo")
    public ResponseEntity<List<CentroCostoDto>> centrosCosto(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCentrosCosto.listar(empresaId));
    }

    @PostMapping("/centros-costo")
    public ResponseEntity<CentroCostoDto> crearCentroCosto(@PathVariable Long empresaId,
                                                           @RequestBody SolicitudCentroCostoDto solicitud,
                                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioCentrosCosto.crear(empresaId, solicitud));
    }

    @GetMapping("/terceros")
    public ResponseEntity<List<TerceroContableDto>> terceros(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioTerceros.listar(empresaId));
    }

    @PostMapping("/terceros")
    public ResponseEntity<TerceroContableDto> crearTercero(@PathVariable Long empresaId,
                                                           @RequestBody SolicitudTerceroDto solicitud,
                                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioTerceros.crear(empresaId, solicitud));
    }

    @GetMapping("/asientos")
    public ResponseEntity<List<AsientoContableDto>> asientos(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioContabilizacion.listar(empresaId));
    }

    @GetMapping("/asientos/origen")
    public ResponseEntity<AsientoContableDto> asientoPorOrigen(@PathVariable Long empresaId,
                                                               @RequestParam String origen,
                                                               @RequestParam Long origenId,
                                                               @RequestParam String tipoEvento,
                                                               Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.of(servicioContabilizacion.buscarPorOrigen(
                empresaId, origen, origenId, tipoEvento));
    }

    @PostMapping("/asientos")
    public ResponseEntity<AsientoContableDto> crearAsiento(@PathVariable Long empresaId,
                                                           @RequestBody SolicitudAsientoDto solicitud,
                                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioAsientos.crear(empresaId, solicitud));
    }

    @PostMapping("/asientos/{asientoId}/reversar")
    public ResponseEntity<AsientoContableDto> reversarAsiento(@PathVariable Long empresaId,
                                                              @PathVariable Long asientoId,
                                                              Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioAsientos.reversar(empresaId, asientoId));
    }

    @PostMapping("/periodos/{periodoId}/cerrar")
    public ResponseEntity<AsientoContableDto> cerrarPeriodo(@PathVariable Long empresaId,
                                                            @PathVariable Long periodoId,
                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioAsientos.cerrarPeriodo(empresaId, periodoId));
    }

    @GetMapping("/reportes/libro-diario")
    public ResponseEntity<PaginaDto<AsientoContableDto>> libroDiario(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "centroCostoId", required = false) Long centroCostoId,
            @RequestParam(value = "terceroId", required = false) Long terceroId,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamano", defaultValue = "20") int tamano,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.libroDiario(empresaId, fechaDesde, fechaHasta,
                centroCostoId, terceroId, pagina, tamano));
    }

    @GetMapping("/reportes/libro-mayor")
    public ResponseEntity<PaginaDto<MovimientoContableDto>> libroMayor(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "codigoCuenta", required = false) String codigoCuenta,
            @RequestParam(value = "centroCostoId", required = false) Long centroCostoId,
            @RequestParam(value = "terceroId", required = false) Long terceroId,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamano", defaultValue = "20") int tamano,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.libroMayor(empresaId, fechaDesde, fechaHasta,
                codigoCuenta, centroCostoId, terceroId, pagina, tamano));
    }

    @GetMapping("/reportes/auxiliares")
    public ResponseEntity<PaginaDto<MovimientoContableDto>> auxiliares(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "codigoCuenta", required = false) String codigoCuenta,
            @RequestParam(value = "centroCostoId", required = false) Long centroCostoId,
            @RequestParam(value = "terceroId", required = false) Long terceroId,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamano", defaultValue = "20") int tamano,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.auxiliar(empresaId, fechaDesde, fechaHasta,
                codigoCuenta, centroCostoId, terceroId, pagina, tamano));
    }

    @GetMapping("/reportes/balance-comprobacion")
    public ResponseEntity<BalanceComprobacionDto> balanceComprobacion(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.balanceComprobacion(empresaId, fechaDesde, fechaHasta));
    }

    @GetMapping("/reportes/balance-general")
    public ResponseEntity<EstadoFinancieroDto> balanceGeneral(
            @PathVariable Long empresaId,
            @RequestParam("fechaCorte") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.balanceGeneral(empresaId, fechaCorte));
    }

    @GetMapping("/reportes/estado-resultados")
    public ResponseEntity<EstadoFinancieroDto> estadoResultados(
            @PathVariable Long empresaId,
            @RequestParam("fechaDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam("fechaHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.estadoResultados(empresaId, fechaDesde, fechaHasta));
    }

    @GetMapping("/reportes/flujo-efectivo")
    public ResponseEntity<FlujoEfectivoDto> flujoEfectivo(
            @PathVariable Long empresaId,
            @RequestParam("fechaDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam("fechaHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.flujoEfectivo(empresaId, fechaDesde, fechaHasta));
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

    @PostMapping("/conciliacion/tesoreria")
    public ResponseEntity<ResultadoConciliacionTesoreriaDto> conciliarTesoreria(
            @PathVariable Long empresaId,
            @RequestBody SolicitudConciliacionTesoreriaDto solicitud,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioConciliacion.conciliarTesoreria(
                empresaId, solicitud.movimientosBanco()));
    }

    @GetMapping("/reportes/resumen-cuentas")
    public ResponseEntity<List<ResumenContableDto>> resumenPorCuenta(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.resumenPorCuenta(empresaId, fechaDesde, fechaHasta));
    }

    @GetMapping("/reportes/resumen-terceros")
    public ResponseEntity<List<ResumenContableDto>> resumenPorTercero(
            @PathVariable Long empresaId,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicioReportes.resumenPorTercero(empresaId, fechaDesde, fechaHasta));
    }
}
