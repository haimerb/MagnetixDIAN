package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CertificadoImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.CertificadoImpuestoRepository;
import com.magnetixdian.infrastructure.persistence.TerceroContableJpa;
import com.magnetixdian.infrastructure.persistence.TerceroContableRepository;
import com.magnetixdian.interfaces.dto.CertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCertificadoImpuestoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCertificadosImpuestoTest {

    @Mock
    private CertificadoImpuestoRepository repository;
    @Mock
    private TerceroContableRepository terceroRepository;

    private ServicioCertificadosImpuesto servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioCertificadosImpuesto(repository, terceroRepository);
    }

    @Test
    void emitirCreaCertificadoNormalizado() {
        when(terceroRepository.findById(9L)).thenReturn(Optional.of(tercero(1L)));
        when(repository.findByEmpresaIdAndTerceroIdAndImpuestoCodigoAndAnioAndNumero(
                1L, 9L, "IVA", 2026, "CERT-1")).thenReturn(Optional.empty());
        when(repository.save(any(CertificadoImpuestoJpa.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CertificadoImpuestoDto resultado = servicio.emitir(1L, new SolicitudCertificadoImpuestoDto(
                9L, " iva ", 2026, " CERT-1 ", new BigDecimal("100.00"), LocalDate.of(2026, 3, 31)));

        assertEquals("IVA", resultado.impuestoCodigo());
        assertEquals("CERT-1", resultado.numero());
        assertEquals(0, new BigDecimal("100.00").compareTo(resultado.valor()));
        assertEquals(LocalDate.of(2026, 3, 31), resultado.fechaEmision());

        ArgumentCaptor<CertificadoImpuestoJpa> captor = ArgumentCaptor.forClass(CertificadoImpuestoJpa.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, captor.getValue().getEmpresaId());
        assertEquals(9L, captor.getValue().getTerceroId());
        assertEquals("IVA", captor.getValue().getImpuestoCodigo());
        assertEquals(2026, captor.getValue().getAnio());
    }

    @Test
    void emitirReutilizaCertificadoExistente() {
        when(terceroRepository.findById(9L)).thenReturn(Optional.of(tercero(1L)));
        when(repository.findByEmpresaIdAndTerceroIdAndImpuestoCodigoAndAnioAndNumero(
                1L, 9L, "IVA", 2026, "CERT-1")).thenReturn(Optional.of(certificado()));

        CertificadoImpuestoDto resultado = servicio.emitir(1L, new SolicitudCertificadoImpuestoDto(
                9L, "iva", 2026, "CERT-1", new BigDecimal("100.00"), LocalDate.of(2026, 3, 31)));

        assertEquals("CERT-1", resultado.numero());
        verify(repository, never()).save(any());
    }

    @Test
    void emitirRechazaTerceroDeOtraEmpresa() {
        when(terceroRepository.findById(9L)).thenReturn(Optional.of(tercero(2L)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.emitir(1L, new SolicitudCertificadoImpuestoDto(
                        9L, "IVA", 2026, "CERT-1", BigDecimal.ZERO, LocalDate.of(2026, 3, 31))));

        assertEquals("Tercero no encontrado.", error.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void emitirRechazaSolicitudesIncompletas() {
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(1L,
                new SolicitudCertificadoImpuestoDto(
                        null, "IVA", 2026, "CERT-1", BigDecimal.ZERO, LocalDate.of(2026, 3, 31))));
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(1L,
                new SolicitudCertificadoImpuestoDto(
                        9L, " ", 2026, "CERT-1", BigDecimal.ZERO, LocalDate.of(2026, 3, 31))));
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(1L,
                new SolicitudCertificadoImpuestoDto(
                        9L, "IVA", 2026, " ", BigDecimal.ZERO, LocalDate.of(2026, 3, 31))));
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(1L,
                new SolicitudCertificadoImpuestoDto(
                        9L, "IVA", 2026, "CERT-1", new BigDecimal("-1.00"), LocalDate.of(2026, 3, 31))));
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(1L,
                new SolicitudCertificadoImpuestoDto(
                        9L, "IVA", null, "CERT-1", BigDecimal.ZERO, LocalDate.of(2026, 3, 31))));
    }

    @Test
    void listarFiltraPorAnio() {
        when(repository.findByEmpresaIdAndAnioOrderByFechaEmisionDescIdDesc(1L, 2026))
                .thenReturn(List.of(certificado()));

        List<CertificadoImpuestoDto> resultado = servicio.listar(1L, 2026);

        assertEquals(1, resultado.size());
        assertEquals("CERT-1", resultado.get(0).numero());
        assertNotNull(resultado.get(0).fechaEmision());
    }

    private static TerceroContableJpa tercero(Long empresaId) {
        TerceroContableJpa tercero = new TerceroContableJpa();
        tercero.setEmpresaId(empresaId);
        tercero.setTipoDocumento("NIT");
        tercero.setNumeroDocumento("900123456");
        tercero.setNombre("Cliente prueba");
        return tercero;
    }

    private static CertificadoImpuestoJpa certificado() {
        CertificadoImpuestoJpa certificado = new CertificadoImpuestoJpa();
        certificado.setEmpresaId(1L);
        certificado.setTerceroId(9L);
        certificado.setImpuestoCodigo("IVA");
        certificado.setAnio(2026);
        certificado.setNumero("CERT-1");
        certificado.setValor(new BigDecimal("100.00"));
        certificado.setFechaEmision(LocalDate.of(2026, 3, 31));
        return certificado;
    }
}
