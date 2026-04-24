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
    public Reserva crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Viaje viaje = viajeRepository.findById(viajeId)
            .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado"));
        
        if (plazasSolicitadas == null || plazasSolicitadas < 1) {
            throw new IllegalArgumentException("Debes reservar al menos 1 plaza.");
        }

        if (viaje.getEstado() != EstadoViaje.PENDIENTE) {
            throw new IllegalArgumentException("El viaje no está disponible para reservas (estado: " + viaje.getEstado() + ")");
        }

        if (viaje.getPlazasDisponibles() < plazasSolicitadas) {
            throw new IllegalArgumentException("Solo quedan " + viaje.getPlazasDisponibles() + " plazas disponibles.");
        }
        
        if (viaje.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("No puedes reservar tu propio viaje");
        }

        List<Parada> paradas = viaje.getParadas();
        Parada paradaSubida = paradas.get(0);
        Parada paradaBajada = paradas.get(paradas.size() - 1);

        Reserva reserva = new Reserva(EstadoReserva.PENDIENTE, LocalDateTime.now(), 
                                    persona, paradaSubida, paradaBajada, viaje, plazasSolicitadas);

        reserva = reservaRepository.save(reserva);

        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - plazasSolicitadas);
        viajeRepository.save(viaje);

        reserva.setSlug("reserva-" + reserva.getId());
        return reservaRepository.save(reserva);
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
        Viaje viaje = reserva.getViaje();
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + reserva.getCantidadPlazas());
        
        viajeRepository.save(viaje);
        return reservaRepository.save(reserva);
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
        Viaje viaje = viajeRepository.findById(viajeId)
            .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado con ID: " + viajeId));
        return reservaRepository.findByViajeId(viajeId);
    }

    @Override
    public Reserva actualizarReserva(String usuarioEmail, Long reservaId, Reserva reserva) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        Reserva reservaExistente = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        if (!reservaExistente.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }
        reservaExistente.setParadaSubida(reserva.getParadaSubida());
        reservaExistente.setParadaBajada(reserva.getParadaBajada());
        reservaExistente.setFechaHoraReserva(LocalDateTime.now()); // Update reservation time
        reservaExistente.setEstado(EstadoReserva.PENDIENTE); // Reset status to pending after update
        return reservaRepository.save(reservaExistente);

    }

    @Override
    public Reserva obtenerReservaPorId(Long reservaId) {
        return reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
    }

    @Override
    public Reserva reservaConfirmada(String conductorEmail, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
        .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada")); 
        if (!reserva.getViaje().getPersona().getEmail().equals(conductorEmail)) {
            throw new IllegalArgumentException("No tienes permiso para confirmar esta reserva");
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva reservaNoPresentado(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        // Update reservation status to no-show
        reserva.setEstado(EstadoReserva.NO_PRESENTADO);
        return reservaRepository.save(reserva);
    }

    @Override
    public List<Reserva> obtenerReservasComoConductor(String conductorEmail) {
        System.out.println("Buscando reservas para el conductor: " + conductorEmail);
        List<Reserva> lista = reservaRepository.findPendientesParaConductor(conductorEmail);
        System.out.println("Reservas encontradas: " + lista.size());
        return lista;
    }

    @Override
    public Reserva rechazarReserva(String conductorEmail, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getViaje().getPersona().getEmail().equals(conductorEmail)) {
            throw new IllegalArgumentException("No tienes permiso para rechazar esta reserva");
        }

        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            reserva.setEstado(EstadoReserva.CANCELADA);
            
            Viaje viaje = reserva.getViaje();
            viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + reserva.getCantidadPlazas());
            viajeRepository.save(viaje);
        }

        return reservaRepository.save(reserva);
    }
    
}
