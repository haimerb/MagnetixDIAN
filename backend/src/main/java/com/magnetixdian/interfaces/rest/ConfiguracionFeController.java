package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * API REST de configuración DIAN por empresa.
 */
@RestController
@RequestMapping("/api/facturacion/config")
public class ConfiguracionFeController {

    private static final long TAMANO_MAXIMO_CERTIFICADO = 1024L * 1024L;

    private final ServicioConfigFe servicio;
    private final AutorizacionEmpresa autorizacion;

    public ConfiguracionFeController(ServicioConfigFe servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> obtener(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId));
    }

    @PutMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> guardar(@PathVariable Long empresaId, @RequestBody ConfigFeDto dto,
                                               Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.guardar(empresaId, dto));
    }

    @PostMapping(value = "/{empresaId}/certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConfigFeDto> subirCertificado(@PathVariable Long empresaId,
                                                         @RequestParam("archivo") MultipartFile archivo,
                                                         @RequestParam(value = "password", required = false) String password,
                                                         Authentication authentication) throws IOException {
        autorizacion.verificar(empresaId, authentication);
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !(nombre.toLowerCase().endsWith(".p12") || nombre.toLowerCase().endsWith(".pfx"))) {
            throw new IllegalArgumentException("El certificado debe ser un archivo .p12 o .pfx");
        }
        if (archivo.isEmpty() || archivo.getSize() > TAMANO_MAXIMO_CERTIFICADO) {
            throw new IllegalArgumentException("El certificado debe tener entre 1 byte y 1 MB.");
        }
        String nombreSeguro = nombre.replace('\\', '/');
        nombreSeguro = nombreSeguro.substring(nombreSeguro.lastIndexOf('/') + 1);
        if (nombreSeguro.isBlank()) {
            throw new IllegalArgumentException("El nombre del certificado no es válido.");
        }
        return ResponseEntity.ok(servicio.guardarCertificado(empresaId, nombreSeguro, archivo.getBytes(), password));

    }
}
