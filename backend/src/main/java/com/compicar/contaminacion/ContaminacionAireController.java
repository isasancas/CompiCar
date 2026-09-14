package com.compicar.contaminacion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contaminacion")
public class ContaminacionAireController {

    private final ContaminacionAireIA contaminacionAireIA;

    public ContaminacionAireController(ContaminacionAireIA contaminacionAireIA) {
        this.contaminacionAireIA = contaminacionAireIA;
    }

    @GetMapping("/actual")
    public ResponseEntity<ContaminacionAireIA.ContaminacionRespuesta> obtenerActual() {
        return ResponseEntity.ok(contaminacionAireIA.obtenerContaminacionActual());
    }
}