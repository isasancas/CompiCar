package com.compicar.viajeRecurrente;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

@RestController
@RequestMapping("/api/viajes-recurrentes")
public class ViajeRecurrenteController {

    private final ViajeRecurrenteService viajeRecurrenteService;

    public ViajeRecurrenteController(ViajeRecurrenteService viajeRecurrenteService) {
        this.viajeRecurrenteService = viajeRecurrenteService;
    }

    @PostMapping("/{slug}/finalizar")
    public ResponseEntity<ViajeRecurrenteDTO> finalizarViajeRecurrente(
            @PathVariable String slug,
            Authentication authentication) {
        
        // Obtiene el email del conductor autenticado mediante Spring Security
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
