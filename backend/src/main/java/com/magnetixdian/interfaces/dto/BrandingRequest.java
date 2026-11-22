package com.magnetixdian.interfaces.dto;

public record BrandingRequest(
        String publicSlug,
        String displayName,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String fontFamily,
        String invoiceFooter,
        String whatsappNumber,
        String whatsappTemplate,
        String pwaThemeColor,
        String pwaBackgroundColor) {
}
