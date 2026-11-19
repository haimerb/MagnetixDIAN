package com.magnetixdian.application;

import com.magnetixdian.domain.apikey.ClaveApi;
import com.magnetixdian.infrastructure.persistence.ApiKeyJpa;
import com.magnetixdian.infrastructure.persistence.ApiKeyRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioJpa;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.ApiKeyCreadaDto;
import com.magnetixdian.interfaces.dto.ApiKeyDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestión y validación de claves de API por empresa.
 */
@Service
public class ServicioApiKeys {

    private final ApiKeyRepository apiKeyRepo;
    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuditoriaService auditoria;

    public ServicioApiKeys(ApiKeyRepository apiKeyRepo,
                           EmpresaRepository empresaRepo,
                           UsuarioRepository usuarioRepo,
                           AuditoriaService auditoria) {
        this.apiKeyRepo = apiKeyRepo;
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> listar(Long empresaId) {
        return apiKeyRepo.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream().map(this::aDto).toList();
    }

    @Transactional
    public ApiKeyCreadaDto crear(Long empresaId, Long usuarioId, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debe indicar un nombre para la clave.");
        }
        var empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        UsuarioJpa usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getEmpresa() != null && !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("El usuario no pertenece a la empresa indicada.");
        }

        ClaveApi.Generada generada = ClaveApi.generar();
        ApiKeyJpa clave = new ApiKeyJpa();
        clave.setEmpresa(empresa);
        clave.setUsuario(usuario);
        clave.setNombre(nombre.trim());
        clave.setPrefijo(generada.prefijo());
        clave.setHash(ClaveApi.hash(generada.clave()));
        apiKeyRepo.save(clave);

        auditoria.registrar(AuditoriaService.usuarioActual(), "API_KEY_CREADA", "api_key", clave.getId(),
                nombre.trim());
        return new ApiKeyCreadaDto(clave.getId(), clave.getNombre(), clave.getPrefijo(), generada.clave());
    }

    @Transactional
    public void revocar(Long empresaId, Long id) {
        ApiKeyJpa clave = apiKeyRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Clave no encontrada."));
        if (clave.isActivo()) {
            clave.setActivo(false);
            clave.setRevocadoAt(OffsetDateTime.now());
            apiKeyRepo.save(clave);
            auditoria.registrar(AuditoriaService.usuarioActual(), "API_KEY_REVOCADA", "api_key", id,
                    clave.getNombre());
        }
    }

    /**
     * Valida una clave de API y devuelve el username del usuario propietario.
     */
    @Transactional
    public Optional<String> autenticar(String clave) {
        String prefijo = ClaveApi.prefijoDe(clave);
        if (prefijo == null) {
            return Optional.empty();
        }
        ApiKeyJpa registro = apiKeyRepo.findFirstByPrefijoAndActivoTrue(prefijo).orElse(null);
        if (registro == null || !ClaveApi.hashCoincide(registro.getHash(), ClaveApi.hash(clave))) {
            return Optional.empty();
        }
        UsuarioJpa usuario = registro.getUsuario();
        if (usuario == null || !Boolean.TRUE.equals(usuario.getEnabled())) {
            return Optional.empty();
        }
        registro.setUltimoUso(OffsetDateTime.now());
        apiKeyRepo.save(registro);
        return Optional.of(usuario.getUsername());
    }

    private ApiKeyDto aDto(ApiKeyJpa k) {
        return new ApiKeyDto(k.getId(), k.getNombre(), k.getPrefijo(), k.isActivo(), k.getUltimoUso(),
                k.getCreatedAt());
    }
}
