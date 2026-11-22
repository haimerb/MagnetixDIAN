package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.CapacidadIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import com.magnetixdian.domain.integracion.ResultadoMigracionContable;
import com.magnetixdian.domain.integracion.SolicitudMigracionContable;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.infrastructure.persistence.MigracionContableDetalleJpa;
import com.magnetixdian.infrastructure.persistence.MigracionContableDetalleRepository;
import com.magnetixdian.infrastructure.persistence.MigracionContableJpa;
import com.magnetixdian.infrastructure.persistence.MigracionContableRepository;
import com.magnetixdian.interfaces.dto.MigracionContableDto;
import com.magnetixdian.interfaces.dto.ResultadoMigracionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioMigracionContableTest {

    private final IntegracionConfigRepository configuraciones = mock(IntegracionConfigRepository.class);
    private final RegistroIntegraciones registro = mock(RegistroIntegraciones.class);
    private final MigracionContableRepository migraciones = mock(MigracionContableRepository.class);
    private final MigracionContableDetalleRepository detalles = mock(MigracionContableDetalleRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        IntegracionConfigJpa config = new IntegracionConfigJpa();
        config.setEmpresa(empresa);
        config.setProveedor("SIIGO");
        config.setCredenciales("{}");
        config.setConfiguracion("{}");
        config.setActivo(true);
        when(configuraciones.findByEmpresaIdAndProveedor(1L, "SIIGO")).thenReturn(Optional.of(config));
        when(migraciones.save(any(MigracionContableJpa.class))).thenAnswer(invocation -> {
            MigracionContableJpa entidad = invocation.getArgument(0);
            if (entidad.getId() == null) {
                ReflectionTestUtils.setField(entidad, "id", 42L);
            }
            return entidad;
        });
        when(detalles.findByEmpresaIdAndProveedorAndTipoAndExternalId(
                anyLong(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void reprocesarNoDuplicaFilasDeStaging() {
        AdaptadorIntegracion adaptador = mock(AdaptadorIntegracion.class);
        Map<String, String> item = Map.of("externalId", "CTA-1", "codigo", "1001", "nombre", "Caja");
        when(adaptador.migrarPlanCuentas(any(), any()))
                .thenReturn(ResultadoMigracionContable.ok("PLAN_CUENTAS", List.of(item)));
        when(registro.obtener(ProveedorIntegracion.SIIGO)).thenReturn(adaptador);

        MigracionContableDetalleJpa yaMigrado = new MigracionContableDetalleJpa();
        yaMigrado.setEstado("EXITOSO");
        when(detalles.findByEmpresaIdAndProveedorAndTipoAndExternalId(1L, "SIIGO", "PLAN_CUENTAS", "CTA-1"))
                .thenReturn(Optional.empty(), Optional.of(yaMigrado));

        SolicitudMigracionContable solicitud = new SolicitudMigracionContable(
                List.of("PLAN_CUENTAS"), "2026-01", Map.of());
        MigracionContableDto primera = servicio().migrar(1L, ProveedorIntegracion.SIIGO, solicitud);
        MigracionContableDto segunda = servicio().migrar(1L, ProveedorIntegracion.SIIGO, solicitud);

        assertEquals(1, primera.exitosos());
        assertEquals(0, primera.omitidos());
        assertEquals(0, segunda.exitosos());
        assertEquals(1, segunda.omitidos());
        assertEquals(1, primera.total());
        verify(detalles, times(1)).save(any(MigracionContableDetalleJpa.class));
    }

    @Test
    void adaptadorSinCapacidadContableGeneraReporteConError() {
        when(registro.obtener(ProveedorIntegracion.SIIGO)).thenReturn(new AdaptadorSinCapacidades());

        MigracionContableDto resultado = servicio().migrar(1L, ProveedorIntegracion.SIIGO,
                new SolicitudMigracionContable(List.of("PLAN_CUENTAS"), null, Map.of()));

        assertEquals(1, resultado.errores());
        assertEquals("COMPLETADO_CON_ERRORES", resultado.estado());
        verify(detalles).save(any(MigracionContableDetalleJpa.class));
    }

    @Test
    void reporteIncluyeContadoresEItems() {
        MigracionContableJpa migracion = new MigracionContableJpa();
        ReflectionTestUtils.setField(migracion, "id", 42L);
        migracion.setEmpresaId(1L);
        migracion.setProveedor("SIIGO");
        migracion.setExitosos(2);
        migracion.setOmitidos(1);
        migracion.setErrores(1);
        migracion.setTotal(4);
        when(migraciones.findByIdAndEmpresaIdAndProveedor(42L, 1L, "SIIGO"))
                .thenReturn(Optional.of(migracion));
        when(detalles.findByMigracionIdOrderByCreatedAtAsc(42L))
                .thenReturn(List.of(detalle("CTA-1", "EXITOSO"), detalle("CTA-2", "ERROR")));

        ResultadoMigracionDto reporte = servicio().reporte(1L, ProveedorIntegracion.SIIGO, 42L);

        assertEquals(2, reporte.exitosos());
        assertEquals(1, reporte.omitidos());
        assertEquals(1, reporte.errores());
        assertEquals(4, reporte.total());
        assertEquals(2, reporte.items().size());
        assertTrue(reporte.items().stream().anyMatch(item -> "ERROR".equals(item.estado())));
        verify(detalles, atLeastOnce()).findByMigracionIdOrderByCreatedAtAsc(42L);
    }

    private MigracionContableDetalleJpa detalle(String externalId, String estado) {
        MigracionContableDetalleJpa detalle = new MigracionContableDetalleJpa();
        detalle.setMigracionId(42L);
        detalle.setEmpresaId(1L);
        detalle.setProveedor("SIIGO");
        detalle.setTipo("PLAN_CUENTAS");
        detalle.setExternalId(externalId);
        detalle.setEstado(estado);
        detalle.setPayload("{}");
        return detalle;
    }

    private ServicioMigracionContable servicio() {
        return new ServicioMigracionContable(configuraciones, registro, migraciones, detalles, mapper);
    }

    private static final class AdaptadorSinCapacidades implements AdaptadorIntegracion {

        @Override
        public ProveedorIntegracion proveedor() {
            return ProveedorIntegracion.SIIGO;
        }

        @Override
        public Set<CapacidadIntegracion> capacidades() {
            return Set.of();
        }

        @Override
        public boolean configurada(DatosConexionIntegracion datos) {
            return true;
        }

        @Override
        public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
            return ResultadoIntegracion.ok("ok", null);
        }
    }
}
