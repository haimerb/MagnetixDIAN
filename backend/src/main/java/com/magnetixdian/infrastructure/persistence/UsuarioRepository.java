package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioJpa, Long> {

    Optional<UsuarioJpa> findByUsername(String username);

    Optional<UsuarioJpa> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);
}