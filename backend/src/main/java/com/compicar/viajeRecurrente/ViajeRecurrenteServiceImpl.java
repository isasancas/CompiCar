package com.compicar.viajeRecurrente;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.StripeService;
import com.compicar.parada.Parada;
import com.compicar.parada.dto.ParadaDTO;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.vehiculo.dto.VehiculoDTO;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;
import com.stripe.exception.StripeException;

@Service
@Transactional
public class ViajeRecurrenteServiceImpl implements ViajeRecurrenteService {

    private final ViajeRecurrenteRepository viajeRecurrenteRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final NotificacionRepository notificacionRepository;
    private final StripeService stripeService;

    public ViajeRecurrenteServiceImpl(ViajeRecurrenteRepository viajeRecurrenteRepository, PersonaRepository personaRepository, 
        ReservaRepository reservaRepository, PagoRepository pagoRepository, NotificacionRepository notificacionRepository, StripeService stripeService) {
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.notificacionRepository = notificacionRepository;
        this.stripeService = stripeService;
    }

    @Override
    public ViajeRecurrenteDTO mapearADTO(ViajeRecurrente vr) {
        if (vr == null) return null;

        VehiculoDTO vehiculoDTO = vr.getVehiculo() != null ? new VehiculoDTO(
            vr.getVehiculo().getId(),
            vr.getVehiculo().getMarca(),
            vr.getVehiculo().getModelo(),
            vr.getVehiculo().getMatricula()
        ) : null;

        List<ParadaDTO> paradasDTO = vr.getParadas() != null ? vr.getParadas().stream()
            .map(parada -> new ParadaDTO(
                parada.getId(),
                parada.getLocalizacion(),
                parada.getTipo().toString(),
                parada.getOrden()
            ))
            .toList() : List.of();

        List<ReservaDTO> reservasDTO = vr.getReservas() != null ? vr.getReservas().stream()
            .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
            .map(r -> new ReservaDTO(
                r.getId(),
                r.getEstado().toString(),
                r.getFechaHoraReserva(),
                r.getViaje() != null ? r.getViaje().getId() : null,
                r.getPersona().getId(),
                r.getPersona().getNombre(),
                r.getPersona().getSlug(),
                r.getParadaSubida() != null ? r.getParadaSubida().getId() : null,
                r.getParadaBajada() != null ? r.getParadaBajada().getId() : null,
                r.getCantidadPlazas()
            )).toList() : List.of();

        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        dto.setId(vr.getId());
        dto.setSlug(vr.getSlug());
        dto.setCheckin(vr.getCheckin());
        dto.setFechaHoraSalida(vr.getFechaHoraSalida());
        dto.setFechaHoraFin(vr.getFechaHoraFin());
        dto.setEstado(vr.getEstado() != null ? vr.getEstado().toString() : null);
        dto.setPlazasDisponibles(vr.getPlazasDisponibles());
        dto.setPrecio(vr.getPrecio());
        dto.setViajePadreId(vr.getViajePadre() != null ? vr.getViajePadre().getId() : null);

        if (vr.getPersona() != null) {
            dto.setConductorId(vr.getPersona().getId());
            dto.setConductorNombre(vr.getPersona().getNombre());
            dto.setConductorSlug(vr.getPersona().getSlug());
        }

        dto.setVehiculo(vehiculoDTO);
        dto.setParadas(paradasDTO);
        dto.setReservas(reservasDTO);

        return dto;
    }

    @Override
    public List<ViajeRecurrente> generarOcurrencias(Viaje viajePadre) {
        if (viajePadre.getDiasSemana() == null || viajePadre.getDiasSemana().isEmpty() || viajePadre.getFechaFinRecurrencia() == null) {
            return List.of();
        }

        Set<DayOfWeek> diasDeseados = viajePadre.getDiasSemana().stream()
            .map(this::mapearDiaSemana)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (diasDeseados.isEmpty()) {
            return List.of();
        }

        LocalDate fechaInicio = viajePadre.getFechaHoraSalida().toLocalDate().plusDays(1);
        
        LocalDate fechaFin = viajePadre.getFechaFinRecurrencia().toLocalDate();

        List<ViajeRecurrente> ocurrencias = new ArrayList<>();

        for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
            if (diasDeseados.contains(fecha.getDayOfWeek())) {
                LocalDateTime fechaSalidaOcurrencia = LocalDateTime.of(fecha, viajePadre.getFechaHoraSalida().toLocalTime());

                ViajeRecurrente vr = new ViajeRecurrente();
                vr.setViajePadre(viajePadre);
                vr.setFechaHoraSalida(fechaSalidaOcurrencia);
                vr.setEstado(EstadoViaje.PENDIENTE);
                vr.setPlazasDisponibles(viajePadre.getPlazasDisponibles());
                vr.setPrecio(viajePadre.getPrecio());
                vr.setPersona(viajePadre.getPersona());
                vr.setVehiculo(viajePadre.getVehiculo());
                vr.setCheckin(generarCheckin());
                vr.setSlug(generarSlugUnico(viajePadre.getSlug() + "-" + fecha.toString()));

                // Clonar paradas con ajuste temporal
                if (viajePadre.getParadas() != null) {
                    for (Parada pPadre : viajePadre.getParadas()) {
                        Parada pNueva = new Parada();
                        pNueva.setLocalizacion(pPadre.getLocalizacion());
                        pNueva.setTipo(pPadre.getTipo());
                        pNueva.setOrden(pPadre.getOrden());
                        pNueva.setLatitud(pPadre.getLatitud());
                        pNueva.setLongitud(pPadre.getLongitud());

                        if (pPadre.getFechaHora() != null && viajePadre.getFechaHoraSalida() != null) {
                            Duration offset = Duration.between(viajePadre.getFechaHoraSalida(), pPadre.getFechaHora());
                            pNueva.setFechaHora(fechaSalidaOcurrencia.plus(offset));
                        } else {
                            pNueva.setFechaHora(fechaSalidaOcurrencia);
                        }

                        vr.addParada(pNueva);
                    }
                }

                ocurrencias.add(vr);
            }
        }

        return viajeRecurrenteRepository.saveAll(ocurrencias);
    }

    /**
     * FINALIZAR VIAJE RECURRENTE:
     * Captura en Stripe el pago total (si es la primera fecha del paquete en finalizar),
     * libera los fondos proporcionales de esta fecha al conductor y actualiza estados.
     */
    @Override
    @Transactional
    public ViajeRecurrenteDTO finalizarViajeRecurrente(String usuarioEmail, String slug) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        ViajeRecurrente viajeRecurrente = viajeRecurrenteRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje recurrente no encontrado"));

        if (!viajeRecurrente.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede finalizar este viaje");
        }

        if (viajeRecurrente.getEstado() == EstadoViaje.CANCELADO || viajeRecurrente.getEstado() == EstadoViaje.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede finalizar un viaje en estado " + viajeRecurrente.getEstado());
        }

        BigDecimal totalGanadoEnEsteViaje = BigDecimal.ZERO;
        List<Reserva> reservasActivas = reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA);

        for (Reserva reserva : reservasActivas) {
            Pago pago = reserva.getPago();

            if (pago != null && pago.getStripePaymentIntentId() != null) {
                
                // 1. Capturar el importe en Stripe si es la primera fecha del paquete que finaliza
                if (pago.getEstado() != EstadoPago.CAPTURADO) {
                    try {
                        stripeService.confirmarCaptura(pago.getStripePaymentIntentId());
                        pago.setEstado(EstadoPago.CAPTURADO);
                    } catch (StripeException e) {
                        throw new ResponseStatusException(
                            HttpStatus.PAYMENT_REQUIRED, 
                            "Error al capturar el pago en Stripe para la reserva #" + reserva.getId() + ": " + e.getMessage()
                        );
                    }
                }

                // 2. Ganancia exclusiva de ESTA fecha concreta (precio * plazas)
                BigDecimal gananciaEstaReserva = viajeRecurrente.getPrecio().multiply(BigDecimal.valueOf(reserva.getCantidadPlazas()));

                // 3. Acumular en la entidad Pago lo que ya se ha liberado al conductor
                BigDecimal liberadoPrevio = pago.getImporteLiberadoConductor() != null 
                        ? pago.getImporteLiberadoConductor() 
                        : BigDecimal.ZERO;
                        
                pago.setImporteLiberadoConductor(liberadoPrevio.add(gananciaEstaReserva));
                pagoRepository.save(pago);

                // 4. Sumar al total que se le abonará al conductor en esta fecha
                totalGanadoEnEsteViaje = totalGanadoEnEsteViaje.add(gananciaEstaReserva);
            }

            // 5. Notificar al pasajero
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fechaSalida = viajeRecurrente.getFechaHoraSalida() != null 
                    ? viajeRecurrente.getFechaHoraSalida().format(formatter) 
                    : "";

            String msj = "El viaje del " + fechaSalida + " ha finalizado. ¡Gracias por viajar!";
            Notificacion noti = new Notificacion(msj, reserva.getPersona(), TipoNotificacion.VIAJE_MODIFICADO);
            notificacionRepository.save(noti);
        }

        // 6. Cambiar estado del viaje recurrente
        viajeRecurrente.setEstado(EstadoViaje.FINALIZADO);
        viajeRecurrenteRepository.save(viajeRecurrente);

        // 7. Liberar saldo al conductor solo por la ganancia correspondiente a este día
        BigDecimal actuales = conductor.getFondosActuales() != null ? conductor.getFondosActuales() : BigDecimal.ZERO;
        BigDecimal totales = conductor.getFondosTotales() != null ? conductor.getFondosTotales() : BigDecimal.ZERO;

        conductor.setFondosActuales(actuales.add(totalGanadoEnEsteViaje));
        conductor.setFondosTotales(totales.add(totalGanadoEnEsteViaje));
        personaRepository.save(conductor);

        return mapearADTO(viajeRecurrente);
    }

    @Override
    @Transactional
    public ViajeRecurrenteDTO cancelarViajeRecurrente(String usuarioEmail, String slug) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        ViajeRecurrente viajeRecurrente = viajeRecurrenteRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje recurrente no encontrado"));

        if (!viajeRecurrente.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede cancelar este viaje");
        }

        if (viajeRecurrente.getEstado() == EstadoViaje.CANCELADO || viajeRecurrente.getEstado() == EstadoViaje.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cancelar un viaje en estado " + viajeRecurrente.getEstado());
        }

        List<Reserva> reservasActivas = reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA);
        boolean teniaReservasActivas = !reservasActivas.isEmpty();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaSalida = viajeRecurrente.getFechaHoraSalida() != null 
                ? viajeRecurrente.getFechaHoraSalida().format(formatter) 
                : "";

        for (Reserva reserva : reservasActivas) {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);

            Pago pago = reserva.getPago();
            if (pago != null && pago.getStripePaymentIntentId() != null) {
                BigDecimal importeADevolver = viajeRecurrente.getPrecio().multiply(BigDecimal.valueOf(reserva.getCantidadPlazas()));

                // Comprobar si al pasajero le quedan OTRAS fechas activas bajo este mismo pago
                List<Reserva> reservasRestantes = reservaRepository.findByPagoIdAndEstadoNot(
                        pago.getId(), EstadoReserva.CANCELADA
                );

                try {
                    if (reservasRestantes.isEmpty()) {
                        // Si era la única fecha o no quedan más activas -> Reembolso / Liberación total
                        stripeService.liberarFondos(pago.getStripePaymentIntentId());
                        pago.setEstado(EstadoPago.REEMBOLSADO);
                        pago.setImporteTotal(BigDecimal.ZERO);
                        pago.setComision(BigDecimal.ZERO);
                        pago.setImporteConductor(BigDecimal.ZERO);
                    } else {
                        // Si el paquete sigue activo en otras fechas -> Reembolso / Ajuste parcial
                        BigDecimal nuevoTotal = pago.getImporteTotal().subtract(importeADevolver);
                        if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) {
                            nuevoTotal = BigDecimal.ZERO;
                        }

                        pago.setImporteTotal(nuevoTotal);
                        BigDecimal nuevaComision = nuevoTotal.multiply(new BigDecimal("0.10"));
                        pago.setComision(nuevaComision);
                        pago.setImporteConductor(nuevoTotal.subtract(nuevaComision));

                        if (pago.getEstado() == EstadoPago.CAPTURADO) {
                            stripeService.reembolsarParcial(pago.getStripePaymentIntentId(), importeADevolver);
                        }
                    }
                    pagoRepository.save(pago);
                } catch (StripeException e) {
                    throw new RuntimeException("Error al procesar el reembolso en Stripe: " + e.getMessage(), e);
                }
            }

            // Notificar al pasajero
            String msj = "El viaje del " + fechaSalida + " ha sido cancelado por el conductor.";
            Notificacion noti = new Notificacion(msj, reserva.getPersona(), TipoNotificacion.VIAJE_CANCELADO);
            notificacionRepository.save(noti);
        }

        viajeRecurrente.setEstado(EstadoViaje.CANCELADO);
        viajeRecurrenteRepository.save(viajeRecurrente);

        if (teniaReservasActivas) {
            conductor.incrementarCancelaciones();
            personaRepository.save(conductor);
        }

        return mapearADTO(viajeRecurrente);
    }

    private DayOfWeek mapearDiaSemana(String dia) {
        if (dia == null) return null;
        
        return switch (dia.trim().toUpperCase()) {
            case "L", "LUNES", "MONDAY" -> DayOfWeek.MONDAY;
            case "M", "MARTES", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "X", "MIERCOLES", "MIÉRCOLES", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "J", "JUEVES", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "V", "VIERNES", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "S", "SABADO", "SÁBADO", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "D", "DOMINGO", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private String generarCheckin() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (viajeRecurrenteRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
    
}
