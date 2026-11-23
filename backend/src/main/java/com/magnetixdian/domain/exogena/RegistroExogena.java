package com.magnetixdian.domain.exogena;

import java.util.Map;

public record RegistroExogena(
        int linea,
        Map<String, String> atributos
) {
}
