package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.PeriodoContableDto;
import com.magnetixdian.interfaces.dto.SolicitudPeriodoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ServicioPeriodos {

    private final JdbcTemplate jdbc;

    public ServicioPeriodos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<PeriodoContableDto> listar(Long empresaId) {
        return jdbc.query("""
                SELECT id, anio, mes, fecha_desde, fecha_hasta, estado
                FROM periodo_contable WHERE empresa_id = ?
                ORDER BY anio, mes
                """, (rs, fila) -> new PeriodoContableDto(
                rs.getLong("id"), rs.getInt("anio"), rs.getInt("mes"),
                rs.getDate("fecha_desde").toLocalDate(), rs.getDate("fecha_hasta").toLocalDate(),
                rs.getString("estado")), empresaId);
    }

    public PeriodoContableDto crear(Long empresaId, SolicitudPeriodoDto solicitud) {
        validar(solicitud);
        jdbc.update("""
                INSERT INTO periodo_contable (empresa_id, anio, mes, fecha_desde, fecha_hasta, estado)
                VALUES (?, ?, ?, ?, ?, 'ABIERTO')
                ON CONFLICT (empresa_id, anio, mes) DO UPDATE
                SET fecha_desde = EXCLUDED.fecha_desde,
                    fecha_hasta = EXCLUDED.fecha_hasta,
                    estado = 'ABIERTO',
                    cerrado_en = NULL,
                    updated_at = now()
                """, empresaId, solicitud.anio(), solicitud.mes(),
                java.sql.Date.valueOf(solicitud.fechaDesde()), java.sql.Date.valueOf(solicitud.fechaHasta()));
        return jdbc.queryForObject("""
                SELECT id, anio, mes, fecha_desde, fecha_hasta, estado
                FROM periodo_contable WHERE empresa_id = ? AND anio = ? AND mes = ?
                """, (rs, fila) -> new PeriodoContableDto(
                rs.getLong("id"), rs.getInt("anio"), rs.getInt("mes"),
                rs.getDate("fecha_desde").toLocalDate(), rs.getDate("fecha_hasta").toLocalDate(),
                rs.getString("estado")), empresaId, solicitud.anio(), solicitud.mes());
    }

    public PeriodoContableDto cerrar(Long empresaId, Long periodoId) {
        jdbc.update("""
                UPDATE periodo_contable
                SET estado = 'CERRADO', cerrado_en = now(), updated_at = now()
                WHERE id = ? AND empresa_id = ?
                """, periodoId, empresaId);
        return jdbc.queryForObject("""
                SELECT id, anio, mes, fecha_desde, fecha_hasta, estado
                FROM periodo_contable WHERE id = ? AND empresa_id = ?
                """, (rs, fila) -> new PeriodoContableDto(
                rs.getLong("id"), rs.getInt("anio"), rs.getInt("mes"),
                rs.getDate("fecha_desde").toLocalDate(), rs.getDate("fecha_hasta").toLocalDate(),
                rs.getString("estado")), periodoId, empresaId);
    }

    private void validar(SolicitudPeriodoDto solicitud) {
        if (solicitud == null || solicitud.anio() == null || solicitud.mes() == null
                || solicitud.fechaDesde() == null || solicitud.fechaHasta() == null) {
            throw new IllegalArgumentException("Todos los campos del periodo contable son obligatorios");
        }
        if (solicitud.mes() < 1 || solicitud.mes() > 12) {
            throw new IllegalArgumentException("Mes de periodo contable inválido: " + solicitud.mes());
        }
        LocalDate desde = solicitud.fechaDesde();
        LocalDate hasta = solicitud.fechaHasta();
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta del periodo debe ser mayor o igual a la fecha desde");
        }
    }
}
