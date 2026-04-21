package com.compicar.reserva;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.parada.Parada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final PersonaRepository personaRepository;
    private final ViajeRepository viajeRepository;

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository, 
                              PersonaRepository personaRepository, 
                              ViajeRepository viajeRepository) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
    }

    @Override
    public Reserva crearReserva(String usuarioEmail, Long viajeId) {
        // Find the user by email
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));

        // Find the trip by ID
        Viaje viaje = viajeRepository.findById(viajeId)
            .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado con ID: " + viajeId));

        // Business rules: Trip must be pending, have available seats, and user can't reserve their own trip
        if (viaje.getEstado() != EstadoViaje.PENDIENTE) {
            throw new IllegalArgumentException("El viaje no está disponible para reservas (estado: " + viaje.getEstado() + ")");
        }
        if (viaje.getPlazasDisponibles() <= 0) {
            throw new IllegalArgumentException("No hay plazas disponibles en este viaje");
        }
        if (viaje.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("No puedes reservar tu propio viaje");
        }

        // Get pickup and drop-off stops from the trip's ordered paradas (first and last)
        List<Parada> paradas = viaje.getParadas();
        if (paradas.isEmpty()) {
            throw new IllegalArgumentException("El viaje no tiene paradas definidas");
        }
        Parada paradaSubida = paradas.get(0);  // First parada (pickup)
        Parada paradaBajada = paradas.get(paradas.size() - 1);  // Last parada (drop-off)

        // Create the reservation
        Reserva reserva = new Reserva(EstadoReserva.PENDIENTE, LocalDateTime.now(), persona, paradaSubida, paradaBajada, viaje);

        // Save to generate ID
        reserva = reservaRepository.save(reserva);

        // Update trip's available seats
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - 1);
        viajeRepository.save(viaje);

        // Generate and set slug after ID is available
        reserva.setSlug("reserva-" + reserva.getId());
        reserva = reservaRepository.save(reserva);

        return reserva;
    }

    @Override
    public Reserva cancelarReserva(String usuarioEmail, Long reservaId) {
        // Find the user by email
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        // Find the reservation by ID
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        // Check if the reservation belongs to the user        
        if (!reserva.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }
        // Update reservation status to cancelled
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        // Update trip's available seats
        Viaje viaje = reserva.getViaje();
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + 1);
        viajeRepository.save(viaje);
        return reserva;
    }

    @Override
    public List<Reserva> obtenerReservasPorPersona(String usuarioEmail) {
        // Find the user by email
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        // Find reservations by persona
        return reservaRepository.findByPersona(persona);
    }

    @Override
    public List<Reserva> obtenerReservasPorViaje(Long viajeId) {
        // Find the trip by ID
        Viaje viaje = viajeRepository.findById(viajeId)
            .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado con ID: " + viajeId));
        // Find reservations by viaje ID
        return reservaRepository.findByViajeId(viajeId);
    }

    @Override
    public Reserva actualizReserva(String usuarioEmail, Long reservaId, Reserva reserva) {
        // Find the user by email
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        // Find the reservation by ID
        Reserva reservaExistente = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        // Check if the reservation belongs to the user        
        if (!reservaExistente.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }
        // Update allowed fields (e.g., pickup and drop-off stops)
        reservaExistente.setParadaSubida(reserva.getParadaSubida());
        reservaExistente.setParadaBajada(reserva.getParadaBajada());
        reservaExistente.setFechaHoraReserva(LocalDateTime.now()); // Update reservation time
        reservaExistente.setEstado(EstadoReserva.PENDIENTE); // Reset status to pending after update
        return reservaRepository.save(reservaExistente);

    }

    @Override
    public Reserva obtenerReservaPorId(Long reservaId) {
        // Find the reservation by ID
        return reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
    }

    @Override
    public Reserva reservaConfirmada(Long reservaId) {
        // Find the reservation by ID
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId)); 
        // Update reservation status to confirmed
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva reservaNoPresentado(Long reservaId) {
        // Find the reservation by ID
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        // Update reservation status to no-show
        reserva.setEstado(EstadoReserva.NO_PRESENTADO);
        return reservaRepository.save(reserva);
    }

    
    
}
