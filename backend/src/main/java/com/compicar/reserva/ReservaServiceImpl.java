package com.compicar.reserva;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.reserva.dto.ReservaCreadaResponse;
import com.compicar.reserva.dto.ReservaRequest;
import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.PagoService;
import com.compicar.pago.StripeService;
import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.compicar.viajeRecurrente.ViajeRecurrenteService;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;
import com.stripe.exception.StripeException;

import jakarta.persistence.EntityNotFoundException;

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
    private final PagoService pagoService;
    private final ViajeRecurrenteRepository viajeRecurrenteRepository;
    private final ViajeRecurrenteService viajeRecurrenteService;
    private final StripeService stripeService;

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              PersonaRepository personaRepository,
                              ViajeRepository viajeRepository,
                              PagoRepository pagoRepository,
                              NotificacionRepository notificacionRepository,
                              ParadaRepository paradaRepository,
                              PagoService pagoService,
                              ViajeRecurrenteRepository viajeRecurrenteRepository,
                              ViajeRecurrenteService viajeRecurrenteService,
                              StripeService stripeService) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
        this.pagoRepository = pagoRepository;
        this.notificacionRepository = notificacionRepository;
        this.paradaRepository = paradaRepository;
        this.pagoService = pagoService;
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
        this.viajeRecurrenteService = viajeRecurrenteService;
        this.stripeService = stripeService;
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
    @Transactional
    public ReservaCreadaResponse crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas, Long paradaSubidaId, Long paradaBajadaId) {

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

        // Novedad: Validar que la fecha/hora de salida no haya transcurrido
        if (viaje.getFechaHoraSalida() != null && viaje.getFechaHoraSalida().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La hora prevista de salida del viaje ya ha pasado.");
        }

        if (viaje.getPlazasDisponibles() < plazasSolicitadas) {
            throw new IllegalArgumentException("Solo quedan " + viaje.getPlazasDisponibles() + " plazas disponibles.");
        }

        if (viaje.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("No puedes reservar tu propio viaje.");
        }

        // Novedad: Validar paradas nulas
        if (paradaSubidaId == null || paradaBajadaId == null) {
            throw new IllegalArgumentException("Debes indicar una parada de subida y de bajada válidas.");
        }

        Parada paradaSubida = paradaRepository.findById(paradaSubidaId)
                .orElseThrow(() -> new IllegalArgumentException("Parada de subida no encontrada"));
        Parada paradaBajada = paradaRepository.findById(paradaBajadaId)
                .orElseThrow(() -> new IllegalArgumentException("Parada de bajada no encontrada"));

        // Novedad: Validar que las paradas pertenezcan a este viaje
        if (!paradaSubida.getViaje().getId().equals(viaje.getId()) || !paradaBajada.getViaje().getId().equals(viaje.getId())) {
            throw new IllegalArgumentException("Las paradas seleccionadas no pertenecen a este viaje.");
        }

        // Novedad: Validar orden de recorrido de las paradas
        if (paradaSubida.getOrden() >= paradaBajada.getOrden()) {
            throw new IllegalArgumentException("La parada de subida debe ser anterior a la parada de bajada.");
        }

        // Novedad: Evitar reservas duplicadas activas del mismo usuario en este viaje
        boolean yaTieneReserva = reservaRepository.existsByPersonaIdAndViajeIdAndEstadoNot(
                persona.getId(), viaje.getId(), EstadoReserva.CANCELADA
        );
        if (yaTieneReserva) {
            throw new IllegalArgumentException("Ya tienes una reserva activa en este viaje.");
        }

        // 3. Crear y guardar la Reserva PRIMERO (sin Pago) para obtener su ID
        Reserva reserva = new Reserva(
                EstadoReserva.PENDIENTE,
                LocalDateTime.now(),
                persona,
                paradaSubida,
                paradaBajada,
                viaje,
                plazasSolicitadas
        );
        reserva.setSlug("reserva-tmp-" + System.currentTimeMillis()); // evita constraint unique
        reserva = reservaRepository.saveAndFlush(reserva);

        // 4. Ahora sí tenemos ID → slug definitivo
        reserva.setSlug("reserva-" + reserva.getId());
        reserva = reservaRepository.saveAndFlush(reserva);

        // 5. Crear el Pago con la Reserva ya persistida
        Pago pago = new Pago();
        BigDecimal total = viaje.getPrecio().multiply(new BigDecimal(plazasSolicitadas));
        pago.setImporteTotal(total);
        BigDecimal comision = total.multiply(new BigDecimal("0.10"));
        pago.setComision(comision);
        pago.setImporteConductor(total.subtract(comision));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setFechaPago(null);
        pago.setReserva(reserva);

        // 6. Guardar el Pago (es el lado dueño de la FK en BD)
        pago = pagoRepository.saveAndFlush(pago);
        reserva.setPago(pago);

        // 7. Llamar a Stripe (si falla, @Transactional hace rollback de todo)
        try {
            String clientSecret = pagoService.crearIntentoDePago(reserva);
            return new ReservaCreadaResponse(reserva.getId(), reserva.getSlug(), clientSecret);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en Stripe: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Reserva anularReservaPorFalloPago(String usuarioEmail, Long reservaId) {
        Persona pasajero = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return reserva;
        }

        Pago pago = reserva.getPago();
        if (pago != null && pago.getStripePaymentIntentId() != null) {
            try {
                pagoService.cancelarPago(pago.getStripePaymentIntentId());
            } catch (StripeException e) {
                throw new RuntimeException("Error al cancelar el pago fallido", e);
            }
        }

        Viaje viaje = reserva.getViaje();
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + reserva.getCantidadPlazas());
        viajeRepository.save(viaje);

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva cancelarReserva(String usuarioEmail, Long reservaId) {
        Persona pasajero = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getPersona().getId().equals(pasajero.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario");
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return reserva;
        }

        Viaje viaje = reserva.getViaje();

        // Solo devolver plazas si la reserva se había pagado
        if (reserva.getEstado() == EstadoReserva.PAGADA) {
            int plazasADevolver = reserva.getCantidadPlazas();
            viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + plazasADevolver);
            viajeRepository.save(viaje);
        }

        String msj = pasajero.getNombre() + " ha cancelado su reserva en tu viaje.";
        notificacionRepository.save(new Notificacion(msj, viaje.getPersona(), TipoNotificacion.RESERVA_CANCELADA));

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

        reserva.setEstado(EstadoReserva.CANCELADA);
        
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva rechazarReservaComoConductor(String usuarioEmail, Long reservaId) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getViaje().getPersona().getId().equals(conductor.getId())) {
            throw new IllegalArgumentException("Solo el conductor del viaje puede rechazar esta reserva");
        }

        // 1. Validar estado: Solo se rechazan reservas PAGADAS (con el dinero retenido)
        if (reserva.getEstado() != EstadoReserva.PAGADA) {
            throw new IllegalStateException("Solo puedes rechazar reservas que están pendientes de tu confirmación.");
        }

        Viaje viaje = reserva.getViaje();
        
        // 2. Devolver las plazas al viaje (porque se restaron al pasar a PAGADA)
        viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() + reserva.getCantidadPlazas());
        viajeRepository.save(viaje);

        // 3. Cancelar la retención en Stripe (Libera el dinero de la tarjeta)
        Pago pago = reserva.getPago();
        if (pago != null && pago.getStripePaymentIntentId() != null) {
            try {
                pagoService.cancelarPago(pago.getStripePaymentIntentId()); 
                pago.setEstado(EstadoPago.REEMBOLSADO);
            } catch (StripeException e) {
                throw new RuntimeException("Error al cancelar la retención de pago en Stripe", e);
            }
        }

        // 4. Notificar al pasajero
        notificacionRepository.save(new Notificacion(
                "El conductor ha rechazado tu reserva en el viaje " + viaje.getSlug() + ".",
                reserva.getPersona(),
                TipoNotificacion.RESERVA_RECHAZADA
        ));

        // 5. Actualizar estado final
        reserva.setEstado(EstadoReserva.RECHAZADA);
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
    @Transactional
    public Reserva reservaConfirmada(String conductorEmail, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada")); 
            
        if (!reserva.getViaje().getPersona().getEmail().equals(conductorEmail)) {
            throw new IllegalArgumentException("No tienes permiso para confirmar esta reserva");
        }

        // NUEVO: Validar que la reserva ya haya sido pagada por el pasajero
        if (reserva.getEstado() != EstadoReserva.PAGADA) {
            throw new IllegalStateException("Solo puedes confirmar reservas que ya han sido pagadas por el pasajero.");
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);

        String mensaje = "El conductor ha confirmado tu reserva en el viaje " + reserva.getViaje().getSlug() + ".";
        if (reserva.getViaje().getCheckin() != null) {
            mensaje += " Código de checkin: " + reserva.getViaje().getCheckin() + ".";
        }
        notificacionRepository.save(new Notificacion(
            mensaje,
            reserva.getPersona(),
            TipoNotificacion.RESERVA_ACEPTADA
        ));
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
    @Transactional
    public ReservaCreadaResponse crearReservasRecurrentes(String usuarioEmail, 
                                                        List<Long> viajeRecurrenteIds, 
                                                        Integer plazasSolicitadas, 
                                                        Long paradaSubidaId, 
                                                        Long paradaBajadaId) {

        if (viajeRecurrenteIds == null || viajeRecurrenteIds.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un viaje recurrente.");
        }

        if (plazasSolicitadas == null || plazasSolicitadas < 1) {
            throw new IllegalArgumentException("Debes reservar al menos 1 plaza.");
        }

        Persona pasajero = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ViajeRecurrente> viajesRecurrentes = viajeRecurrenteRepository.findAllById(viajeRecurrenteIds);

        if (viajesRecurrentes.size() != viajeRecurrenteIds.size()) {
            throw new IllegalArgumentException("Alguno de los viajes recurrentes seleccionados no existe.");
        }

        BigDecimal totalAcumulado = BigDecimal.ZERO;
        List<Reserva> reservasCreadas = new ArrayList<>();
        Persona conductor = viajesRecurrentes.get(0).getPersona();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder detalleFechas = new StringBuilder();

        for (ViajeRecurrente vr : viajesRecurrentes) {
            // Validaciones por cada fecha seleccionada
            if (vr.getEstado() != EstadoViaje.PENDIENTE) {
                throw new IllegalArgumentException("El viaje del " + vr.getFechaHoraSalida().format(formatter) + " no está disponible.");
            }

            if (vr.getFechaHoraSalida() != null && vr.getFechaHoraSalida().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("El viaje del " + vr.getFechaHoraSalida().format(formatter) + " ya ha transcurrido.");
            }

            if (vr.getPlazasDisponibles() < plazasSolicitadas) {
                throw new IllegalArgumentException("No hay suficiente sitio para la fecha " + vr.getFechaHoraSalida().format(formatter));
            }

            if (vr.getPersona().getId().equals(pasajero.getId())) {
                throw new IllegalArgumentException("No puedes reservar tu propio viaje.");
            }

            boolean yaTieneReserva = reservaRepository.existsByPersonaIdAndViajeRecurrenteIdAndEstadoNot(
                    pasajero.getId(), vr.getId(), EstadoReserva.CANCELADA
            );
            
            if (yaTieneReserva) {
                throw new IllegalArgumentException("Ya tienes una reserva activa en la fecha " + vr.getFechaHoraSalida().format(formatter));
            }

            Parada paradaSubida = paradaRepository.findById(paradaSubidaId)
                    .orElseThrow(() -> new IllegalArgumentException("Parada de subida no encontrada"));
            Parada paradaBajada = paradaRepository.findById(paradaBajadaId)
                    .orElseThrow(() -> new IllegalArgumentException("Parada de bajada no encontrada"));

            // Crear la reserva para esta fecha concreta
            Reserva reserva = new Reserva(
                    EstadoReserva.PENDIENTE,
                    LocalDateTime.now(),
                    pasajero,
                    paradaSubida,
                    paradaBajada,
                    vr, // Si ViajeRecurrente hereda de Viaje, si no usar vr.getViajePadre() o setViajeRecurrente
                    plazasSolicitadas
            );
            reserva.setSlug("reserva-rec-tmp-" + System.currentTimeMillis() + "-" + vr.getId());
            reserva = reservaRepository.saveAndFlush(reserva);
            reserva.setSlug("reserva-" + reserva.getId());
            reserva = reservaRepository.saveAndFlush(reserva);

            reservasCreadas.add(reserva);

            // Sumar importe: precio de la instancia * plazas
            BigDecimal subtotal = vr.getPrecio().multiply(new BigDecimal(plazasSolicitadas));
            totalAcumulado = totalAcumulado.add(subtotal);

            // Formatear texto para la notificación agrupada
            detalleFechas.append("\n• ").append(vr.getFechaHoraSalida().format(formatter));
        }

        // Usar la primera reserva como principal para la pasarela Stripe
        Reserva reservaPrincipal = reservasCreadas.get(0);

        // Crear 1 único pago acumulado para Stripe
        Pago pago = new Pago();
        pago.setImporteTotal(totalAcumulado);
        BigDecimal comision = totalAcumulado.multiply(new BigDecimal("0.10"));
        pago.setComision(comision);
        pago.setImporteConductor(totalAcumulado.subtract(comision));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setReserva(reservaPrincipal);

        pago = pagoRepository.saveAndFlush(pago);
        reservaPrincipal.setPago(pago);

        // GUARDAR 1 SOLA NOTIFICACIÓN AGRUPADA PARA EL CONDUCTOR
        String msjNotificación = String.format(
            "El usuario %s ha solicitado reservar %d fechas para tu viaje recurrente:%s\nImporte total: %.2f €",
            pasajero.getNombre(),
            viajesRecurrentes.size(),
            detalleFechas.toString(),
            totalAcumulado
        );

        notificacionRepository.save(new Notificacion(
            msjNotificación,
            conductor,
            TipoNotificacion.NUEVA_RESERVA
        ));

        pago = pagoRepository.saveAndFlush(pago);

        // Asignar el pago a TODAS las reservas del paquete recurrente
        for (Reserva r : reservasCreadas) {
            r.setPago(pago);
            reservaRepository.save(r);
        }

        try {
            String clientSecret = pagoService.crearIntentoDePago(reservaPrincipal);
            return new ReservaCreadaResponse(reservaPrincipal.getId(), reservaPrincipal.getSlug(), clientSecret);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en Stripe: " + e.getMessage());
        }
    }

    @Override
    public List<ViajeRecurrenteDTO> obtenerRecurrentesPorViajePadre(Long viajePadreId) {
        List<ViajeRecurrente> recurrentes = viajeRecurrenteRepository.findByViajePadreId(viajePadreId);
        return recurrentes.stream()
                .map(viajeRecurrenteService::mapearADTO)
                .toList();
    }

    @Override
    @Transactional
    public void cancelarOcurrenciaPorConductor(Long viajeRecurrenteId, String conductorEmail) throws StripeException {
        ViajeRecurrente vr = viajeRecurrenteRepository.findById(viajeRecurrenteId)
                .orElseThrow(() -> new EntityNotFoundException("Viaje recurrente no encontrado"));

        // 1. Validar que la persona que cancela sea el conductor de dicho viaje
        if (!vr.getPersona().getEmail().equals(conductorEmail)) {
            throw new IllegalArgumentException("Solo el conductor del viaje puede realizar esta cancelación.");
        }

        // 2. Marcar la ocurrencia del viaje como CANCELADO (opcional si usas EstadoViaje)
        vr.setEstado(EstadoViaje.CANCELADO);
        viajeRecurrenteRepository.save(vr);

        // 3. Buscar TODAS las reservas activas de los distintos pasajeros en este viaje
        List<Reserva> reservasAfectadas = reservaRepository.findByViajeRecurrenteIdAndEstadoNot(
                viajeRecurrenteId, EstadoReserva.CANCELADA
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = vr.getFechaHoraSalida().format(formatter);

        // 4. Procesar el reembolso e independizar la lógica para CADA PASAJERO
        for (Reserva reserva : reservasAfectadas) {
            
            // Marcar la reserva individual como CANCELADA
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);

            Pago pagoPasajero = reserva.getPago();

            if (pagoPasajero != null && pagoPasajero.getStripePaymentIntentId() != null) {
                
                // Importe a devolver a ESTE pasajero = precioUnitario * plazasReservadasPorEl
                BigDecimal importeDevolucionPasajero = vr.getPrecio()
                        .multiply(BigDecimal.valueOf(reserva.getCantidadPlazas()));

                // Comprobar si al pasajero le quedan OTRAS reservas activas vinculadas a este mismo pago
                List<Reserva> reservasRestantesPasajero = reservaRepository.findByPagoIdAndEstadoNot(
                        pagoPasajero.getId(), EstadoReserva.CANCELADA
                );

                if (reservasRestantesPasajero.isEmpty()) {
                    // Caso A: Era la única fecha que tenía reservada el pasajero -> Reembolsar/Liberar el 100% de su Pago
                    stripeService.liberarFondos(pagoPasajero.getStripePaymentIntentId());
                    pagoPasajero.setEstado(EstadoPago.REEMBOLSADO);
                    pagoPasajero.setImporteTotal(BigDecimal.ZERO);
                    pagoPasajero.setComision(BigDecimal.ZERO);
                    pagoPasajero.setImporteConductor(BigDecimal.ZERO);
                } else {
                    // Caso B: El pasajero reservó varias fechas en paquete -> Reembolso Parcial de esta fecha
                    BigDecimal nuevoTotalPago = pagoPasajero.getImporteTotal().subtract(importeDevolucionPasajero);
                    
                    if (nuevoTotalPago.compareTo(BigDecimal.ZERO) < 0) {
                        nuevoTotalPago = BigDecimal.ZERO;
                    }

                    pagoPasajero.setImporteTotal(nuevoTotalPago);
                    BigDecimal nuevaComision = nuevoTotalPago.multiply(new BigDecimal("0.10"));
                    pagoPasajero.setComision(nuevaComision);
                    pagoPasajero.setImporteConductor(nuevoTotalPago.subtract(nuevaComision));

                    // Si Stripe ya había cobrado el dinero (succeeded), hacemos el reembolso parcial
                    if (pagoPasajero.getEstado() == EstadoPago.CAPTURADO) {
                        stripeService.reembolsarParcial(
                            pagoPasajero.getStripePaymentIntentId(), 
                            importeDevolucionPasajero
                        );
                    }
                }
                
                pagoRepository.save(pagoPasajero);
            }

            // 5. Notificar de manera individual a cada pasajero afectado
            notificacionRepository.save(new Notificacion(
                    String.format("El conductor ha cancelado el viaje del %s. Se ha procesado el reembolso de tus %d plaza(s).",
                            fechaFormateada, reserva.getCantidadPlazas()),
                    reserva.getPersona(),
                    TipoNotificacion.RESERVA_CANCELADA
            ));
        }
    }

}