package com.magnetixdian.application;

import com.magnetixdian.domain.cufe.QrUrlGenerator;
import com.magnetixdian.domain.dian.ConfiguracionQrDian;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.dian.ClienteDianSoap;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import com.magnetixdian.interfaces.dto.ConfigFeMascaraDto;
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
    private final CifradorAes cifrador;
    private final ClienteDianSoap clienteDian;
    private final boolean failFast;
    private final String uploadDir;
    private final String qrBaseUrlProduccion;
    private final String qrBaseUrlHabilitacion;

    public ServicioConfigFe(EmpresaRepository empresaRepository, EmpresaFeConfigRepository configRepository,
                            AuditoriaService auditoria, CifradorAes cifrador, ClienteDianSoap clienteDian,
                            @Value("${magnetixdian.seguridad.fail-fast:false}") boolean failFast,
                            @Value("${magnetixdian.upload.dir:./uploads}") String uploadDir,
                            @Value("${magnetixdian.dian.qr.base-url-produccion:}") String qrBaseUrlProduccion,
                            @Value("${magnetixdian.dian.qr.base-url-habilitacion:}") String qrBaseUrlHabilitacion) {
        this.empresaRepository = empresaRepository;
        this.configRepository = configRepository;
        this.auditoria = auditoria;
        this.cifrador = cifrador;
        this.clienteDian = clienteDian;
        this.failFast = failFast;
        this.uploadDir = uploadDir;
        this.qrBaseUrlProduccion = qrBaseUrlProduccion;
        this.qrBaseUrlHabilitacion = qrBaseUrlHabilitacion;
    }

    @Transactional
    public ConfigFeDto obtener(Long empresaId) {
        return aDto(config(empresaId));
    }

    @Transactional
    public ConfigFeDto guardar(Long empresaId, ConfigFeDto dto) {
        EmpresaFeConfigJpa c = config(empresaId);
        short ambienteSolicitado = dto.ambiente() == null ? c.getAmbiente() : dto.ambiente().shortValue();
        String canalSolicitado = dto.canal() == null || dto.canal().isBlank()
                ? c.getCanal() : dto.canal().toUpperCase();
        validarSwitchover(empresaId, c.getAmbiente(), c.getCanal(), ambienteSolicitado, canalSolicitado);
        c.setAmbiente(ambienteSolicitado);
        c.setSoftwareId(dto.softwareId());
        if (dto.softwarePin() != null && !dto.softwarePin().isBlank()) {
            c.setSoftwarePin(cifrador.cifrar(dto.softwarePin().trim()));
        }
        c.setTestSetId(dto.testSetId());
        c.setResolucionNumero(dto.resolucionNumero());
        c.setResolucionFecha(dto.resolucionFecha());
        c.setPrefijo(dto.prefijo());
        c.setRangoDesde(dto.rangoDesde());
        c.setRangoHasta(dto.rangoHasta());
        c.setRangoVigenciaDesde(dto.rangoVigenciaDesde());
        c.setRangoVigenciaHasta(dto.rangoVigenciaHasta());
        c.setCanal(canalSolicitado);
        c.setPacProveedor(dto.pacProveedor());
        c.setPacBaseUrl(dto.pacBaseUrl());
        if (dto.pacApiKey() != null && !dto.pacApiKey().isBlank()) {
            c.setPacApiKey(cifrador.cifrar(dto.pacApiKey().trim()));
        }
        c.setCertificadoNombre(dto.certificadoNombre());
        c.setCertificadoPath(dto.certificadoPath());
        if (dto.certificadoPassword() != null && !dto.certificadoPassword().isBlank()) {
            c.setCertificadoPassword(cifrador.cifrar(dto.certificadoPassword().trim()));
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
        validarSeguridadProduccion(ambienteSolicitado, canalSolicitado, c);
        c.setUpdatedAt(java.time.OffsetDateTime.now());
        EmpresaFeConfigJpa guardada = configRepository.save(c);
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "CONFIG_FE_ACTUALIZADA",
                "empresa", empresaId, "canal=" + guardada.getCanal() + ", ambiente=" + guardada.getAmbiente());
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
                c.setCertificadoPassword(cifrador.cifrar(password.trim()));
            }
            c.setUpdatedAt(OffsetDateTime.now());
            EmpresaFeConfigJpa guardada = configRepository.save(c);
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "CERTIFICADO_FE_ACTUALIZADO",
                    "empresa", empresaId, nombre);
            return aDto(guardada);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo guardar el certificado: " + e.getMessage(), e);
        }
    }

    private void validarSwitchover(Long empresaId, short ambienteActual, String canalActual,
                                   short ambienteSolicitado, String canalSolicitado) {
        boolean entraProduccion = ambienteSolicitado == 1 && ambienteActual != 1;
        boolean activaDianEnProduccion = ambienteSolicitado == 1
                && "DIAN".equalsIgnoreCase(canalSolicitado) && !"DIAN".equalsIgnoreCase(canalActual);
        if (entraProduccion || activaDianEnProduccion) {
            auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(),
                    "CAMBIO_PRODUCCION_BLOQUEADO", "empresa", empresaId,
                    "ambiente=" + ambienteSolicitado + ", canal=" + canalSolicitado);
            throw new IllegalStateException("El cambio a producción requiere el switchover auditado tras aprobar el set de pruebas");
        }
    }

    private void validarSeguridadProduccion(short ambiente, String canal, EmpresaFeConfigJpa config) {
        if (!failFast || ambiente != 1 || !"DIAN".equalsIgnoreCase(canal)) {
            return;
        }
        if (config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()
                || config.getCertificadoPassword() == null || config.getCertificadoPassword().isBlank()) {
            throw new IllegalStateException("Certificado DIAN obligatorio en producción con SEGURIDAD_FAIL_FAST");
        }
    }

    @Transactional(readOnly = true)
    public ConfigFeMascaraDto obtenerMascara(Long empresaId) {
        EmpresaFeConfigJpa c = config(empresaId);
        boolean tieneCertificado = c.getCertificadoPath() != null && !c.getCertificadoPath().isBlank();
        return new ConfigFeMascaraDto((int) c.getAmbiente(), c.getCanal(), c.getCertificadoNombre(),
                tieneCertificado ? "********" : "NO_CONFIGURADO", tieneCertificado,
                c.getPacApiKey() != null && !c.getPacApiKey().isBlank(), clienteDian.versionPorDefecto());
    }

    @Transactional
    public void activarProduccion(Long empresaId, String trackId) {
        EmpresaFeConfigJpa c = config(empresaId);
        c.setAmbiente((short) 1);
        c.setCanal("DIAN");
        c.setActivo(true);
        c.setUpdatedAt(OffsetDateTime.now());
        configRepository.save(c);
        auditoria.registrarEnEmpresa(empresaId, AuditoriaService.usuarioActual(), "CONFIG_FE_ACTUALIZADA",
                "empresa", empresaId, "ambiente=1, canal=DIAN, trackId=" + (trackId == null ? "" : trackId));
    }

    @Transactional(readOnly = true)
    public String urlQr(Long empresaId, String cufe, String hash) {
        EmpresaFeConfigJpa c = config(empresaId);
        return QrUrlGenerator.generar(cufe, c.getAmbiente(),
                new ConfiguracionQrDian(qrBaseUrlProduccion, qrBaseUrlHabilitacion, hash));
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
                (int) c.getAmbiente(), c.getSoftwareId(), null, null, c.getTestSetId(),
                c.getResolucionNumero(), c.getResolucionFecha(), c.getPrefijo(), c.getRangoDesde(),
                c.getRangoHasta(), c.getRangoVigenciaDesde(), c.getRangoVigenciaHasta(), c.getCanal(),
                c.getPacProveedor(), c.getPacBaseUrl(), null, c.getCertificadoNombre(), null,
                null, c.getResponsabilidadFiscal(), c.getCodigoMunicipio(), c.isActivo(),
                c.getCertificadoPath() != null && !c.getCertificadoPath().isBlank(),
                c.getPacApiKey() != null && !c.getPacApiKey().isBlank(),
                c.isNotificarAdquirente());
    }
}
