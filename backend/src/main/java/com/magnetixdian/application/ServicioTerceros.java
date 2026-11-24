package com.magnetixdian.application;

import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.interfaces.dto.SolicitudTerceroDto;
import com.magnetixdian.interfaces.dto.TerceroContableDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class ServicioTerceros {

    private static final Set<String> TIPOS_DOCUMENTO = Set.of(
            "NIT", "CC", "TI", "CE", "PA", "RC", "NU");

    private final JdbcTemplate jdbc;

    public ServicioTerceros(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<TerceroContableDto> listar(Long empresaId) {
        return jdbc.query("""
                SELECT id, tipo_documento, numero_documento, dv, nombre, municipio, pais, activo
                FROM tercero_contable WHERE empresa_id = ? ORDER BY nombre
                """, (rs, fila) -> new TerceroContableDto(
                rs.getLong("id"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("dv"), rs.getString("nombre"), rs.getString("municipio"),
                rs.getString("pais"), rs.getBoolean("activo")), empresaId);
    }

    public TerceroContableDto crear(Long empresaId, SolicitudTerceroDto solicitud) {
        validar(solicitud);
        String tipoDocumento = solicitud.tipoDocumento().trim().toUpperCase(Locale.ROOT);
        String numeroDocumento = solicitud.numeroDocumento().trim();
        String dv = solicitud.digitoVerificacion() == null ? "" : solicitud.digitoVerificacion().trim();
        if (numeroDocumento.contains("-")) {
            String[] partes = numeroDocumento.split("-", 2);
            numeroDocumento = partes[0].trim();
            if (dv.isBlank()) {
                dv = partes[1].trim();
            }
        }
        if ("NIT".equals(tipoDocumento)) {
            Integer esperado = NitDigitoVerificacionRegla.calcularDv(numeroDocumento);
            if (esperado == null) {
                throw new IllegalArgumentException("El NIT del tercero debe ser numérico");
            }
            if (dv.isBlank()) {
                dv = String.valueOf(esperado);
            } else if (!dv.equals(String.valueOf(esperado))) {
                throw new IllegalArgumentException("Dígito de verificación del NIT no corresponde; esperado " + esperado);
            }
        }
        boolean activo = solicitud.activo() == null || solicitud.activo();
        jdbc.update("""
                INSERT INTO tercero_contable (empresa_id, tipo_documento, numero_documento, dv,
                                              nombre, municipio, pais, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (empresa_id, tipo_documento, numero_documento) DO UPDATE
                SET dv = EXCLUDED.dv, nombre = EXCLUDED.nombre, municipio = EXCLUDED.municipio,
                    pais = EXCLUDED.pais, activo = EXCLUDED.activo
                """, empresaId, tipoDocumento, numeroDocumento, dv.isBlank() ? null : dv,
                solicitud.nombre().trim(), solicitud.municipio(),
                solicitud.pais() == null || solicitud.pais().isBlank() ? "CO" : solicitud.pais().trim(), activo);
        return buscar(empresaId, tipoDocumento, numeroDocumento);
    }

    private TerceroContableDto buscar(Long empresaId, String tipoDocumento, String numeroDocumento) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, tipo_documento, numero_documento, dv, nombre, municipio, pais, activo
                    FROM tercero_contable
                    WHERE empresa_id = ? AND tipo_documento = ? AND numero_documento = ?
                    """, (rs, fila) -> new TerceroContableDto(
                    rs.getLong("id"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                    rs.getString("dv"), rs.getString("nombre"), rs.getString("municipio"),
                    rs.getString("pais"), rs.getBoolean("activo")), empresaId, tipoDocumento, numeroDocumento);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("No fue posible recuperar el tercero contable creado", ex);
        }
    }

    private void validar(SolicitudTerceroDto solicitud) {
        if (solicitud == null || solicitud.tipoDocumento() == null || solicitud.numeroDocumento() == null
                || solicitud.numeroDocumento().isBlank() || solicitud.nombre() == null
                || solicitud.nombre().isBlank()) {
            throw new IllegalArgumentException("Tipo de documento, número y nombre del tercero son obligatorios");
        }
        String tipoDocumento = solicitud.tipoDocumento().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_DOCUMENTO.contains(tipoDocumento)) {
            throw new IllegalArgumentException("Tipo de documento de tercero no soportado: " + solicitud.tipoDocumento());
        }
    }
}
