package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.RoleJpa;
import com.magnetixdian.infrastructure.persistence.RoleRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.RegistroEmpresaRequest;
import com.magnetixdian.interfaces.dto.RegistroEmpresaResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ServicioRegistro {

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final ServicioSuscripciones suscripciones;
    private final AuditoriaService auditoria;

    public ServicioRegistro(EmpresaRepository empresaRepo,
                            UsuarioRepository usuarioRepo,
                            RoleRepository roleRepo,
                            PasswordEncoder passwordEncoder,
                            ServicioSuscripciones suscripciones,
                            AuditoriaService auditoria) {
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.suscripciones = suscripciones;
        this.auditoria = auditoria;
    }

    @Transactional
    public RegistroEmpresaResponse registrar(RegistroEmpresaRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            throw new IllegalArgumentException("No se pudo completar el registro.");
        }
        if (!request.aceptaTerminos()) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones.");
        }
        String nit = normalizarNit(request.nit());
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();

        if (nit.isBlank()) {
            throw new IllegalArgumentException("El NIT no es válido.");
        }
        if (empresaRepo.findByNit(nit).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con el NIT " + nit + ".");
        }
        if (usuarioRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre " + username + ".");
        }
        if (usuarioRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + email + ".");
        }
        RoleJpa rol = roleRepo.findByCode("ROLE_CLIENTE")
                .orElseThrow(() -> new IllegalStateException("No está configurado el rol de cliente."));

        EmpresaJpa empresa = new EmpresaJpa(nit, request.razonSocial().trim());
        empresa.setEmail(email);
        EmpresaJpa guardadaEmpresa = empresaRepo.save(empresa);

        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setEmail(email);
        usuario.setNombre(request.nombre().trim());
        usuario.setEmpresa(guardadaEmpresa);
        usuario.setEnabled(Boolean.TRUE);
        usuario.setTerminosAceptadosEn(OffsetDateTime.now());
        usuario.getRoles().add(rol);
        UsuarioJpa guardadoUsuario = usuarioRepo.save(usuario);

        suscripciones.cambiarPlan(guardadaEmpresa.getId(), "FREE", "SIMULADO");
        auditoria.registrar("REGISTRO", "USUARIO_REGISTRADO", "usuario", guardadoUsuario.getId(),
                username + " · empresa " + nit);

        return new RegistroEmpresaResponse(guardadaEmpresa.getId(), guardadoUsuario.getId(), username,
                "Cuenta creada. Ingrese con su usuario y contraseña.");
    }

    private static String normalizarNit(String nit) {
        if (nit == null) {
            return "";
        }
        return nit.trim().replaceAll("[\\s.\\-]", "").toUpperCase();
    }
}
