package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpa, Long> {

    Optional<PasswordResetTokenJpa> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenJpa t set t.usadoAt = CURRENT_TIMESTAMP "
            + "where t.usuario.id = :usuarioId and t.tipo = :tipo and t.usadoAt is null")
    void invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo);
}
