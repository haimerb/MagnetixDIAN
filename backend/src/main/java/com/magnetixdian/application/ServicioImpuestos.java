package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.CatalogoImpuestoDto;
import com.magnetixdian.interfaces.dto.DeclaracionAsientoDto;
import com.magnetixdian.interfaces.dto.ResumenImpuestoAsientoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class ServicioImpuestos {

    private static final Map<String, String> ALIAS = new LinkedHashMap<>();

    static {
        ALIAS.put("IVA", "01");
        ALIAS.put("IMPUESTO_VALOR_AGREGADO", "01");
        ALIAS.put("RETEFUENTE", "05");
        ALIAS.put("RETENCION_FUENTE", "05");
        ALIAS.put("RTE_FUENTE", "05");
        ALIAS.put("RETEIVA", "06");
        ALIAS.put("RETE_IVA", "06");
        ALIAS.put("ICA", "07");
        ALIAS.put("RETEICA", "07");
    }

    private static final List<CatalogoImpuestoDto> CATALOGO = List.of(
            new CatalogoImpuestoDto("01", "IVA", "IVA", "Impuesto sobre las ventas"),
            new CatalogoImpuestoDto("05", "Retención en la fuente", "RETEFUENTE",
                    "Retención en la fuente sobre ingresos"),
            new CatalogoImpuestoDto("06", "Retención de IVA", "RETEIVA", "Retención del impuesto sobre las ventas"),
            new CatalogoImpuestoDto("07", "Retención ICA", "ICA", "Retención del impuesto industrial y comercial"));

    private final JdbcTemplate jdbc;

    public ServicioImpuestos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CatalogoImpuestoDto> catalogo() {
        return CATALOGO;
    }

    public String normalizar(String impuestoCodigo) {
        if (impuestoCodigo == null || impuestoCodigo.isBlank()) {
            throw new IllegalArgumentException("El código de impuesto es obligatorio");
        }
        String valor = impuestoCodigo.trim().toUpperCase(Locale.ROOT);
        if (ALIAS.containsKey(valor)) {
            return ALIAS.get(valor);
        }
        if (CATALOGO.stream().anyMatch(item -> item.codigo().equals(valor))) {
            return valor;
        }
        throw new IllegalArgumentException("Código de impuesto no soportado: " + impuestoCodigo);
    }

    @Transactional(readOnly = true)
    public List<ResumenImpuestoAsientoDto> causacionDesdeAsientos(Long empresaId,
                                                                  LocalDate fechaDesde,
                                                                  LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null || fechaHasta.isBefore(fechaDesde)) {
            throw new IllegalArgumentException("El rango de fechas de causación no es válido");
        }
        List<ResumenImpuestoAsientoDto> resumen = consultarResumen(empresaId, fechaDesde, fechaHasta);
        return fusionar(resumen);
    }

    @Transactional(readOnly = true)
    public DeclaracionAsientoDto declaracionDesdeAsientos(Long empresaId, Integer anio, String periodo) {
        LocalDate[] rango = resolverRango(anio, periodo);
        List<ResumenImpuestoAsientoDto> impuestos = fusionar(
                consultarResumen(empresaId, rango[0], rango[1]));
        BigDecimal totalBase = impuestos.stream()
                .map(ResumenImpuestoAsientoDto::base)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalImpuesto = impuestos.stream()
                .map(ResumenImpuestoAsientoDto::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer totalAsientos = impuestos.stream()
                .map(ResumenImpuestoAsientoDto::cantidadAsientos)
                .reduce(0, Integer::sum);
        return new DeclaracionAsientoDto(rango[0].getYear(), periodo, rango[0], rango[1],
                totalBase, totalImpuesto, totalAsientos, impuestos);
    }

    private LocalDate[] resolverRango(Integer anio, String periodo) {
        if (periodo == null || periodo.isBlank()) {
            int anioEfectivo = anio == null ? LocalDate.now().getYear() : anio;
            return new LocalDate[]{LocalDate.of(anioEfectivo, 1, 1), LocalDate.of(anioEfectivo, 12, 31)};
        }
        if (periodo.matches("\\d{4}-\\d{2}")) {
            YearMonth mes = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
            return new LocalDate[]{mes.atDay(1), mes.atEndOfMonth()};
        }
        if (periodo.matches("\\d{4}")) {
            int anioEfectivo = Integer.parseInt(periodo);
            return new LocalDate[]{LocalDate.of(anioEfectivo, 1, 1), LocalDate.of(anioEfectivo, 12, 31)};
        }
        throw new IllegalArgumentException("Periodo de declaración no soportado: " + periodo);
    }

    private List<ResumenImpuestoAsientoDto> consultarResumen(Long empresaId,
                                                             LocalDate fechaDesde,
                                                             LocalDate fechaHasta) {
        return jdbc.query("""
                SELECT COALESCE(l.impuesto_codigo, 'SIN_IMPUESTO') AS codigo,
                       COALESCE(l.base_imponible, 0) AS base,
                       COALESCE(l.debito, 0) - COALESCE(l.credito, 0) AS valor,
                       1 AS cantidad
                FROM asiento_contable_linea l
                JOIN asiento_contable a ON a.id = l.asiento_contable_id
                WHERE a.empresa_id = ? AND a.estado = 'CONTABILIZADO'
                  AND a.fecha BETWEEN ? AND ?
                  AND l.impuesto_codigo IS NOT NULL
                """, (rs, fila) -> {
            String codigo = rs.getString("codigo");
            String normalizado;
            try {
                normalizado = normalizar(codigo);
            } catch (IllegalArgumentException ex) {
                normalizado = codigo;
            }
            return new ResumenImpuestoAsientoDto(normalizado, nombre(normalizado),
                    rs.getBigDecimal("base"), rs.getBigDecimal("valor"), rs.getInt("cantidad"));
        }, empresaId, java.sql.Date.valueOf(fechaDesde), java.sql.Date.valueOf(fechaHasta));
    }

    private List<ResumenImpuestoAsientoDto> fusionar(List<ResumenImpuestoAsientoDto> resumen) {
        Map<String, ResumenImpuestoAsientoDto> agrupados = new LinkedHashMap<>();
        for (ResumenImpuestoAsientoDto item : resumen) {
            ResumenImpuestoAsientoDto actual = agrupados.get(item.codigo());
            if (actual == null) {
                agrupados.put(item.codigo(), item);
            } else {
                agrupados.put(item.codigo(), new ResumenImpuestoAsientoDto(item.codigo(), item.nombre(),
                        actual.base().add(item.base() == null ? BigDecimal.ZERO : item.base()),
                        actual.valor().add(item.valor() == null ? BigDecimal.ZERO : item.valor()),
                        actual.cantidadAsientos() + item.cantidadAsientos()));
            }
        }
        return new ArrayList<>(agrupados.values());
    }

    private String nombre(String codigo) {
        return CATALOGO.stream()
                .filter(item -> item.codigo().equals(codigo))
                .map(CatalogoImpuestoDto::nombre)
                .findFirst()
                .orElse(codigo);
    }
}
