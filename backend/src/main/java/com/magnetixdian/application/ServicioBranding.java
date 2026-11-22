package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaBrandingJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaBrandingRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.BrandingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

@Service
public class ServicioBranding {

    public static final String COLOR_PRIMARY = "#0E5C55";
    public static final String COLOR_ACCENT = "#D98A2B";
    public static final String FUENTE_DEFAULT = "Roboto";
    public static final String COLOR_FONDO_PWA = "#FFFFFF";
    public static final String PLANTILLA_WHATSAPP =
            "Hola, le compartimos su factura {numero} por un total de {total}. ¡Gracias!";

    private final EmpresaBrandingRepository repository;
    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoria;
    private final String colorPrimary;
    private final String colorAccent;
    private final String fuente;
    private final String colorFondoPwa;
    private final String plantillaWhatsapp;

    @Autowired
    public ServicioBranding(EmpresaBrandingRepository repository,
                            EmpresaRepository empresaRepository,
                            AuditoriaService auditoria,
                            @Value("${magnetixdian.branding.primary-color:#0E5C55}") String colorPrimary,
                            @Value("${magnetixdian.branding.accent-color:#D98A2B}") String colorAccent,
                            @Value("${magnetixdian.branding.font-family:Roboto}") String fuente,
                            @Value("${magnetixdian.branding.pwa-background-color:#FFFFFF}") String colorFondoPwa,
                            @Value("${magnetixdian.whatsapp.default-template:Hola, le compartimos su factura {numero} por un total de {total}. ¡Gracias!}") String plantillaWhatsapp) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.auditoria = auditoria;
        this.colorPrimary = valor(colorPrimary, COLOR_PRIMARY);
        this.colorAccent = valor(colorAccent, COLOR_ACCENT);
        this.fuente = valor(fuente, FUENTE_DEFAULT);
        this.colorFondoPwa = valor(colorFondoPwa, COLOR_FONDO_PWA);
        this.plantillaWhatsapp = valor(plantillaWhatsapp, PLANTILLA_WHATSAPP);
    }

    public ServicioBranding(EmpresaBrandingRepository repository, EmpresaRepository empresaRepository,
                            AuditoriaService auditoria) {
        this(repository, empresaRepository, auditoria, COLOR_PRIMARY, COLOR_ACCENT, FUENTE_DEFAULT,
                COLOR_FONDO_PWA, PLANTILLA_WHATSAPP);
    }

    @Transactional(readOnly = true)
    public BrandingDto obtener(Long empresaId) {
        EmpresaJpa empresa = empresa(empresaId);
        return aDto(empresa, repository.findByEmpresaId(empresaId).orElse(null));
    }

    @Transactional(readOnly = true)
    public Optional<BrandingDto> porSlug(String publicSlug) {
        if (publicSlug == null || publicSlug.isBlank()) {
            return Optional.empty();
        }
        return repository.findByPublicSlug(publicSlug.trim().toLowerCase(Locale.ROOT))
                .map(branding -> aDto(null, branding));
    }

    @Transactional
    public BrandingDto guardar(Long empresaId, BrandingRequest request) {
        EmpresaJpa empresa = empresa(empresaId);
        EmpresaBrandingJpa branding = repository.findByEmpresaId(empresaId)
                .orElseGet(() -> new EmpresaBrandingJpa(empresaId, null));

        String slugBase = request != null && request.publicSlug() != null && !request.publicSlug().isBlank()
                ? slugify(request.publicSlug()) : slugBase(empresa);
        branding.setPublicSlug(slugUnico(slugBase, empresaId));
        branding.setDisplayName(limpiar(request == null ? null : request.displayName(), empresa.getRazonSocial(), 120));
        branding.setLogoPath(limpiar(request == null ? null : request.logoUrl(), null, 1000));
        branding.setPrimaryColor(limpiar(request == null ? null : request.primaryColor(), colorPrimary, 20));
        branding.setAccentColor(limpiar(request == null ? null : request.accentColor(), colorAccent, 20));
        branding.setFontFamily(limpiar(request == null ? null : request.fontFamily(), fuente, 60));
        branding.setInvoiceFooter(limpiar(request == null ? null : request.invoiceFooter(), null, 2000));
        branding.setWhatsappNumber(limpiar(request == null ? null : request.whatsappNumber(), null, 20));
        branding.setWhatsappTemplate(limpiar(request == null ? null : request.whatsappTemplate(),
                plantillaWhatsapp, 2000));
        String tema = limpiar(request == null ? null : request.pwaThemeColor(), branding.getPrimaryColor(), 20);
        branding.setPwaThemeColor(tema);
        branding.setPwaBackgroundColor(limpiar(request == null ? null : request.pwaBackgroundColor(),
                colorFondoPwa, 20));
        branding.setUpdatedAt(java.time.OffsetDateTime.now());

        BrandingDto resultado = aDto(empresa, repository.save(branding));
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "BRANDING_ACTUALIZADO",
                "empresa", empresaId, resultado.publicSlug());
        return resultado;
    }

    public String slugBase(EmpresaJpa empresa) {
        String slug = slugify(empresa.getRazonSocial());
        if (slug.isEmpty()) {
            slug = slugify(empresa.getNit());
        }
        return slug.isEmpty() ? "empresa" : slug;
    }

    public String slugify(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "");
        return slug.length() > 60 ? slug.substring(0, 60).replaceAll("-+$", "") : slug;
    }

    private String slugUnico(String base, Long empresaId) {
        String candidato = base == null || base.isBlank() ? "empresa" : base;
        int sufijo = 2;
        while (true) {
            Optional<EmpresaBrandingJpa> existente = repository.findByPublicSlug(candidato);
            if (existente.isEmpty() || empresaId.equals(existente.get().getEmpresaId())) {
                return candidato;
            }
            candidato = base + "-" + sufijo++;
        }
    }

    private BrandingDto aDto(EmpresaJpa empresa, EmpresaBrandingJpa branding) {
        Long empresaId = branding == null ? empresa.getId() : branding.getEmpresaId();
        String razonSocial = empresa == null ? null : empresa.getRazonSocial();
        String slug = branding == null ? (empresa == null ? null : slugBase(empresa)) : branding.getPublicSlug();
        if (slug == null || slug.isBlank()) {
            slug = slugify(razonSocial);
        }
        if (slug == null || slug.isBlank()) {
            slug = "empresa-" + empresaId;
        }
        String colorPrimario = valor(branding == null ? null : branding.getPrimaryColor(), colorPrimary);
        return new BrandingDto(
                empresaId,
                slug,
                valor(branding == null ? null : branding.getDisplayName(), razonSocial),
                branding == null ? null : branding.getLogoPath(),
                colorPrimario,
                valor(branding == null ? null : branding.getAccentColor(), colorAccent),
                valor(branding == null ? null : branding.getFontFamily(), fuente),
                branding == null ? null : branding.getInvoiceFooter(),
                branding == null ? null : branding.getWhatsappNumber(),
                valor(branding == null ? null : branding.getWhatsappTemplate(), plantillaWhatsapp),
                valor(branding == null ? null : branding.getPwaThemeColor(), colorPrimario),
                valor(branding == null ? null : branding.getPwaBackgroundColor(), colorFondoPwa));
    }

    private EmpresaJpa empresa(Long empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa no encontrada");
        }
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private static String valor(String candidato, String defecto) {
        return candidato == null || candidato.isBlank() ? defecto : candidato;
    }

    private static String limpiar(String candidato, String defecto, int max) {
        String valor = candidato == null || candidato.isBlank() ? defecto : candidato.trim();
        if (valor == null || valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max);
    }
}
