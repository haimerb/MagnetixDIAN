package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EventoAuditoriaJpa;
import com.magnetixdian.infrastructure.persistence.EventoAuditoriaRepository;
import com.magnetixdian.interfaces.dto.EventoAuditoriaDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Bitácora de auditoría. El registro nunca debe interrumpir la operación de
 * negocio: los errores se registran en el log y se descartan.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final EventoAuditoriaRepository repository;

    public AuditoriaService(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String accion, String entidad, Object entidadId, String detalle) {
        registrarEnEmpresa(null, usuario, accion, entidad, entidadId, detalle);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnEmpresa(Long empresaId, String usuario, String accion, String entidad,
                                   Object entidadId, String detalle) {
        try {
            String id = entidadId == null ? null : String.valueOf(entidadId);
            repository.save(new EventoAuditoriaJpa(empresaId, usuario, accion, entidad, id,
                    recortar(detalle, 1000), ipActual()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el evento de auditoría {}: {}", accion, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaginaDto<EventoAuditoriaDto> listar(int pagina, int tamano, String accion, String usuario) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean conAccion = accion != null && !accion.isBlank();
        boolean conUsuario = usuario != null && !usuario.isBlank();
        Page<EventoAuditoriaJpa> page;
        if (conAccion && conUsuario) {
            page = repository.findByAccionContainingIgnoreCaseAndUsuarioContainingIgnoreCase(accion, usuario, pageable);
        } else if (conAccion) {
            page = repository.findByAccionContainingIgnoreCase(accion, pageable);
        } else if (conUsuario) {
            page = repository.findByUsuarioContainingIgnoreCase(usuario, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return PaginaDto.desde(page.map(this::toDto));
    }

    private EventoAuditoriaDto toDto(EventoAuditoriaJpa e) {
        return new EventoAuditoriaDto(e.getId(), e.getUsuario(), e.getAccion(), e.getEntidad(),
                e.getEntidadId(), e.getDetalle(), e.getIp(), e.getCreatedAt());
    }

    public static String usuarioActual() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return recortar(forwarded.split(",")[0].trim(), 64);
            }
            return recortar(request.getRemoteAddr(), 64);
        }
        return null;
    }

    private static String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
