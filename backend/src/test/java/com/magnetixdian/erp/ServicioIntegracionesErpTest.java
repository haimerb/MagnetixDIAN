package com.magnetixdian.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.erp.ServicioIntegracionesErp;
import com.magnetixdian.domain.erp.modelo.ProveedorErp;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.integracion.erp.AdaptadorSiigoErp;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpIntegracionConfigJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpIntegracionConfigRepository;
import com.magnetixdian.interfaces.dto.erp.IntegracionErpConfigRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioIntegracionesErpTest {

    @Mock
    ErpIntegracionConfigRepository configs;

    @Test
    void guardaCredencialesCifradasYSimulaSinCredenciales() {
        CifradorAes cifrador = new CifradorAes("test-key-erp");
        ObjectMapper mapper = new ObjectMapper();
        ServicioIntegracionesErp servicio = new ServicioIntegracionesErp(configs, cifrador, mapper, List.of());
        servicio = new ServicioIntegracionesErp(configs, cifrador, mapper, List.of(new AdaptadorSiigoErp(servicio)));

        when(configs.findByEmpresaIdAndProveedorAndTipo(1L, "SIIGO", "ERP")).thenReturn(Optional.empty());
        when(configs.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = servicio.guardar(1L, "siigo", new IntegracionErpConfigRequestDto(true, Map.of("username", "u", "accessKey", "k"), Map.of()));
        assertTrue(dto.configurada());
        assertEquals("CONFIGURADA", dto.estado());
        assertFalse(dto.credencialesConfiguradas().isEmpty());

        var sync = servicio.sincronizar(1L, "siigo", "CLIENTES");
        assertTrue(sync.exitoso());
    }

    @Test
    void fallaSinCredencialesRequeridasCuandoHayConfigParcial() {
        CifradorAes cifrador = new CifradorAes("test-key-erp");
        ObjectMapper mapper = new ObjectMapper();
        ServicioIntegracionesErp servicioBase = new ServicioIntegracionesErp(configs, cifrador, mapper, List.of());
        ServicioIntegracionesErp servicio = new ServicioIntegracionesErp(configs, cifrador, mapper, List.of(new AdaptadorSiigoErp(servicioBase)));

        ErpIntegracionConfigJpa cfg = new ErpIntegracionConfigJpa();
        cfg.setEmpresaId(1L);
        cfg.setProveedor("SIIGO");
        cfg.setTipo("ERP");
        cfg.setCredencialesCifradas(cifrador.cifrar("{\"username\":\"u\"}"));
        when(configs.findByEmpresaIdAndProveedorAndTipo(1L, "SIIGO", "ERP")).thenReturn(Optional.of(cfg));
        when(configs.save(any())).thenAnswer(i -> i.getArgument(0));

        var sync = servicio.sincronizar(1L, "siigo", "CLIENTES");
        assertFalse(sync.exitoso());
        assertTrue(sync.mensaje().contains("Faltan credenciales"));
    }
}
