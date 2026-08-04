package com.compicar.valoracion;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.valoracion.dto.ValoracionDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/valoraciones")
public class ValoracionController {

    private final ValoracionService valoracionService;

    @Autowired
    public ValoracionController(ValoracionService valoracionService) {
        this.valoracionService = valoracionService;
    }

    @PostMapping
    public ResponseEntity<ValoracionDTO> crearValoracion(@Valid @RequestBody ValoracionDTO valoracionDTO) {
        return ResponseEntity.ok(valoracionService.crearValoracion(valoracionDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ValoracionDTO> actualizarValoracion(@PathVariable Long id,
            @Valid @RequestBody ValoracionDTO valoracionDTO) {
        return ResponseEntity.ok(valoracionService.actualizarValoracion(id, valoracionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarValoracion(@PathVariable Long id) {
        valoracionService.eliminarValoracion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValoracionDTO> obtenerValoracionPorId(@PathVariable Long id) {
        return valoracionService.encontrarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/autor/{autorId}")
    public ResponseEntity<List<ValoracionDTO>> obtenerValoracionesPorAutor(@PathVariable Long autorId) {
        return ResponseEntity.ok(valoracionService.encontrarPorAutor(autorId));
    }

    @GetMapping("/valorado/{valoradoId}")
    public ResponseEntity<List<ValoracionDTO>> obtenerValoracionesPorValorado(@PathVariable Long valoradoId) {
        return ResponseEntity.ok(valoracionService.encontrarPorValorado(valoradoId));
    }

    @GetMapping("/reputacion/{personaId}")
    public ResponseEntity<Double> calcularReputacion(@PathVariable Long personaId) {
        return ResponseEntity.ok(valoracionService.calcularReputacion(personaId));
    }
}
