package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.ConciliacionJpa;
import com.magnetixdian.infrastructure.persistence.ConciliacionLineaJpa;
import com.magnetixdian.infrastructure.persistence.ConciliacionRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.ConciliacionLineaDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ServicioConciliacion {

    private static final String COINCIDE = "COINCIDE";
    private static final String DIFERENCIA = "DIFERENCIA";
    private static final String SIN_ASIENTO = "SIN_ASIENTO";
    private static final String INFO = "INFO";
    private static final String ADVERTENCIA = "ADVERTENCIA";
    private static final String ERROR = "ERROR";
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final DocumentoRadianRepository documentoRepository;
    private final AsientoContableRepository asientoRepository;
    private final ConciliacionRepository conciliacionRepository;

    public ServicioConciliacion(DocumentoRadianRepository documentoRepository,
                                AsientoContableRepository asientoRepository,
                                ConciliacionRepository conciliacionRepository) {
        this.documentoRepository = documentoRepository;
        this.asientoRepository = asientoRepository;
        this.conciliacionRepository = conciliacionRepository;
    }

    public ResultadoConciliacionDto conciliar(Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
                                              String direccion, String tipoDocumento) {
        List<DocumentoRadianJpa> documentos = documentoRepository.buscar(
                empresaId, direccion, tipoDocumento, null, fechaDesde, fechaHasta);
        List<AsientoContableJpa> asientos = asientoRepository.findByEmpresaIdAndDocumentoTipo(
                empresaId, ServicioDocumentosRadian.TIPO_DOCUMENTO);
        Map<Long, AsientoContableJpa> asientosPorDocumento = new HashMap<>();
        for (AsientoContableJpa asiento : asientos) {
            asientosPorDocumento.putIfAbsent(asiento.getDocumentoId(), asiento);
        }

        ConciliacionJpa conciliacion = new ConciliacionJpa();
        conciliacion.setEmpresaId(empresaId);
        conciliacion.setFechaDesde(fechaDesde);
        conciliacion.setFechaHasta(fechaHasta);
        conciliacion.setTotalDocumentos(documentos.size());

        int coincidencias = 0;
        int diferencias = 0;
        int sinAsiento = 0;
        BigDecimal valorDiferencia = CERO;

        for (DocumentoRadianJpa documento : documentos) {
            AsientoContableJpa asiento = asientosPorDocumento.get(documento.getId());
            boolean documentoDiferente = false;
            if (asiento == null) {
                documentoDiferente = true;
                sinAsiento++;
                conciliacion.agregarLinea(linea(documento, null, SIN_ASIENTO, ERROR,
                        "ASIENTO", CERO, CERO, CERO,
                        "El documento no tiene un asiento contable radicado."));
            } else {
                ValorComparado total = comparar(documento, asiento, "TOTAL",
                        documento.getTotal(), asiento.getValorTotal(), ERROR);
                ValorComparado impuestos = comparar(documento, asiento, "IMPUESTOS",
                        documento.getImpuestos(), asiento.getValorImpuestos(), ADVERTENCIA);
                ValorComparado retenciones = comparar(documento, asiento, "RETENCIONES",
                        documento.getRetenciones(), asiento.getValorRetenciones(), ADVERTENCIA);
                conciliacion.agregarLinea(total.linea());
                conciliacion.agregarLinea(impuestos.linea());
                conciliacion.agregarLinea(retenciones.linea());
                valorDiferencia = valorDiferencia
                        .add(total.diferenciaAbsoluta())
                        .add(impuestos.diferenciaAbsoluta())
                        .add(retenciones.diferenciaAbsoluta());
                documentoDiferente = total.diferente() || impuestos.diferente() || retenciones.diferente();

                if (asiento.getTotalDebito() != null && asiento.getTotalCredito() != null
                        && asiento.getTotalDebito().compareTo(asiento.getTotalCredito()) != 0) {
                    documentoDiferente = true;
                    BigDecimal desbalance = asiento.getTotalDebito()
                            .subtract(asiento.getTotalCredito()).abs();
                    valorDiferencia = valorDiferencia.add(desbalance);
                    conciliacion.agregarLinea(linea(documento, asiento, DIFERENCIA, ERROR,
                            "BALANCE", asiento.getTotalDebito(), asiento.getTotalCredito(), desbalance,
                            "El asiento contable no está balanceado."));
                }
            }
            if (documentoDiferente) {
                diferencias++;
            } else {
                coincidencias++;
            }
        }

        conciliacion.setCoincidencias(coincidencias);
        conciliacion.setDiferencias(diferencias);
        conciliacion.setDocumentosSinAsiento(sinAsiento);
        conciliacion.setValorDiferencia(valorDiferencia);
        ConciliacionJpa guardada = conciliacionRepository.saveAndFlush(conciliacion);
        ConciliacionJpa resultado = guardada == null ? conciliacion : guardada;

        List<ConciliacionLineaDto> lineas = resultado.getLineas().stream()
                .map(ServicioConciliacion::mapear).toList();
        return new ResultadoConciliacionDto(
                resultado.getId(),
                empresaId,
                fechaDesde,
                fechaHasta,
                documentos.size(),
                coincidencias,
                diferencias,
                sinAsiento,
                valorDiferencia,
                lineas);
    }

    private static ValorComparado comparar(DocumentoRadianJpa documento, AsientoContableJpa asiento,
                                           String campo, BigDecimal valorDocumento,
                                           BigDecimal valorAsiento, String severidad) {
        BigDecimal documentoEscalado = escala(valorDocumento);
        BigDecimal asientoEscalado = escala(valorAsiento);
        BigDecimal diferencia = documentoEscalado.subtract(asientoEscalado);
        boolean distinto = diferencia.compareTo(CERO) != 0;
        ConciliacionLineaJpa linea = linea(documento, asiento,
                distinto ? DIFERENCIA : COINCIDE,
                distinto ? severidad : INFO,
                campo,
                documentoEscalado,
                asientoEscalado,
                diferencia.abs(),
                distinto ? "El valor del documento difiere del valor contabilizado."
                        : "El valor del documento coincide con el valor contabilizado.");
        return new ValorComparado(distinto, diferencia.abs(), linea);
    }

    private static ConciliacionLineaJpa linea(DocumentoRadianJpa documento, AsientoContableJpa asiento,
                                              String resultado, String severidad, String campo,
                                              BigDecimal valorDocumento, BigDecimal valorAsiento,
                                              BigDecimal diferencia, String mensaje) {
        ConciliacionLineaJpa linea = new ConciliacionLineaJpa();
        linea.setDocumentoRadianId(documento.getId());
        linea.setConsecutivo(documento.getConsecutivo());
        linea.setAsientoContableId(asiento == null ? null : asiento.getId());
        linea.setResultado(resultado);
        linea.setSeveridad(severidad);
        linea.setCampo(campo);
        linea.setValorDocumento(escala(valorDocumento));
        linea.setValorAsiento(escala(valorAsiento));
        linea.setDiferencia(escala(diferencia));
        linea.setMensaje(mensaje);
        return linea;
    }

    private static ConciliacionLineaDto mapear(ConciliacionLineaJpa linea) {
        return new ConciliacionLineaDto(
                linea.getDocumentoRadianId(),
                linea.getConsecutivo(),
                linea.getAsientoContableId(),
                linea.getResultado(),
                linea.getSeveridad(),
                linea.getCampo(),
                linea.getValorDocumento(),
                linea.getValorAsiento(),
                linea.getDiferencia(),
                linea.getMensaje());
    }

    private static BigDecimal escala(BigDecimal valor) {
        if (valor == null) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private record ValorComparado(boolean diferente, BigDecimal diferenciaAbsoluta,
                                  ConciliacionLineaJpa linea) {
    }
}
