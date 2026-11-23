package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPlanCuentasTest {

    @Mock
    private CuentaContableRepository cuentaRepository;

    private ServicioPlanCuentas servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioPlanCuentas(cuentaRepository);
    }

    @Test
    void asegurarCopiaCuentasGlobalesYRespetaPropias() {
        CuentaContableJpa cajaGlobal = cuenta(null, "110505", "Caja", "ACTIVO", "DEUDORA");
        CuentaContableJpa ventasGlobal = cuenta(null, "420505", "Ventas", "INGRESO", "ACREEDORA");
        ventasGlobal.setNivel(2);
        ventasGlobal.setVersionPuc("2024");
        ventasGlobal.setAceptaAuxiliar(true);
        CuentaContableJpa cajaPropia = cuenta(1L, "110505", "Caja propia", "ACTIVO", "DEUDORA");

        when(cuentaRepository.findByEmpresaId(1L)).thenReturn(List.of(cajaPropia));
        when(cuentaRepository.findByEmpresaIdIsNull()).thenReturn(List.of(cajaGlobal, ventasGlobal));
        when(cuentaRepository.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Map<String, CuentaContableJpa> resultado = servicio.asegurar(1L);

        assertEquals(2, resultado.size());
        assertSame(cajaPropia, resultado.get("110505"));
        assertEquals(1L, resultado.get("420505").getEmpresaId());
        assertEquals("Ventas", resultado.get("420505").getNombre());
        assertEquals("ACREEDORA", resultado.get("420505").getNaturaleza());
        assertEquals(2, resultado.get("420505").getNivel());
        assertEquals("2024", resultado.get("420505").getVersionPuc());
        assertTrue(resultado.get("420505").isAceptaAuxiliar());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CuentaContableJpa>> captor = ArgumentCaptor.forClass(List.class);
        verify(cuentaRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("420505", captor.getValue().get(0).getCodigo());
        assertEquals(1L, captor.getValue().get(0).getEmpresaId());
    }

    @Test
    void obtenerPriorizaCuentaPropia() {
        CuentaContableJpa propia = cuenta(1L, "110505", "Caja propia", "ACTIVO", "DEUDORA");
        when(cuentaRepository.findByEmpresaIdAndCodigo(1L, "110505")).thenReturn(Optional.of(propia));

        assertSame(propia, servicio.obtener(1L, "110505"));
    }

    @Test
    void obtenerUsaCuentaGlobalCuandoNoHayPropia() {
        CuentaContableJpa global = cuenta(null, "110505", "Caja", "ACTIVO", "DEUDORA");
        when(cuentaRepository.findByEmpresaIdAndCodigo(1L, "110505")).thenReturn(Optional.empty());
        when(cuentaRepository.findByEmpresaIdIsNullAndCodigo("110505")).thenReturn(Optional.of(global));

        assertSame(global, servicio.obtener(1L, "110505"));
    }

    @Test
    void obtenerFallaCuandoLaCuentaNoExiste() {
        when(cuentaRepository.findByEmpresaIdAndCodigo(1L, "999999")).thenReturn(Optional.empty());
        when(cuentaRepository.findByEmpresaIdIsNullAndCodigo("999999")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtener(1L, "999999"));

        assertEquals("Cuenta contable no configurada: 999999", error.getMessage());
    }

    private static CuentaContableJpa cuenta(Long empresaId, String codigo, String nombre,
                                            String tipo, String naturaleza) {
        CuentaContableJpa cuenta = new CuentaContableJpa();
        cuenta.setEmpresaId(empresaId);
        cuenta.setCodigo(codigo);
        cuenta.setNombre(nombre);
        cuenta.setTipo(tipo);
        cuenta.setNaturaleza(naturaleza);
        cuenta.setActiva(true);
        cuenta.setNivel(1);
        cuenta.setVersionPuc("2025");
        return cuenta;
    }
}
