package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.DianConceptoDto;
import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgregadorExogena {

    public List<ExogenaRenglonDto> agregar(String formato, List<MovimientoTributarioDto> movimientos,
                                           Map<Long, DianConceptoDto> conceptos) {
        Map<String, ExogenaRenglonDto> agrupados = new LinkedHashMap<>();
        if (movimientos == null || conceptos == null) {
            return List.of();
        }
        for (MovimientoTributarioDto movimiento : movimientos) {
            if (movimiento == null || movimiento.conceptoId() == null) {
                continue;
            }
            DianConceptoDto concepto = conceptos.get(movimiento.conceptoId());
            if (concepto == null) {
                continue;
            }
            String clave = (movimiento.terceroId() == null ? 0 : movimiento.terceroId())
                    + "|" + movimiento.conceptoId();
            BigDecimal valor = movimiento.valor() == null ? BigDecimal.ZERO : movimiento.valor();
            ExogenaRenglonDto actual = agrupados.get(clave);
            if (actual == null) {
                agrupados.put(clave, new ExogenaRenglonDto(
                        movimiento.ano(), formato, movimiento.terceroId(), movimiento.tipoDocumento(),
                        movimiento.numeroDocumento(), movimiento.digitoVerificacion(), movimiento.razonSocial(),
                        movimiento.municipio(), movimiento.pais(), concepto.id(), concepto.codigo(),
                        concepto.descripcion(), valor, 1));
            } else {
                agrupados.put(clave, new ExogenaRenglonDto(
                        actual.ano(), actual.formato(), actual.terceroId(), actual.tipoDocumento(),
                        actual.numeroDocumento(), actual.digitoVerificacion(), actual.razonSocial(),
                        actual.municipio(), actual.pais(), actual.conceptoId(), actual.codigoConcepto(),
                        actual.descripcionConcepto(), actual.valor().add(valor), actual.cantidadMovimientos() + 1));
            }
        }
        return new ArrayList<>(agrupados.values());
    }
}
