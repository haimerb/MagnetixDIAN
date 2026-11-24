package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.FormatoTributarioDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormatoTributarioCatalogo {

    public static final String FORMATO_1001 = "1001";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_REGISTRADO = "REGISTRADO";

    private static final Map<String, String> FORMATOS = new LinkedHashMap<>();

    static {
        FORMATOS.put("1001", "Información de ingresos y gastos - formato 1001");
        FORMATOS.put("1003", "Información de compras y gastos - formato 1003");
        FORMATOS.put("1005", "Información de retenciones practicadas - formato 1005");
        FORMATOS.put("1006", "Información de retenciones recibidas - formato 1006");
        FORMATOS.put("1007", "Información de ingresos recibidos - formato 1007");
        FORMATOS.put("1008", "Información de pagos al exterior - formato 1008");
        FORMATOS.put("1009", "Información de activos - formato 1009");
        FORMATOS.put("2276", "Información de cuentas por cobrar - formato 2276");
        FORMATOS.put("1125", "Información de cuentas por pagar - formato 1125");
        FORMATOS.put("2278", "Información de pasivos - formato 2278");
        FORMATOS.put("5253", "Información de patrimonio - formato 5253");
    }

    public List<FormatoTributarioDto> todos() {
        return FORMATOS.entrySet().stream()
                .map(entry -> new FormatoTributarioDto(entry.getKey(), entry.getValue(),
                        estado(entry.getKey()), activo(entry.getKey()), rutaXsd(2026, entry.getKey()), false))
                .toList();
    }

    public FormatoTributarioDto obtener(String formato) {
        validar(formato);
        return new FormatoTributarioDto(formato, FORMATOS.get(formato), estado(formato), activo(formato),
                rutaXsd(2026, formato), false);
    }

    public boolean activo(String formato) {
        validar(formato);
        return FORMATO_1001.equals(formato);
    }

    public String estado(String formato) {
        return activo(formato) ? ESTADO_ACTIVO : ESTADO_REGISTRADO;
    }

    public String rutaXsd(int ano, String formato) {
        validar(formato);
        return "/xsd/exogena/" + ano + "/" + formato + ".xsd";
    }

    public void validar(String formato) {
        if (formato == null || !FORMATOS.containsKey(formato)) {
            throw new IllegalArgumentException("Formato exógena no soportado: " + formato);
        }
    }
}
