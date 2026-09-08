package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioMedioMagnetico;
import com.magnetixdian.application.ServicioMedioMagnetico.CargaResultado;
import com.magnetixdian.interfaces.dto.MedioMagneticoDto;
import com.magnetixdian.interfaces.dto.TercerosResumenDto;
import com.magnetixdian.interfaces.dto.ValidacionProcesoDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API REST del ciclo de medios magnéticos:
 * carga → validación → generación XML → descarga.
 */
@RestController
@RequestMapping("/api/mediomagnetico")
public class MedioMagneticoController {

    private final ServicioMedioMagnetico servicio;

    public MedioMagneticoController(ServicioMedioMagnetico servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(@RequestParam(defaultValue = "1001") String formato) throws java.io.IOException {
        byte[] contenido = servicio.generarPlantilla(formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-" + formato + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }

    @PostMapping(value = "/{empresaId}/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargaResultado> cargar(@PathVariable Long empresaId,
                                                 @RequestParam String formato,
                                                 @RequestParam Integer anioGravable,
                                                 @RequestParam MultipartFile archivo) throws java.io.IOException {
        return ResponseEntity.ok(servicio.cargarDesdeExcel(empresaId, formato, anioGravable, archivo));
    }

    @PostMapping("/{medioMagneticoId}/validar")
    public ResponseEntity<ValidacionProcesoDto> validar(@PathVariable Long medioMagneticoId,
                                                        @RequestParam Integer anioGravable) {
        return ResponseEntity.ok(servicio.validarDto(medioMagneticoId, anioGravable));
    }

    @GetMapping("/{medioMagneticoId}/reporte")
    public ResponseEntity<ValidacionProcesoDto> reporte(@PathVariable Long medioMagneticoId) {
        ValidacionProcesoDto dto = servicio.reporteDto(medioMagneticoId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{medioMagneticoId}/generar-xml")
    public ResponseEntity<String> generarXml(@PathVariable Long medioMagneticoId) {
        String xml = servicio.generarXml(medioMagneticoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/{medioMagneticoId}/presentar")
    public ResponseEntity<MedioMagneticoDto> presentar(@PathVariable Long medioMagneticoId) {
        return ResponseEntity.ok(servicio.marcarPresentado(medioMagneticoId));
    }

    @GetMapping("/{medioMagneticoId}/terceros")
    public ResponseEntity<TercerosResumenDto> terceros(@PathVariable Long medioMagneticoId) {
        return ResponseEntity.ok(servicio.resumenTerceros(medioMagneticoId));
    }

    @PostMapping("/{medioMagneticoId}/aplicar-dv")
    public ResponseEntity<java.util.Map<String, Long>> aplicarDv(@PathVariable Long medioMagneticoId) {
        long corregidos = servicio.aplicarCorreccionesDv(medioMagneticoId);
        return ResponseEntity.ok(java.util.Map.of("corregidos", corregidos));
    }

    @GetMapping("/{medioMagneticoId}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long medioMagneticoId) {
        String xml = servicio.generarXml(medioMagneticoId);
        String formato = servicio.formatoDe(medioMagneticoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=medio-magnetico-" + formato + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<MedioMagneticoDto>> listar(@PathVariable Long empresaId) {
        return ResponseEntity.ok(servicio.listarPorEmpresaDto(empresaId));
    }
}