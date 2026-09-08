package com.magnetixdian.application;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.infrastructure.excel.ImportadorExcel;
import com.magnetixdian.infrastructure.excel.PlantillaExcelGenerator;
import com.magnetixdian.infrastructure.xml.GeneradorXml1001;
import com.magnetixdian.infrastructure.xml.GeneradorXml1002;
import com.magnetixdian.interfaces.dto.SugerenciaDvDto;
import com.magnetixdian.interfaces.dto.TercerosResumenDto;
import com.magnetixdian.infrastructure.persistence.DetalleErrorJpa;
import com.magnetixdian.infrastructure.persistence.DetalleErrorRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.infrastructure.persistence.OperacionJpa;
import com.magnetixdian.infrastructure.persistence.OperacionRepository;
import com.magnetixdian.infrastructure.persistence.ValidacionJpa;
import com.magnetixdian.infrastructure.persistence.ValidacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Orquesta el ciclo de vida de un medio magnético: creación, carga de
 * operaciones desde archivo, validación contra el motor de reglas y
 * persistencia de resultados.
 */
@Service
public class ServicioMedioMagnetico {

    private final MedioMagneticoRepository medioRepo;
    private final OperacionRepository operacionRepo;
    private final EmpresaRepository empresaRepo;
    private final ValidacionRepository validacionRepo;
    private final DetalleErrorRepository detalleRepo;
    private final ImportadorExcel importadorExcel;
    private final PlantillaExcelGenerator plantillaExcel;
    private final ServicioValidacion servicioValidacion;
    private final GeneradorXml1001 generadorXml1001;
    private final GeneradorXml1002 generadorXml1002;

    public ServicioMedioMagnetico(MedioMagneticoRepository medioRepo,
                                  OperacionRepository operacionRepo,
                                  EmpresaRepository empresaRepo,
                                  ValidacionRepository validacionRepo,
                                  DetalleErrorRepository detalleRepo,
                                  ImportadorExcel importadorExcel,
                                  PlantillaExcelGenerator plantillaExcel,
                                  ServicioValidacion servicioValidacion,
                                  GeneradorXml1001 generadorXml1001,
                                  GeneradorXml1002 generadorXml1002) {
        this.medioRepo = medioRepo;
        this.operacionRepo = operacionRepo;
        this.empresaRepo = empresaRepo;
        this.validacionRepo = validacionRepo;
        this.detalleRepo = detalleRepo;
        this.importadorExcel = importadorExcel;
        this.plantillaExcel = plantillaExcel;
        this.servicioValidacion = servicioValidacion;
        this.generadorXml1001 = generadorXml1001;
        this.generadorXml1002 = generadorXml1002;
    }

    /**
     * Genera la plantilla Excel descargable del formato indicado.
     */
    public byte[] generarPlantilla(String formato) throws IOException {
        return plantillaExcel.generar(formato);
    }

    /**
     * Crea (o recupera) el medio magnético del formato/año indicado para una
     * empresa. Aplica idempotencia: si ya existe, lo retorna.
     */
    @Transactional
    public MedioMagneticoJpa crearOReusar(Long empresaId, String formato, Integer anioGravable) {
        return medioRepo.findByEmpresaIdAndFormatoAndAnioGravable(empresaId, formato, anioGravable)
                .orElseGet(() -> {
                    var empresa = empresaRepo.findById(empresaId)
                            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + empresaId));
                    return medioRepo.save(new MedioMagneticoJpa(empresa, formato, anioGravable));
                });
    }

    /**
     * Carga operaciones desde Excel y las persiste reemplazando las existentes.
     */
    @Transactional
    public CargaResultado cargarDesdeExcel(Long empresaId, String formato, Integer anioGravable,
                                           MultipartFile archivo) throws IOException {
        MedioMagneticoJpa medio = crearOReusar(empresaId, formato, anioGravable);
        Long medioId = medio.getId();

        operacionRepo.borrarPorMedio(medioId);

        List<OperacionDatos> datos = importadorExcel.importar(archivo);
        for (OperacionDatos dato : datos) {
            OperacionJpa op = new OperacionJpa();
            op.persistir(medio, dato);
            operacionRepo.save(op);
        }

        medio.setEstado("CARGADO");
        medio.touch();
        medioRepo.save(medio);

        return new CargaResultado(medioId, datos.size());
    }

    /**
     * Ejecuta las reglas de validación sobre todas las operaciones del medio
     * magnético y persiste la corrida + detalles de error.
     */
    @Transactional
    public ValidacionJpa validar(Long medioMagneticoId, Integer anioGravable) {
        MedioMagneticoJpa medio = medioRepo.findById(medioMagneticoId)
                .orElseThrow(() -> new IllegalArgumentException("Medio magnético no encontrado: " + medioMagneticoId));

        List<OperacionJpa> operaciones = operacionRepo.findByMedioMagneticoId(medioMagneticoId);

        ValidacionJpa validacion = validacionRepo.save(new ValidacionJpa(medio));

        int errores = 0;
        int advertencias = 0;

        for (OperacionJpa op : operaciones) {
            OperacionDatos datos = toDatos(op);
            List<ResultadoRegla> resultados = servicioValidacion.validar(datos, anioGravable, null, medio.getFormato());
            for (ResultadoRegla r : resultados) {
                if (r.valida() || r.severidad() == null) {
                    continue;
                }
                DetalleErrorJpa detalle = new DetalleErrorJpa();
                detalle.cargar(null, r.linea(), r.campo(), r.valorEsperado(), r.valorEncontrado(),
                        r.mensaje(), r.severidad().name());
                validacion.agregarDetalle(detalle);
                if (r.severidad() == Severidad.ERROR) {
                    errores++;
                } else {
                    advertencias++;
                }
            }
        }

        validacion.completar(errores, advertencias, operaciones.size());
        validacionRepo.save(validacion);

        medio.setEstado(errores == 0 ? "VALIDADO" : "VALIDADO_CON_ERRORES");
        medio.touch();
        medioRepo.save(medio);

        return validacion;
    }

    private OperacionDatos toDatos(OperacionJpa op) {
        return new OperacionDatos(
                op.getLinea(), op.getTipoDocumento(), op.getNumeroIdentificacion(), op.getDv(),
                op.getConcepto(), op.getValorPago(), op.getRetencionRenta(), op.getRetencionIva(),
                op.getRetencionIca(), op.getRetencionTimbre(), op.getIvaPagado(),
                op.getIvaDescontable(), op.getValorGasto(), op.getValorCosto(), op.getValorNc(),
                op.isCuantiaMenor());
    }

    public List<MedioMagneticoJpa> listarPorEmpresa(Long empresaId) {
        return medioRepo.findByEmpresaIdOrderByCreatedAtDesc(empresaId);
    }

    public List<com.magnetixdian.interfaces.dto.MedioMagneticoDto> listarPorEmpresaDto(Long empresaId) {
        return listarPorEmpresa(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    public com.magnetixdian.interfaces.dto.ValidacionProcesoDto validarDto(Long medioMagneticoId, Integer anioGravable) {
        ValidacionJpa corrida = validar(medioMagneticoId, anioGravable);
        return toDto(corrida);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.magnetixdian.interfaces.dto.ValidacionProcesoDto reporteDto(Long medioMagneticoId) {
        return validacionRepo.findTopByMedioMagneticoIdOrderByInicioDesc(medioMagneticoId)
                .map(this::toDto)
                .orElse(null);
    }

    private com.magnetixdian.interfaces.dto.MedioMagneticoDto toDto(MedioMagneticoJpa m) {
        var empresa = m.getEmpresa();
        return new com.magnetixdian.interfaces.dto.MedioMagneticoDto(
                m.getId(), empresa.getId(), empresa.getNit(), empresa.getRazonSocial(),
                m.getFormato(), m.getVersionFormato(), m.getAnioGravable(), m.getEstado(),
                m.getFechaLimite(), m.getCreatedAt(), m.getUpdatedAt());
    }

    private com.magnetixdian.interfaces.dto.ValidacionProcesoDto toDto(ValidacionJpa v) {
        List<com.magnetixdian.interfaces.dto.ValidacionProcesoDto.DetalleErrorDto> errores = detalleRepo
                .findByValidacionIdOrderByLineaAsc(v.getId()).stream()
                .map(d -> new com.magnetixdian.interfaces.dto.ValidacionProcesoDto.DetalleErrorDto(
                        d.getLinea(), d.getCampo(), d.getValorEsperado(), d.getValorEncontrado(),
                        d.getMensaje(), d.getSeveridad()))
                .toList();
        return new com.magnetixdian.interfaces.dto.ValidacionProcesoDto(
                v.getId(), v.getMedioMagnetico().getId(), v.getEstado(),
                v.getErroresTotal(), v.getAdvertenciasTotal(), v.getRegistrosValidados(),
                v.getInicio(), v.getFin(), errores);
    }

    public long contarOperaciones(Long medioMagneticoId) {
        return operacionRepo.countByMedioMagneticoId(medioMagneticoId);
    }

    /**
     * Genera el XML del medio magnético y lo retorna como String.
     * Requiere que las operaciones estén validadas (marca XML_GENERADO).
     */
    @org.springframework.transaction.annotation.Transactional
    public String generarXml(Long medioMagneticoId) {
        MedioMagneticoJpa medio = medioRepo.findById(medioMagneticoId)
                .orElseThrow(() -> new IllegalArgumentException("Medio magnético no encontrado: " + medioMagneticoId));
        if ("BORRADOR".equals(medio.getEstado()) || "CARGADO".equals(medio.getEstado())) {
            throw new IllegalStateException("Debe validar el medio magnético antes de generar el XML.");
        }
        List<OperacionJpa> operaciones = operacionRepo.findByMedioMagneticoId(medioMagneticoId);
        String xml = "1002".equalsIgnoreCase(medio.getFormato())
                ? generadorXml1002.generar(medio, operaciones)
                : generadorXml1001.generar(medio, operaciones);
        medio.setEstado("XML_GENERADO");
        medio.touch();
        medioRepo.save(medio);
        return xml;
    }

    /**
     * Marca el medio magnético como presentado ante la DIAN.
     * Solo se permite partiendo de un estado ya validado.
     */
    @org.springframework.transaction.annotation.Transactional
    public com.magnetixdian.interfaces.dto.MedioMagneticoDto marcarPresentado(Long medioMagneticoId) {
        MedioMagneticoJpa medio = medioRepo.findById(medioMagneticoId)
                .orElseThrow(() -> new IllegalArgumentException("Medio magnético no encontrado: " + medioMagneticoId));
        String estado = medio.getEstado();
        if ("BORRADOR".equals(estado) || "CARGADO".equals(estado)) {
            throw new IllegalStateException("Debe validar el medio magnético antes de marcarlo como presentado.");
        }
        medio.setEstado("PRESENTADO");
        medio.touch();
        return toDto(medioRepo.save(medio));
    }

    /**
     * Formato del medio magnético (para nombres de descarga).
     */
    public String formatoDe(Long medioMagneticoId) {
        return medioRepo.findById(medioMagneticoId)
                .map(MedioMagneticoJpa::getFormato)
                .orElse("1001");
    }

    // ------------------------------------------------------------------
    // Terceros y normalización de DV (catálogo NIT)
    // ------------------------------------------------------------------

    /**
     * Resume los terceros reportados en el medio: total de registros, terceros
     * únicos y cantidad de NIT con dígito de verificación inconsistente.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TercerosResumenDto resumenTerceros(Long medioMagneticoId) {
        List<OperacionJpa> operaciones = operacionRepo.findByMedioMagneticoId(medioMagneticoId);
        long tercerosUnicos = operaciones.stream()
                .map(op -> op.getTipoDocumento() + "|" + op.getNumeroIdentificacion())
                .distinct()
                .count();
        List<SugerenciaDvDto> sugerencias = operaciones.stream()
                .map(this::sugerenciaDv)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new TercerosResumenDto(
                operaciones.size(),
                tercerosUnicos,
                sugerencias.size(),
                sugerencias);
    }

    /**
     * Aplica las correcciones de DV sugeridas y devuelve cuántos registros
     * fueron corregidos.
     */
    @org.springframework.transaction.annotation.Transactional
    public int aplicarCorreccionesDv(Long medioMagneticoId) {
        List<OperacionJpa> operaciones = operacionRepo.findByMedioMagneticoId(medioMagneticoId);
        int corregidos = 0;
        for (OperacionJpa op : operaciones) {
            SugerenciaDvDto sugerencia = sugerenciaDv(op);
            if (sugerencia != null) {
                op.setDv(sugerencia.dvEsperado());
                operacionRepo.save(op);
                corregidos++;
            }
        }
        return corregidos;
    }

    private SugerenciaDvDto sugerenciaDv(OperacionJpa op) {
        if (!"NIT".equalsIgnoreCase(op.getTipoDocumento())
                && !"NITCE".equalsIgnoreCase(op.getTipoDocumento())) {
            return null;
        }
        String numero = op.getNumeroIdentificacion();
        Integer dvEsperado = NitDigitoVerificacionRegla.calcularDv(numero == null ? "" : numero.replaceAll("[^0-9]", ""));
        if (dvEsperado == null || Integer.valueOf(dvEsperado).equals(op.getDv())) {
            return null;
        }
        return new SugerenciaDvDto(op.getId(), op.getLinea(), op.getTipoDocumento(),
                numero, op.getDv(), dvEsperado, op.getConcepto(), op.getValorPago());
    }

    public record CargaResultado(Long medioMagneticoId, int registrosCargados) {
    }
}