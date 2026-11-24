package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ClasificadorMovimientos {

    public List<MovimientoTributarioDto> clasificar(List<MovimientoTributarioDto> movimientos) {
        if (movimientos == null || movimientos.isEmpty()) {
            return List.of();
        }
        List<MovimientoTributarioDto> resultado = new ArrayList<>(movimientos.size());
        for (MovimientoTributarioDto movimiento : movimientos) {
            if (movimiento == null) {
                continue;
            }
            String numero = normalizar(movimiento.numeroDocumento());
            String dv = normalizar(movimiento.digitoVerificacion());
            if (numero.contains("-")) {
                String[] partes = numero.split("-", 2);
                numero = partes[0].trim();
                if (dv.isBlank()) {
                    dv = partes[1].trim();
                }
            }
            String tipoDocumento = normalizar(movimiento.tipoDocumento());
            if (tipoDocumento.isBlank()) {
                tipoDocumento = "NIT";
            }
            String pais = normalizar(movimiento.pais());
            if (pais.isBlank()) {
                pais = "CO";
            }
            Integer ano = movimiento.ano() != null ? movimiento.ano()
                    : movimiento.fecha() != null ? movimiento.fecha().getYear() : null;
            String conceptoCodigo = movimiento.conceptoCodigo() != null && !movimiento.conceptoCodigo().isBlank()
                    ? normalizar(movimiento.conceptoCodigo())
                    : conceptoPorCuenta(movimiento.cuentaCodigo(), movimiento.conceptoCodigo());
            String origen = movimiento.origen() == null || movimiento.origen().isBlank()
                    ? "ASIENTO" : normalizar(movimiento.origen());
            resultado.add(new MovimientoTributarioDto(
                    movimiento.empresaId(), ano, movimiento.asientoId(), movimiento.asientoLineaId(),
                    movimiento.terceroId(), tipoDocumento.toUpperCase(Locale.ROOT), numero, dv,
                    movimiento.razonSocial(), movimiento.municipio(), pais.toUpperCase(Locale.ROOT),
                    normalizar(movimiento.cuentaCodigo()), conceptoCodigo, movimiento.conceptoId(),
                    movimiento.valor(), movimiento.fecha(), movimiento.origenTipoDocumento(),
                    movimiento.origenNumeroDocumento(), origen.toUpperCase(Locale.ROOT)));
        }
        return resultado;
    }

    private String conceptoPorCuenta(String cuentaCodigo, String sugerido) {
        if (sugerido != null && !sugerido.isBlank()) {
            return switch (sugerido.toUpperCase(Locale.ROOT)) {
                case "IVA", "IVA_GENERADO", "IVA_DESCONTABLE" -> "1005";
                case "RETEFUENTE", "RETEIVA", "ICA", "RETENCION" -> "1006";
                default -> sugerido;
            };
        }
        if (cuentaCodigo == null || cuentaCodigo.isBlank()) {
            return null;
        }
        if (cuentaCodigo.startsWith("413515") || cuentaCodigo.startsWith("529515")) {
            return "1002";
        }
        if (cuentaCodigo.startsWith("413") || cuentaCodigo.startsWith("419")) {
            return "1001";
        }
        if (cuentaCodigo.startsWith("519") || cuentaCodigo.startsWith("61") || cuentaCodigo.startsWith("629")) {
            return "1003";
        }
        if (cuentaCodigo.startsWith("2408")) {
            return "1004";
        }
        if (cuentaCodigo.startsWith("135515")) {
            return "1005";
        }
        if (cuentaCodigo.startsWith("2365") || cuentaCodigo.startsWith("236510")
                || cuentaCodigo.startsWith("236515") || cuentaCodigo.startsWith("135520")) {
            return "1006";
        }
        return null;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
