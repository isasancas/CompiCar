package com.compicar.reserva;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.reserva.dto.ReservaRequest;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final PersonaRepository personaRepository;

    @Autowired
    public ReservaController(ReservaService reservaService, PersonaRepository personaRepository ) {
        this.reservaService = reservaService;
        this.personaRepository = personaRepository;
    }

    @PostMapping("/crear")
    public Reserva crearReserva(@RequestBody ReservaRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        
        return reservaService.crearReserva(usuarioEmail, request.viajeId(), request.plazas());
    }

    @PutMapping("/cancelar")
    public Reserva cancelarReserva(@RequestParam Long reservaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return reservaService.cancelarReserva(usuarioEmailAuth, reservaId);
    }

    @GetMapping("/mis-reservas")
    public List<ReservaDTO> obtenerReservasPorPersona() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }

        String email = auth.getName();

        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario no encontrado"
            ));

        return reservaService.obtenerReservasPorPersona(persona);
    }

    @GetMapping("/viaje")
    public List<Reserva> obtenerReservasPorViaje(@RequestParam Long viajeId) {
        return reservaService.obtenerReservasPorViaje(viajeId);
    }

    @PutMapping("/actualizar")
    public Reserva actualizarReserva(String usuarioEmail, Long reservaId, Reserva reserva) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmailAuth = auth.getName();
        return reservaService.actualizarReserva(usuarioEmailAuth, reservaId, reserva);
    }

    @GetMapping("/{reservaId}")  
    public Reserva obtenerReservaPorId(@PathVariable Long reservaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }
        String usuarioEmail = auth.getName();
        
        // Fetch the reservation and check ownership
        Reserva reserva = reservaService.obtenerReservaPorId(reservaId);
        if (!reserva.getPersona().getEmail().equals(usuarioEmail)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta reserva"
            );
        }
        return reserva;
    }

    @RequestMapping("/confirmar")
    public ResponseEntity<Reserva> reservaConfirmada(@RequestParam("reservaId") Long reservaId, Principal principal) {
        Reserva confirmada = reservaService.reservaConfirmada(principal.getName(), reservaId);
        return ResponseEntity.ok(confirmada);
    }

    @RequestMapping("/noPresentado")
    public ResponseEntity<Reserva> reservaNoPresentado(Long reservaId) {
        Reserva noPresentado = reservaService.reservaNoPresentado(reservaId);
        return ResponseEntity.ok(noPresentado);
    }

    @GetMapping("/pendientes-conductor")
    public ResponseEntity<List<Reserva>> obtenerPendientes(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        List<Reserva> pendientes = reservaService.obtenerReservasComoConductor(principal.getName());
        return ResponseEntity.ok(pendientes);
    }

    @PutMapping("/rechazar")
    public ResponseEntity<Reserva> rechazarReserva(@RequestParam Long reservaId, Principal principal) {
        Reserva rechazada = reservaService.rechazarReserva(principal.getName(), reservaId);
        return ResponseEntity.ok(rechazada);
    }
    
}
