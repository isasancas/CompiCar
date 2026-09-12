package com.compicar.parada;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.parada.dto.SolicitudNuevaParadaRequest;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
@Transactional
public class ParadaServiceImpl implements ParadaService {

    private static final Set<EstadoReserva> RESERVAS_CONFIRMADAS = EnumSet.of(
        EstadoReserva.PAGADA, EstadoReserva.CONFIRMADA, EstadoReserva.PRESENTE);

    private final ParadaRepository paradaRepository;
    private final ViajeRepository viajeRepository;
    private final NotificacionRepository notificacionRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;
    private final ViajeRecurrenteRepository viajeRecurrenteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    public ParadaServiceImpl(ParadaRepository paradaRepository, ViajeRepository viajeRepository,
            NotificacionRepository notificacionRepository, PersonaRepository personaRepository,
            ReservaRepository reservaRepository, ViajeRecurrenteRepository viajeRecurrenteRepository) {
        this.paradaRepository = paradaRepository;
        this.viajeRepository = viajeRepository;
        this.notificacionRepository = notificacionRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
    }

    @Override
    public Parada crearParada(Long viajeId, Parada parada) {
            Viaje viaje = viajeRepository.findById(viajeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    
            parada.setViaje(viaje);
            return paradaRepository.save(parada);   
    }

    @Override
    public Viaje anadirParadas(Long viajeId, List<Parada> paradas) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (viaje.getParadas() == null) {
            viaje.setParadas(new java.util.ArrayList<>());
        }

        paradas.forEach(p -> {
            p.setViaje(viaje);
            viaje.getParadas().add(p);
        });

        return paradaRepository.saveAll(paradas).isEmpty() ? viaje : viaje;
    }

    @Override
    public List<Parada> obtenerParadasPorViaje(Viaje viaje) {
        return paradaRepository.findByViaje(viaje);
    }

    @Override
    public Notificacion solicitarNuevaParada(String pasajeroEmail, SolicitudNuevaParadaRequest request) {
        if (request == null || request.reservaId() == null || request.localizacion() == null
                || request.localizacion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La reserva y la localizacion son obligatorias");
        }

        Persona pasajero = buscarPersona(pasajeroEmail);
        Reserva reserva = reservaRepository.findById(request.reservaId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        if (reserva.getPersona() == null || !reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva no pertenece al pasajero");
        }
        validarReservaConfirmada(reserva);

        Viaje viaje = reserva.getViaje();
        ViajeRecurrente recurrente = reserva.getViajeRecurrente();
        if (viaje == null && recurrente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva no esta asociada a un viaje");
        }
        if (request.todaRecurrencia() && recurrente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Toda la recurrencia solo esta disponible para viajes recurrentes");
        }

        LocalDateTime fechaHora = request.fechaHora() != null
            ? request.fechaHora() : viaje != null ? viaje.getFechaHoraSalida() : recurrente.getFechaHoraSalida();
        Persona conductor = viaje != null ? viaje.getPersona() : recurrente.getPersona();
        String mensaje = crearMensaje(request, reserva.getId(), fechaHora);
        return notificacionRepository.save(new Notificacion(
            mensaje, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA));
    }

    @Override
    public Notificacion aceptarSolicitudNuevaParada(String conductorEmail, Long notificacionId) {
        Notificacion solicitud = obtenerSolicitudPendiente(notificacionId);
        Reserva reserva = obtenerReserva(solicitud);
        validarConductor(reserva, buscarPersona(conductorEmail));
        DatosParada datos = leerMensaje(solicitud.getMensaje());

        if (datos.todaRecurrencia && reserva.getViajeRecurrente() != null) {
            reservaRepository.findReservasConfirmadasDeRecurrencia(
                reserva.getViajeRecurrente().getViajePadre().getId(), reserva.getPersona().getId(), RESERVAS_CONFIRMADAS)
                .forEach(r -> anadirParada(r, datos));
        } else {
            anadirParada(reserva, datos);
        }

        solicitud.setTipo(TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA);
        solicitud.setLeida(true);
        notificacionRepository.save(solicitud);
        notificarPasajero(reserva.getPersona(), TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA,
            "El conductor ha aceptado la nueva parada " + datos.localizacion + ".");
        return solicitud;
    }

    @Override
    public Notificacion rechazarSolicitudNuevaParada(String conductorEmail, Long notificacionId) {
        Notificacion solicitud = obtenerSolicitudPendiente(notificacionId);
        Reserva reserva = obtenerReserva(solicitud);
        validarConductor(reserva, buscarPersona(conductorEmail));
        DatosParada datos = leerMensaje(solicitud.getMensaje());

        solicitud.setTipo(TipoNotificacion.SOLICITUD_NUEVA_PARADA_RECHAZADA);
        solicitud.setLeida(true);
        notificacionRepository.save(solicitud);
        notificarPasajero(reserva.getPersona(), TipoNotificacion.SOLICITUD_NUEVA_PARADA_RECHAZADA,
            "El conductor ha rechazado la nueva parada " + datos.localizacion + ".");
        return solicitud;
    }

    private void anadirParada(Reserva reserva, DatosParada datos) {
        if (reserva.getViaje() != null) {
            insertarParada(reserva.getViaje().getParadas(), datos, reserva.getViaje().getFechaHoraSalida(), reserva.getViaje(), null);
            viajeRepository.save(reserva.getViaje());
        } else if (reserva.getViajeRecurrente() != null) {
            insertarParada(reserva.getViajeRecurrente().getParadas(), datos, reserva.getViajeRecurrente().getFechaHoraSalida(), null, reserva.getViajeRecurrente());
            viajeRecurrenteRepository.save(reserva.getViajeRecurrente());
        }
    }

    private void insertarParada(List<Parada> paradas, DatosParada datos, LocalDateTime fecha,
            Viaje viaje, ViajeRecurrente recurrente) {
        Parada destino = paradas.stream().filter(p -> p.getTipo() == TipoParada.DESTINO).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje no tiene destino"));
        int ordenDestino = destino.getOrden();
        paradas.stream().filter(p -> p.getOrden() >= ordenDestino)
            .forEach(p -> p.setOrden(p.getOrden() + 1));

        Parada parada = new Parada();
        parada.setFechaHora(datos.fechaHora != null ? datos.fechaHora : fecha);
        parada.setLocalizacion(datos.localizacion);
        parada.setTipo(TipoParada.INTERMEDIA);
        parada.setOrden(ordenDestino);
        parada.setLatitud(datos.latitud);
        parada.setLongitud(datos.longitud);
        parada.setViaje(viaje);
        parada.setViajeRecurrente(recurrente);
        paradaRepository.save(parada);
    }

    private String crearMensaje(SolicitudNuevaParadaRequest request, Long reservaId, LocalDateTime fechaHora) {
        try {
            return objectMapper.writeValueAsString(new DatosParada(
                reservaId, request.localizacion().trim(), fechaHora, request.latitud(), request.longitud(), request.todaRecurrencia()));
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear la solicitud");
        }
    }

    private DatosParada leerMensaje(String mensaje) {
        try {
            JsonNode json = objectMapper.readTree(mensaje);
            return new DatosParada(json.path("reservaId").asLong(), json.path("localizacion").asText(null),
                json.path("fechaHora").isTextual() ? LocalDateTime.parse(json.path("fechaHora").asText()) : null,
                decimal(json, "latitud"), decimal(json, "longitud"), json.path("todaRecurrencia").asBoolean(false));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud de parada no tiene un mensaje valido");
        }
    }

    private BigDecimal decimal(JsonNode json, String field) {
        return json.path(field).isNumber() ? json.path(field).decimalValue() : null;
    }

    private Notificacion obtenerSolicitudPendiente(Long id) {
        Notificacion solicitud = notificacionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificacion no encontrada"));
        if (solicitud.getTipo() != TipoNotificacion.SOLICITUD_NUEVA_PARADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue resuelta");
        }
        return solicitud;
    }

    private Reserva obtenerReserva(Notificacion solicitud) {
        DatosParada datos = leerMensaje(solicitud.getMensaje());
        return reservaRepository.findById(datos.reservaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private Persona buscarPersona(String email) {
        return personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private void validarReservaConfirmada(Reserva reserva) {
        if (!RESERVAS_CONFIRMADAS.contains(reserva.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva aun no esta confirmada");
        }
    }

    private void validarConductor(Reserva reserva, Persona conductor) {
        Persona titular = reserva.getViaje() != null ? reserva.getViaje().getPersona() : reserva.getViajeRecurrente().getPersona();
        if (titular == null || !titular.getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede resolver la solicitud");
        }
    }

    private void notificarPasajero(Persona pasajero, TipoNotificacion tipo, String mensaje) {
        notificacionRepository.save(new Notificacion(mensaje, pasajero, tipo));
    }

    private record DatosParada(Long reservaId, String localizacion, LocalDateTime fechaHora,
            BigDecimal latitud, BigDecimal longitud, boolean todaRecurrencia) { }
    
}
