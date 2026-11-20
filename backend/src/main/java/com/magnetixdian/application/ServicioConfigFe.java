package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * Lectura y actualización de la configuración DIAN de una empresa.
 */
@Service
public class ServicioConfigFe {

    private final EmpresaRepository empresaRepository;
    private final EmpresaFeConfigRepository configRepository;
    private final AuditoriaService auditoria;
    private final String uploadDir;

    public ServicioConfigFe(EmpresaRepository empresaRepository, EmpresaFeConfigRepository configRepository,
                            AuditoriaService auditoria,
                            @Value("${magnetixdian.upload.dir:./uploads}") String uploadDir) {
        this.empresaRepository = empresaRepository;
        this.configRepository = configRepository;
        this.auditoria = auditoria;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public ConfigFeDto obtener(Long empresaId) {
        return aDto(config(empresaId));
    }

    @Transactional
    public ConfigFeDto guardar(Long empresaId, ConfigFeDto dto) {
        EmpresaFeConfigJpa c = config(empresaId);
        if (dto.ambiente() != null) {
            c.setAmbiente(dto.ambiente().shortValue());
        }
        c.setSoftwareId(dto.softwareId());
        if (dto.softwarePin() != null && !dto.softwarePin().isBlank()) {
            c.setSoftwarePin(dto.softwarePin());
        }
        c.setTestSetId(dto.testSetId());
        c.setResolucionNumero(dto.resolucionNumero());
        c.setResolucionFecha(dto.resolucionFecha());
        c.setPrefijo(dto.prefijo());
        c.setRangoDesde(dto.rangoDesde());
        c.setRangoHasta(dto.rangoHasta());
        c.setRangoVigenciaDesde(dto.rangoVigenciaDesde());
        c.setRangoVigenciaHasta(dto.rangoVigenciaHasta());
        if (dto.canal() != null && !dto.canal().isBlank()) {
            c.setCanal(dto.canal().toUpperCase());
        }
        c.setPacProveedor(dto.pacProveedor());
        c.setPacBaseUrl(dto.pacBaseUrl());
        if (dto.pacApiKey() != null && !dto.pacApiKey().isBlank()) {
            c.setPacApiKey(dto.pacApiKey());
        }
        c.setCertificadoNombre(dto.certificadoNombre());
        c.setCertificadoPath(dto.certificadoPath());
        if (dto.certificadoPassword() != null && !dto.certificadoPassword().isBlank()) {
            c.setCertificadoPassword(dto.certificadoPassword());
        }
        if (dto.responsabilidadFiscal() != null && !dto.responsabilidadFiscal().isBlank()) {
            c.setResponsabilidadFiscal(dto.responsabilidadFiscal());
        }
        c.setCodigoMunicipio(dto.codigoMunicipio());
        if (dto.activo() != null) {
            c.setActivo(dto.activo());
        }
        if (dto.notificarAdquirente() != null) {
            c.setNotificarAdquirente(dto.notificarAdquirente());
        }
        c.setUpdatedAt(java.time.OffsetDateTime.now());
        EmpresaFeConfigJpa guardada = configRepository.save(c);
        auditoria.registrar(AuditoriaService.usuarioActual(), "CONFIG_FE_ACTUALIZADA", "empresa", empresaId,
                "canal=" + guardada.getCanal() + ", ambiente=" + guardada.getAmbiente());
        return aDto(guardada);
    }

    @Transactional
    public ConfigFeDto guardarCertificado(Long empresaId, String nombreArchivo, byte[] contenido, String password) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo del certificado está vacío");
        }
        String nombre = nombreArchivo == null ? "certificado.p12" : nombreArchivo;
        String ext = nombre.toLowerCase().endsWith(".pfx") ? ".pfx" : ".p12";
        char[] clave = password == null || password.isBlank() ? new char[0] : password.toCharArray();
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(contenido), clave);
            boolean tieneClave = false;
            for (String alias : Collections.list(almacen.aliases())) {
                if (almacen.isKeyEntry(alias)) {
                    tieneClave = true;
                    break;
                }
            }
            if (!tieneClave) {
                throw new IllegalArgumentException("El certificado no contiene una clave privada.");
            }
            Path dir = Paths.get(uploadDir, "certificados");
            Files.createDirectories(dir);
            Path destino = dir.resolve("empresa-" + empresaId + "-" + UUID.randomUUID() + ext);
            Files.write(destino, contenido);
            EmpresaFeConfigJpa c = config(empresaId);
            c.setCertificadoPath(destino.toAbsolutePath().toString());
            c.setCertificadoNombre(nombre);
            if (password != null && !password.isBlank()) {
                c.setCertificadoPassword(password);
            }
            c.setUpdatedAt(OffsetDateTime.now());
            EmpresaFeConfigJpa guardada = configRepository.save(c);
            auditoria.registrar(AuditoriaService.usuarioActual(), "CERTIFICADO_FE_ACTUALIZADO", "empresa", empresaId,
                    nombre);
            return aDto(guardada);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo guardar el certificado: " + e.getMessage(), e);
        }
    }

    private EmpresaFeConfigJpa config(Long empresaId) {
        return configRepository.findConEmpresa(empresaId).orElseGet(() -> {
            EmpresaJpa e = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
            EmpresaFeConfigJpa c = new EmpresaFeConfigJpa();
            c.setEmpresa(e);
            c.setCanal("SIMULADO");
            return configRepository.save(c);
        });
    }

    private ConfigFeDto aDto(EmpresaFeConfigJpa c) {
        return new ConfigFeDto(
                (int) c.getAmbiente(), c.getSoftwareId(), null, c.getSoftwareSecurityCode(), c.getTestSetId(),
                c.getResolucionNumero(), c.getResolucionFecha(), c.getPrefijo(), c.getRangoDesde(),
                c.getRangoHasta(), c.getRangoVigenciaDesde(), c.getRangoVigenciaHasta(), c.getCanal(),
                c.getPacProveedor(), c.getPacBaseUrl(), null, c.getCertificadoNombre(), null,
                c.getCertificadoPath(), c.getResponsabilidadFiscal(), c.getCodigoMunicipio(), c.isActivo(),
                c.getCertificadoPath() != null && !c.getCertificadoPath().isBlank(),
                c.getPacApiKey() != null && !c.getPacApiKey().isBlank(),
                c.isNotificarAdquirente());
    }
}
