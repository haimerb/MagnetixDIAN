package com.magnetixdian.application;

import com.magnetixdian.domain.exogena.AgregadorExogena;
import com.magnetixdian.domain.exogena.ClasificadorMovimientos;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.domain.exogena.FormatoTributarioCatalogo;
import com.magnetixdian.domain.exogena.GeneradorExcelExogena;
import com.magnetixdian.domain.exogena.GeneradorXml1001Exogena;
import com.magnetixdian.domain.exogena.MapeadorCuentaConcepto;
import com.magnetixdian.domain.exogena.ValidadorExogena;
import com.magnetixdian.domain.exogena.ValidadorXsdExogena;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.interfaces.dto.DianConceptoDto;
import com.magnetixdian.interfaces.dto.DatosDeclaranteDto;
import com.magnetixdian.interfaces.dto.DianCuentaConceptoDto;
import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import com.magnetixdian.interfaces.dto.ExogenaValidacionDto;
import com.magnetixdian.interfaces.dto.FormatoTributarioDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import com.magnetixdian.interfaces.dto.ResultadoCatalogoExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoExogenaGenerarDto;
import com.magnetixdian.interfaces.dto.SolicitudCatalogoExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudExogenaGenerarDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioExogena {

    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;
    private final FormatoTributarioCatalogo catalogoFormatos;
    private final FormatoExogenaRegistry registryFormatos;
    private final ClasificadorMovimientos clasificador;
    private final MapeadorCuentaConcepto mapeador;
    private final ValidadorExogena validador;
    private final AgregadorExogena agregador;
    private final GeneradorExcelExogena generadorExcel;
    private final GeneradorXml1001Exogena generadorXml;
    private final ValidadorXsdExogena validadorXsd;

    public ServicioExogena(JdbcTemplate jdbc, AuditoriaService auditoria,
                           FormatoTributarioCatalogo catalogoFormatos,
                           FormatoExogenaRegistry registryFormatos,
                           ClasificadorMovimientos clasificador,
                           MapeadorCuentaConcepto mapeador,
                           ValidadorExogena validador,
                           AgregadorExogena agregador,
                           GeneradorExcelExogena generadorExcel,
                           GeneradorXml1001Exogena generadorXml,
                           ValidadorXsdExogena validadorXsd) {
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.catalogoFormatos = catalogoFormatos;
        this.registryFormatos = registryFormatos;
        this.clasificador = clasificador;
        this.mapeador = mapeador;
        this.validador = validador;
        this.agregador = agregador;
        this.generadorExcel = generadorExcel;
        this.generadorXml = generadorXml;
        this.validadorXsd = validadorXsd;
    }

    public List<FormatoTributarioDto> formatos() {
        return catalogoFormatos.todos();
    }

    public List<DianConceptoDto> conceptos(Integer ano, String formato) {
        return jdbc.query("""
                SELECT id, ano, formato, codigo_concepto, descripcion, tipo_tercero,
                       tipo_documento, valor_minimo, activo
                FROM dian_concepto
                WHERE ano = ? AND (CAST(? AS VARCHAR) IS NULL OR formato = CAST(? AS VARCHAR))
                ORDER BY formato, codigo_concepto
                """, (rs, fila) -> new DianConceptoDto(
                rs.getLong("id"), rs.getInt("ano"), rs.getString("formato"),
                rs.getString("codigo_concepto"), rs.getString("descripcion"),
                rs.getString("tipo_tercero"), rs.getString("tipo_documento"),
                rs.getBigDecimal("valor_minimo"), rs.getBoolean("activo")),
                ano, formato, formato);
    }

    @Transactional
    public ResultadoCatalogoExogenaDto cargarCatalogo(Long empresaId, SolicitudCatalogoExogenaDto solicitud) {
        validarSolicitudCatalogo(solicitud);
        Integer ano = solicitud.ano();
        String formato = solicitud.formato();
        List<DianConceptoDto> conceptos = solicitud.conceptos() == null ? List.of() : solicitud.conceptos();
        List<Object[]> conceptosBatch = new ArrayList<>();
        for (DianConceptoDto concepto : conceptos) {
            conceptosBatch.add(new Object[]{
                    ano, formato, concepto.codigo(), concepto.descripcion(), concepto.tipoTercero(),
                    concepto.tipoDocumento(), concepto.valorMinimo() == null ? BigDecimal.ZERO : concepto.valorMinimo(),
                    concepto.activo()});
        }
        jdbc.batchUpdate("""
                INSERT INTO dian_concepto (ano, formato, codigo_concepto, descripcion,
                                           tipo_tercero, tipo_documento, valor_minimo, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (ano, formato, codigo_concepto) DO UPDATE
                SET descripcion = EXCLUDED.descripcion,
                    tipo_tercero = EXCLUDED.tipo_tercero,
                    tipo_documento = EXCLUDED.tipo_documento,
                    valor_minimo = EXCLUDED.valor_minimo,
                    activo = EXCLUDED.activo,
                    updated_at = now()
                """, conceptosBatch);

        Map<String, Long> idsPorCodigo = conceptos(ano, formato).stream()
                .collect(Collectors.toMap(DianConceptoDto::codigo, DianConceptoDto::id, (a, b) -> a));
        List<Object[]> relacionesBatch = new ArrayList<>();
        List<DianCuentaConceptoDto> cuentas =
                solicitud.cuentas() == null ? List.of() : solicitud.cuentas();
        for (DianCuentaConceptoDto cuenta : cuentas) {
            Long conceptoId = idsPorCodigo.get(cuenta.codigoConcepto());
            if (conceptoId == null) {
                continue;
            }
            relacionesBatch.add(new Object[]{
                    empresaId, ano, cuenta.cuentaCodigo(), conceptoId,
                    cuenta.activo() == null || cuenta.activo()});
        }
        jdbc.batchUpdate("""
                INSERT INTO dian_cuenta_concepto (empresa_id, ano, cuenta_codigo, concepto_id, activo)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (COALESCE(empresa_id, 0), ano, cuenta_codigo, concepto_id) DO UPDATE
                SET activo = EXCLUDED.activo
                """, relacionesBatch);
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "CARGA_CATALOGO_EXOGENA",
                "DIAN_CONCEPTO", ano + "-" + formato,
                conceptosBatch.size() + " conceptos y " + relacionesBatch.size() + " relaciones");
        return new ResultadoCatalogoExogenaDto(ano, formato, conceptosBatch.size(), relacionesBatch.size());
    }

    @Transactional
    public List<MovimientoTributarioDto> clasificarDesdeAsientos(Long empresaId, Integer ano,
                                                                 List<MovimientoTributarioDto> movimientos) {
        List<MovimientoTributarioDto> clasificados = clasificador.clasificar(movimientos);
        if (clasificados.isEmpty()) {
            return List.of();
        }
        Map<String, Long> conceptosPorCodigo = new HashMap<>();
        for (FormatoTributarioDto formato : catalogoFormatos.todos()) {
            conceptosPorCodigo.putAll(conceptos(ano, formato.codigo()).stream()
                    .collect(Collectors.toMap(DianConceptoDto::codigo, DianConceptoDto::id, (a, b) -> a)));
        }
        Map<String, Long> cuentasMapa = new HashMap<>();
        jdbc.query("""
                SELECT cuenta_codigo, concepto_id
                FROM dian_cuenta_concepto
                WHERE ano = ? AND activo AND (empresa_id IS NULL OR empresa_id = ?)
                ORDER BY length(cuenta_codigo) DESC
                """, rs -> {
            cuentasMapa.putIfAbsent(rs.getString("cuenta_codigo"), rs.getLong("concepto_id"));
        }, ano, empresaId);

        List<MovimientoTributarioDto> resueltos = new ArrayList<>(clasificados.size());
        for (MovimientoTributarioDto movimiento : clasificados) {
            Long conceptoId = movimiento.conceptoId();
            if (conceptoId == null && movimiento.conceptoCodigo() != null) {
                conceptoId = conceptosPorCodigo.get(movimiento.conceptoCodigo());
            }
            if (conceptoId == null) {
                conceptoId = mapeador.mapear(movimiento.cuentaCodigo(), cuentasMapa, conceptosPorCodigo).orElse(null);
            }
            resueltos.add(new MovimientoTributarioDto(
                    empresaId, ano, movimiento.asientoId(), movimiento.asientoLineaId(), movimiento.terceroId(),
                    movimiento.tipoDocumento(), movimiento.numeroDocumento(), movimiento.digitoVerificacion(),
                    movimiento.razonSocial(), movimiento.municipio(), movimiento.pais(), movimiento.cuentaCodigo(),
                    movimiento.conceptoCodigo(), conceptoId, movimiento.valor(), movimiento.fecha(),
                    movimiento.origenTipoDocumento(), movimiento.origenNumeroDocumento(), movimiento.origen()));
        }
        insertarMovimientos(resueltos);
        return resueltos;
    }

    public List<MovimientoTributarioDto> movimientos(Long empresaId, Integer ano, String formato) {
        if (formato != null) {
            catalogoFormatos.validar(formato);
        }
        return jdbc.query("""
                SELECT m.empresa_id, m.ano, m.asiento_id, m.asiento_linea_id, m.tercero_id,
                       COALESCE(t.tipo_documento, m.tipo_documento) AS tipo_documento,
                       COALESCE(t.numero_documento, m.numero_documento) AS numero_documento,
                       COALESCE(t.dv, m.numero_documento_dv) AS digito_verificacion,
                       COALESCE(t.nombre, 'TERCERO ' || COALESCE(m.tercero_id::text, '0')) AS razon_social,
                       t.municipio, COALESCE(t.pais, m.pais, 'CO') AS pais,
                       m.cuenta_codigo, c.codigo_concepto, m.concepto_id, m.valor, m.fecha,
                       m.origen_tipo_documento, m.origen_numero_documento, m.origen
                FROM movimiento_tributario m
                LEFT JOIN dian_concepto c ON c.id = m.concepto_id
                LEFT JOIN tercero_contable t ON t.id = m.tercero_id
                WHERE m.empresa_id = ? AND m.ano = ?
                  AND (CAST(? AS VARCHAR) IS NULL OR c.formato = CAST(? AS VARCHAR) OR m.concepto_id IS NULL)
                ORDER BY m.fecha, m.id
                """, (rs, fila) -> new MovimientoTributarioDto(
                rs.getLong("empresa_id"), rs.getInt("ano"),
                (Long) rs.getObject("asiento_id"), (Long) rs.getObject("asiento_linea_id"),
                (Long) rs.getObject("tercero_id"), rs.getString("tipo_documento"),
                rs.getString("numero_documento"), rs.getString("digito_verificacion"),
                rs.getString("razon_social"), rs.getString("municipio"), rs.getString("pais"),
                rs.getString("cuenta_codigo"), rs.getString("codigo_concepto"),
                (Long) rs.getObject("concepto_id"), rs.getBigDecimal("valor"), rs.getDate("fecha").toLocalDate(),
                rs.getString("origen_tipo_documento"), rs.getString("origen_numero_documento"),
                rs.getString("origen")),
                empresaId, ano, formato, formato);
    }

    public List<ExogenaValidacionDto> validar(Long empresaId, Integer ano, String formato,
                                              List<MovimientoTributarioDto> movimientos) {
        catalogoFormatos.validar(formato);
        return validador.validar(ano, formato, movimientos, mapaConceptos(ano, formato), cuentasValidas(empresaId));
    }

    public List<ExogenaRenglonDto> renglones(Long empresaId, Integer ano, String formato,
                                             List<MovimientoTributarioDto> movimientos) {
        catalogoFormatos.validar(formato);
        return agregador.agregar(formato, movimientos, mapaConceptos(ano, formato));
    }

    public List<ExogenaRenglonDto> renglonesDesdeBase(Long empresaId, Integer ano, String formato) {
        catalogoFormatos.validar(formato);
        return jdbc.query("""
                SELECT m.ano, ? AS formato, m.tercero_id,
                       COALESCE(t.tipo_documento, m.tipo_documento) AS tipo_documento,
                       COALESCE(t.numero_documento, m.numero_documento) AS numero_documento,
                       COALESCE(t.dv, m.numero_documento_dv) AS digito_verificacion,
                       COALESCE(t.nombre, 'TERCERO ' || COALESCE(m.tercero_id::text, '0')) AS razon_social,
                       t.municipio, COALESCE(t.pais, m.pais, 'CO') AS pais,
                       m.concepto_id, c.codigo_concepto, c.descripcion,
                       SUM(m.valor) AS valor, COUNT(*) AS cantidad
                FROM movimiento_tributario m
                JOIN dian_concepto c ON c.id = m.concepto_id
                LEFT JOIN tercero_contable t ON t.id = m.tercero_id
                WHERE m.empresa_id = ? AND m.ano = ? AND c.formato = ?
                GROUP BY m.ano, m.tercero_id, t.tipo_documento, m.tipo_documento,
                         t.numero_documento, m.numero_documento, t.dv, m.numero_documento_dv,
                         t.nombre, t.municipio, t.pais, m.pais, m.concepto_id,
                         c.codigo_concepto, c.descripcion
                ORDER BY c.codigo_concepto, COALESCE(t.numero_documento, m.numero_documento)
                """, (rs, fila) -> new ExogenaRenglonDto(
                rs.getInt("ano"), rs.getString("formato"), (Long) rs.getObject("tercero_id"),
                rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("digito_verificacion"), rs.getString("razon_social"),
                rs.getString("municipio"), rs.getString("pais"), rs.getLong("concepto_id"),
                rs.getString("codigo_concepto"), rs.getString("descripcion"),
                rs.getBigDecimal("valor"), rs.getInt("cantidad")),
                formato, empresaId, ano, formato);
    }

    public List<ExogenaValidacionDto> validaciones(Long empresaId, Integer ano, String formato) {
        catalogoFormatos.validar(formato);
        return jdbc.query("""
                SELECT severidad, codigo, mensaje, tercero_id, concepto_id, cuenta_codigo, identificador
                FROM exogena_validacion
                WHERE empresa_id = ? AND ano = ? AND formato = ?
                ORDER BY CASE severidad WHEN 'BLOQUEANTE' THEN 0 ELSE 1 END, codigo, identificador
                """, (rs, fila) -> new ExogenaValidacionDto(
                rs.getString("severidad"), rs.getString("codigo"), rs.getString("mensaje"),
                (Long) rs.getObject("tercero_id"), (Long) rs.getObject("concepto_id"),
                rs.getString("cuenta_codigo"), rs.getString("identificador")),
                empresaId, ano, formato);
    }

    public byte[] excel(Long empresaId, Integer ano, String formato) {
        catalogoFormatos.validar(formato);
        return generadorExcel.generar(formato, renglonesDesdeBase(empresaId, ano, formato));
    }

    @Transactional
    public ResultadoExogenaGenerarDto generar(Long empresaId, SolicitudExogenaGenerarDto solicitud) {
        validarSolicitudGenerar(solicitud);
        Integer ano = solicitud.ano();
        String formato = solicitud.formato();
        catalogoFormatos.validar(formato);
        validarGenerador(formato);
        Long generacionId = jdbc.queryForObject("""
                INSERT INTO exogena_generacion (empresa_id, ano, formato, estado, incluir_advertencias, creado_por)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, empresaId, ano, formato, FormatoTributarioCatalogo.ESTADO_REGISTRADO,
                solicitud.incluirAdvertencias(), AuditoriaService.usuarioActual());

        List<MovimientoTributarioDto> movimientos = movimientos(empresaId, ano, formato);
        List<ExogenaValidacionDto> validaciones = validador.validar(ano, formato, movimientos,
                mapaConceptos(ano, formato), cuentasValidas(empresaId));
        persistirValidaciones(generacionId, empresaId, ano, formato, validaciones);

        if (validador.hayBloqueantes(validaciones)) {
            long bloqueantes = validaciones.stream()
                    .filter(v -> ValidadorExogena.BLOQUEANTE.equals(v.severidad())).count();
            String mensaje = "Generación bloqueada por " + bloqueantes + " validaciones bloqueantes";
            jdbc.update("""
                    UPDATE exogena_generacion
                    SET estado = 'ERROR', total_validaciones = ?, mensaje = ?, finished_at = now()
                    WHERE id = ?
                    """, validaciones.size(), mensaje, generacionId);
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "ERROR_GENERACION_EXOGENA",
                    "EXOGENA_GENERACION", generacionId, mensaje);
            throw new IllegalStateException(mensaje);
        }

        List<ExogenaRenglonDto> renglones = renglonesDesdeBase(empresaId, ano, formato);
        if (!solicitud.incluirAdvertencias()) {
            Set<String> conAdvertencia = validaciones.stream()
                    .filter(v -> ValidadorExogena.ADVERTENCIA.equals(v.severidad()) && v.identificador() != null)
                    .map(ExogenaValidacionDto::identificador)
                    .collect(Collectors.toSet());
            renglones = renglones.stream()
                    .filter(r -> !conAdvertencia.contains((r.terceroId() == null ? 0 : r.terceroId())
                            + "|" + r.codigoConcepto()))
                    .toList();
        }
        byte[] excel = generadorExcel.generar(formato, renglones);
        String xml = null;
        String estado = FormatoTributarioCatalogo.ESTADO_REGISTRADO;
        String mensaje = "Formato " + formato + " registrado en catálogo; pendiente de generador/XML oficial";
        if (catalogoFormatos.activo(formato)) {
            xml = generadorXml.generar(declarante(empresaId), renglones);
            validadorXsd.validar(ano, formato, xml);
            estado = "GENERADO";
            mensaje = "Generación exógena " + formato + " validada con XSD interno";
        }
        jdbc.update("""
                UPDATE exogena_generacion
                SET estado = ?, total_renglones = ?, total_validaciones = ?, xml = ?, excel = ?,
                    mensaje = ?, finished_at = now()
                WHERE id = ?
                """, estado, renglones.size(), validaciones.size(), xml, excel, mensaje, generacionId);
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "GENERACION_EXOGENA",
                "EXOGENA_GENERACION", generacionId, formato + " año " + ano + ": " + mensaje);
        return new ResultadoExogenaGenerarDto(generacionId, estado, ano, formato, renglones.size(),
                validaciones.size(), false, xml, excel, mensaje, validaciones);
    }

    private void validarGenerador(String formato) {
        if (FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS.contains(formato)
                && "GeneradorXmlExogenaOficial".equals(registryFormatos.obtener(formato).generadorXml())) {
            throw new IllegalArgumentException("El formato " + formato
                    + " se genera con el generador oficial: POST /api/empresas/{empresaId}/exogena/"
                    + formato + "/generar");
        }
    }

    private Map<Long, DianConceptoDto> mapaConceptos(Integer ano, String formato) {
        return conceptos(ano, formato).stream()
                .collect(Collectors.toMap(DianConceptoDto::id, Function.identity(), (a, b) -> a));
    }

    private Set<String> cuentasValidas(Long empresaId) {
        String version = versionPuc(empresaId);
        return new HashSet<>(jdbc.queryForList("""
                SELECT codigo FROM cuenta_contable
                WHERE activa AND (empresa_id IS NULL OR empresa_id = ?)
                  AND version_puc IN ('2025', ?)
                """, String.class, empresaId, version));
    }

    private DatosDeclaranteDto declarante(Long empresaId) {
        return jdbc.query("""
                SELECT nit, razon_social, tipo_documento, regimen, ciudad
                FROM empresa WHERE id = ?
                """, rs -> {
            String nitCompleto = rs.getString("nit");
            String nit;
            String digitoVerificacion;
            if (nitCompleto != null && nitCompleto.contains("-")) {
                int separador = nitCompleto.indexOf('-');
                nit = nitCompleto.substring(0, separador);
                digitoVerificacion = nitCompleto.substring(separador + 1);
            } else {
                nit = nitCompleto;
                Integer calculado = NitDigitoVerificacionRegla.calcularDv(nit);
                digitoVerificacion = calculado == null ? null : String.valueOf(calculado);
            }
            return new DatosDeclaranteDto(nit, digitoVerificacion, rs.getString("razon_social"),
                    rs.getString("tipo_documento"), rs.getString("ciudad"), "CO",
                    rs.getString("regimen"), versionPuc(empresaId));
        }, empresaId);
    }

    private String versionPuc(Long empresaId) {
        try {
            String regimen = jdbc.queryForObject("SELECT regimen_niif FROM empresa WHERE id = ?",
                    String.class, empresaId);
            if ("NIIF_FULL".equals(regimen)) {
                return "NIIF_FULL";
            }
            if ("NIIF_PYMES".equals(regimen)) {
                return "NIIF_PYMES";
            }
        } catch (EmptyResultDataAccessException ignored) {
            return "2025";
        }
        return "2025";
    }

    private void insertarMovimientos(List<MovimientoTributarioDto> movimientos) {
        jdbc.batchUpdate("""
                INSERT INTO movimiento_tributario (
                    empresa_id, ano, asiento_id, asiento_linea_id, tercero_id, cuenta_codigo,
                    concepto_id, valor, fecha, tipo_documento, numero_documento, numero_documento_dv,
                    municipio, pais, origen_tipo_documento, origen_numero_documento, origen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (empresa_id, ano, COALESCE(asiento_id, 0), cuenta_codigo,
                             COALESCE(concepto_id, 0), origen, COALESCE(numero_documento, '')) DO NOTHING
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MovimientoTributarioDto movimiento = movimientos.get(i);
                ps.setObject(1, movimiento.empresaId());
                ps.setObject(2, movimiento.ano());
                ps.setObject(3, movimiento.asientoId());
                ps.setObject(4, movimiento.asientoLineaId());
                ps.setObject(5, movimiento.terceroId());
                ps.setString(6, movimiento.cuentaCodigo());
                ps.setObject(7, movimiento.conceptoId());
                ps.setBigDecimal(8, movimiento.valor() == null ? BigDecimal.ZERO : movimiento.valor());
                ps.setDate(9, movimiento.fecha() == null ? Date.valueOf(java.time.LocalDate.now())
                        : Date.valueOf(movimiento.fecha()));
                ps.setString(10, movimiento.tipoDocumento());
                ps.setString(11, movimiento.numeroDocumento());
                ps.setString(12, movimiento.digitoVerificacion());
                ps.setString(13, movimiento.municipio());
                ps.setString(14, movimiento.pais());
                ps.setString(15, movimiento.origenTipoDocumento());
                ps.setString(16, movimiento.origenNumeroDocumento());
                ps.setString(17, movimiento.origen());
            }

            @Override
            public int getBatchSize() {
                return movimientos.size();
            }
        });
    }

    private void persistirValidaciones(Long generacionId, Long empresaId, Integer ano, String formato,
                                       List<ExogenaValidacionDto> validaciones) {
        if (validaciones == null || validaciones.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO exogena_validacion (
                    empresa_id, ano, formato, generacion_id, severidad, codigo, mensaje,
                    tercero_id, concepto_id, cuenta_codigo, identificador)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, validaciones, validaciones.size(), (ps, validacion) -> {
            ps.setLong(1, empresaId);
            ps.setInt(2, ano);
            ps.setString(3, formato);
            ps.setObject(4, generacionId);
            ps.setString(5, validacion.severidad());
            ps.setString(6, validacion.codigo());
            ps.setString(7, validacion.mensaje());
            ps.setObject(8, validacion.terceroId());
            ps.setObject(9, validacion.conceptoId());
            ps.setString(10, validacion.cuentaCodigo());
            ps.setString(11, validacion.identificador());
        });
    }

    private void validarSolicitudCatalogo(SolicitudCatalogoExogenaDto solicitud) {
        if (solicitud == null || solicitud.ano() == null) {
            throw new IllegalArgumentException("El año del catálogo exógena es obligatorio");
        }
        catalogoFormatos.validar(solicitud.formato());
    }

    private void validarSolicitudGenerar(SolicitudExogenaGenerarDto solicitud) {
        if (solicitud == null || solicitud.ano() == null) {
            throw new IllegalArgumentException("El año de generación exógena es obligatorio");
        }
        if (solicitud.formato() == null || solicitud.formato().isBlank()) {
            throw new IllegalArgumentException("El formato de generación exógena es obligatorio");
        }
        if (solicitud.ano() < 2000 || solicitud.ano() > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("Año de generación exógena fuera de rango: " + solicitud.ano());
        }
    }
}
