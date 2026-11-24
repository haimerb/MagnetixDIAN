package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.application.ServicioHabilitacionDian;
import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import com.magnetixdian.interfaces.dto.ConfigFeMascaraDto;
import com.magnetixdian.interfaces.dto.DianHabilitacionDto;
import com.magnetixdian.interfaces.dto.ResultadoHabilitacionDianDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * API REST de configuración DIAN por empresa.
 */
@RestController
@RequestMapping("/api/facturacion/config")
public class ConfiguracionFeController {

    private static final long TAMANO_MAXIMO_CERTIFICADO = 1024L * 1024L;
    private static final long TAMANO_MAXIMO_XML_TEST = 5L * 1024L * 1024L;
    private static final int TOTAL_DOCUMENTOS_TEST = 10;

    private final ServicioConfigFe servicio;
    private final ServicioHabilitacionDian habilitacion;
    private final AutorizacionEmpresa autorizacion;

    public ConfiguracionFeController(ServicioConfigFe servicio, ServicioHabilitacionDian habilitacion,
                                     AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.habilitacion = habilitacion;
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

    @GetMapping("/{empresaId}/mascara")
    public ResponseEntity<ConfigFeMascaraDto> obtenerMascara(@PathVariable Long empresaId,
                                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtenerMascara(empresaId));
    }

    @GetMapping("/{empresaId}/qr")
    public ResponseEntity<String> urlQr(@PathVariable Long empresaId, @RequestParam String cufe,
                                        @RequestParam(value = "hash", required = false) String hash,
                                        Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.urlQr(empresaId, cufe, hash));
    }

    @PostMapping("/{empresaId}/validar")
    public ResponseEntity<ResultadoHabilitacionDianDto> validar(@PathVariable Long empresaId,
                                                                @RequestBody DianHabilitacionDto solicitud,
                                                                Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(habilitacion.validar(empresaId, solicitud));
    }

    @PostMapping(value = "/{empresaId}/test-set", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoHabilitacionDianDto> enviarTestSet(
            @PathVariable Long empresaId,
            @RequestPart("solicitud") DianHabilitacionDto solicitud,
            @RequestPart("documentos") List<MultipartFile> documentos,
            @RequestParam(value = "version", required = false) String version,
            Authentication authentication) throws IOException {
        autorizacion.verificar(empresaId, authentication);
        if (documentos == null || documentos.size() != TOTAL_DOCUMENTOS_TEST) {
            throw new IllegalArgumentException("El set de pruebas debe tener 8 facturas, 1 nota débito y 1 nota crédito");
        }
        List<byte[]> xmls = new ArrayList<>(TOTAL_DOCUMENTOS_TEST);
        for (MultipartFile documento : documentos) {
            if (documento.isEmpty() || documento.getSize() > TAMANO_MAXIMO_XML_TEST) {
                throw new IllegalArgumentException("Cada XML del set de pruebas debe tener entre 1 byte y 5 MB");
            }
            xmls.add(documento.getBytes());
        }
        return ResponseEntity.ok(habilitacion.enviarLotePruebas(empresaId, solicitud, xmls, version));
    }

    @GetMapping("/{empresaId}/estado")
    public ResponseEntity<ResultadoHabilitacionDianDto> consultarEstado(
            @PathVariable Long empresaId,
            @RequestParam String trackId,
            @RequestParam(defaultValue = "2") int ambiente,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "modo", required = false) ModoOperacionDian modo,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        DianHabilitacionDto solicitud = modo == null ? null
                : new DianHabilitacionDto(modo, null, null, null, null, null, null, null, false);
        return ResponseEntity.ok(habilitacion.consultarEstado(empresaId, solicitud, trackId, ambiente, version));
    }

    @PatchMapping("/{empresaId}/switchover")
    public ResponseEntity<ResultadoHabilitacionDianDto> switchover(
            @PathVariable Long empresaId,
            @RequestBody DianHabilitacionDto solicitud,
            @RequestParam String trackId,
            @RequestParam(value = "version", required = false) String version,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(habilitacion.switchoverProduccion(empresaId, solicitud, trackId, version));
    }
}
