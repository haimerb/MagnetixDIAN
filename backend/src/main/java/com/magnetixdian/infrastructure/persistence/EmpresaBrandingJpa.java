package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "empresa_branding")
public class EmpresaBrandingJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private Long empresaId;

    @Column(name = "public_slug", nullable = false, unique = true, length = 80)
    private String publicSlug;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "logo_path", columnDefinition = "TEXT")
    private String logoPath;

    @Column(name = "primary_color", nullable = false, length = 20)
    private String primaryColor = "#0E5C55";

    @Column(name = "accent_color", nullable = false, length = 20)
    private String accentColor = "#D98A2B";

    @Column(name = "font_family", nullable = false, length = 60)
    private String fontFamily = "Roboto";

    @Column(name = "invoice_footer", columnDefinition = "TEXT")
    private String invoiceFooter;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "whatsapp_template", columnDefinition = "TEXT")
    private String whatsappTemplate;

    @Column(name = "pwa_theme_color", nullable = false, length = 20)
    private String pwaThemeColor = "#0E5C55";

    @Column(name = "pwa_background_color", nullable = false, length = 20)
    private String pwaBackgroundColor = "#FFFFFF";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public EmpresaBrandingJpa() {
    }

    public EmpresaBrandingJpa(Long empresaId, String publicSlug) {
        this.empresaId = empresaId;
        this.publicSlug = publicSlug;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getPublicSlug() {
        return publicSlug;
    }

    public void setPublicSlug(String publicSlug) {
        this.publicSlug = publicSlug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getInvoiceFooter() {
        return invoiceFooter;
    }

    public void setInvoiceFooter(String invoiceFooter) {
        this.invoiceFooter = invoiceFooter;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getWhatsappTemplate() {
        return whatsappTemplate;
    }

    public void setWhatsappTemplate(String whatsappTemplate) {
        this.whatsappTemplate = whatsappTemplate;
    }

    public String getPwaThemeColor() {
        return pwaThemeColor;
    }

    public void setPwaThemeColor(String pwaThemeColor) {
        this.pwaThemeColor = pwaThemeColor;
    }

    public String getPwaBackgroundColor() {
        return pwaBackgroundColor;
    }

    public void setPwaBackgroundColor(String pwaBackgroundColor) {
        this.pwaBackgroundColor = pwaBackgroundColor;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
