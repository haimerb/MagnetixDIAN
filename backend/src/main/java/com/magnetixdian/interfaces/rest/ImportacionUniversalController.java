package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioMapeoColumnas;
import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import com.magnetixdian.interfaces.dto.ConfirmacionImportacionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionUniversalDto;
import com.magnetixdian.interfaces.dto.VistaPreviaArchivoDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/importaciones")
public class ImportacionUniversalController {

    private final ServicioMapeoColumnas servicio;
    private final AutorizacionEmpresa autorizacion;

    public ImportacionUniversalController(ServicioMapeoColumnas servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @PostMapping(value = "/vista-previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VistaPreviaArchivoDto vistaPrevia(@PathVariable Long empresaId,
                                             @RequestParam("archivo") MultipartFile archivo,
                                             Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.vistaPrevia(empresaId, archivo);
    }

    @PostMapping("/confirmar")
    public ResultadoImportacionUniversalDto confirmar(@PathVariable Long empresaId,
                                                      @RequestBody ConfirmacionImportacionDto solicitud,
                                                      Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.confirmar(empresaId, solicitud);
    }

    @GetMapping("/mapeos")
    public List<MapeoColumnaDto> mapeos(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.mapeos(empresaId);
    }
}
