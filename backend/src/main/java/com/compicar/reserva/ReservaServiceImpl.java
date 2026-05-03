package com.compicar.reserva;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.reserva.dto.ReservaRequest;
import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.PagoService;
import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.stripe.exception.StripeException;

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
    // 1. Inyecta el PagoService en el constructor
    private final PagoService pagoService;

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              PersonaRepository personaRepository,
                              ViajeRepository viajeRepository,
                              PagoRepository pagoRepository,
                              NotificacionRepository notificacionRepository,
                              ParadaRepository paradaRepository,
                              PagoService pagoService) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
        this.pagoRepository = pagoRepository;
        this.notificacionRepository = notificacionRepository;
        this.paradaRepository = paradaRepository;
        this.pagoService = pagoService;
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
    @Transactional // Importante para que si Stripe falla, la reserva no se guarde
    public String crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas, Long paradaSubidaId, Long paradaBajadaId) {
        
        // 1. Obtener entidades
        Persona persona = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado"));
        
        // 2. Validaciones de negocio
        if (plazasSolicitadas == null || plazasSolicitadas < 1) {
            throw new IllegalArgumentException("Debes reservar al menos 1 plaza.");
        }

        if (viaje.getEstado() != EstadoViaje.PENDIENTE) {
            throw new IllegalArgumentException("El viaje no está disponible (estado: " + viaje.getEstado() + ")");
        }

        if (viaje.getPlazasDisponibles() < plazasSolicitadas) {
            throw new IllegalArgumentException("Solo quedan " + viaje.getPlazasDisponibles() + " plazas disponibles.");
        }
        
        if (viaje.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("No puedes reservar tu propio viaje");
        }

        Parada paradaSubida = paradaRepository.findById(paradaSubidaId)
                .orElseThrow(() -> new IllegalArgumentException("La parada de subida no existe."));
        
        Parada paradaBajada = paradaRepository.findById(paradaBajadaId)
                .orElseThrow(() -> new IllegalArgumentException("La parada de bajada no existe."));

        // 3. Crear la instancia de Reserva
        Reserva reserva = new Reserva(
            EstadoReserva.PENDIENTE, 
            LocalDateTime.now(), 
            persona, 
            paradaSubida, 
            paradaBajada, 
            viaje, 
            plazasSolicitadas
        );

        // 4. Crear el objeto Pago asociado
        Pago pago = new Pago();
        pago.setReserva(reserva);
        // Calculamos el total basado en el precio del viaje
        BigDecimal total = viaje.getPrecio().multiply(new BigDecimal(plazasSolicitadas));
        pago.setImporteTotal(total);
        pago.setEstado(EstadoPago.PENDIENTE);
        
        // Establecer relación bidireccional
        reserva.setPago(pago); 

        // 5. Guardar primero para generar ID y asignar Slug
        reserva = reservaRepository.save(reserva);
        reserva.setSlug("reserva-" + reserva.getId());
        reserva = reservaRepository.save(reserva); // Actualizamos con el slug

        // 6. Actualizar plazas del viaje
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - plazasSolicitadas);
        viajeRepository.save(viaje);

        // 7. Llamada a Stripe para obtener el clientSecret
        try {
            // Este método en PagoService debe devolver el intent.getClientSecret()
            return pagoService.crearIntentoDePago(reserva); 
        } catch (StripeException e) {
            // Si Stripe falla, lanzamos excepción para que @Transactional haga rollback
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error al conectar con la pasarela de pagos: " + e.getMessage());
        }
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
        if (pago != null && pago.getStripePaymentIntentId() != null) {
            try {
                // Si faltan menos de 12h, capturamos el dinero (penalización)
                if (horasHastaSalida < HORAS_LIMITE_CANCELACION) {
                    pagoService.capturarPago(pago.getStripePaymentIntentId());
                } else {
                    // Si es pronto, liberamos el dinero (el pasajero no paga nada)
                    pagoService.cancelarPago(pago.getStripePaymentIntentId());
                }
            } catch (StripeException e) {
                throw new RuntimeException("Error al procesar la devolución en Stripe");
            }
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
    public Reserva actualizarReserva(String usuarioEmail, Long reservaId, ReservaRequest reservaModificada) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        
        Reserva reservaExistente = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));

        if (!reservaExistente.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        Viaje viaje = reservaExistente.getViaje();
        if (LocalDateTime.now().isAfter(viaje.getFechaHoraSalida().minusHours(12))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No se puede modificar la reserva a falta de menos de 12 horas para el viaje");
        }

        if (reservaModificada.plazas() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de plazas no puede estar vacío");
        }

        int plazasAnteriores = reservaExistente.getCantidadPlazas();
        int plazasNuevas = reservaModificada.plazas();

        if (plazasAnteriores != plazasNuevas) {
            int diferencia = plazasAnteriores - plazasNuevas;

            if (viaje.getPlazasDisponibles() + diferencia < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "No hay suficientes plazas disponibles para ampliar la reserva");
            }

            viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + diferencia);
            viajeRepository.save(viaje);
        }

        if (reservaModificada.paradaSubidaId() == null || reservaModificada.paradaBajadaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las paradas no pueden ser nulas");
        }

        Parada subida = paradaRepository.findById(reservaModificada.paradaSubidaId())
                .orElseThrow(() -> new IllegalArgumentException("Parada de subida no encontrada"));
        
        Parada bajada = paradaRepository.findById(reservaModificada.paradaBajadaId())
                .orElseThrow(() -> new IllegalArgumentException("Parada de bajada no válida"));

        reservaExistente.setParadaSubida(subida);
        reservaExistente.setParadaBajada(bajada);
        reservaExistente.setCantidadPlazas(plazasNuevas);
        reservaExistente.setFechaHoraReserva(LocalDateTime.now());
        reservaExistente.setEstado(EstadoReserva.PENDIENTE);

        Reserva actualizada = reservaRepository.save(reservaExistente);

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
            pago.setEstado(EstadoPago.CAPTURADO);
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