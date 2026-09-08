package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CalendarioDianJpa;
import com.magnetixdian.infrastructure.persistence.CalendarioDianRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.infrastructure.persistence.RoleJpa;
import com.magnetixdian.infrastructure.persistence.RoleRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.CalendarioDianDto;
import com.magnetixdian.interfaces.dto.CalendarioDianRequest;
import com.magnetixdian.interfaces.dto.EmpresaDto;
import com.magnetixdian.interfaces.dto.EmpresaRequest;
import com.magnetixdian.interfaces.dto.UsuarioDto;
import com.magnetixdian.interfaces.dto.UsuarioRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operaciones de administración: empresas clientes, usuarios del sistema y
 * calendario DIAN de vencimientos.
 */
@Service
public class ServicioAdmin {

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final CalendarioDianRepository calendarioRepo;
    private final MedioMagneticoRepository medioRepo;
    private final PasswordEncoder passwordEncoder;

    public ServicioAdmin(EmpresaRepository empresaRepo,
                         UsuarioRepository usuarioRepo,
                         RoleRepository roleRepo,
                         CalendarioDianRepository calendarioRepo,
                         MedioMagneticoRepository medioRepo,
                         PasswordEncoder passwordEncoder) {
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.calendarioRepo = calendarioRepo;
        this.medioRepo = medioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------- Empresas

    @Transactional(readOnly = true)
    public List<EmpresaDto> listarEmpresas() {
        return empresaRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public EmpresaDto crearEmpresa(EmpresaRequest request) {
        if (empresaRepo.findByNit(request.nit()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con el NIT " + request.nit());
        }
        EmpresaJpa empresa = new EmpresaJpa(request.nit(), request.razonSocial());
        aplicar(empresa, request);
        return toDto(empresaRepo.save(empresa));
    }

    @Transactional
    public EmpresaDto actualizarEmpresa(Long id, EmpresaRequest request) {
        EmpresaJpa empresa = empresaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + id));
        if (!empresa.getNit().equals(request.nit()) && empresaRepo.findByNit(request.nit()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con el NIT " + request.nit());
        }
        empresa.setNit(request.nit());
        empresa.setRazonSocial(request.razonSocial());
        aplicar(empresa, request);
        return toDto(empresaRepo.save(empresa));
    }

    @Transactional
    public void eliminarEmpresa(Long id) {
        if (medioRepo.findByEmpresaIdOrderByCreatedAtDesc(id).stream().findAny().isPresent()) {
            throw new IllegalStateException("No se puede eliminar: la empresa tiene medios magnéticos asociados.");
        }
        empresaRepo.deleteById(id);
    }

    private void aplicar(EmpresaJpa empresa, EmpresaRequest request) {
        empresa.setRegimen(request.regimen() == null || request.regimen().isBlank() ? "ORDINARIO" : request.regimen());
        empresa.setTipoDocumento(request.tipoDocumento() == null || request.tipoDocumento().isBlank() ? "NIT" : request.tipoDocumento());
        empresa.setGranContribuyente(request.granContribuyente());
        empresa.setDireccion(request.direccion());
        empresa.setCiudad(request.ciudad());
        empresa.setDepartamento(request.departamento());
        empresa.setCodigoDane(request.codigoDane());
        empresa.setEmail(request.email());
    }

    private EmpresaDto toDto(EmpresaJpa e) {
        return new EmpresaDto(e.getId(), e.getNit(), e.getRazonSocial(), e.getRegimen(),
                e.getTipoDocumento(), e.isGranContribuyente(), e.getDireccion(), e.getCiudad(),
                e.getDepartamento(), e.getCodigoDane(), e.getEmail(), e.getCreatedAt());
    }

    // ---------------------------------------------------------------- Usuarios

    @Transactional(readOnly = true)
    public List<UsuarioDto> listarUsuarios() {
        return usuarioRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public UsuarioDto crearUsuario(UsuarioRequest request) {
        if (usuarioRepo.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre " + request.username());
        }
        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(request.username());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setEmail(request.email());
        usuario.setNombre(request.nombre());
        usuario.setEnabled(request.enabled() == null || request.enabled());
        for (String codigo : request.roles()) {
            RoleJpa rol = roleRepo.findByCode(codigo)
                    .orElseThrow(() -> new IllegalArgumentException("Rol no existe: " + codigo));
            usuario.getRoles().add(rol);
        }
        Long empresaId = request.empresaId();
        if (empresaId != null) {
            usuario.setEmpresa(empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + empresaId)));
        }
        return toDto(usuarioRepo.save(usuario));
    }

    @Transactional
    public UsuarioDto alternarEstadoUsuario(Long id, boolean enabled) {
        UsuarioJpa usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        usuario.setEnabled(enabled);
        return toDto(usuarioRepo.save(usuario));
    }

    private UsuarioDto toDto(UsuarioJpa u) {
        return new UsuarioDto(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(),
                Boolean.TRUE.equals(u.getEnabled()),
                u.getRoles().stream().map(RoleJpa::getCode).sorted().toList(),
                u.getEmpresa() == null ? null : u.getEmpresa().getId());
    }

    // ---------------------------------------------------------------- Calendario

    @Transactional(readOnly = true)
    public List<CalendarioDianDto> listarCalendario(Long anioGravable) {
        List<CalendarioDianJpa> calendario = anioGravable != null
                ? calendarioRepo.findByAnioGravableOrderByFechaLimite(anioGravable)
                : calendarioRepo.findAll().stream()
                    .sorted(java.util.Comparator.comparing(CalendarioDianJpa::getFechaLimite))
                    .toList();
        return calendario.stream().map(this::toDto).toList();
    }

    @Transactional
    public CalendarioDianDto crearCalendario(CalendarioDianRequest request) {
        CalendarioDianJpa entrada = new CalendarioDianJpa(
                request.anioGravable(), request.anioPresentacion(), request.tipoReporte(),
                request.rangoNitIni(), request.rangoNitFin(), request.fechaLimite());
        return toDto(calendarioRepo.save(entrada));
    }

    @Transactional
    public void eliminarCalendario(Long id) {
        calendarioRepo.deleteById(id);
    }

    private CalendarioDianDto toDto(CalendarioDianJpa c) {
        return new CalendarioDianDto(c.getId(), c.getAnioGravable(), c.getAnioPresentacion(),
                c.getTipoReporte(), c.getRangoNitIni(), c.getRangoNitFin(), c.getFechaLimite());
    }
}