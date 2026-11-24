package com.magnetixdian.interfaces.dto;

import com.magnetixdian.domain.dian.ModoOperacionDian;
import java.time.LocalDate;

public record DianHabilitacionDto(
        ModoOperacionDian modo,
        String resolucionNumero,
        LocalDate resolucionFecha,
        String prefijo,
        Integer rangoDesde,
        Integer rangoHasta,
        String testSetId,
        String softwareId,
        boolean notificarAdquirente) {
}
