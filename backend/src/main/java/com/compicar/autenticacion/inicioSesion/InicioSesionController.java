package com.compicar.autenticacion.inicioSesion;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InicioSesionController {

    private final InicioSesionService inicioSesionService;

    public InicioSesionController(InicioSesionService inicioSesionService) {
        this.inicioSesionService = inicioSesionService;
    }

    @PostMapping("/login")
    public ResponseEntity<Token> login(@Valid @RequestBody InicioSesion request) {
        String jwt = inicioSesionService.iniciarSesion(request);
        return ResponseEntity.ok(new Token(jwt));
    }
}
