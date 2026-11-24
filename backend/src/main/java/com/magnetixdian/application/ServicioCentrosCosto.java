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

    String sql = "SELECT id, codigo, nombre, activo FROM centro_costo WHERE empresa_id = ? ORDER BY codigo";
List<CentroCostoDto> lista = jdbc.query(sql, (rs, fila) -> new CentroCostoDto(rs.getLong("id"), rs.getString("codigo"),
                rs.getString("nombre"), rs.getBoolean("activo")), empresaId);
return lista;

    public CentroCostoDto crear(Long empresaId, SolicitudCentroCostoDto solicitud) {
        if (solicitud == null || solicitud.codigo() == null || solicitud.codigo().isBlank()
                || solicitud.nombre() == null || solicitud.nombre().isBlank()) {
            throw new IllegalArgumentException("Código y nombre del centro de costo son obligatorios");
        }
        boolean activo = solicitud.activo() == null || solicitud.activo();
        String sqlInsert = "INSERT INTO centro_costo (empresa_id, codigo, nombre, activo) VALUES (?, ?, ?, ?)";
        jdbc.update(sqlInsert, empresaId, solicitud.codigo().trim(), solicitud.nombre().trim(), activo);
        String sqlQuery = "SELECT id, codigo, nombre, activo FROM centro_costo WHERE empresa_id = ? AND codigo = ?";
        return jdbc.queryForObject(sqlQuery, (rs, fila) -> new CentroCostoDto(rs.getLong("id"), rs.getString("codigo"),
                rs.getString("nombre"), rs.getBoolean("activo")), empresaId, solicitud.codigo().trim());
    }
}
