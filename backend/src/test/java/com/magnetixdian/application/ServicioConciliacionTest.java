package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.ConciliacionJpa;
import com.magnetixdian.infrastructure.persistence.ConciliacionRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.ConciliacionLineaDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioConciliacionTest {

    @Mock
    private DocumentoRadianRepository documentoRepository;
    @Mock
    private AsientoContableRepository asientoRepository;
    @Mock
    private ConciliacionRepository conciliacionRepository;

    private ServicioConciliacion servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioConciliacion(documentoRepository, asientoRepository, conciliacionRepository);
    }

    @Test
    void documentoConciliadoNoGeneraDiferencias() {
        when(documentoRepository.buscar(1L, null, null, null, null, null))
                .thenReturn(List.of(documento()));
        when(asientoRepository.findByEmpresaIdAndDocumentoTipo(1L, "RADIAN"))
                .thenReturn(List.of(asiento("1190.00", "190.00", "50.00")));
        when(conciliacionRepository.saveAndFlush(any(ConciliacionJpa.class))).thenAnswer(invocation -> {
            ConciliacionJpa conciliacion = invocation.getArgument(0);
            conciliacion.setId(3L);
            return conciliacion;
        });

        ResultadoConciliacionDto resultado = servicio.conciliar(1L, null, null, null, null);

        assertEquals(1, resultado.coincidencias());
        assertEquals(0, resultado.diferencias());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.valorDiferencia());
        assertEquals(3, resultado.lineas().size());
        assertTrue(resultado.lineas().stream().allMatch(linea -> "COINCIDE".equals(linea.resultado())));
    }

    @Test
    void diferenciaEnTotalTieneSeveridadError() {
        when(documentoRepository.buscar(1L, null, null, null, null, null))
                .thenReturn(List.of(documento()));
        when(asientoRepository.findByEmpresaIdAndDocumentoTipo(1L, "RADIAN"))
                .thenReturn(List.of(asiento("1200.00", "190.00", "50.00")));
        when(conciliacionRepository.saveAndFlush(any(ConciliacionJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoConciliacionDto resultado = servicio.conciliar(1L, null, null, null, null);

        ConciliacionLineaDto lineaTotal = resultado.lineas().stream()
                .filter(linea -> "TOTAL".equals(linea.campo()))
                .findFirst().orElseThrow();
        assertEquals("DIFERENCIA", lineaTotal.resultado());
        assertEquals("ERROR", lineaTotal.severidad());
        assertEquals(new BigDecimal("10.00"), lineaTotal.diferencia());
        assertEquals(1, resultado.diferencias());
        assertEquals(new BigDecimal("10.00"), resultado.valorDiferencia());
    }

    @Test
    void documentoSinAsientoSeReporta() {
        when(documentoRepository.buscar(1L, null, null, null, null, null))
                .thenReturn(List.of(documento()));
        when(asientoRepository.findByEmpresaIdAndDocumentoTipo(1L, "RADIAN"))
                .thenReturn(List.of());
        when(conciliacionRepository.saveAndFlush(any(ConciliacionJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoConciliacionDto resultado = servicio.conciliar(1L, null, null, null, null);

        assertEquals(1, resultado.documentosSinAsiento());
        assertEquals(1, resultado.lineas().size());
        assertEquals("SIN_ASIENTO", resultado.lineas().get(0).resultado());
        assertEquals("ERROR", resultado.lineas().get(0).severidad());
    }

    private static DocumentoRadianJpa documento() {
        DocumentoRadianJpa entidad = new DocumentoRadianJpa();
        entidad.setId(5L);
        entidad.setEmpresaId(1L);
        entidad.setDireccion("EMITIDO");
        entidad.setTipoDocumento("01");
        entidad.setCufeCude("CUFE-5");
        entidad.setConsecutivo("F-005");
        entidad.setFecha(LocalDate.of(2026, 2, 20));
        entidad.setSubtotal(new BigDecimal("1000.00"));
        entidad.setImpuestos(new BigDecimal("190.00"));
        entidad.setRetenciones(new BigDecimal("50.00"));
        entidad.setTotal(new BigDecimal("1190.00"));
        return entidad;
    }

    private static AsientoContableJpa asiento(String total, String impuestos, String retenciones) {
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(8L);
        asiento.setEmpresaId(1L);
        asiento.setDocumentoTipo("RADIAN");
        asiento.setDocumentoId(5L);
        asiento.setValorTotal(new BigDecimal(total));
        asiento.setValorImpuestos(new BigDecimal(impuestos));
        asiento.setValorRetenciones(new BigDecimal(retenciones));
        asiento.setTotalDebito(new BigDecimal("1240.00"));
        asiento.setTotalCredito(new BigDecimal("1240.00"));
        return asiento;
    }
}
