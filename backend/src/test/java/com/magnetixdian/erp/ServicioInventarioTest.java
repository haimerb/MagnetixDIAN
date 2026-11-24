package com.magnetixdian.erp;

import com.magnetixdian.application.erp.ServicioInventario;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioMovimientoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioMovimientoRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioSaldoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioSaldoRepository;
import com.magnetixdian.interfaces.dto.erp.MovimientoInventarioRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioInventarioTest {

    @Mock
    ErpInventarioMovimientoRepository movimientos;
    @Mock
    ErpInventarioSaldoRepository saldos;
    @InjectMocks
    ServicioInventario servicio;

    @Test
    void entradaCalculaCostoPromedio() {
        ErpInventarioSaldoJpa saldo = new ErpInventarioSaldoJpa();
        saldo.setEmpresaId(1L);
        saldo.setBodega("B1");
        saldo.setProductoId("P1");
        saldo.setExistencia(new BigDecimal("10.0000"));
        saldo.setCostoPromedio(new BigDecimal("100.0000"));
        when(saldos.findByEmpresaIdAndBodegaAndProductoId(1L, "B1", "P1")).thenReturn(Optional.of(saldo));
        when(saldos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientos.save(any())).thenAnswer(i -> {
            ErpInventarioMovimientoJpa m = i.getArgument(0);
            return m;
        });

        var dto = servicio.registrarMovimiento(1L, new MovimientoInventarioRequestDto("ENTRADA", "B1", "P1", "Prod", new BigDecimal("10"), new BigDecimal("200"), LocalDate.now(), "COMPRA", "1", null));

        assertEquals(new BigDecimal("20.0000"), dto.existenciaNueva());
        assertEquals(new BigDecimal("150.0000"), dto.costoPromedio());
        ArgumentCaptor<ErpInventarioSaldoJpa> cap = ArgumentCaptor.forClass(ErpInventarioSaldoJpa.class);
        verify(saldos).save(cap.capture());
        assertEquals(new BigDecimal("150.0000"), cap.getValue().getCostoPromedio());
    }

    @Test
    void salidaConExistenciaInsuficienteFalla() {
        ErpInventarioSaldoJpa saldo = new ErpInventarioSaldoJpa();
        saldo.setEmpresaId(1L);
        saldo.setBodega("B1");
        saldo.setProductoId("P1");
        saldo.setExistencia(new BigDecimal("2.0000"));
        saldo.setCostoPromedio(new BigDecimal("50.0000"));
        when(saldos.findByEmpresaIdAndBodegaAndProductoId(1L, "B1", "P1")).thenReturn(Optional.of(saldo));

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMovimiento(1L,
                new MovimientoInventarioRequestDto("SALIDA", "B1", "P1", "Prod", new BigDecimal("3"), null, LocalDate.now(), null, null, null)));
    }
}
