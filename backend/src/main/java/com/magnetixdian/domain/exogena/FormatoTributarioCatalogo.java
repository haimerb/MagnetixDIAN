package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.FormatoTributarioDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FormatoTributarioCatalogo {

    public static final String FORMATO_1001 = "1001";
    public static final String ESTADO_ACTIVO = FormatoExogenaRegistry.ESTADO_ACTIVO;
    public static final String ESTADO_REGISTRADO = "REGISTRADO";

    private static final int ANO_CATALOGO = 2026;
    private static final List<String> FORMATOS = List.of(
            "1001", "1003", "1005", "1006", "1007", "1008", "1009",
            "2276", "1125", "2278", "5253");

    private final FormatoExogenaRegistry registry = new FormatoExogenaRegistry();

    public List<FormatoTributarioDto> todos() {
        return FORMATOS.stream().map(this::obtener).toList();
    }

    public FormatoTributarioDto obtener(String formato) {
        FormatoExogena item = registry.obtener(formato);
        return new FormatoTributarioDto(item.codigo(), item.descripcion(), estado(item), item.activo(),
                rutaXsd(ANO_CATALOGO, item.codigo()), false);
    }

    public boolean activo(String formato) {
        return registry.obtener(formato).activo();
    }

    public String estado(String formato) {
        return estado(registry.obtener(formato));
    }

    public String rutaXsd(int ano, String formato) {
        FormatoExogena item = registry.obtener(formato);
        if (FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS.contains(item.codigo())) {
            return "/" + item.xsd();
        }
        return "/xsd/exogena/" + ano + "/" + item.codigo() + ".xsd";
    }

    public void validar(String formato) {
        registry.obtener(formato);
    }

    private String estado(FormatoExogena formato) {
        return formato.activo() ? ESTADO_ACTIVO : ESTADO_REGISTRADO;
    }
}
