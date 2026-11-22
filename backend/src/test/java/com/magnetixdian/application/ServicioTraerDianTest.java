package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.dian.ArchivoListadoDian;
import com.magnetixdian.domain.dian.OrigenListadoDian;
import com.magnetixdian.domain.dian.ProveedorListadosDian;
import com.magnetixdian.domain.dian.SolicitudListadoDian;
import com.magnetixdian.infrastructure.dian.ArchivoDianAdapter;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigJpa;
import com.magnetixdian.infrastructure.persistence.IntegracionConfigRepository;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.ResultadoTraerDianDto;
import com.magnetixdian.interfaces.dto.SolicitudTraerDianTokenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioTraerDianTest {

    @Mock
    private ServicioDocumentosRadian documentos;
    @Mock
    private IntegracionConfigRepository configuraciones;
    @Mock
    private EmpresaRepository empresas;
    @Mock
    private AuditoriaService auditoria;

    private final ObjectMapper mapper = new ObjectMapper();
    private ServicioTraerDian servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioTraerDian(List.of(new ArchivoDianAdapter()), documentos,
                configuraciones, empresas, auditoria, mapper);
    }

    @Test
    void porArchivoImportaListadoExportadoPorLaDian() {
        byte[] csv = ("direccion;tipoDocumento;cufeCude;consecutivo;fecha;total\n"
                + "RECIBIDO;01;CUFE-100;F-100;2026-01-10;1190.00\n")
                .getBytes(StandardCharsets.UTF_8);
        when(documentos.importar(eq(1L), eq("RECIBIDO"), any(byte[].class), eq("listado.csv")))
                .thenReturn(List.of(documento()));

        ResultadoTraerDianDto resultado = servicio.porArchivo(1L, csv, "listado.csv", "RECIBIDO",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);

        assertEquals("ARCHIVO", resultado.origen());
        assertEquals("listado.csv", resultado.archivo());
        assertEquals(1, resultado.documentos());
        verify(auditoria).registrarEnEmpresa(eq(1L), isNull(), eq("TRAER_DIAN"), eq("dian"), eq(1L), anyString());
    }

    @Test
    void porTokenGuardaCredencialCifradaYTraeDocumentos() {
        ProveedorListadosDian falso = new ProveedorListadosDian() {
            @Override
            public OrigenListadoDian origen() {
                return OrigenListadoDian.TOKEN;
            }

            @Override
            public boolean soporta(SolicitudListadoDian solicitud) {
                return "token-secreto".equals(solicitud.token());
            }

            @Override
            public ArchivoListadoDian obtener(SolicitudListadoDian solicitud) {
                return new ArchivoListadoDian("listado-dian.xlsx",
                        "direccion;tipoDocumento;cufeCude;consecutivo;fecha;total\n".getBytes(StandardCharsets.UTF_8));
            }
        };
        ServicioTraerDian conToken = new ServicioTraerDian(List.of(falso), documentos,
                configuraciones, empresas, auditoria, mapper);
        IntegracionConfigJpa config = new IntegracionConfigJpa();
        when(configuraciones.findByEmpresaIdAndProveedor(1L, "DIAN")).thenReturn(Optional.of(config));
        when(empresas.findById(1L)).thenReturn(Optional.of(mock(EmpresaJpa.class)));
        when(documentos.importar(eq(1L), eq("RECIBIDO"), any(byte[].class), eq("listado-dian.xlsx")))
                .thenReturn(List.of(documento()));

        ResultadoTraerDianDto resultado = conToken.porToken(1L, new SolicitudTraerDianTokenDto(
                "token-secreto", "RECIBIDO", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                null, null, true));

        assertEquals("TOKEN", resultado.origen());
        assertEquals("CONECTADO", config.getEstado());
        verify(configuraciones, atLeastOnce()).save(config);
    }

    private static DocumentoRadianDto documento() {
        return new DocumentoRadianDto(1L, 1L, "RECIBIDO", "01", "CUFE-100", "F-100", "900123456",
                "Empresa Demo", LocalDate.of(2026, 1, 10), null, null, null, null,
                "PENDIENTE", "PENDIENTE", null, null, null, List.of(), List.of());
    }
}
