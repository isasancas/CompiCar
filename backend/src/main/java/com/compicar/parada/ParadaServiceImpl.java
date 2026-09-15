package com.compicar.parada;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import com.compicar.correo.CorreoService;
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
    private final CorreoService correoService;

    @Autowired
    public ParadaServiceImpl(ParadaRepository paradaRepository, ViajeRepository viajeRepository,
            NotificacionRepository notificacionRepository, PersonaRepository personaRepository,
            CorreoService correoService, ReservaRepository reservaRepository, ViajeRecurrenteRepository viajeRecurrenteRepository) {
        this.paradaRepository = paradaRepository;
        this.viajeRepository = viajeRepository;
        this.notificacionRepository = notificacionRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
        this.correoService = correoService;
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
    public List<Notificacion> solicitarNuevaParada(String pasajeroEmail, SolicitudNuevaParadaRequest request) {
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
        Viaje viajePadre = recurrente != null ? recurrente.getViajePadre() : viaje;
        boolean tieneOcurrenciasRecurrentes = viajePadre != null
            && !viajeRecurrenteRepository.findByViajePadreId(viajePadre.getId()).isEmpty();
        if (request.todaRecurrencia() && !tieneOcurrenciasRecurrentes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Toda la recurrencia solo esta disponible para viajes recurrentes");
        }

        Persona conductor = viaje != null ? viaje.getPersona() : recurrente.getPersona();
        List<Reserva> reservasObjetivo = request.todaRecurrencia()
            ? new ArrayList<>(reservaRepository.findReservasConfirmadasDeRecurrencia(
                viajePadre.getId(), pasajero.getId(), RESERVAS_CONFIRMADAS))
            : List.of(reserva);

        if (request.todaRecurrencia()) {
            Reserva reservaPadre = reserva.getViaje() != null
                ? reserva
                : reservaRepository.findByViajeIdAndPersonaIdAndEstadoNot(
                    viajePadre.getId(), pasajero.getId(), EstadoReserva.CANCELADA).orElse(null);

            if (reservaPadre != null && RESERVAS_CONFIRMADAS.contains(reservaPadre.getEstado())
                    && reservasObjetivo.stream().noneMatch(item -> item.getId().equals(reservaPadre.getId()))) {
                reservasObjetivo.add(0, reservaPadre);
            }
        }

        if (reservasObjetivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No hay reservas confirmadas para aplicar la solicitud");
        }

        if (reservasObjetivo.stream().anyMatch(this::tieneSolicitudPendiente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya tienes una solicitud de parada pendiente para uno de los viajes seleccionados");
        }

        List<Notificacion> solicitudes = reservasObjetivo.stream()
            .map(reservaObjetivo -> {
                Viaje viajeObjetivo = reservaObjetivo.getViaje();
                ViajeRecurrente recurrenteObjetivo = reservaObjetivo.getViajeRecurrente();
                LocalDateTime fechaHora = request.fechaHora() != null
                    ? request.fechaHora()
                    : viajeObjetivo != null ? viajeObjetivo.getFechaHoraSalida() : recurrenteObjetivo.getFechaHoraSalida();
                String mensaje = crearMensaje(request, reservaObjetivo.getId(), fechaHora, false);
                return new Notificacion(mensaje, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);
            })
            .toList();
        
        // 1. Extraer origen y destino desde las paradas de la reserva
        String origen = "Origen";
        if (reserva.getParadaSubida() != null && reserva.getParadaSubida().getLocalizacion() != null) {
            String origenRaw = reserva.getParadaSubida().getLocalizacion();
            origen = origenRaw.contains(",") ? origenRaw.split(",")[0].trim() : origenRaw.trim();
        }

        String destino = "Destino";
        if (reserva.getParadaBajada() != null && reserva.getParadaBajada().getLocalizacion() != null) {
            String destinoRaw = reserva.getParadaBajada().getLocalizacion();
            destino = destinoRaw.contains(",") ? destinoRaw.split(",")[0].trim() : destinoRaw.trim();
        }

        // 2. Extraer localización desde el DTO 'request'
        String ubicacionParada = "Ubicación no especificada";
        if (request.localizacion() != null) {
            String paradaRaw = request.localizacion();
            ubicacionParada = paradaRaw.contains(",") ? paradaRaw.split(",")[0].trim() : paradaRaw.trim();
        }

        // 3. Obtener fecha y hora según tipo de viaje
        LocalDateTime fechaHoraSalida = null;
        if (reserva.getViaje() != null) {
            fechaHoraSalida = reserva.getViaje().getFechaHoraSalida();
        } else if (reserva.getViajeRecurrente() != null) {
            fechaHoraSalida = reserva.getViajeRecurrente().getFechaHoraSalida();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        String fechaFormateada = fechaHoraSalida != null ? fechaHoraSalida.format(formatter) : "Fecha no especificada";

        // 4. Envío de correo
        if (conductor != null && conductor.getEmail() != null) {
            String nombreConductor = conductor.getNombre() != null ? conductor.getNombre() : "Conductor";
            String nombrePasajero = pasajero.getNombre() != null ? pasajero.getNombre() : "Un pasajero";

            correoService.sendSolicitudNuevaParadaConductor(
                conductor.getEmail(),
                nombreConductor,
                nombrePasajero,
                origen,
                destino,
                ubicacionParada,
                fechaFormateada
            );
        }

        return notificacionRepository.saveAll(solicitudes);
    }

    @Override
    public boolean tieneSolicitudNuevaParadaPendiente(String pasajeroEmail, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        Persona pasajero = buscarPersona(pasajeroEmail);
        if (reserva.getPersona() == null || !reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva no pertenece al pasajero");
        }
        return tieneSolicitudPendiente(reserva);
    }
    
    @Override
    public Notificacion aceptarSolicitudNuevaParada(String conductorEmail, Long notificacionId) {
        Notificacion solicitud = obtenerSolicitudPendiente(notificacionId);
        Reserva reserva = obtenerReserva(solicitud);
        Persona conductor = buscarPersona(conductorEmail);
        validarConductor(reserva, conductor);
        DatosParada datos = leerMensaje(solicitud.getMensaje());

        anadirParada(reserva, datos);

        solicitud.setTipo(TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA);
        solicitud.setLeida(true);
        notificacionRepository.save(solicitud);
        notificarPasajero(reserva.getPersona(), TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA,
            "El conductor ha aceptado la nueva parada " + datos.localizacion + ".");

        // Envío de correo al pasajero
        Persona pasajero = reserva.getPersona();
        if (pasajero != null && pasajero.getEmail() != null) {
            String origen = "Origen";
            if (reserva.getParadaSubida() != null && reserva.getParadaSubida().getLocalizacion() != null) {
                String origenRaw = reserva.getParadaSubida().getLocalizacion();
                origen = origenRaw.contains(",") ? origenRaw.split(",")[0].trim() : origenRaw.trim();
            }

            String destino = "Destino";
            if (reserva.getParadaBajada() != null && reserva.getParadaBajada().getLocalizacion() != null) {
                String destinoRaw = reserva.getParadaBajada().getLocalizacion();
                destino = destinoRaw.contains(",") ? destinoRaw.split(",")[0].trim() : destinoRaw.trim();
            }

            String ubicacionParada = "Ubicación no especificada";
            if (datos != null && datos.localizacion != null) {
                String paradaRaw = datos.localizacion;
                ubicacionParada = paradaRaw.contains(",") ? paradaRaw.split(",")[0].trim() : paradaRaw.trim();
            }

            LocalDateTime fechaHoraSalida = null;
            if (reserva.getViaje() != null) {
                fechaHoraSalida = reserva.getViaje().getFechaHoraSalida();
            } else if (reserva.getViajeRecurrente() != null) {
                fechaHoraSalida = reserva.getViajeRecurrente().getFechaHoraSalida();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
            String fechaFormateada = fechaHoraSalida != null ? fechaHoraSalida.format(formatter) : "Fecha no especificada";

            String nombrePasajero = pasajero.getNombre() != null ? pasajero.getNombre() : "Pasajero";
            String nombreConductor = (conductor != null && conductor.getNombre() != null) ? conductor.getNombre() : "El conductor";

            correoService.sendSolicitudParadaAceptadaPasajero(
                pasajero.getEmail(),
                nombrePasajero,
                nombreConductor,
                origen,
                destino,
                ubicacionParada,
                fechaFormateada
            );
        }

        return solicitud;
    }

    @Override
    public Notificacion rechazarSolicitudNuevaParada(String conductorEmail, Long notificacionId) {
        Notificacion solicitud = obtenerSolicitudPendiente(notificacionId);
        Reserva reserva = obtenerReserva(solicitud);
        Persona conductor = buscarPersona(conductorEmail);
        validarConductor(reserva, conductor);
        DatosParada datos = leerMensaje(solicitud.getMensaje());

        solicitud.setTipo(TipoNotificacion.SOLICITUD_NUEVA_PARADA_RECHAZADA);
        solicitud.setLeida(true);
        notificacionRepository.save(solicitud);
        notificarPasajero(reserva.getPersona(), TipoNotificacion.SOLICITUD_NUEVA_PARADA_RECHAZADA,
            "El conductor ha rechazado la nueva parada " + datos.localizacion + ".");

        // Envío de correo al pasajero
        Persona pasajero = reserva.getPersona();
        if (pasajero != null && pasajero.getEmail() != null) {
            String origen = "Origen";
            if (reserva.getParadaSubida() != null && reserva.getParadaSubida().getLocalizacion() != null) {
                String origenRaw = reserva.getParadaSubida().getLocalizacion();
                origen = origenRaw.contains(",") ? origenRaw.split(",")[0].trim() : origenRaw.trim();
            }

            String destino = "Destino";
            if (reserva.getParadaBajada() != null && reserva.getParadaBajada().getLocalizacion() != null) {
                String destinoRaw = reserva.getParadaBajada().getLocalizacion();
                destino = destinoRaw.contains(",") ? destinoRaw.split(",")[0].trim() : destinoRaw.trim();
            }

            String ubicacionParada = "Ubicación no especificada";
            if (datos != null && datos.localizacion != null) {
                String paradaRaw = datos.localizacion;
                ubicacionParada = paradaRaw.contains(",") ? paradaRaw.split(",")[0].trim() : paradaRaw.trim();
            }

            LocalDateTime fechaHoraSalida = null;
            if (reserva.getViaje() != null) {
                fechaHoraSalida = reserva.getViaje().getFechaHoraSalida();
            } else if (reserva.getViajeRecurrente() != null) {
                fechaHoraSalida = reserva.getViajeRecurrente().getFechaHoraSalida();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
            String fechaFormateada = fechaHoraSalida != null ? fechaHoraSalida.format(formatter) : "Fecha no especificada";

            String nombrePasajero = pasajero.getNombre() != null ? pasajero.getNombre() : "Pasajero";
            String nombreConductor = (conductor != null && conductor.getNombre() != null) ? conductor.getNombre() : "El conductor";

            correoService.sendSolicitudParadaRechazadaPasajero(
                pasajero.getEmail(),
                nombrePasajero,
                nombreConductor,
                origen,
                destino,
                ubicacionParada,
                fechaFormateada
            );
        }

        return solicitud;
    }

    @Override
    public List<Object[]> obtenerTop5Localizaciones() {
        return paradaRepository.findTop5Localizaciones();
    }

    private boolean tieneSolicitudPendiente(Reserva reserva) {
        Persona conductor = reserva.getViaje() != null
            ? reserva.getViaje().getPersona()
            : reserva.getViajeRecurrente().getPersona();
        if (conductor == null || conductor.getEmail() == null) return false;

        Long viajePadreId = obtenerViajePadreId(reserva);

        return notificacionRepository.findByReceptorEmailAndTipo(
                conductor.getEmail(), TipoNotificacion.SOLICITUD_NUEVA_PARADA).stream()
            .map(Notificacion::getMensaje)
            .map(this::leerMensajeSeguro)
            .filter(datos -> datos != null)
            .anyMatch(datos -> datos.reservaId().equals(reserva.getId())
                || esSolicitudDeLaMismaRecurrencia(datos.reservaId(), reserva, viajePadreId));
    }

    private boolean esSolicitudDeLaMismaRecurrencia(Long reservaId, Reserva reserva, Long viajePadreId) {
        if (viajePadreId == null) return false;
        return reservaRepository.findById(reservaId)
            .map(otraReserva -> viajePadreId.equals(obtenerViajePadreId(otraReserva))
                && otraReserva.getPersona() != null
                && reserva.getPersona() != null
                && reserva.getPersona().getId().equals(otraReserva.getPersona().getId()))
            .orElse(false);
    }

    private Long obtenerViajePadreId(Reserva reserva) {
        if (reserva.getViajeRecurrente() != null && reserva.getViajeRecurrente().getViajePadre() != null) {
            return reserva.getViajeRecurrente().getViajePadre().getId();
        }
        if (reserva.getViaje() != null
            && !viajeRecurrenteRepository.findByViajePadreId(reserva.getViaje().getId()).isEmpty()) {
            return reserva.getViaje().getId();
        }
        return null;
    }

    private void anadirParada(Reserva reserva, DatosParada datos) {
        if (reserva.getViaje() != null) {
            insertarParada(reserva.getViaje().getParadas(), datos, reserva.getViaje().getFechaHoraSalida(), reserva.getViaje(), null);
            recalcularKilometros(reserva.getViaje());
            viajeRepository.save(reserva.getViaje());
        } else if (reserva.getViajeRecurrente() != null) {
            insertarParada(reserva.getViajeRecurrente().getParadas(), datos, reserva.getViajeRecurrente().getFechaHoraSalida(), null, reserva.getViajeRecurrente());
            recalcularKilometros(reserva.getViajeRecurrente());
            viajeRecurrenteRepository.save(reserva.getViajeRecurrente());
        }
    }

    private void recalcularKilometros(com.compicar.viajeBase.ViajeBase viaje) {
        List<Parada> paradas = viaje instanceof Viaje viajeNormal
            ? viajeNormal.getParadas()
            : ((ViajeRecurrente) viaje).getParadas();

        List<Parada> paradasConCoordenadas = paradas.stream()
            .filter(parada -> parada.getLatitud() != null && parada.getLongitud() != null)
            .sorted(Comparator.comparing(Parada::getOrden))
            .toList();

        if (paradasConCoordenadas.size() < 2) return;

        double kilometros = 0;
        for (int i = 1; i < paradasConCoordenadas.size(); i++) {
            kilometros += distanciaEnKilometros(paradasConCoordenadas.get(i - 1), paradasConCoordenadas.get(i));
        }
        viaje.setKilometrosRecorridos((int) Math.round(kilometros));
    }

    private double distanciaEnKilometros(Parada origen, Parada destino) {
        double latitudOrigen = Math.toRadians(origen.getLatitud().doubleValue());
        double latitudDestino = Math.toRadians(destino.getLatitud().doubleValue());
        double diferenciaLatitud = latitudDestino - latitudOrigen;
        double diferenciaLongitud = Math.toRadians(destino.getLongitud().doubleValue() - origen.getLongitud().doubleValue());
        double a = Math.sin(diferenciaLatitud / 2) * Math.sin(diferenciaLatitud / 2)
            + Math.cos(latitudOrigen) * Math.cos(latitudDestino)
            * Math.sin(diferenciaLongitud / 2) * Math.sin(diferenciaLongitud / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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

    private String crearMensaje(SolicitudNuevaParadaRequest request, Long reservaId,
        LocalDateTime fechaHora, boolean todaRecurrencia) {
        try {
            return objectMapper.writeValueAsString(new DatosParada(
                reservaId, request.localizacion().trim(), fechaHora, request.latitud(), request.longitud(), todaRecurrencia));
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

    private DatosParada leerMensajeSeguro(String mensaje) {
        try {
            return leerMensaje(mensaje);
        } catch (ResponseStatusException exception) {
            return null;
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