package com.magnetixdian.domain.exogena;

public record CabeceraExogena(
        String formato,
        String version,
        Integer anio,
        Integer numEnvio,
        String fecEnvio,
        String fecInicial,
        String fecFinal,
        String concepto
) {
}
