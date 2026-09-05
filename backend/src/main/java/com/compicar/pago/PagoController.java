package com.compicar.pago;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.ReservaRepository;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;

    @Autowired
    public PagoController(PagoService pagoService, PersonaRepository personaRepository, ReservaRepository reservaRepository) {
        this.pagoService = pagoService;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
    }

    @PutMapping("/completar")
    public Pago completarPago(Long pagoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        return pagoService.pagoCompletado(pagoId);
    }

    @PutMapping("/fallar")
    public Pago fallarPago(Long pagoId) {   
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        return pagoService.pagoFallido(pagoId);
    }

    @PutMapping("/reembolsar")
    public Pago reembolsarPago(Long pagoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        return pagoService.pagoReembolsado(pagoId);
    }

    @PutMapping("/actualizar")
    public Pago actualizarPago(String usuarioEmail, Long reservaId, Pago pagoActualizado) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return pagoService.actualizarPago(usuarioEmailAuth, reservaId, pagoActualizado);
    }

    @RequestMapping("/{pagoId}")
    public Pago obtenerPagoPorId(@PathVariable("pagoId") Long pagoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        return pagoService.obtenerPagoPorId(pagoId);
    }

    @RequestMapping("/mis-pagos")
    public List<Pago> obtenerPagosPorPersona() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        return pagoService.obtenerPagosPorPersona(persona);
    }

    @PostMapping("/capturar/{id}")
    public ResponseEntity<?> capturarPago(@PathVariable("id") String stripePaymentIntentId) {
        try {
            // Llamamos al método que ya tienes creado en el Service
            pagoService.capturarPago(stripePaymentIntentId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Pago capturado con éxito",
                "id", stripePaymentIntentId
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al capturar el pago: " + e.getMessage()));
        }
    }
    
}
