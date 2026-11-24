package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.CentroCostoDto;
import com.magnetixdian.interfaces.dto.SolicitudCentroCostoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServicioCentrosCosto {

    private final JdbcTemplate jdbc;

    public ServicioCentrosCosto(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CentroCostoDto> listar(Long empresaId) {
        return jdbc.query("""
                SELECT id, codigo, nombre, activo FROM centro_costo
                WHERE empresa_id = ? ORDER BY codigo
                """, (rs, fila) -> new CentroCostoDto(rs.getLong("id"), rs.getString("codigo"),
                rs.getString("nombre"), rs.getBoolean("activo")), empresaId);
    }

    public CentroCostoDto crear(Long empresaId, SolicitudCentroCostoDto solicitud) {
        if (solicitud == null || solicitud.codigo() == null || solicitud.codigo().isBlank()
                || solicitud.nombre() == null || solicitud.nombre().isBlank()) {
            throw new IllegalArgumentException("Código y nombre del centro de costo son obligatorios");
        }
        boolean activo = solicitud.activo() == null || solicitud.activo();
        jdbc.update("""
                INSERT INTO centro_costo (empresa_id, codigo, nombre, activo)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (empresa_id, codigo) DO UPDATE
                SET nombre = EXCLUDED.nombre, activo = EXCLUDED.activo
                """, empresaId, solicitud.codigo().trim(), solicitud.nombre().trim(), activo);
        return jdbc.queryForObject("""
                SELECT id, codigo, nombre, activo FROM centro_costo
                WHERE empresa_id = ? AND codigo = ?
                """, (rs, fila) -> new CentroCostoDto(rs.getLong("id"), rs.getString("codigo"),
                rs.getString("nombre"), rs.getBoolean("activo")), empresaId, solicitud.codigo().trim());
    }
}
