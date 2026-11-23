package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.AsientoContableLineaDto;
import com.magnetixdian.interfaces.dto.BalanceComprobacionDto;
import com.magnetixdian.interfaces.dto.EstadoFinancieroDto;
import com.magnetixdian.interfaces.dto.FlujoEfectivoDto;
import com.magnetixdian.interfaces.dto.GrupoEstadoDto;
import com.magnetixdian.interfaces.dto.MovimientoContableDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.SaldoCuentaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ServicioReportesContables {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("CONTABILIZADO", "APROBADO", "CERRADO");
    private static final int TAMANO_MAXIMO = 200;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AsientoContableRepository asientoRepository;
    private final ServicioPlanCuentas servicioPlanCuentas;

    public ServicioReportesContables(AsientoContableRepository asientoRepository,
                                     ServicioPlanCuentas servicioPlanCuentas) {
        this.asientoRepository = asientoRepository;
        this.servicioPlanCuentas = servicioPlanCuentas;
    }

    public PaginaDto<AsientoContableDto> libroDiario(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
                                                     Long centroCostoId, Long terceroId, int pagina, int tamano) {
        List<AsientoContableDto> asientos = asientosOrdenados(empresaId, fechaDesde, fechaHasta).stream()
                .filter(asiento -> !lineasFiltradas(asiento, null, centroCostoId, terceroId).isEmpty())
                .map(asiento -> mapear(asiento, centroCostoId, terceroId))
                .toList();
        return paginar(asientos, pagina, tamano);
    }

    public PaginaDto<MovimientoContableDto> libroMayor(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
                                                       String codigoCuenta, Long centroCostoId, Long terceroId,
                                                       int pagina, int tamano) {
        return paginar(movimientos(empresaId, fechaDesde, fechaHasta, codigoCuenta,
                centroCostoId, terceroId), pagina, tamano);
    }

    public PaginaDto<MovimientoContableDto> auxiliar(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
                                                     String codigoCuenta, Long centroCostoId, Long terceroId,
                                                     int pagina, int tamano) {
        return libroMayor(empresaId, fechaDesde, fechaHasta, codigoCuenta,
                centroCostoId, terceroId, pagina, tamano);
    }

    public BalanceComprobacionDto balanceComprobacion(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);
        Map<String, BigDecimal> iniciales = fechaDesde == null
                ? new HashMap<>()
                : saldosPorCuenta(empresaId, null, fechaDesde.minusDays(1));
        Map<String, BigDecimal> finales = saldosPorCuenta(empresaId, null, fechaHasta);
        Map<String, BigDecimal[]> movimientos = new HashMap<>();
        for (AsientoContableJpa asiento : asientosOrdenados(empresaId, fechaDesde, fechaHasta)) {
            for (AsientoContableLineaJpa linea : asiento.getLineas()) {
                BigDecimal[] totales = movimientos.computeIfAbsent(
                        linea.getCuentaContable().getCodigo(), clave -> new BigDecimal[]{CERO, CERO});
                totales[0] = totales[0].add(escala(linea.getDebito()));
                totales[1] = totales[1].add(escala(linea.getCredito()));
            }
        }
        List<SaldoCuentaDto> filas = new ArrayList<>();
        BigDecimal totalInicial = CERO;
        BigDecimal totalDebito = CERO;
        BigDecimal totalCredito = CERO;
        BigDecimal totalFinal = CERO;
        for (CuentaContableJpa cuenta : cuentas.values().stream()
                .sorted(Comparator.comparing(CuentaContableJpa::getCodigo)).toList()) {
            String codigo = cuenta.getCodigo();
            BigDecimal[] moved = movimientos.getOrDefault(codigo, new BigDecimal[]{CERO, CERO});
            BigDecimal inicial = iniciales.getOrDefault(codigo, CERO);
            BigDecimal saldoFinal = finales.getOrDefault(codigo, CERO);
            BigDecimal debito = moved[0];
            BigDecimal credito = moved[1];
            if (inicial.signum() == 0 && debito.signum() == 0 && credito.signum() == 0
                    && saldoFinal.signum() == 0) {
                continue;
            }
            filas.add(new SaldoCuentaDto(codigo, cuenta.getNombre(), cuenta.getTipo(), cuenta.getNaturaleza(),
                    inicial, debito, credito, saldoFinal));
            totalInicial = totalInicial.add(inicial);
            totalDebito = totalDebito.add(debito);
            totalCredito = totalCredito.add(credito);
            totalFinal = totalFinal.add(saldoFinal);
        }
        return new BalanceComprobacionDto(filas, totalInicial, totalDebito, totalCredito, totalFinal);
    }

    public EstadoFinancieroDto balanceGeneral(Long empresaId, LocalDate fechaCorte) {
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);
        Map<String, BigDecimal> saldos = saldosPorCuenta(empresaId, null, fechaCorte);
        List<GrupoEstadoDto> grupos = List.of(
                grupo("Activos", "ACTIVO", cuentas, saldos),
                grupo("Pasivos", "PASIVO", cuentas, saldos),
                grupo("Patrimonio", "PATRIMONIO", cuentas, saldos));
        BigDecimal totalActivos = grupos.get(0).total();
        return new EstadoFinancieroDto("Balance general", null, fechaCorte, grupos, totalActivos);
    }

    public EstadoFinancieroDto estadoResultados(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);
        Map<String, BigDecimal> saldos = saldosPorCuenta(empresaId, fechaDesde, fechaHasta);
        GrupoEstadoDto ingresos = grupo("Ingresos", "INGRESO", cuentas, saldos);
        GrupoEstadoDto gastos = grupo("Gastos", "EGRESO", cuentas, saldos);
        return new EstadoFinancieroDto("Estado de resultados", fechaDesde, fechaHasta,
                List.of(ingresos, gastos), ingresos.total().subtract(gastos.total()));
    }

    public FlujoEfectivoDto flujoEfectivo(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        List<MovimientoContableDto> movimientos = new ArrayList<>();
        BigDecimal entradas = CERO;
        BigDecimal salidas = CERO;
        BigDecimal acumulado = CERO;
        for (AsientoContableJpa asiento : asientosOrdenados(empresaId, fechaDesde, fechaHasta)) {
            for (AsientoContableLineaJpa linea : lineasFiltradas(asiento, null, null, null)) {
                String codigo = linea.getCuentaContable().getCodigo();
                if (!codigo.startsWith("11")) {
                    continue;
                }
                BigDecimal debito = escala(linea.getDebito());
                BigDecimal credito = escala(linea.getCredito());
                acumulado = acumulado.add(debito.subtract(credito));
                entradas = entradas.add(debito);
                salidas = salidas.add(credito);
                movimientos.add(new MovimientoContableDto(codigo, linea.getCuentaContable().getNombre(),
                        asiento.getFecha(), asiento.getNumero(), linea.getDescripcion(),
                        debito, credito, acumulado, linea.getCentroCostoId(), linea.getTerceroId()));
            }
        }
        return new FlujoEfectivoDto(fechaDesde, fechaHasta, movimientos, entradas, salidas,
                entradas.subtract(salidas));
    }

    private List<MovimientoContableDto> movimientos(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
                                                    String codigoCuenta, Long centroCostoId, Long terceroId) {
        Map<String, BigDecimal> saldos = new HashMap<>();
        List<MovimientoContableDto> movimientos = new ArrayList<>();
        for (AsientoContableJpa asiento : asientosOrdenados(empresaId, fechaDesde, fechaHasta)) {
            for (AsientoContableLineaJpa linea : lineasFiltradas(asiento, codigoCuenta, centroCostoId, terceroId)) {
                CuentaContableJpa cuenta = linea.getCuentaContable();
                BigDecimal saldo = saldos.getOrDefault(cuenta.getCodigo(), CERO)
                        .add(importe(cuenta, linea.getDebito(), linea.getCredito()));
                saldos.put(cuenta.getCodigo(), saldo);
                movimientos.add(new MovimientoContableDto(cuenta.getCodigo(), cuenta.getNombre(),
                        asiento.getFecha(), asiento.getNumero(), linea.getDescripcion(),
                        escala(linea.getDebito()), escala(linea.getCredito()), saldo,
                        linea.getCentroCostoId(), linea.getTerceroId()));
            }
        }
        return movimientos;
    }

    private Map<String, BigDecimal> saldosPorCuenta(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        Map<String, BigDecimal> saldos = new HashMap<>();
        for (AsientoContableJpa asiento : asientosOrdenados(empresaId, fechaDesde, fechaHasta)) {
            for (AsientoContableLineaJpa linea : asiento.getLineas()) {
                CuentaContableJpa cuenta = linea.getCuentaContable();
                saldos.merge(cuenta.getCodigo(),
                        importe(cuenta, linea.getDebito(), linea.getCredito()), BigDecimal::add);
            }
        }
        return saldos;
    }

    private GrupoEstadoDto grupo(String nombre, String tipo, Map<String, CuentaContableJpa> cuentas,
                                 Map<String, BigDecimal> saldos) {
        List<SaldoCuentaDto> filas = new ArrayList<>();
        BigDecimal total = CERO;
        for (CuentaContableJpa cuenta : cuentas.values().stream()
                .filter(cuenta -> tipo.equals(cuenta.getTipo()))
                .sorted(Comparator.comparing(CuentaContableJpa::getCodigo)).toList()) {
            BigDecimal saldo = saldos.getOrDefault(cuenta.getCodigo(), CERO);
            if (saldo.signum() == 0) {
                continue;
            }
            filas.add(new SaldoCuentaDto(cuenta.getCodigo(), cuenta.getNombre(), cuenta.getTipo(),
                    cuenta.getNaturaleza(), CERO, CERO, CERO, saldo));
            total = total.add(saldo);
        }
        return new GrupoEstadoDto(nombre, filas, total);
    }

    private List<AsientoContableJpa> asientosOrdenados(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        return asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream()
                .filter(asiento -> enRango(asiento.getFecha(), fechaDesde, fechaHasta))
                .filter(asiento -> ESTADOS_VALIDOS.contains(asiento.getEstado()))
                .sorted(Comparator.comparing(AsientoContableJpa::getFecha)
                        .thenComparing(AsientoContableJpa::getNumero,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static List<AsientoContableLineaJpa> lineasFiltradas(AsientoContableJpa asiento, String codigoCuenta,
                                                                 Long centroCostoId, Long terceroId) {
        return asiento.getLineas().stream()
                .filter(linea -> codigoCuenta == null
                        || codigoCuenta.equals(linea.getCuentaContable().getCodigo()))
                .filter(linea -> centroCostoId == null || centroCostoId.equals(linea.getCentroCostoId()))
                .filter(linea -> terceroId == null || terceroId.equals(linea.getTerceroId()))
                .sorted(Comparator.comparingInt(AsientoContableLineaJpa::getNumeroLinea))
                .toList();
    }

    private static AsientoContableDto mapear(AsientoContableJpa asiento, Long centroCostoId, Long terceroId) {
        List<AsientoContableLineaDto> lineas = lineasFiltradas(asiento, null, centroCostoId, terceroId).stream()
                .map(linea -> new AsientoContableLineaDto(linea.getId(),
                        linea.getCuentaContable().getCodigo(),
                        linea.getCuentaContable().getNombre(),
                        linea.getDescripcion(),
                        escala(linea.getDebito()),
                        escala(linea.getCredito())))
                .toList();
        BigDecimal debito = lineas.stream().map(AsientoContableLineaDto::debito)
                .reduce(CERO, BigDecimal::add);
        BigDecimal credito = lineas.stream().map(AsientoContableLineaDto::credito)
                .reduce(CERO, BigDecimal::add);
        return new AsientoContableDto(asiento.getId(), asiento.getEmpresaId(), asiento.getNumero(),
                asiento.getFecha(), asiento.getDescripcion(), asiento.getDocumentoTipo(),
                asiento.getDocumentoId(), asiento.getTipoEvento(), asiento.getEstado(),
                debito, credito, asiento.getValorTotal(), asiento.getValorImpuestos(),
                asiento.getValorRetenciones(), asiento.getCreatedAt(), lineas);
    }

    private static boolean enRango(LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta) {
        return fecha != null
                && (fechaDesde == null || !fecha.isBefore(fechaDesde))
                && (fechaHasta == null || !fecha.isAfter(fechaHasta));
    }

    private static BigDecimal importe(CuentaContableJpa cuenta, BigDecimal debito, BigDecimal credito) {
        BigDecimal debitoEscalado = escala(debito);
        BigDecimal creditoEscalado = escala(credito);
        if ("ACREEDORA".equals(cuenta.getNaturaleza())) {
            return creditoEscalado.subtract(debitoEscalado);
        }
        return debitoEscalado.subtract(creditoEscalado);
    }

    private static BigDecimal escala(BigDecimal valor) {
        if (valor == null) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static <T> PaginaDto<T> paginar(List<T> contenido, int pagina, int tamano) {
        int paginaNormalizada = Math.max(pagina, 0);
        int tamanoNormalizado = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        int desde = Math.min(paginaNormalizada * tamanoNormalizado, contenido.size());
        int hasta = Math.min(desde + tamanoNormalizado, contenido.size());
        int totalPaginas = (int) Math.ceil((double) contenido.size() / tamanoNormalizado);
        return new PaginaDto<>(contenido.subList(desde, hasta), paginaNormalizada, tamanoNormalizado,
                contenido.size(), totalPaginas, paginaNormalizada == 0,
                totalPaginas == 0 || paginaNormalizada >= totalPaginas - 1);
    }
}
