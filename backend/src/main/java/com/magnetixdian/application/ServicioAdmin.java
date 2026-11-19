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
import com.magnetixdian.interfaces.dto.EnlaceAccesoResponse;
import com.magnetixdian.interfaces.dto.EmpresaRequest;
import com.magnetixdian.interfaces.dto.InvitacionRequest;
import com.magnetixdian.interfaces.dto.InvitacionResponse;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.UsuarioDto;
import com.magnetixdian.interfaces.dto.UsuarioRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ServicioRecuperacion recuperacion;
    private final AuditoriaService auditoria;

    public ServicioAdmin(EmpresaRepository empresaRepo,
                         UsuarioRepository usuarioRepo,
                         RoleRepository roleRepo,
                         CalendarioDianRepository calendarioRepo,
                         MedioMagneticoRepository medioRepo,
                         PasswordEncoder passwordEncoder,
                         ServicioRecuperacion recuperacion,
                         AuditoriaService auditoria) {
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.calendarioRepo = calendarioRepo;
        this.medioRepo = medioRepo;
        this.passwordEncoder = passwordEncoder;
        this.recuperacion = recuperacion;
        this.auditoria = auditoria;
    }

    // ---------------------------------------------------------------- Empresas

    @Transactional(readOnly = true)
    public List<EmpresaDto> listarEmpresas() {
        return empresaRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PaginaDto<EmpresaDto> listarEmpresasPaginado(int pagina, int tamano) {
        Pageable pageable = paginacion(pagina, tamano, "id");
        Page<EmpresaDto> page = empresaRepo.findAll(pageable).map(this::toDto);
        return PaginaDto.desde(page);
    }

    @Transactional
    public EmpresaDto crearEmpresa(EmpresaRequest request) {
        if (empresaRepo.findByNit(request.nit()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con el NIT " + request.nit());
        }
        EmpresaJpa empresa = new EmpresaJpa(request.nit(), request.razonSocial());
        aplicar(empresa, request);
        EmpresaDto dto = toDto(empresaRepo.save(empresa));
        auditoria.registrar(AuditoriaService.usuarioActual(), "EMPRESA_CREADA", "empresa", dto.id(), dto.nit());
        return dto;
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
        EmpresaDto dto = toDto(empresaRepo.save(empresa));
        auditoria.registrar(AuditoriaService.usuarioActual(), "EMPRESA_ACTUALIZADA", "empresa", dto.id(), dto.nit());
        return dto;
    }

    @Transactional
    public void eliminarEmpresa(Long id) {
        if (medioRepo.findByEmpresaIdOrderByCreatedAtDesc(id).stream().findAny().isPresent()) {
            throw new IllegalStateException("No se puede eliminar: la empresa tiene medios magnéticos asociados.");
        }
        empresaRepo.deleteById(id);
        auditoria.registrar(AuditoriaService.usuarioActual(), "EMPRESA_ELIMINADA", "empresa", id, null);
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

    @Transactional(readOnly = true)
    public PaginaDto<UsuarioDto> listarUsuariosPaginado(int pagina, int tamano) {
        Pageable pageable = paginacion(pagina, tamano, "id");
        Page<UsuarioDto> page = usuarioRepo.findAll(pageable).map(this::toDto);
        return PaginaDto.desde(page);
    }

    private static Pageable paginacion(int pagina, int tamano, String orden) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100),
                Sort.by(Sort.Direction.ASC, orden));
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
        UsuarioDto dto = toDto(usuarioRepo.save(usuario));
        auditoria.registrar(AuditoriaService.usuarioActual(), "USUARIO_CREADO", "usuario", dto.id(), dto.username());
        return dto;
    }

    @Transactional
    public InvitacionResponse invitarUsuario(InvitacionRequest request) {
        if (usuarioRepo.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre " + request.username());
        }
        if (usuarioRepo.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + request.email());
        }
        UsuarioJpa usuario = new UsuarioJpa();
        usuario.setUsername(request.username());
        usuario.setEmail(request.email());
        usuario.setNombre(request.nombre());
        usuario.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        usuario.setEnabled(Boolean.TRUE);
        usuario.setInvitacionPendiente(Boolean.TRUE);
        for (String codigo : request.roles()) {
            RoleJpa rol = roleRepo.findByCode(codigo)
                    .orElseThrow(() -> new IllegalArgumentException("Rol no existe: " + codigo));
            usuario.getRoles().add(rol);
        }
        if (request.empresaId() != null) {
            usuario.setEmpresa(empresaRepo.findById(request.empresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + request.empresaId())));
        }
        UsuarioJpa guardado = usuarioRepo.save(usuario);
        String enlace = recuperacion.crearInvitacion(guardado);
        return new InvitacionResponse(toDto(guardado), enlace);
    }

    @Transactional
    public UsuarioDto alternarEstadoUsuario(Long id, boolean enabled) {
        UsuarioJpa usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        usuario.setEnabled(enabled);
        UsuarioDto dto = toDto(usuarioRepo.save(usuario));
        auditoria.registrar(AuditoriaService.usuarioActual(), enabled ? "USUARIO_HABILITADO" : "USUARIO_DESHABILITADO",
                "usuario", id, usuario.getUsername());
        return dto;
    }

    @Transactional
    public EnlaceAccesoResponse generarEnlaceAcceso(Long id) {
        UsuarioJpa usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        boolean activacion = !Boolean.TRUE.equals(usuario.getEnabled());
        String enlace = activacion
                ? recuperacion.crearEnlaceInvitacion(usuario)
                : recuperacion.crearEnlaceReset(usuario);
        auditoria.registrar(AuditoriaService.usuarioActual(), "ENLACE_ACCESO_GENERADO", "usuario", id,
                usuario.getUsername() + " (" + (activacion ? "activación" : "recuperación") + ")");
        return new EnlaceAccesoResponse(enlace, activacion ? "ACTIVACION" : "RECUPERACION", usuario.getUsername());
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
        CalendarioDianDto dto = toDto(calendarioRepo.save(entrada));
        auditoria.registrar(AuditoriaService.usuarioActual(), "CALENDARIO_CREADO", "calendario", dto.id(),
                request.tipoReporte() + " " + request.anioGravable());
        return dto;
    }

    @Transactional
    public void eliminarCalendario(Long id) {
        calendarioRepo.deleteById(id);
        auditoria.registrar(AuditoriaService.usuarioActual(), "CALENDARIO_ELIMINADO", "calendario", id, null);
    }

    private CalendarioDianDto toDto(CalendarioDianJpa c) {
        return new CalendarioDianDto(c.getId(), c.getAnioGravable(), c.getAnioPresentacion(),
                c.getTipoReporte(), c.getRangoNitIni(), c.getRangoNitFin(), c.getFechaLimite());
    }
}