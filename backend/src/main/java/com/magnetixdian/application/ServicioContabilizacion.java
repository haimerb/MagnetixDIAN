package com.magnetixdian.application;

import com.magnetixdian.domain.contabilidad.MotorAsientos;
import com.magnetixdian.domain.radian.EstadoCausacion;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.AsientoContableLineaDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ServicioContabilizacion {

    private final DocumentoRadianRepository documentoRepository;
    private final ServicioPlanCuentas servicioPlanCuentas;
    private final AsientoContableRepository asientoRepository;

    public ServicioContabilizacion(DocumentoRadianRepository documentoRepository,
                                   ServicioPlanCuentas servicioPlanCuentas,
                                   AsientoContableRepository asientoRepository) {
        this.documentoRepository = documentoRepository;
        this.servicioPlanCuentas = servicioPlanCuentas;
        this.asientoRepository = asientoRepository;
    }

    public AsientoContableDto contabilizar(Long empresaId, Long documentoId) {
        DocumentoRadianJpa documento = documentoRepository.findByIdAndEmpresaId(documentoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Documento RADIAN no encontrado."));

        Optional<AsientoContableJpa> previo = asientoRepository
                .findByEmpresaIdAndDocumentoTipoAndDocumentoIdAndTipoEvento(
                        empresaId, ServicioDocumentosRadian.TIPO_DOCUMENTO,
                        documentoId, ServicioDocumentosRadian.TIPO_EVENTO_CAUSACION);
        if (previo.isPresent()) {
            return mapear(previo.get());
        }

        DocumentoRadianDto dto = ServicioDocumentosRadian.mapear(documento);
        MotorAsientos.Resultado generado = MotorAsientos.desdeDocumentoRadian(dto);
        Map<String, CuentaContableJpa> cuentas = servicioPlanCuentas.asegurar(empresaId);

        AsientoContableJpa asiento = new AsientoContableJpa();
        asiento.setEmpresaId(empresaId);
        asiento.setNumero("AS-" + empresaId + "-" + documentoId + "-CAUSACION");
        asiento.setFecha(dto.fecha());
        asiento.setDescripcion("Causación RADIAN " + dto.consecutivo());
        asiento.setDocumentoTipo(ServicioDocumentosRadian.TIPO_DOCUMENTO);
        asiento.setDocumentoId(documentoId);
        asiento.setTipoEvento(ServicioDocumentosRadian.TIPO_EVENTO_CAUSACION);
        asiento.setTotalDebito(generado.totalDebito());
        asiento.setTotalCredito(generado.totalCredito());
        asiento.setValorTotal(dto.total());
        asiento.setValorImpuestos(dto.impuestos());
        asiento.setValorRetenciones(dto.retenciones());

        int numeroLinea = 1;
        for (MotorAsientos.Linea linea : generado.lineas()) {
            CuentaContableJpa cuenta = cuentas.get(linea.codigoCuenta());
            if (cuenta == null) {
                throw new IllegalStateException("Cuenta contable no configurada: " + linea.codigoCuenta());
            }
            AsientoContableLineaJpa entidadLinea = new AsientoContableLineaJpa();
            entidadLinea.setNumeroLinea(numeroLinea++);
            entidadLinea.setCuentaContable(cuenta);
            entidadLinea.setDescripcion(linea.descripcion());
            entidadLinea.setDebito(linea.debito());
            entidadLinea.setCredito(linea.credito());
            asiento.agregarLinea(entidadLinea);
        }

        DocumentoAsientoJpa vinculo = new DocumentoAsientoJpa();
        vinculo.setEmpresaId(empresaId);
        vinculo.setDocumentoRadianId(documentoId);
        vinculo.setTipoEvento(ServicioDocumentosRadian.TIPO_EVENTO_CAUSACION);
        asiento.agregarDocumento(vinculo);

        AsientoContableJpa guardado = asientoRepository.saveAndFlush(asiento);
        documento.setEstadoCausacion(EstadoCausacion.CONTABILIZADA);
        documentoRepository.save(documento);
        return mapear(guardado == null ? asiento : guardado);
    }

    @Transactional(readOnly = true)
    public List<AsientoContableDto> listar(Long empresaId) {
        return asientoRepository.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream()
                .map(ServicioContabilizacion::mapear).toList();
    }

    @Transactional(readOnly = true)
    public Optional<AsientoContableDto> buscarPorOrigen(Long empresaId, String origen, Long origenId,
                                                        String tipoEvento) {
        return asientoRepository.findByEmpresaIdAndOrigenAndOrigenIdAndTipoEvento(
                        empresaId, origen, origenId, tipoEvento)
                .map(ServicioContabilizacion::mapear);
    }

    public static AsientoContableDto mapear(AsientoContableJpa asiento) {
        List<AsientoContableLineaDto> lineas = asiento.getLineas().stream()
                .map(linea -> new AsientoContableLineaDto(
                        linea.getId(),
                        linea.getCuentaContable().getCodigo(),
                        linea.getCuentaContable().getNombre(),
                        linea.getDescripcion(),
                        linea.getDebito(),
                        linea.getCredito()))
                .toList();
        return new AsientoContableDto(
                asiento.getId(),
                asiento.getEmpresaId(),
                asiento.getNumero(),
                asiento.getFecha(),
                asiento.getDescripcion(),
                asiento.getDocumentoTipo(),
                asiento.getDocumentoId(),
                asiento.getTipoEvento(),
                asiento.getOrigen(),
                asiento.getOrigenId(),
                asiento.getEstado(),
                asiento.getTotalDebito(),
                asiento.getTotalCredito(),
                asiento.getValorTotal(),
                asiento.getValorImpuestos(),
                asiento.getValorRetenciones(),
                asiento.getCreatedAt(),
                lineas);
    }
}
