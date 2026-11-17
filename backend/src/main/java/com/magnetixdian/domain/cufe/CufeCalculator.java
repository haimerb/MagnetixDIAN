package com.magnetixdian.domain.cufe;

import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Cálculo de los mecanismos de control de la DIAN (Anexo Técnico FEV v1.9):
 * CUFE (facturas), CUDE (notas y demás documentos) y el código de seguridad
 * del software. Todos usan SHA-384.
 */
public final class CufeCalculator {

    public static final String IVA = "01";
    public static final String INC = "04";
    public static final String ICA = "03";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss-05:00");

    private CufeCalculator() {
    }

    /**
     * CUFE/CUDE según el anexo v1.9:
     * NumFac + FecFac + HorFac + ValFac + CodImp1 + ValImp1 + CodImp2 + ValImp2 +
     * CodImp3 + ValImp3 + ValTot + NitOFE + NumAdq + ClTec + TipoAmbiente.
     */
    public static String calcular(FacturaDatos f) {
        BigDecimal valorIva = valorImpuesto(f, IVA);
        BigDecimal valorInc = valorImpuesto(f, INC);
        BigDecimal valorIca = valorImpuesto(f, ICA);

        StringBuilder sb = new StringBuilder();
        sb.append(f.numeroCompleto());
        sb.append(f.fechaEmision().format(FECHA));
        sb.append(f.horaEmision().format(HORA));
        sb.append(formato(f.valorSubtotal()));
        sb.append(IVA).append(formato(valorIva));
        sb.append(INC).append(formato(valorInc));
        sb.append(ICA).append(formato(valorIca));
        sb.append(formato(f.total() == null ? BigDecimal.ZERO : f.total()));
        sb.append(f.nitEmisor());
        sb.append(f.nitAdquirente());
        sb.append(nvl(f.softwarePin()));
        sb.append(f.ambiente());
        return sha384Hex(sb.toString());
    }

    /**
     * Código de seguridad del software: SHA-384(IdSoftware + PinSoftware + NumeroDocumento).
     */
    public static String softwareSecurityCode(String softwareId, String pin, String numeroDocumento) {
        return sha384Hex(nvl(softwareId) + nvl(pin) + nvl(numeroDocumento));
    }

    public static String sha384Hex(String entrada) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-384");
            byte[] hash = md.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 no disponible", e);
        }
    }

    private static BigDecimal valorImpuesto(FacturaDatos f, String codigo) {
        if (f.impuestos() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ImpuestoFactura imp : f.impuestos()) {
            if (codigo.equals(imp.tipo()) && imp.valor() != null) {
                total = total.add(imp.valor());
            }
        }
        return total;
    }

    private static String formato(BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
