package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioRegistro;
import com.magnetixdian.interfaces.dto.RegistroEmpresaRequest;
import com.magnetixdian.interfaces.dto.RegistroEmpresaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    private final ServicioRegistro servicio;

    public RegistroController(ServicioRegistro servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<RegistroEmpresaResponse> registrar(@Valid @RequestBody RegistroEmpresaRequest request) {
        return ResponseEntity.ok(servicio.registrar(request));
    }
}
