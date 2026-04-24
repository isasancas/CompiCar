package com.compicar.reserva;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.parada.Parada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private static final long HORAS_LIMITE_CANCELACION = 12L;

    private final ReservaRepository reservaRepository;
    private final PersonaRepository personaRepository;
    private final ViajeRepository viajeRepository;
    private final PagoRepository pagoRepository;

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              PersonaRepository personaRepository,
                              ViajeRepository viajeRepository,
                              PagoRepository pagoRepository) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
        this.pagoRepository = pagoRepository;
    }

    public ReservaDTO toDTO(Reserva r) {
    return new ReservaDTO(
        r.getId(),
        r.getEstado().name(),
        r.getFechaHoraReserva(),
        r.getViaje().getId(),
        r.getPersona().getId(),
        r.getParadaSubida().getId(),
        r.getParadaBajada().getId()
    );
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
        Persona pasajero = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));

        if (!reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return reserva;
        }

        Viaje viaje = reserva.getViaje();
        if (viaje.getEstado() == EstadoViaje.CANCELADO || viaje.getEstado() == EstadoViaje.FINALIZADO) {
            throw new IllegalArgumentException("No se puede cancelar una reserva de un viaje en estado " + viaje.getEstado());
        }

        LocalDateTime ahora = LocalDateTime.now();
        long horasHastaSalida = Duration.between(ahora, viaje.getFechaHoraSalida()).toHours();
        boolean menosDeDoceHoras = horasHastaSalida < HORAS_LIMITE_CANCELACION;

        reserva.setEstado(EstadoReserva.CANCELADA);
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + 1);

        Pago pago = reserva.getPago();
        if (pago != null) {
            if (menosDeDoceHoras) {
                pago.setEstado(EstadoPago.COMPLETADO);
            } else {
                pago.setEstado(EstadoPago.REEMBOLSADO);
            }
            pagoRepository.save(pago);
        }

        pasajero.incrementarCancelaciones();
        personaRepository.save(pasajero);
        // Update reservation status to cancelled
        reserva.setEstado(EstadoReserva.CANCELADA);
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + reserva.getCantidadPlazas());
        viajeRepository.save(viaje);
        return reservaRepository.save(reserva);
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorPersona(Persona persona) {
    List<Reserva> reservas = reservaRepository.findByPersona(persona);
    return reservas.stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public List<Reserva> obtenerReservasPorViaje(Long viajeId) {
        viajeRepository.findById(viajeId)
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
        reservaExistente.setFechaHoraReserva(LocalDateTime.now());
        reservaExistente.setEstado(EstadoReserva.PENDIENTE);
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
        reserva.setEstado(EstadoReserva.NO_PRESENTADO);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva marcarNoPresentadoPorConductor(String usuarioEmail, Long reservaId) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));

        Viaje viaje = reserva.getViaje();

        if (!viaje.getPersona().getId().equals(conductor.getId())) {
            throw new IllegalArgumentException("Solo el conductor del viaje puede marcar no presentado");
        }

        if (viaje.getEstado() != EstadoViaje.INICIADO) {
            throw new IllegalArgumentException("Solo se puede marcar no presentado cuando el viaje está INICIADO");
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA || reserva.getEstado() == EstadoReserva.NO_PRESENTADO) {
            throw new IllegalArgumentException("La reserva ya está cancelada o marcada como no presentado");
        }

        reserva.setEstado(EstadoReserva.NO_PRESENTADO);

        Persona pasajero = reserva.getPersona();
        pasajero.incrementarCancelaciones();
        personaRepository.save(pasajero);

        Pago pago = reserva.getPago();
        if (pago != null) {
            pago.setEstado(EstadoPago.COMPLETADO);
            pagoRepository.save(pago);
        }

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