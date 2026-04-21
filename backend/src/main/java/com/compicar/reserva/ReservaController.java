package com.compicar.reserva;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    @Autowired
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/crear")  // Changed to POST for creation; get usuarioEmail from auth
    public Reserva crearReserva(@RequestBody Long viajeId) {  // Removed usuarioEmail param; expect viajeId in body or as param
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        return reservaService.crearReserva(usuarioEmail, viajeId);
    }

    @PutMapping("/cancelar")
    public Reserva cancelarReserva(String usuarioEmail, Long reservaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return reservaService.cancelarReserva(usuarioEmailAuth, reservaId);
    }

    @GetMapping("/mis-reservas")  // Renamed for clarity; fetches authenticated user's reservations
    public List<Reserva> obtenerReservasPorPersona() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        return reservaService.obtenerReservasPorPersona(usuarioEmail);
    }

    @GetMapping("/viaje")
    public List<Reserva> obtenerReservasPorViaje(Long viajeId) {
        return reservaService.obtenerReservasPorViaje(viajeId);
    }

    @PutMapping("/actualizar")
    public Reserva actualizReserva(String usuarioEmail, Long reservaId, Reserva reserva) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return reservaService.actualizReserva(usuarioEmailAuth, reservaId, reserva);
    }

    @GetMapping("/{reservaId}")  
    public Reserva obtenerReservaPorId(@PathVariable Long reservaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        
        // Fetch the reservation and check ownership
        Reserva reserva = reservaService.obtenerReservaPorId(reservaId);
        if (!reserva.getPersona().getEmail().equals(usuarioEmail)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta reserva"
            );
        }
        return reserva;
    }

    @RequestMapping("/confirmar")
    public Reserva reservaConfirmada(Long reservaId) {
        return reservaService.reservaConfirmada(reservaId);
    }

    @RequestMapping("/noPresentado")
    public Reserva reservaNoPresentado(Long reservaId) {
        return reservaService.reservaNoPresentado(reservaId);
    }
    
}
