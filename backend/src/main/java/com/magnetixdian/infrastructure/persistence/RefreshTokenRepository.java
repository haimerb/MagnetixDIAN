package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenJpa, Long> {

    Optional<RefreshTokenJpa> findByJti(String jti);

    @Modifying
    @Query("update RefreshTokenJpa r set r.revocado = true, r.revocadoAt = CURRENT_TIMESTAMP "
            + "where r.usuario.id = :usuarioId and r.revocado = false")
    void revocarTodos(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from RefreshTokenJpa r where r.expiraAt < :limite")
    int eliminarExpirados(@Param("limite") OffsetDateTime limite);
}
