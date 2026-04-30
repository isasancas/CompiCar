package com.compicar.reserva;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
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
    private final NotificacionRepository notificacionRepository;
    private final ParadaRepository paradaRepository;

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              PersonaRepository personaRepository,
                              ViajeRepository viajeRepository,
                              PagoRepository pagoRepository,
                              NotificacionRepository notificacionRepository,
                              ParadaRepository paradaRepository) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
        this.pagoRepository = pagoRepository;
        this.notificacionRepository = notificacionRepository;
        this.paradaRepository = paradaRepository;
    }

    public ReservaDTO toDTO(Reserva r) {
    return new ReservaDTO(
        r.getId(),
        r.getEstado().name(),
        r.getFechaHoraReserva(),
        r.getViaje().getId(),
        r.getPersona().getId(),
        r.getPersona().getNombre(),
        r.getPersona().getSlug(),
        r.getParadaSubida().getId(),
        r.getParadaBajada().getId(),
        r.getCantidadPlazas()
    );
}

    @Override
    public Reserva crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas, Long paradaSubidaId, Long paradaBajadaId) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        Viaje viaje = viajeRepository.findById(viajeId)
            .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado"));
        
        // 1. Validaciones básicas
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

        // 2. BUSCAR PARADAS REALES SELECCIONADAS
        Parada paradaSubida = paradaRepository.findById(paradaSubidaId)
            .orElseThrow(() -> new IllegalArgumentException("La parada de subida seleccionada no existe."));
        
        Parada paradaBajada = paradaRepository.findById(paradaBajadaId)
            .orElseThrow(() -> new IllegalArgumentException("La parada de bajada seleccionada no existe."));

        // 3. Crear la reserva con las paradas del usuario
        Reserva reserva = new Reserva(
            EstadoReserva.PENDIENTE, 
            LocalDateTime.now(), 
            persona, 
            paradaSubida, 
            paradaBajada, 
            viaje, 
            plazasSolicitadas
        );

        // Guardar reserva inicial para obtener ID
        reserva = reservaRepository.save(reserva);

        // 4. Actualizar plazas del viaje
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - plazasSolicitadas);
        viajeRepository.save(viaje);

        // 5. Generar slug y guardado final
        reserva.setSlug("reserva-" + reserva.getId());
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva cancelarReserva(String usuarioEmail, Long reservaId) {
        // 1. Buscamos las entidades
        Persona pasajero = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // 2. Verificaciones de seguridad
        if (!reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        // Si ya está cancelada, no hacemos nada más
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return reserva;
        }

        Viaje viaje = reserva.getViaje();
        
        // 3. Notificación al conductor
        String msj = pasajero.getNombre() + " ha cancelado su reserva en tu viaje.";
        notificacionRepository.save(new Notificacion(msj, viaje.getPersona(), TipoNotificacion.RESERVA_CANCELADA));

        // 4. LÓGICA DE PLAZAS (UNA SOLA VEZ)
        // Devolvemos al viaje EXACTAMENTE las plazas que tenía la reserva
        int plazasADevolver = reserva.getCantidadPlazas();
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + plazasADevolver);
        viajeRepository.save(viaje);

        // 5. Lógica de Pagos y penalizaciones
        LocalDateTime ahora = LocalDateTime.now();
        long horasHastaSalida = Duration.between(ahora, viaje.getFechaHoraSalida()).toHours();
        
        Pago pago = reserva.getPago();
        if (pago != null) {
            if (horasHastaSalida < HORAS_LIMITE_CANCELACION) {
                pago.setEstado(EstadoPago.COMPLETADO); // Se le cobra igual por cancelar tarde
            } else {
                pago.setEstado(EstadoPago.REEMBOLSADO);
            }
            pagoRepository.save(pago);
        }

        pasajero.incrementarCancelaciones();
        personaRepository.save(pasajero);

        // 6. Cambiamos el estado de la reserva al final
        reserva.setEstado(EstadoReserva.CANCELADA);
        
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
    public Reserva actualizarReserva(String usuarioEmail, Long reservaId, Reserva reservaModificada) {
        // 1. Cargar entidades principales
        Persona persona = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        
        Reserva reservaExistente = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));

        // 2. Validaciones de seguridad y tiempo
        if (!reservaExistente.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        Viaje viaje = reservaExistente.getViaje();
        if (LocalDateTime.now().isAfter(viaje.getFechaHoraSalida().minusHours(12))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No se puede modificar la reserva a falta de menos de 12 horas para el viaje");
        }

        // 3. Lógica de ajuste de plazas en el viaje
        int plazasAnteriores = reservaExistente.getCantidadPlazas();
        int plazasNuevas = reservaModificada.getCantidadPlazas();

        if (plazasAnteriores != plazasNuevas) {
            // Calculamos la diferencia: 
            // Si pasas de 2 a 3 plazas, diferencia = -1 (el viaje pierde una plaza disponible)
            // Si pasas de 3 a 1 plaza, diferencia = +2 (el viaje gana dos plazas disponibles)
            int diferencia = plazasAnteriores - plazasNuevas;

            if (viaje.getPlazasDisponibles() + diferencia < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "No hay suficientes plazas disponibles para ampliar la reserva");
            }

            // Actualizamos el stock del viaje
            viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + diferencia);
            viajeRepository.save(viaje);
        }

        // 4. Validar y cargar las nuevas paradas
        if (reservaModificada.getParadaSubida() == null || reservaModificada.getParadaBajada() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las paradas no pueden ser nulas");
        }

        Parada subida = paradaRepository.findById(reservaModificada.getParadaSubida().getId())
                .orElseThrow(() -> new IllegalArgumentException("Parada de subida no encontrada"));
        
        Parada bajada = paradaRepository.findById(reservaModificada.getParadaBajada().getId())
                .orElseThrow(() -> new IllegalArgumentException("Parada de bajada no válida"));

        // 5. Aplicar cambios a la reserva existente
        reservaExistente.setParadaSubida(subida);
        reservaExistente.setParadaBajada(bajada);
        reservaExistente.setCantidadPlazas(plazasNuevas);
        reservaExistente.setFechaHoraReserva(LocalDateTime.now());
        
        // Volvemos a ponerla en PENDIENTE para que el conductor deba aceptarla de nuevo si quieres
        reservaExistente.setEstado(EstadoReserva.PENDIENTE);

        Reserva actualizada = reservaRepository.save(reservaExistente);

        // 6. Notificar al conductor
        String msj = "El pasajero " + actualizada.getPersona().getNombre() + 
                     " ha modificado su reserva para el viaje " + actualizada.getViaje().getSlug() + 
                     ". Revisa los cambios.";

        Notificacion noti = new Notificacion(
                msj, 
                viaje.getPersona(), 
                TipoNotificacion.RESERVA_MODIFICADA
        );        
        notificacionRepository.save(noti);

        return actualizada;
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