package com.magnetixdian.erp;

import com.magnetixdian.application.erp.PublicadorEventosErp;
import com.magnetixdian.application.erp.ServicioVentas;
import com.magnetixdian.domain.erp.evento.PedidoAprobadoEvent;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpOrdenVentaRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpPedidoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpPedidoRepository;
import com.magnetixdian.interfaces.dto.erp.PedidoRequestDto;
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
class ServicioVentasTest {

    @Mock
    ErpOrdenVentaRepository ordenes;
    @Mock
    ErpPedidoRepository pedidos;
    @Mock
    PublicadorEventosErp publicador;
    @InjectMocks
    ServicioVentas servicio;

    @Test
    void aprobarPedidoPublicaEvento() {
        ErpPedidoJpa pedido = new ErpPedidoJpa();
        pedido.setEmpresaId(1L);
        pedido.setNumero("PED-1");
        pedido.setClienteNombre("Cliente");
        pedido.setFecha(LocalDate.now());
        pedido.setEstado("BORRADOR");
        pedido.setSubtotal(new BigDecimal("100"));
        pedido.setImpuestos(new BigDecimal("19"));
        pedido.setTotal(new BigDecimal("119"));
        when(pedidos.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = servicio.aprobarPedido(1L, 10L);

        assertEquals("APROBADO", dto.estado());
        ArgumentCaptor<PedidoAprobadoEvent> cap = ArgumentCaptor.forClass(PedidoAprobadoEvent.class);
        verify(publicador).publicar(cap.capture());
        assertEquals(1L, cap.getValue().empresaId());
        assertEquals("PEDIDO_APROBADO", cap.getValue().tipo());
    }

    @Test
    void crearPedidoValidaTotal() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crearPedido(1L,
                new PedidoRequestDto("P1", null, "C1", "Cliente", LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1"), null)));
    }
}
