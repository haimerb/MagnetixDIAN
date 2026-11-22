package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioDocumentosRadianTest {

    @Mock
    private DocumentoRadianRepository repository;
    @Mock
    private DocumentoAsientoRepository documentoAsientoRepository;

    private ServicioDocumentosRadian servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioDocumentosRadian(repository, documentoAsientoRepository);
    }

    @Test
    void importarJsonEsIdempotentePorCufe() {
        String json = """
                [
                  {"direccion":"RECIBIDO","tipoDocumento":"01","cufeCude":"CUFE-1",
                   "consecutivo":"F-001","nitContraparte":"900123456",
                   "fecha":"2026-01-15","subtotal":1000.00,"impuestos":190.00,
                   "retenciones":50.00,"total":1190.00,"estadoRadian":"RECIBIDO"},
                  {"direccion":"RECIBIDO","tipoDocumento":"01","cufeCude":"CUFE-1",
                   "consecutivo":"F-001","fecha":"2026-01-15","total":1190.00}
                ]
                """;
        DocumentoRadianJpa existente = documento(1L, "CUFE-1");
        existente.setTotal(new BigDecimal("100.00"));
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existente));
        when(repository.save(any(DocumentoRadianJpa.class))).thenAnswer(invocation -> {
            DocumentoRadianJpa entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        List<DocumentoRadianDto> resultado = servicio.importar(1L, null,
                json.getBytes(StandardCharsets.UTF_8), "documentos.json");

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).id());
        assertEquals(1L, resultado.get(1).id());
        assertEquals(new BigDecimal("1190.00"), resultado.get(0).total());
        assertEquals(new BigDecimal("1190.00"), resultado.get(1).total());
        assertEquals("PENDIENTE", resultado.get(1).estadoContabilizacion());
        assertNull(resultado.get(1).asientoContableId());
        verify(repository, times(1)).save(any(DocumentoRadianJpa.class));
        verify(repository, never()).saveAndFlush(any(DocumentoRadianJpa.class));
    }

    @Test
    void importarCsvConEncabezados() {
        String csv = "direccion;tipoDocumento;cufeCude;consecutivo;fecha;subtotal;impuestos;retenciones;total\n"
                + "RECIBIDO;01;CUFE-2;F-002;2026-02-10;1000.00;190.00;0.00;1190.00\n";
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-2"))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentoRadianJpa.class))).thenAnswer(invocation -> {
            DocumentoRadianJpa entidad = invocation.getArgument(0);
            entidad.setId(2L);
            return entidad;
        });

        List<DocumentoRadianDto> resultado = servicio.importar(1L, "RECIBIDO",
                csv.getBytes(StandardCharsets.UTF_8), "documentos.csv");

        assertEquals(1, resultado.size());
        assertEquals("01", resultado.get(0).tipoDocumento());
        assertEquals(LocalDate.of(2026, 2, 10), resultado.get(0).fecha());
        assertEquals(new BigDecimal("190.00"), resultado.get(0).impuestos());
        verify(repository, never()).saveAndFlush(any(DocumentoRadianJpa.class));
    }

    @Test
    void importarRechazaTipoDocumentoInvalido() {
        String json = "[{\"tipoDocumento\":\"99\",\"cufeCude\":\"CUFE-3\","
                + "\"consecutivo\":\"F-003\",\"fecha\":\"2026-01-01\"}]";

        assertThrows(IllegalArgumentException.class, () -> servicio.importar(1L, "RECIBIDO",
                json.getBytes(StandardCharsets.UTF_8), "documentos.json"));
    }

    @Test
    void listarPasaFiltrosAlRepositorio() {
        when(repository.buscar(eq(1L), eq("EMITIDO"), eq("01"), eq("CAUSADA"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                .thenReturn(List.of(documento(7L, "CUFE-7")));

        List<DocumentoRadianDto> resultado = servicio.listar(1L, "EMITIDO", "01", "CAUSADA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(1, resultado.size());
        assertEquals("CUFE-7", resultado.get(0).cufeCude());
        assertEquals("PENDIENTE", resultado.get(0).estadoContabilizacion());
        assertNull(resultado.get(0).asientoContableId());
        verify(repository).buscar(1L, "EMITIDO", "01", "CAUSADA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    }

    @Test
    void listarResuelveElAsientoVinculado() {
        when(repository.buscar(eq(1L), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(documento(7L, "CUFE-7")));
        when(documentoAsientoRepository.findFirstByDocumentoRadianIdOrderByIdDesc(7L))
                .thenReturn(Optional.of(vinculo(20L)));

        List<DocumentoRadianDto> resultado = servicio.listar(1L, null, null, null, null, null);

        assertEquals("CONTABILIZADO", resultado.get(0).estadoContabilizacion());
        assertEquals(20L, resultado.get(0).asientoContableId());
    }

    @Test
    void importarResuelveElAsientoVinculadoDelDocumentoExistente() {
        String json = "[{\"direccion\":\"RECIBIDO\",\"tipoDocumento\":\"01\",\"cufeCude\":\"CUFE-1\","
                + "\"consecutivo\":\"F-001\",\"fecha\":\"2026-01-15\",\"total\":1190.00}]";
        when(repository.findByEmpresaIdAndDireccionAndCufeCude(1L, "RECIBIDO", "CUFE-1"))
                .thenReturn(Optional.of(documento(1L, "CUFE-1")));
        when(documentoAsientoRepository.findFirstByDocumentoRadianIdOrderByIdDesc(1L))
                .thenReturn(Optional.of(vinculo(20L)));

        List<DocumentoRadianDto> resultado = servicio.importar(1L, null,
                json.getBytes(StandardCharsets.UTF_8), "documentos.json");

        assertEquals("CONTABILIZADO", resultado.get(0).estadoContabilizacion());
        assertEquals(20L, resultado.get(0).asientoContableId());
        verify(repository, never()).save(any(DocumentoRadianJpa.class));
    }

    private static DocumentoAsientoJpa vinculo(Long asientoId) {
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setId(asientoId);
        DocumentoAsientoJpa vinculo = new DocumentoAsientoJpa();
        vinculo.setDocumentoRadianId(7L);
        vinculo.setAsiento(asiento);
        return vinculo;
    }

    private static DocumentoRadianJpa documento(Long id, String cufe) {
        DocumentoRadianJpa entidad = new DocumentoRadianJpa();
        entidad.setId(id);
        entidad.setEmpresaId(1L);
        entidad.setDireccion("RECIBIDO");
        entidad.setTipoDocumento("01");
        entidad.setCufeCude(cufe);
        entidad.setConsecutivo("F-001");
        entidad.setFecha(LocalDate.of(2026, 1, 15));
        entidad.setSubtotal(new BigDecimal("1000.00"));
        entidad.setImpuestos(new BigDecimal("190.00"));
        entidad.setTotal(new BigDecimal("1190.00"));
        entidad.setEstadoCausacion("PENDIENTE");
        return entidad;
    }
}
