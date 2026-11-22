package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaBrandingJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaBrandingRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.BrandingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioBrandingTest {

    private EmpresaBrandingRepository repository;
    private EmpresaRepository empresaRepository;
    private AuditoriaService auditoria;
    private ServicioBranding servicio;

    @BeforeEach
    void setUp() {
        repository = mock(EmpresaBrandingRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        auditoria = mock(AuditoriaService.class);
        servicio = new ServicioBranding(repository, empresaRepository, auditoria);
    }

    private EmpresaJpa empresa(Long id, String nit, String razonSocial) {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(id);
        when(empresa.getNit()).thenReturn(nit);
        when(empresa.getRazonSocial()).thenReturn(razonSocial);
        when(empresaRepository.findById(id)).thenReturn(Optional.of(empresa));
        return empresa;
    }

    @Test
    void devuelveValoresPorDefectoCundoNoHayConfiguracion() {
        empresa(1L, "900123456", "MAGNETIXDIAN SAS");
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());

        BrandingDto dto = servicio.obtener(1L);

        assertThat(dto.empresaId()).isEqualTo(1L);
        assertThat(dto.publicSlug()).isEqualTo("magnetixdian-sas");
        assertThat(dto.displayName()).isEqualTo("MAGNETIXDIAN SAS");
        assertThat(dto.primaryColor()).isEqualTo("#0E5C55");
        assertThat(dto.accentColor()).isEqualTo("#D98A2B");
        assertThat(dto.fontFamily()).isEqualTo("Roboto");
        assertThat(dto.pwaThemeColor()).isEqualTo("#0E5C55");
        assertThat(dto.pwaBackgroundColor()).isEqualTo("#FFFFFF");
        assertThat(dto.whatsappTemplate()).contains("{numero}").contains("{total}");
    }

    @Test
    void generaSlugNormalizadoYSufijoUnico() {
        empresa(1L, "900123456", "Café & Ñ SC");
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());
        when(repository.findByPublicSlug("mi-marca")).thenReturn(Optional.of(new EmpresaBrandingJpa(2L, "mi-marca")));
        when(repository.findByPublicSlug("mi-marca-2")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BrandingRequest request = new BrandingRequest("  Mi Marca  ", "Mi marca", "logo.png", null, null,
                null, null, null, null, null, null);
        BrandingDto dto = servicio.guardar(1L, request);

        assertThat(dto.publicSlug()).isEqualTo("mi-marca-2");
        assertThat(dto.displayName()).isEqualTo("Mi marca");
        assertThat(dto.logoUrl()).isEqualTo("logo.png");
        assertThat(dto.primaryColor()).isEqualTo("#0E5C55");
        verify(auditoria).registrarEnEmpresa(eq(1L), any(), eq("BRANDING_ACTUALIZADO"), eq("empresa"),
                eq(1L), anyString());
    }

    @Test
    void mantieneAislamientoEntreEmpresas() {
        empresa(1L, "900123456", "Primera SAS");
        empresa(2L, "900999999", "Segunda SAS");
        EmpresaBrandingJpa segunda = new EmpresaBrandingJpa(2L, "segunda-sas");
        segunda.setDisplayName("Segunda marca");
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());
        when(repository.findByEmpresaId(2L)).thenReturn(Optional.of(segunda));
        when(repository.findByPublicSlug("segunda-sas")).thenReturn(Optional.of(segunda));

        BrandingDto primera = servicio.obtener(1L);
        BrandingDto porSlug = servicio.porSlug("segunda-sas").orElseThrow();

        assertThat(primera.empresaId()).isEqualTo(1L);
        assertThat(primera.displayName()).isEqualTo("Primera SAS");
        assertThat(primera.publicSlug()).isEqualTo("primera-sas");
        assertThat(porSlug.empresaId()).isEqualTo(2L);
        assertThat(porSlug.displayName()).isEqualTo("Segunda marca");
        assertThat(servicio.porSlug("desconocido")).isEmpty();
    }

    @Test
    void slugifyEliminaAcentosYSeparadores() {
        assertThat(servicio.slugify("  Águila -- Negra! ")).isEqualTo("aguila-negra");
        assertThat(servicio.slugify("")).isEmpty();
    }
}
