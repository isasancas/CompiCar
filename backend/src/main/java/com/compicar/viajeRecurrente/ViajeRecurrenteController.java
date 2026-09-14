package com.compicar.viajeRecurrente;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

@RestController
@RequestMapping("/api/viajes-recurrentes")
public class ViajeRecurrenteController {

    private final ViajeRecurrenteService viajeRecurrenteService;

    public ViajeRecurrenteController(ViajeRecurrenteService viajeRecurrenteService) {
        this.viajeRecurrenteService = viajeRecurrenteService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ViajeRecurrenteDTO> obtenerViajeRecurrentePorSlug(@PathVariable String slug) {
        ViajeRecurrenteDTO viaje = viajeRecurrenteService.obtenerViajeRecurrentePorSlug(slug);
        return ResponseEntity.ok(viaje);
    }

    @PutMapping("/{slug}/iniciar")
    public ResponseEntity<ViajeRecurrenteDTO> iniciarViajeRecurrente(
            @PathVariable String slug,
            Authentication authentication) {
        String usuarioEmail = authentication.getName();
        ViajeRecurrenteDTO viaje = viajeRecurrenteService.iniciarViajeRecurrente(usuarioEmail, slug);
        return ResponseEntity.ok(viaje);
    }

    @PutMapping("/{slug}/checkin")
    public ResponseEntity<ViajeRecurrenteDTO> confirmarCheckinRecurrente(
            @PathVariable String slug,
            @RequestParam("checkin") String checkin,
            Authentication authentication) {
        String usuarioEmail = authentication.getName();
        ViajeRecurrenteDTO viaje = viajeRecurrenteService.confirmarCheckinRecurrente(usuarioEmail, slug, checkin);
        return ResponseEntity.ok(viaje);
    }

    @PutMapping("/{slug}/finalizar")
    public ResponseEntity<ViajeRecurrenteDTO> finalizarViajeRecurrente(
            @PathVariable String slug,
            Authentication authentication) {
        String usuarioEmail = authentication.getName(); 
        ViajeRecurrenteDTO viajeFinalizado = viajeRecurrenteService.finalizarViajeRecurrente(usuarioEmail, slug);
        return ResponseEntity.ok(viajeFinalizado);
    }

    @PutMapping("/{slug}/cancelar")
    public ResponseEntity<ViajeRecurrenteDTO> cancelarViajeRecurrente(
            @PathVariable String slug,
            Authentication authentication) {
        String usuarioEmail = authentication.getName();
        ViajeRecurrenteDTO viajeCancelado = viajeRecurrenteService.cancelarViajeRecurrente(usuarioEmail, slug);
        return ResponseEntity.ok(viajeCancelado);
    }
}