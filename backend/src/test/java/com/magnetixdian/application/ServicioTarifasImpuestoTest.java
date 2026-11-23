package com.magnetixdian.application;

import com.magnetixdian.domain.rule.CatalogoConceptos;
import com.magnetixdian.domain.uvt.UvtManager;
import com.magnetixdian.infrastructure.persistence.ImpuestoTarifaJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoTarifaRepository;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioTarifasImpuestoTest {

    @Mock
    private ImpuestoTarifaRepository repository;
    @Mock
    private UvtManager uvtManager;

    private ServicioTarifasImpuesto servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioTarifasImpuesto(repository, uvtManager, new CatalogoConceptos());
    }

    @Test
    void calculaRetencionSegunVigenciaYConcepto() {
        when(repository.findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc("05", 2026))
                .thenReturn(List.of(tarifa("05", 2026, "2004", "0.0040", "3", null)));
        when(uvtManager.uvtParaAnio(2026)).thenReturn(new BigDecimal("52374.00"));

        ResultadoCalculoImpuestoDto resultado = servicio.calcular(
                "05", 2026, "2004", new BigDecimal("1000000.00"));

        assertTrue(resultado.aplica());
        assertEquals(0, new BigDecimal("157122.00").compareTo(resultado.baseMinima()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(resultado.valor()));
    }

    @Test
    void noAplicaRetencionCuandoBaseEsInferiorAlUmbralUvt() {
        when(repository.findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc("05", 2026))
                .thenReturn(List.of(tarifa("05", 2026, "2004", "0.0040", "3", null)));
        when(uvtManager.uvtParaAnio(2026)).thenReturn(new BigDecimal("52374.00"));

        ResultadoCalculoImpuestoDto resultado = servicio.calcular(
                "05", 2026, "2004", new BigDecimal("100000.00"));

        assertFalse(resultado.aplica());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.valor()));
    }

    @Test
    void calculaIvaConUvtDeAnioAnterior() {
        when(repository.findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc("01", 2025))
                .thenReturn(List.of(tarifa("01", 2025, null, "0.1900", "0", null)));
        when(uvtManager.uvtParaAnio(2025)).thenReturn(new BigDecimal("49799.00"));

        ResultadoCalculoImpuestoDto resultado = servicio.calcular(
                "01", 2025, null, new BigDecimal("1000000.00"));

        assertEquals(0, new BigDecimal("190000.00").compareTo(resultado.valor()));
    }

    @Test
    void rechazaConceptoNoCatalogado() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcular("05", 2026, "9999", new BigDecimal("1000000.00")));
    }

    private ImpuestoTarifaJpa tarifa(String impuesto,
                                     Integer anio,
                                     String concepto,
                                     String porcentaje,
                                     String minUvt,
                                     String maxUvt) {
        ImpuestoTarifaJpa tarifa = new ImpuestoTarifaJpa();
        tarifa.setImpuestoCodigo(impuesto);
        tarifa.setNombre("Tarifa " + impuesto);
        tarifa.setAnio(anio);
        tarifa.setConceptoCodigo(concepto);
        tarifa.setPorcentaje(new BigDecimal(porcentaje));
        tarifa.setBaseMinUvt(new BigDecimal(minUvt));
        tarifa.setBaseMaxUvt(maxUvt == null ? null : new BigDecimal(maxUvt));
        tarifa.setActivo(true);
        return tarifa;
    }
}
