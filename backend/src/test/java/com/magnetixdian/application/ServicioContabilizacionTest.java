package com.magnetixdian.application;

import com.magnetixdian.domain.contabilidad.PlanCuentas;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.AsientoContableLineaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioContabilizacionTest {

    @Mock
    private DocumentoRadianRepository documentoRepository;
    @Mock
    private CuentaContableRepository cuentaRepository;
    @Mock
    private AsientoContableRepository asientoRepository;

    private ServicioContabilizacion servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioContabilizacion(documentoRepository, cuentaRepository, asientoRepository);
    }

    @Test
    void emitido01GeneraAsientoBalanceadoYCopiaPlanCuentas() {
        DocumentoRadianJpa documento = documento("EMITIDO", "01",
                "1000.00", "190.00", "50.00", "1190.00");
        prepararContabilizacion(documento);

        AsientoContableDto resultado = servicio.contabilizar(1L, 5L);

        assertEquals(new BigDecimal("1240.00"), resultado.totalDebito());
        assertEquals(new BigDecimal("1240.00"), resultado.totalCredito());
        assertEquals(new BigDecimal("1190.00"), linea(resultado, PlanCuentas.CLIENTES).debito());
        assertEquals(new BigDecimal("1000.00"), linea(resultado, PlanCuentas.VENTAS).credito());
        assertEquals(new BigDecimal("190.00"), linea(resultado, PlanCuentas.IVA_GENERADO).credito());
        assertEquals(new BigDecimal("50.00"), linea(resultado, PlanCuentas.RETENCIONES).debito());
        assertEquals("CONTABILIZADA", documento.getEstadoCausacion());
        verify(cuentaRepository).saveAll(anyList());
        verify(asientoRepository).saveAndFlush(any(AsientoContableJpa.class));
    }

    @Test
    void recibido01GeneraAsientoBalanceado() {
        DocumentoRadianJpa documento = documento("RECIBIDO", "01",
                "1000.00", "190.00", "50.00", "1190.00");
        prepararContabilizacion(documento);

        AsientoContableDto resultado = servicio.contabilizar(1L, 5L);

        assertEquals(new BigDecimal("1190.00"), resultado.totalDebito());
        assertEquals(new BigDecimal("1190.00"), resultado.totalCredito());
        assertEquals(new BigDecimal("1000.00"), linea(resultado, PlanCuentas.COMPRAS).debito());
        assertEquals(new BigDecimal("190.00"), linea(resultado, PlanCuentas.IVA_DESCONTABLE).debito());
        assertEquals(new BigDecimal("50.00"), linea(resultado, PlanCuentas.RETENCION_FUENTE).credito());
        assertEquals(new BigDecimal("1140.00"), linea(resultado, PlanCuentas.PROVEEDORES).credito());
    }

    @Test
    void notaCreditoEmitidaInvierteVentas() {
        DocumentoRadianJpa documento = documento("EMITIDO", "91",
                "1000.00", "190.00", "0.00", "1190.00");
        prepararContabilizacion(documento);

        AsientoContableDto resultado = servicio.contabilizar(1L, 5L);

        assertEquals(new BigDecimal("1000.00"),
                linea(resultado, PlanCuentas.DEVOLUCIONES_VENTAS).debito());
        assertEquals(new BigDecimal("1190.00"), linea(resultado, PlanCuentas.CLIENTES).credito());
        assertEquals(resultado.totalDebito(), resultado.totalCredito());
    }

    @Test
    void documento05UsaProveedores() {
        DocumentoRadianJpa documento = documento("EMITIDO", "05",
                "1000.00", "190.00", "0.00", "1190.00");
        prepararContabilizacion(documento);

        AsientoContableDto resultado = servicio.contabilizar(1L, 5L);

        assertEquals(new BigDecimal("1190.00"), linea(resultado, PlanCuentas.PROVEEDORES).credito());
        assertEquals(resultado.totalDebito(), resultado.totalCredito());
    }

    @Test
    void contabilizarDosVecesNoCreaOtroAsiento() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(
                Optional.of(documento("EMITIDO", "01", "1000.00", "190.00", "0.00", "1190.00")));
        AsientoContableJpa previo = asiento();
        when(asientoRepository.findByEmpresaIdAndDocumentoTipoAndDocumentoIdAndTipoEvento(
                1L, "RADIAN", 5L, "CAUSACION")).thenReturn(Optional.of(previo));

        AsientoContableDto resultado = servicio.contabilizar(1L, 5L);

        assertEquals(11L, resultado.id());
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
        verify(cuentaRepository, never()).saveAll(anyList());
    }

    @Test
    void retencionesSuperioresAlTotalFallan() {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(
                Optional.of(documento("EMITIDO", "01", "100.00", "19.00", "150.00", "119.00")));
        when(asientoRepository.findByEmpresaIdAndDocumentoTipoAndDocumentoIdAndTipoEvento(
                1L, "RADIAN", 5L, "CAUSACION")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> servicio.contabilizar(1L, 5L));
        verify(asientoRepository, never()).saveAndFlush(any(AsientoContableJpa.class));
    }

    private void prepararContabilizacion(DocumentoRadianJpa documento) {
        when(documentoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(documento));
        when(asientoRepository.findByEmpresaIdAndDocumentoTipoAndDocumentoIdAndTipoEvento(
                1L, "RADIAN", 5L, "CAUSACION")).thenReturn(Optional.empty());
        when(cuentaRepository.findByEmpresaId(1L)).thenReturn(List.of());
        when(cuentaRepository.findByEmpresaIdIsNull()).thenReturn(cuentasGlobales());
        when(cuentaRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<CuentaContableJpa> guardadas = invocation.getArgument(0);
            long id = 100L;
            for (CuentaContableJpa cuenta : guardadas) {
                cuenta.setId(id++);
            }
            return guardadas;
        });
        when(asientoRepository.saveAndFlush(any(AsientoContableJpa.class))).thenAnswer(invocation -> {
            AsientoContableJpa asiento = invocation.getArgument(0);
            asiento.setId(20L);
            return asiento;
        });
    }

    private static List<CuentaContableJpa> cuentasGlobales() {
        String[] codigos = {
                PlanCuentas.CLIENTES, PlanCuentas.PROVEEDORES, PlanCuentas.VENTAS,
                PlanCuentas.DEVOLUCIONES_VENTAS, PlanCuentas.COMPRAS,
                PlanCuentas.DEVOLUCIONES_COMPRAS, PlanCuentas.IVA_GENERADO,
                PlanCuentas.IVA_DESCONTABLE, PlanCuentas.RETENCIONES,
                PlanCuentas.RETENCION_FUENTE, PlanCuentas.CAJA, PlanCuentas.AJUSTES
        };
        List<CuentaContableJpa> cuentas = new ArrayList<>();
        long id = 1L;
        for (String codigo : codigos) {
            CuentaContableJpa cuenta = new CuentaContableJpa();
            cuenta.setId(id++);
            cuenta.setCodigo(codigo);
            cuenta.setNombre(PlanCuentas.nombre(codigo));
            cuenta.setTipo("OTRO");
            cuentas.add(cuenta);
        }
        return cuentas;
    }

    private static DocumentoRadianJpa documento(String direccion, String tipo, String subtotal,
                                                String impuestos, String retenciones, String total) {
        DocumentoRadianJpa entidad = new DocumentoRadianJpa();
        entidad.setId(5L);
        entidad.setEmpresaId(1L);
        entidad.setDireccion(direccion);
        entidad.setTipoDocumento(tipo);
        entidad.setCufeCude("CUFE-5");
        entidad.setConsecutivo("F-005");
        entidad.setFecha(LocalDate.of(2026, 2, 20));
        entidad.setSubtotal(new BigDecimal(subtotal));
        entidad.setImpuestos(new BigDecimal(impuestos));
        entidad.setRetenciones(new BigDecimal(retenciones));
        entidad.setTotal(new BigDecimal(total));
        return entidad;
    }

    private static AsientoContableJpa asiento() {
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(11L);
        asiento.setEmpresaId(1L);
        asiento.setNumero("AS-1-5-CAUSACION");
        asiento.setFecha(LocalDate.of(2026, 2, 20));
        asiento.setDocumentoTipo("RADIAN");
        asiento.setDocumentoId(5L);
        asiento.setTipoEvento("CAUSACION");
        asiento.setTotalDebito(new BigDecimal("1190.00"));
        asiento.setTotalCredito(new BigDecimal("1190.00"));
        return asiento;
    }

    private static AsientoContableLineaDto linea(AsientoContableDto asiento, String codigo) {
        return asiento.lineas().stream()
                .filter(linea -> codigo.equals(linea.codigoCuenta()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se generó la cuenta " + codigo));
    }
}
