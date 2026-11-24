package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.erp.*;
import com.magnetixdian.interfaces.dto.erp.*;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/erp")
public class ErpController {

    private final ServicioVentas ventas;
    private final ServicioCompras compras;
    private final ServicioInventario inventario;
    private final ServicioTesoreria tesoreria;
    private final ServicioActivos activos;
    private final ServicioIntegracionesErp integraciones;
    private final AutorizacionEmpresa autorizacion;

    public ErpController(ServicioVentas ventas, ServicioCompras compras, ServicioInventario inventario,
                         ServicioTesoreria tesoreria, ServicioActivos activos, ServicioIntegracionesErp integraciones,
                         AutorizacionEmpresa autorizacion) {
        this.ventas = ventas;
        this.compras = compras;
        this.inventario = inventario;
        this.tesoreria = tesoreria;
        this.activos = activos;
        this.integraciones = integraciones;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/ventas/ordenes")
    public ResponseEntity<OrdenVentaDto> crearOrden(@PathVariable Long empresaId, @RequestBody OrdenVentaRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(ventas.crearOrden(empresaId, request));
    }

    @GetMapping("/ventas/ordenes")
    public ResponseEntity<List<OrdenVentaDto>> listarOrdenes(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(ventas.listarOrdenes(empresaId));
    }

    @PostMapping("/ventas/pedidos")
    public ResponseEntity<PedidoDto> crearPedido(@PathVariable Long empresaId, @RequestBody PedidoRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(ventas.crearPedido(empresaId, request));
    }

    @GetMapping("/ventas/pedidos")
    public ResponseEntity<List<PedidoDto>> listarPedidos(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(ventas.listarPedidos(empresaId));
    }

    @PostMapping("/ventas/pedidos/{pedidoId}/aprobar")
    public ResponseEntity<PedidoDto> aprobarPedido(@PathVariable Long empresaId, @PathVariable Long pedidoId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(ventas.aprobarPedido(empresaId, pedidoId));
    }

    @PostMapping("/compras/ordenes")
    public ResponseEntity<CompraDto> crearCompra(@PathVariable Long empresaId, @RequestBody CompraRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(compras.crearOrdenCompra(empresaId, request));
    }

    @GetMapping("/compras/ordenes")
    public ResponseEntity<List<CompraDto>> listarCompras(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(compras.listarCompras(empresaId));
    }

    @PostMapping("/compras/ordenes/{compraId}/recepciones")
    public ResponseEntity<RecepcionDto> recibirCompra(@PathVariable Long empresaId, @PathVariable Long compraId, @RequestBody RecepcionRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(compras.recibirCompra(empresaId, compraId, request));
    }

    @GetMapping("/compras/recepciones")
    public ResponseEntity<List<RecepcionDto>> listarRecepciones(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(compras.listarRecepciones(empresaId));
    }

    @PostMapping("/inventario/movimientos")
    public ResponseEntity<MovimientoInventarioDto> registrarMovimiento(@PathVariable Long empresaId, @RequestBody MovimientoInventarioRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(inventario.registrarMovimiento(empresaId, request));
    }

    @GetMapping("/inventario/movimientos")
    public ResponseEntity<List<MovimientoInventarioDto>> listarMovimientos(@PathVariable Long empresaId, @RequestParam(required = false) String bodega, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(inventario.listarMovimientos(empresaId, bodega));
    }

    @GetMapping("/inventario/existencias")
    public ResponseEntity<List<ExistenciaInventarioDto>> listarExistencias(@PathVariable Long empresaId, @RequestParam(required = false) String bodega, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(inventario.listarExistencias(empresaId, bodega));
    }

    @PostMapping("/tesoreria/cuentas-bancarias")
    public ResponseEntity<CuentaBancariaDto> crearCuenta(@PathVariable Long empresaId, @RequestBody CuentaBancariaRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.crearCuenta(empresaId, request));
    }

    @GetMapping("/tesoreria/cuentas-bancarias")
    public ResponseEntity<List<CuentaBancariaDto>> listarCuentas(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.listarCuentas(empresaId));
    }

    @GetMapping("/tesoreria/movimientos")
    public ResponseEntity<List<MovimientoTesoreriaDto>> listarMovimientosTesoreria(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.listarMovimientos(empresaId));
    }

    @PostMapping("/tesoreria/pagos-proveedor")
    public ResponseEntity<PagoProveedorDto> registrarPagoProveedor(@PathVariable Long empresaId, @RequestBody PagoProveedorRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.registrarPagoProveedor(empresaId, request));
    }

    @GetMapping("/tesoreria/pagos-proveedor")
    public ResponseEntity<List<PagoProveedorDto>> listarPagosProveedor(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.listarPagosProveedor(empresaId));
    }

    @PostMapping("/tesoreria/cobros-cliente")
    public ResponseEntity<CobroClienteDto> registrarCobroCliente(@PathVariable Long empresaId, @RequestBody CobroClienteRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.registrarCobroCliente(empresaId, request));
    }

    @GetMapping("/tesoreria/cobros-cliente")
    public ResponseEntity<List<CobroClienteDto>> listarCobrosCliente(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(tesoreria.listarCobrosCliente(empresaId));
    }

    @PostMapping("/activos")
    public ResponseEntity<ActivoDto> darAltaActivo(@PathVariable Long empresaId, @RequestBody ActivoRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(activos.darAlta(empresaId, request));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ActivoDto>> listarActivos(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(activos.listar(empresaId));
    }

    @PostMapping("/activos/{activoId}/depreciaciones")
    public ResponseEntity<DepreciacionDto> depreciarActivo(@PathVariable Long empresaId, @PathVariable Long activoId, @RequestBody(required = false) DepreciacionRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(activos.calcularDepreciacion(empresaId, activoId, request == null ? new DepreciacionRequestDto(null, null) : request));
    }

    @GetMapping("/activos/depreciaciones")
    public ResponseEntity<List<DepreciacionDto>> listarDepreciaciones(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(activos.listarDepreciaciones(empresaId));
    }

    @PostMapping("/activos/{activoId}/baja")
    public ResponseEntity<ActivoDto> darBajaActivo(@PathVariable Long empresaId, @PathVariable Long activoId, @RequestBody(required = false) BajaActivoRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(activos.darBaja(empresaId, activoId, request));
    }

    @GetMapping("/integraciones")
    public ResponseEntity<List<IntegracionErpConfigDto>> listarIntegraciones(@PathVariable Long empresaId, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(integraciones.listar(empresaId));
    }

    @PutMapping("/integraciones/{proveedor}")
    public ResponseEntity<IntegracionErpConfigDto> guardarIntegracion(@PathVariable Long empresaId, @PathVariable String proveedor, @RequestBody IntegracionErpConfigRequestDto request, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(integraciones.guardar(empresaId, proveedor, request));
    }

    @PostMapping("/integraciones/{proveedor}/sincronizar")
    public ResponseEntity<SincronizacionErpDto> sincronizar(@PathVariable Long empresaId, @PathVariable String proveedor, @RequestParam(defaultValue = "GENERAL") String tipo, Authentication auth) {
        autorizacion.verificar(empresaId, auth);
        return ResponseEntity.ok(integraciones.sincronizar(empresaId, proveedor, tipo));
    }
}
