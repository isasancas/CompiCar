package com.compicar.pago;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final PersonaRepository personaRepository;

    @Autowired
    public PagoController(PagoService pagoService, PersonaRepository personaRepository) {
        this.pagoService = pagoService;
        this.personaRepository = personaRepository;
    }

    @RequestMapping("/crear")
    public Pago crearPago(String usuarioEmail, Long reservaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return pagoService.crearPago(usuarioEmailAuth, reservaId);
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
    public Pago obtenerPagoPorId(Long pagoId) {
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

    
}
