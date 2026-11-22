package com.magnetixdian.application;

import com.magnetixdian.domain.contabilidad.GeneradorAsientoRadian;
import com.magnetixdian.domain.radian.EstadoCausacion;
import com.magnetixdian.infrastructure.persistence.AsientoContableJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableLineaJpa;
import com.magnetixdian.infrastructure.persistence.AsientoContableRepository;
import com.magnetixdian.infrastructure.persistence.CuentaContableJpa;
import com.magnetixdian.infrastructure.persistence.CuentaContableRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoAsientoJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.AsientoContableLineaDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ServicioContabilizacion {

    private final DocumentoRadianRepository documentoRepository;
    private final CuentaContableRepository cuentaRepository;
    private final AsientoContableRepository asientoRepository;

    public ServicioContabilizacion(DocumentoRadianRepository documentoRepository,
                                   CuentaContableRepository cuentaRepository,
                                   AsientoContableRepository asientoRepository) {
        this.documentoRepository = documentoRepository;
        this.cuentaRepository = cuentaRepository;
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
        GeneradorAsientoRadian.Asiento generado = GeneradorAsientoRadian.generar(dto);
        Map<String, CuentaContableJpa> cuentas = asegurarPlanCuentas(empresaId);

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
        for (GeneradorAsientoRadian.Linea linea : generado.lineas()) {
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
                asiento.getEstado(),
                asiento.getTotalDebito(),
                asiento.getTotalCredito(),
                asiento.getValorTotal(),
                asiento.getValorImpuestos(),
                asiento.getValorRetenciones(),
                asiento.getCreatedAt(),
                lineas);
    }

    private Map<String, CuentaContableJpa> asegurarPlanCuentas(Long empresaId) {
        List<CuentaContableJpa> propias = cuentaRepository.findByEmpresaId(empresaId);
        List<CuentaContableJpa> globales = cuentaRepository.findByEmpresaIdIsNull();
        Map<String, CuentaContableJpa> porCodigo = new LinkedHashMap<>();
        for (CuentaContableJpa cuenta : globales) {
            porCodigo.put(cuenta.getCodigo(), cuenta);
        }
        for (CuentaContableJpa cuenta : propias) {
            porCodigo.put(cuenta.getCodigo(), cuenta);
        }
        List<CuentaContableJpa> faltantes = new ArrayList<>();
        for (CuentaContableJpa cuenta : globales) {
            boolean existe = propias.stream()
                    .anyMatch(propia -> propia.getCodigo().equals(cuenta.getCodigo()));
            if (!existe) {
                CuentaContableJpa copia = new CuentaContableJpa();
                copia.setEmpresaId(empresaId);
                copia.setCodigo(cuenta.getCodigo());
                copia.setNombre(cuenta.getNombre());
                copia.setTipo(cuenta.getTipo());
                copia.setActiva(cuenta.isActiva());
                faltantes.add(copia);
            }
        }
        if (!faltantes.isEmpty()) {
            List<CuentaContableJpa> guardadas = cuentaRepository.saveAll(faltantes);
            if (guardadas == null) {
                guardadas = faltantes;
            }
            for (CuentaContableJpa cuenta : guardadas) {
                porCodigo.put(cuenta.getCodigo(), cuenta);
            }
        }
        return porCodigo;
    }
}
