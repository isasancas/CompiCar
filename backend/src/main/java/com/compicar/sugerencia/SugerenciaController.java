package com.compicar.sugerencia;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sugerencias")
public class SugerenciaController {

    private final SugerenciaService sugerenciaService;

    public SugerenciaController(SugerenciaService sugerenciaService) {
        this.sugerenciaService = sugerenciaService;
    }

    @PostMapping
    public ResponseEntity<Void> enviar(@Valid @RequestBody SugerenciaDTO request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        sugerenciaService.enviar(email, request.getMensaje().trim());
        return ResponseEntity.ok().build();
    }
}