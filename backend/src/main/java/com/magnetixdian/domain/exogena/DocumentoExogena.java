package com.magnetixdian.domain.exogena;

import java.util.List;

public record DocumentoExogena(
        String formato,
        String version,
        Integer anio,
        Integer codCpt,
        Integer numEnvio,
        String fecEnvio,
        String fecInicial,
        String fecFinal,
        String valorTotal,
        Integer cantReg,
        String elementoDetalle,
        List<RegistroExogena> registros
) {
}
