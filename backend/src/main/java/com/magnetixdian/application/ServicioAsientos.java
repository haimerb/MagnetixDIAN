package com.magnetixdian.application;

import com.magnetixdian.domain.contabilidad.MotorAsientos;
import com.magnetixdian.domain.contabilidad.PlanCuentas;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableJpa;
import com.magnetixdian.infrastructure.persistence.PeriodoContableRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import com.magnetixdian.interfaces.dto.SolicitudLineaAsientoDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ServicioAsientos {

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("MANUAL", "AJUSTE", "REVERSION", "CIERRE", "APERTURA");
    private static final String CUENTA_CIERRE = "399999";
    private static final String CUENTA_RESULTADO = "320505";
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AsientoContableRepository asientoRepository;
    private final PeriodoContableRepository periodoRepository;
    private final ServicioPlanCuentas servicioPlanCuentas;
    private EntityManager entityManager;

    public ServicioAsientos(AsientoContableRepository asientoRepository,
                            PeriodoContableRepository periodoRepository,
                            ServicioPlanCuentas servicioPlanCuentas) {
        this.asientoRepository = asientoRepository;
        this.periodoRepository = periodoRepository;
        this.servicioPlanCuentas = servicioPlanCuentas;
    }

    @Autowired(required = false)
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public AsientoContableDto crear(Long empresaId, SolicitudAsientoDto solicitud) {
        if (solicitud == null || solicitud.fecha() == null) {
            throw new IllegalArgumentException("La fecha del asiento es obligatoria.");
        }
        String tipoEvento = normalizarTipo(solicitud.tipoEvento());
        Optional<AsientoContableJpa> previo = buscarPorOrigen(empresaId, solicitud.origen(),
                solicitud.origenId(), tipoEvento);
        if (previo.isPresent()) {
            return ServicioContabilizacion.mapear(previo.get());
        }
        List<MotorAsientos.Linea> lineas = new ArrayList<>();
        if (solicitud.lineas() == null) {
            throw new IllegalArgumentException("El asiento debe tener líneas.");
        }
        for (SolicitudLineaAsientoDto linea : solicitud.lineas()) {
            String descripcion = linea.descripcion() == null || linea.descripcion().isBlank()
                    ? PlanCuentas.nombre(linea.codigoCuenta())
                    : linea.descripcion();
            lineas.add(new MotorAsientos.Linea(linea.codigoCuenta(), descripcion,
                    linea.debito(), linea.credito(), linea.centroCostoId(), linea.terceroId(),
                    linea.baseImponible(), linea.impuestoCodigo()));
        }
        MotorAsientos.Resultado resultado = MotorAsientos.validar(lineas);
        PeriodoContableJpa periodo = periodoAbierto(empresaId, solicitud.fecha());
        return guardar(empresaId, solicitud.fecha(), periodo, solicitud.descripcion(), tipoEvento,
                solicitud.origen(), solicitud.origenId(), null, resultado);
    }

    public AsientoContableDto reversar(Long empresaId, Long asientoId) {
        AsientoContableJpa original = asientoRepository.findById(asientoId)
                .filter(asiento -> empresaId.equals(asiento.getEmpresaId()))
                .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado."));
        if ("ANULADO".equals(original.getEstado())) {
            throw new IllegalArgumentException("El asiento ya está anulado.");
        }
        Optional<AsientoContableJpa> previo = buscarPorOrigen(empresaId, "REVERSION", asientoId, "REVERSION");
        if (previo.isPresent()) {
            return ServicioContabilizacion.mapear(previo.get());
        }
        PeriodoContableJpa periodo = periodoAbierto(empresaId, original.getFecha());
        MotorAsientos.Resultado invertido = MotorAsientos.invertir(resultadoDe(original),
                "Reversión de asiento " + original.getNumero());
        AsientoContableDto reversa = guardar(empresaId, original.getFecha(), periodo,
                "Reversión de " + original.getNumero(), "REVERSION", "REVERSION", asientoId,
                original.getId(), invertido);
        original.setEstado("ANULADO");
        asientoRepository.save(original);
        return reversa;
    }

    public AsientoContableDto cerrarPeriodo(Long empresaId, Long periodoId) {
        PeriodoContableJpa periodo = periodoRepository.findById(periodoId)
                .filter(entidad -> empresaId.equals(entidad.getEmpresaId()))
                .orElseThrow(() -> new IllegalArgumentException("Periodo contable no encontrado."));
        if (!"ABIERTO".equals(periodo.getEstado())) {
            throw new IllegalArgumentException("El periodo contable ya está cerrado.");
        }
        Optional<AsientoContableJpa> previo = buscarPorOrigen(empresaId, "CIERRE", periodoId, "CIERRE");
        if (previo.isPresent()) {
            return ServicioContabilizacion.mapear(previo.get());
        }
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);
        List<AsientoContableJpa> asientos = asientoRepository
                .findByEmpresaIdAndPeriodoIdOrderByNumeroAsc(empresaId, periodoId);
        Map<String, BigDecimal> saldos = new LinkedHashMap<>();
        for (AsientoContableJpa asiento : asientos) {
            if (!"CONTABILIZADO".equals(asiento.getEstado()) && !"APROBADO".equals(asiento.getEstado())) {
                continue;
            }
            for (AsientoContableLineaJpa linea : asiento.getLineas()) {
                String codigo = linea.getCuentaContable().getCodigo();
                if (CUENTA_CIERRE.equals(codigo) || CUENTA_RESULTADO.equals(codigo)) {
                    continue;
                }
                CuentaContableJpa cuenta = cuentas.get(codigo);
                if (cuenta == null) {
                    throw new IllegalArgumentException("Cuenta contable no configurada: " + codigo);
                }
                BigDecimal saldo = saldos.getOrDefault(codigo, CERO);
                if ("ACREEDORA".equals(cuenta.getNaturaleza())) {
                    saldo = saldo.add(linea.getCredito()).subtract(linea.getDebito());
                } else {
                    saldo = saldo.add(linea.getDebito()).subtract(linea.getCredito());
                }
                saldos.put(codigo, saldo);
            }
        }
        List<MotorAsientos.Linea> lineasCierre = new ArrayList<>();
        BigDecimal totalIngresos = CERO;
        BigDecimal totalGastos = CERO;
        for (Map.Entry<String, BigDecimal> entrada : saldos.entrySet()) {
            CuentaContableJpa cuenta = cuentas.get(entrada.getKey());
            BigDecimal saldo = entrada.getValue();
            if (saldo.signum() == 0) {
                continue;
            }
            if ("INGRESO".equals(cuenta.getTipo())) {
                lineasCierre.add(MotorAsientos.linea(cuenta.getCodigo(), saldo, CERO, "Cierre de ingresos"));
                lineasCierre.add(MotorAsientos.linea(CUENTA_CIERRE, CERO, saldo, "Cierre de ingresos"));
                totalIngresos = totalIngresos.add(saldo);
            } else if ("EGRESO".equals(cuenta.getTipo())) {
                lineasCierre.add(MotorAsientos.linea(CUENTA_CIERRE, saldo, CERO, "Cierre de gastos"));
                lineasCierre.add(MotorAsientos.linea(cuenta.getCodigo(), CERO, saldo, "Cierre de gastos"));
                totalGastos = totalGastos.add(saldo);
            }
        }
        if (lineasCierre.isEmpty()) {
            throw new IllegalArgumentException("El periodo no tiene movimientos para cerrar.");
        }
        BigDecimal resultado = totalIngresos.subtract(totalGastos);
        if (resultado.signum() > 0) {
            lineasCierre.add(MotorAsientos.linea(CUENTA_CIERRE, resultado, CERO, "Cierre del resultado"));
            lineasCierre.add(MotorAsientos.linea(CUENTA_RESULTADO, CERO, resultado, "Utilidad del periodo"));
        } else if (resultado.signum() < 0) {
            BigDecimal perdida = resultado.abs();
            lineasCierre.add(MotorAsientos.linea(CUENTA_RESULTADO, perdida, CERO, "Pérdida del periodo"));
            lineasCierre.add(MotorAsientos.linea(CUENTA_CIERRE, CERO, perdida, "Cierre del resultado"));
        }
        MotorAsientos.Resultado resultadoCierre = MotorAsientos.validar(lineasCierre);
        AsientoContableDto asientoCierre = guardar(empresaId, periodo.getFechaHasta(), periodo,
                "Cierre del periodo " + periodo.getAnio() + "-" + periodo.getMes(), "CIERRE",
                "CIERRE", periodoId, null, resultadoCierre);
        periodo.setEstado("CERRADO");
        periodo.setCerradoEn(OffsetDateTime.now());
        periodo.setUpdatedAt(OffsetDateTime.now());
        periodoRepository.save(periodo);
        return asientoCierre;
    }

    private AsientoContableDto guardar(Long empresaId, LocalDate fecha, PeriodoContableJpa periodo,
                                       String descripcion, String tipoEvento, String origen,
                                       Long origenId, Long reversaDeId, MotorAsientos.Resultado resultado) {
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);
        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setEmpresaId(empresaId);
        asiento.setNumero(siguienteNumero(empresaId, periodo.getId(), fecha));
        asiento.setFecha(fecha);
        asiento.setDescripcion(descripcion);
        asiento.setTipoEvento(tipoEvento);
        asiento.setEstado("CONTABILIZADO");
        asiento.setPeriodoId(periodo.getId());
        asiento.setOrigen(origen);
        asiento.setOrigenId(origenId);
        asiento.setReversaDeId(reversaDeId);
        asiento.setTotalDebito(resultado.totalDebito());
        asiento.setTotalCredito(resultado.totalCredito());
        int numeroLinea = 1;
        for (MotorAsientos.Linea linea : resultado.lineas()) {
            CuentaContableJpa cuenta = cuentas.get(linea.codigoCuenta());
            if (cuenta == null) {
                throw new IllegalArgumentException("Cuenta contable no configurada: " + linea.codigoCuenta());
            }
            AsientoContableLineaJpa entidadLinea = new AsientoContableLineaJpa();
            entidadLinea.setNumeroLinea(numeroLinea++);
            entidadLinea.setCuentaContable(cuenta);
            entidadLinea.setDescripcion(linea.descripcion());
            entidadLinea.setDebito(linea.debito());
            entidadLinea.setCredito(linea.credito());
            entidadLinea.setCentroCostoId(linea.centroCostoId());
            entidadLinea.setTerceroId(linea.terceroId());
            entidadLinea.setBaseImponible(linea.baseImponible());
            entidadLinea.setImpuestoCodigo(linea.impuestoCodigo());
            asiento.agregarLinea(entidadLinea);
        }
        AsientoContableJpa guardado = asientoRepository.saveAndFlush(asiento);
        return ServicioContabilizacion.mapear(guardado == null ? asiento : guardado);
    }

    private MotorAsientos.Resultado resultadoDe(AsientoContableJpa asiento) {
        List<MotorAsientos.Linea> lineas = asiento.getLineas().stream()
                .map(linea -> new MotorAsientos.Linea(
                        linea.getCuentaContable().getCodigo(),
                        linea.getDescripcion(),
                        linea.getDebito(),
                        linea.getCredito(),
                        linea.getCentroCostoId(),
                        linea.getTerceroId(),
                        linea.getBaseImponible(),
                        linea.getImpuestoCodigo()))
                .toList();
        return MotorAsientos.validar(lineas);
    }

    private Optional<AsientoContableJpa> buscarPorOrigen(Long empresaId, String origen,
                                                         Long origenId, String tipoEvento) {
        if (origen == null) {
            return Optional.empty();
        }
        return asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                empresaId, origen, origenId, tipoEvento);
    }

    private PeriodoContableJpa periodoAbierto(Long empresaId, LocalDate fecha) {
        PeriodoContableJpa periodo = periodoRepository.findVigentePorFecha(empresaId, fecha)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe periodo contable para la fecha " + fecha + "."));
        if (!"ABIERTO".equals(periodo.getEstado())) {
            throw new IllegalArgumentException("El periodo contable está cerrado.");
        }
        return periodo;
    }

    private String siguienteNumero(Long empresaId, Long periodoId, LocalDate fecha) {
        if (entityManager != null) {
            entityManager.find(PeriodoContableJpa.class, periodoId, LockModeType.PESSIMISTIC_WRITE);
        }
        long secuencial = asientoRepository.findByEmpresaIdAndPeriodoIdOrderByNumeroAsc(empresaId, periodoId).size() + 1L;
        return String.format(Locale.ROOT, "AS-%d-%04d%02d-%04d",
                empresaId, fecha.getYear(), fecha.getMonthValue(), secuencial);
    }

    private static String normalizarTipo(String tipoEvento) {
        String tipo = tipoEvento == null || tipoEvento.isBlank() ? "AJUSTE" : tipoEvento.toUpperCase(Locale.ROOT);
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de asiento no soportado: " + tipoEvento);
        }
        return tipo;
    }
}
