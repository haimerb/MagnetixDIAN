package com.magnetixdian.domain.exogena;

import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.interfaces.dto.DianConceptoDto;
import com.magnetixdian.interfaces.dto.ExogenaValidacionDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ValidadorExogena {

    public static final String BLOQUEANTE = "BLOQUEANTE";
    public static final String ADVERTENCIA = "ADVERTENCIA";

    private static final Set<String> TIPOS_DOCUMENTO = Set.of(
            "NIT", "CC", "TI", "CE", "PA", "RC", "NU");

    public List<ExogenaValidacionDto> validar(Integer ano, String formato,
                                              List<MovimientoTributarioDto> movimientos,
                                              Map<Long, DianConceptoDto> conceptos,
                                              Set<String> cuentasValidas) {
        List<ExogenaValidacionDto> validaciones = new ArrayList<>();
        if (movimientos == null || movimientos.isEmpty()) {
            return validaciones;
        }
        for (MovimientoTributarioDto movimiento : movimientos) {
            if (movimiento == null) {
                continue;
            }
            String identificador = (movimiento.terceroId() == null ? "0" : movimiento.terceroId())
                    + "|" + (movimiento.conceptoCodigo() == null ? "SIN_CONCEPTO" : movimiento.conceptoCodigo());
            validarConcepto(ano, movimiento, conceptos, identificador, validaciones);
            validarCuenta(movimiento, cuentasValidas, identificador, validaciones);
            validarTercero(movimiento, identificador, validaciones);
            validarValor(movimiento, conceptos, identificador, validaciones);
        }
        return validaciones;
    }

    public boolean hayBloqueantes(List<ExogenaValidacionDto> validaciones) {
        return validaciones != null && validaciones.stream()
                .anyMatch(v -> BLOQUEANTE.equals(v.severidad()));
    }

    private void validarConcepto(Integer ano, MovimientoTributarioDto movimiento,
                                 Map<Long, DianConceptoDto> conceptos, String identificador,
                                 List<ExogenaValidacionDto> validaciones) {
        if (movimiento.conceptoId() == null) {
            validaciones.add(nueva(BLOQUEANTE, "SIN_MAPEOS",
                    "Movimiento sin concepto DIAN vigente para la cuenta " + movimiento.cuentaCodigo(),
                    movimiento, identificador));
            return;
        }
        DianConceptoDto concepto = conceptos.get(movimiento.conceptoId());
        if (concepto == null || !concepto.activo() || !ano.equals(concepto.ano())) {
            validaciones.add(nueva(BLOQUEANTE, "CONCEPTO_NO_VIGENTE",
                    "El concepto DIAN no está vigente para el año " + ano, movimiento, identificador));
        }
    }

    private void validarCuenta(MovimientoTributarioDto movimiento, Set<String> cuentasValidas,
                               String identificador, List<ExogenaValidacionDto> validaciones) {
        if (movimiento.cuentaCodigo() == null || movimiento.cuentaCodigo().isBlank()
                || cuentasValidas == null || !cuentasValidas.contains(movimiento.cuentaCodigo().trim())) {
            validaciones.add(nueva(BLOQUEANTE, "CUENTA_NO_PUC",
                    "La cuenta contable no existe en el PUC vigente: " + movimiento.cuentaCodigo(),
                    movimiento, identificador));
        }
    }

    private void validarTercero(MovimientoTributarioDto movimiento, String identificador,
                                List<ExogenaValidacionDto> validaciones) {
        String tipoDocumento = movimiento.tipoDocumento() == null ? ""
                : movimiento.tipoDocumento().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_DOCUMENTO.contains(tipoDocumento)) {
            validaciones.add(nueva(BLOQUEANTE, "TIPO_DOCUMENTO_INVALIDO",
                    "Tipo de documento de tercero no soportado: " + movimiento.tipoDocumento(),
                    movimiento, identificador));
        }
        if ("NIT".equals(tipoDocumento)) {
            validarDvNit(movimiento, identificador, validaciones);
        }
        if (movimiento.municipio() == null || movimiento.municipio().isBlank()) {
            validaciones.add(nueva(ADVERTENCIA, "MUNICIPIO_OMITIDO",
                    "El tercero no tiene municipio registrado", movimiento, identificador));
        }
        if (movimiento.pais() == null || movimiento.pais().isBlank()) {
            validaciones.add(nueva(ADVERTENCIA, "PAIS_OMITIDO",
                    "El tercero no tiene país registrado", movimiento, identificador));
        }
    }

    private void validarDvNit(MovimientoTributarioDto movimiento, String identificador,
                              List<ExogenaValidacionDto> validaciones) {
        String numero = movimiento.numeroDocumento() == null ? "" : movimiento.numeroDocumento().trim();
        String dv = movimiento.digitoVerificacion() == null ? "" : movimiento.digitoVerificacion().trim();
        if (numero.contains("-")) {
            String[] partes = numero.split("-", 2);
            numero = partes[0].trim();
            if (dv.isBlank()) {
                dv = partes[1].trim();
            }
        }
        Integer esperado = NitDigitoVerificacionRegla.calcularDv(numero);
        if (esperado == null) {
            validaciones.add(nueva(ADVERTENCIA, "DV_NO_CALCULABLE",
                    "No fue posible calcular el dígito de verificación del NIT", movimiento, identificador));
            return;
        }
        if (dv.isBlank()) {
            validaciones.add(nueva(ADVERTENCIA, "DV_NO_REGISTRADO",
                    "El tercero no tiene dígito de verificación registrado; esperado " + esperado,
                    movimiento, identificador));
            return;
        }
        try {
            if (Integer.parseInt(dv) != esperado) {
                validaciones.add(nueva(BLOQUEANTE, "DV_INVALIDO",
                        "El dígito de verificación del NIT no corresponde; esperado " + esperado,
                        movimiento, identificador));
            }
        } catch (NumberFormatException ex) {
            validaciones.add(nueva(BLOQUEANTE, "DV_INVALIDO",
                    "El dígito de verificación del NIT no es numérico", movimiento, identificador));
        }
    }

    private void validarValor(MovimientoTributarioDto movimiento, Map<Long, DianConceptoDto> conceptos,
                              String identificador, List<ExogenaValidacionDto> validaciones) {
        BigDecimal valor = movimiento.valor();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            validaciones.add(nueva(ADVERTENCIA, "VALOR_CERO",
                    "El movimiento tributario tiene valor cero", movimiento, identificador));
            return;
        }
        if (movimiento.conceptoId() == null || conceptos == null) {
            return;
        }
        DianConceptoDto concepto = conceptos.get(movimiento.conceptoId());
        if (concepto != null && concepto.valorMinimo() != null
                && valor.abs().compareTo(concepto.valorMinimo()) < 0) {
            validaciones.add(nueva(ADVERTENCIA, "VALOR_INFERIOR_MINIMO",
                    "El valor es inferior al mínimo definido para el concepto", movimiento, identificador));
        }
    }

    private ExogenaValidacionDto nueva(String severidad, String codigo, String mensaje,
                                       MovimientoTributarioDto movimiento, String identificador) {
        return new ExogenaValidacionDto(severidad, codigo, mensaje, movimiento.terceroId(),
                movimiento.conceptoId(), movimiento.cuentaCodigo(), identificador);
    }
}
