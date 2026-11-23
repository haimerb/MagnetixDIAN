package com.magnetixdian.application;

import com.magnetixdian.domain.rule.CatalogoConceptos;
import com.magnetixdian.domain.uvt.UvtManager;
import com.magnetixdian.infrastructure.persistence.ImpuestoTarifaJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoTarifaRepository;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.TarifaImpuestoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioTarifasImpuesto {

    private final ImpuestoTarifaRepository repository;
    private final UvtManager uvtManager;
    private final CatalogoConceptos catalogoConceptos;

    public ServicioTarifasImpuesto(ImpuestoTarifaRepository repository,
                                   UvtManager uvtManager,
                                   CatalogoConceptos catalogoConceptos) {
        this.repository = repository;
        this.uvtManager = uvtManager;
        this.catalogoConceptos = catalogoConceptos;
    }

    public List<TarifaImpuestoDto> listar(String impuestoCodigo, Integer anio) {
        int anioEfectivo = anio == null ? Year.now().getValue() : anio;
        List<ImpuestoTarifaJpa> tarifas;
        if (impuestoCodigo == null || impuestoCodigo.isBlank()) {
            tarifas = repository.findByAnioOrderByImpuestoCodigoAscConceptoCodigoAsc(anioEfectivo)
                    .stream()
                    .filter(tarifa -> Boolean.TRUE.equals(tarifa.getActivo()))
                    .toList();
        } else {
            tarifas = repository.findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc(
                    normalizar(impuestoCodigo), anioEfectivo);
        }
        return tarifas.stream().map(this::mapear).toList();
    }

    public ResultadoCalculoImpuestoDto calcular(String impuestoCodigo,
                                                Integer anio,
                                                String conceptoCodigo,
                                                BigDecimal base) {
        String impuesto = requerir(impuestoCodigo, "El código de impuesto es obligatorio.");
        int anioEfectivo = anio == null ? Year.now().getValue() : anio;
        String concepto = normalizar(conceptoCodigo);

        if (concepto != null && !catalogoConceptos.conceptosFormato1001().containsKey(concepto)) {
            throw new IllegalArgumentException("Concepto no válido para retenciones: " + concepto);
        }
        if (base == null || base.signum() < 0) {
            throw new IllegalArgumentException("La base de cálculo debe ser mayor o igual a cero.");
        }

        ImpuestoTarifaJpa tarifa = resolverTarifa(impuesto, anioEfectivo, concepto);
        BigDecimal uvt = uvtManager.uvtParaAnio(anioEfectivo);
        BigDecimal baseMinima = tarifa.getBaseMinUvt().multiply(uvt);
        BigDecimal baseMaxima = tarifa.getBaseMaxUvt() == null
                ? null
                : tarifa.getBaseMaxUvt().multiply(uvt);
        boolean aplica = base.compareTo(baseMinima) >= 0;
        BigDecimal baseGravable = BigDecimal.ZERO;
        if (aplica) {
            baseGravable = baseMaxima == null || base.compareTo(baseMaxima) <= 0
                    ? base
                    : baseMaxima;
        }
        BigDecimal valor = baseGravable.multiply(tarifa.getPorcentaje())
                .setScale(2, RoundingMode.HALF_UP);

        return new ResultadoCalculoImpuestoDto(
                tarifa.getImpuestoCodigo(),
                tarifa.getNombre(),
                anioEfectivo,
                tarifa.getConceptoCodigo(),
                base,
                uvt,
                baseMinima,
                baseMaxima,
                baseGravable,
                tarifa.getPorcentaje(),
                aplica,
                valor);
    }

    private ImpuestoTarifaJpa resolverTarifa(String impuesto, Integer anio, String concepto) {
        List<ImpuestoTarifaJpa> tarifas = repository.findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc(
                impuesto, anio);
        if (tarifas.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay tarifa configurada para " + impuesto + " en el año " + anio);
        }
        if (concepto != null) {
            return tarifas.stream()
                    .filter(tarifa -> concepto.equals(tarifa.getConceptoCodigo()))
                    .findFirst()
                    .orElseGet(() -> tarifas.stream()
                            .filter(tarifa -> tarifa.getConceptoCodigo() == null)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "No hay tarifa configurada para " + impuesto + "/" + concepto
                                            + " en el año " + anio)));
        }
        return tarifas.stream()
                .filter(tarifa -> tarifa.getConceptoCodigo() == null)
                .findFirst()
                .orElseGet(() -> tarifas.get(0));
    }

    private TarifaImpuestoDto mapear(ImpuestoTarifaJpa tarifa) {
        return new TarifaImpuestoDto(
                tarifa.getId(),
                tarifa.getImpuestoCodigo(),
                tarifa.getNombre(),
                tarifa.getAnio(),
                tarifa.getConceptoCodigo(),
                tarifa.getPorcentaje(),
                tarifa.getBaseMinUvt(),
                tarifa.getBaseMaxUvt(),
                Boolean.TRUE.equals(tarifa.getActivo()));
    }

    private String requerir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
