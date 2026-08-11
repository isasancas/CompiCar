package com.compicar.viaje;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.config.SlugUtils;
import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.StripeService;
import com.compicar.parada.Parada;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import com.compicar.viaje.dto.VehiculoDTO;
import com.compicar.viaje.dto.ParadaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;

@Service
@Transactional
public class ViajeServiceImpl implements ViajeService {

    private final ViajeRepository viajeRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final CalculoPrecioIA calculoPrecioIA;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final NotificacionRepository notificacionRepository;
    private final StripeService stripeService;

    private static final long HORAS_LIMITE_CANCELACION = 12L;

    @Value("${pricing.fallback.fuel-price-eur-per-liter:1.65}")
    private BigDecimal fallbackFuelPrice;

    public ViajeServiceImpl(ViajeRepository viajeRepository, PersonaRepository personaRepository,
            VehiculoRepository vehiculoRepository, CalculoPrecioIA calculoPrecioIA,
            ReservaRepository reservaRepository, PagoRepository pagoRepository, 
            NotificacionRepository notificacionRepository, StripeService stripeService) {
        this.viajeRepository = viajeRepository;
        this.personaRepository = personaRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.calculoPrecioIA = calculoPrecioIA;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.notificacionRepository = notificacionRepository;
        this.stripeService = stripeService;
    }

    public boolean tieneReservasActivas(Viaje viaje) {
        return viaje.getReservas() != null && viaje.getReservas().stream()
            .anyMatch(reserva -> reserva.getEstado() == EstadoReserva.CONFIRMADA || reserva.getEstado() == EstadoReserva.PAGADA);
    }

    @Override
    public Viaje crearViaje(String usuarioEmail, Viaje viaje) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (viaje.getVehiculo() == null || viaje.getVehiculo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje debe incluir un vehículo válido");
        }

        if (viaje.getParadas() == null || viaje.getParadas().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar al menos origen y destino");
        }

        Vehiculo vehiculo = vehiculoRepository.findById(viaje.getVehiculo().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo no existe"));

        if (vehiculo.getPersona() == null || !vehiculo.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehículo no pertenece al usuario autenticado");
        }

        viaje.setPersona(conductor);
        viaje.setVehiculo(vehiculo);

        // Generar checkin aleatorio de 6 caracteres alfanuméricos
        viaje.setCheckin(generarCheckin());

        if (viaje.getParadas() != null) {
            for (int i = 0; i < viaje.getParadas().size(); i++) {
                Parada parada = viaje.getParadas().get(i);
                parada.setViaje(viaje);

                if (parada.getFechaHora() == null) {
                    parada.setFechaHora(viaje.getFechaHoraSalida());
                }

                if (parada.getOrden() == null) {
                    parada.setOrden(i + 1);
                }
            }
        }

        validarParadas(viaje);
        String baseSlug = construirBaseSlug(viaje);
        viaje.setSlug(generarSlugUnico(baseSlug));
        return viajeRepository.save(viaje);
    }

    @Override
    public PrecioTrayectoResponseDTO calcularPrecioTrayecto(String usuarioEmail, CalcularPrecioTrayectoRequestDTO request) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo no existe"));

        if (vehiculo.getPersona() == null || !vehiculo.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehículo no pertenece al usuario autenticado");
        }

        BigDecimal distanciaKm = BigDecimal.valueOf(request.getDistanciaKm());
        BigDecimal consumoL100 = BigDecimal.valueOf(vehiculo.getConsumo());

        BigDecimal litrosEstimados = consumoL100
            .multiply(distanciaKm)
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal precioLitro = obtenerPrecioLitroConGemini(vehiculo);
        String fuente = "GEMINI";
        String detalle = "Estimacion con Gemini";

        if (precioLitro == null) {
            precioLitro = fallbackFuelPrice;
            fuente = "FALLBACK";
            detalle = "Gemini no disponible, se usa precio fallback";
        }

        // 1. Coste de combustible
        BigDecimal costeCombustible = litrosEstimados.multiply(precioLitro).setScale(4, RoundingMode.HALF_UP);

        // 2. Coste de desgaste
        BigDecimal costeDesgaste = obtenerCosteDesgasteConGemini(vehiculo, distanciaKm);
        
        // 3. Coste operativo base: combustible + desgaste
        BigDecimal costeOperativoBase = costeCombustible.add(costeDesgaste);
        
        // 4. Aplicar nuestra comisión del 20%
        BigDecimal comisionPlataforma = costeOperativoBase.multiply(BigDecimal.valueOf(0.20));
        BigDecimal subtotalConComision = costeOperativoBase.add(comisionPlataforma);

        // 5. Aplicar comisión de Stripe (Ejemplo: 1.5% + 0.25€)
        // Formula inversa o directa aproximada para cubrir pasarela: (Subtotal + 0.25) / (1 - 0.015)
        BigDecimal porcentajeStripe = BigDecimal.valueOf(0.015);
        BigDecimal fijoStripe = BigDecimal.valueOf(0.25);
        BigDecimal precioFinalConStripe = subtotalConComision.add(fijoStripe)
            .divide(BigDecimal.ONE.subtract(porcentajeStripe), 2, RoundingMode.HALF_UP);

        // Márgenes orientativos para el pasajero basados en el precio final calculado
        BigDecimal precioMin = precioFinalConStripe.multiply(BigDecimal.valueOf(0.90)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioMax = precioFinalConStripe.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP);

        PrecioTrayectoResponseDTO response = new PrecioTrayectoResponseDTO();
        response.setLitrosEstimados(litrosEstimados.setScale(2, RoundingMode.HALF_UP));
        response.setPrecioCombustibleLitro(precioLitro.setScale(3, RoundingMode.HALF_UP));
        response.setCosteTotalCombustible(costeCombustible.setScale(2, RoundingMode.HALF_UP));
        response.setPrecioMinimoPasajero(precioMin);
        response.setPrecioMaximoPasajero(precioMax);
        response.setFuente(fuente);
        response.setDetalle(detalle);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeDTO obtenerViajePorSlug(String slug) {
        Viaje viaje = viajeRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        return convertirADTO(viaje);
    }

        @Override
    public List<ViajeDTO> obtenerMisViajes(String email) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Viaje> viajes = viajeRepository.findByPersonaId(persona.getId());
        return viajes.stream().map(this::convertirADTO).toList();
    }

    @Override
    public List<ViajeDTO> obtenerViajesParticipados(String email) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Viaje> viajes = viajeRepository.findViajesParticipadosByPersonaId(persona.getId());
        return viajes.stream().map(this::convertirADTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeDTO> buscarViajesPublicos(String origen, String destino, LocalDate fecha) {
        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;

        Set<EstadoViaje> estadosPublicos = Set.of(EstadoViaje.PENDIENTE, EstadoViaje.INICIADO);

        List<Viaje> base = (inicio != null && fin != null)
            ? viajeRepository.buscarViajesPublicosConFecha(estadosPublicos, inicio, fin)
            : viajeRepository.buscarViajesPublicosSinFecha(estadosPublicos);

        String origenNorm = normalizar(origen);
        String destinoNorm = normalizar(destino);

        return base.stream()
            .filter(v -> coincideEnParadas(v, origenNorm, destinoNorm))
            .map(this::convertirADTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeDTO> obtenerViajesPublicosPorConductor(String conductorSlug) {
        if (conductorSlug == null || conductorSlug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug de conductor invalido");
        }

        List<Viaje> viajes = viajeRepository.findByPersonaSlugOrderByFechaHoraSalidaDesc(conductorSlug);
        return viajes.stream().map(this::convertirADTO).toList();
    }

    @Override
    @Transactional
    public ViajeDTO cancelarViaje(String usuarioEmail, String slug) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Viaje viaje = viajeRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (!viaje.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede cancelar este viaje");
        }

        if (viaje.getEstado() == EstadoViaje.CANCELADO || viaje.getEstado() == EstadoViaje.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cancelar un viaje en estado " + viaje.getEstado());
        }

        boolean tieneReservasActivas = viaje.getReservas() != null && viaje.getReservas().stream()
            .anyMatch(reserva -> reserva.getEstado() == EstadoReserva.CONFIRMADA || reserva.getEstado() == EstadoReserva.PAGADA);

        boolean penalizaConductor = tieneReservasActivas;

        cancelarReservasYReembolsar(viaje, true);

        viaje.setEstado(EstadoViaje.CANCELADO);
        viajeRepository.save(viaje);

        if (penalizaConductor) {
            conductor.incrementarCancelaciones();
            personaRepository.save(conductor);
        }

        return convertirADTO(viaje);
    }

    @Override
    @Transactional
    public int cancelarViajesPendientesExpirados() {
        LocalDateTime limite = LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusHours(1);

        List<Viaje> viajesExpirados = viajeRepository.findByEstadoAndFechaHoraSalidaBefore(EstadoViaje.PENDIENTE, limite);

        for (Viaje viaje : viajesExpirados) {

            boolean teniaReservasActivas = viaje.getReservas() != null && viaje.getReservas().stream()
                .anyMatch(reserva -> reserva.getEstado() == EstadoReserva.CONFIRMADA || reserva.getEstado() == EstadoReserva.PAGADA);

            cancelarReservasYReembolsar(viaje, true);

            viaje.setEstado(EstadoViaje.CANCELADO);
            viajeRepository.save(viaje);

            if (teniaReservasActivas) {
                Persona conductor = viaje.getPersona();
                conductor.incrementarCancelaciones();
                personaRepository.save(conductor);
            }
        }

        return viajesExpirados.size();
    }

    @Override
    public ViajeDTO actualizarViaje(String usuarioEmail, String slug, Viaje viajeEditado) {
        // 1. Validaciones de Identidad
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Viaje viajeExistente = viajeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (!viajeExistente.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede editar el viaje");
        }

        // 2. Validación de margen de tiempo (12 horas)
        if (LocalDateTime.now().isAfter(viajeExistente.getFechaHoraSalida().minusHours(12))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No se puede editar el viaje a falta de menos de 12 horas para la salida");
        }

        // 3. Actualización de Fecha (Solo si viene en el body)
        if (viajeEditado.getFechaHoraSalida() != null) {
            viajeExistente.setFechaHoraSalida(viajeEditado.getFechaHoraSalida());
        }

        // 4. Actualización de Precio (Protección contra NULL)
        // Solo actualizamos si el valor enviado no es nulo y es mayor que 0
        if (viajeEditado.getPrecio() != null && viajeEditado.getPrecio().compareTo(BigDecimal.ZERO) > 0) {
            viajeExistente.setPrecio(viajeEditado.getPrecio());
        }

        // 5. Lógica de Plazas Disponibles
        if (viajeEditado.getPlazasDisponibles() != null) {
            // Contamos plazas ocupadas actualmente
            int plazasOcupadas = reservaRepository.findByViajeAndEstadoNot(viajeExistente, EstadoReserva.CANCELADA)
                    .stream()
                    .mapToInt(Reserva::getCantidadPlazas)
                    .sum();

            // Interpretamos el valor del Front como "Capacidad Total"
            int nuevoTotal = viajeEditado.getPlazasDisponibles();

            if (nuevoTotal < plazasOcupadas) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "El total de plazas no puede ser inferior a las plazas ya reservadas: " + plazasOcupadas);
            }

            // Seteamos la disponibilidad real (Total - Ocupadas)
            viajeExistente.setPlazasDisponibles(nuevoTotal - plazasOcupadas);
        }

        // 6. Guardado y Notificaciones
        validarParadas(viajeExistente);
        Viaje guardado = viajeRepository.save(viajeExistente);
        
        List<Reserva> reservasActivas = reservaRepository.findByViajeAndEstadoNot(guardado, EstadoReserva.CANCELADA);

        for (Reserva r : reservasActivas) {
            String msj = "Se han modificado los detalles del viaje " + guardado.getSlug() + ". Revisa el nuevo horario o número de plazas disponibles.";
            
            Notificacion noti = new Notificacion(
                msj, 
                r.getPersona(), 
                TipoNotificacion.VIAJE_MODIFICADO
            );
            notificacionRepository.save(noti);
        }

        return convertirADTO(guardado);
    }

    /**
     * FINALIZAR VIAJE:
     * Captura los pagos en Stripe de cada reserva activa, actualiza los estados
     * e incrementa los fondos del conductor (fondosActuales y fondosTotales).
     */
    @Override
    public ViajeDTO finalizarViaje(String usuarioEmail, String slug) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Viaje viaje = viajeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (!viaje.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede finalizar este viaje");
        }

        if (viaje.getEstado() == EstadoViaje.CANCELADO || viaje.getEstado() == EstadoViaje.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede finalizar un viaje en estado " + viaje.getEstado());
        }

        BigDecimal totalGanado = BigDecimal.ZERO;
        List<Reserva> reservasActivas = reservaRepository.findByViajeAndEstadoNot(viaje, EstadoReserva.CANCELADA);

        for (Reserva reserva : reservasActivas) {
            Pago pago = reserva.getPago();

            if (pago != null && pago.getStripePaymentIntentId() != null) {
                try {
                    // 1. Cobrar definitivamente en Stripe (descongelar)
                    stripeService.confirmarCaptura(pago.getStripePaymentIntentId());
                    
                    pago.setEstado(EstadoPago.CAPTURADO);
                    pagoRepository.save(pago);

                    if (pago.getImporteTotal() != null) {
                        totalGanado = totalGanado.add(pago.getImporteTotal());
                    }
                } catch (StripeException e) {
                    throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED, 
                        "Error al procesar la captura del pago en Stripe para la reserva #" + reserva.getId() + ": " + e.getMessage()
                    );
                }
            }

            // 2. Notificar al pasajero
            String msj = "El viaje a " + viaje.getSlug() + " ha finalizado. ¡Gracias por viajar!";
            Notificacion noti = new Notificacion(msj, reserva.getPersona(), TipoNotificacion.VIAJE_MODIFICADO); // Ajusta el TipoNotificacion si tienes uno específico
            notificacionRepository.save(noti);
        }

        // 3. Cambiar estado del viaje
        viaje.setEstado(EstadoViaje.FINALIZADO);
        viajeRepository.save(viaje);

        // 4. Sumar ganancias a las cuentas del conductor
        BigDecimal actuales = conductor.getFondosActuales() != null ? conductor.getFondosActuales() : BigDecimal.ZERO;
        BigDecimal totales = conductor.getFondosTotales() != null ? conductor.getFondosTotales() : BigDecimal.ZERO;

        conductor.setFondosActuales(actuales.add(totalGanado));
        conductor.setFondosTotales(totales.add(totalGanado));
        personaRepository.save(conductor);

        return convertirADTO(viaje);
    }

    /**
     * INICIAR VIAJE:
     * Verifica que sea el conductor y que haya llegado la fecha/hora de salida
     * para cambiar su estado a INICIADO.
     */
    @Override
    public ViajeDTO iniciarViaje(String usuarioEmail, String slug) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Viaje viaje = viajeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        // 1. Validar que la persona sea el conductor de este viaje
        if (!viaje.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede iniciar este viaje");
        }

        // 2. Validar estado actual del viaje: no permitir si ya está iniciado,en curso, finalizado o cancelado
        if (viaje.getEstado() == EstadoViaje.INICIADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje ya está iniciado");
        }

        if (viaje.getEstado() == EstadoViaje.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje ya está en curso");
        }

        if (viaje.getEstado() == EstadoViaje.CANCELADO || viaje.getEstado() == EstadoViaje.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede iniciar un viaje en estado " + viaje.getEstado());
        }

        // 3. Validar que la fecha y hora de salida hayan llegado
        if (LocalDateTime.now().isBefore(viaje.getFechaHoraSalida())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Aún no ha llegado la fecha u hora de salida programada"
            );
        }

        // 4. Cambiar estado a INICIADO. El check-in se valida en una segunda acción.
        viaje.setEstado(EstadoViaje.INICIADO);
        viajeRepository.save(viaje);

        return convertirADTO(viaje);
    }

    /**
     * CONFIRMAR CHECK-IN:
     * Solo permite pasar de INICIADO a EN_CURSO si el código es correcto.
     */
    @Override
    public ViajeDTO confirmarCheckin(String usuarioEmail, String slug, String checkin) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Viaje viaje = viajeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (!viaje.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor puede realizar el check-in de este viaje");
        }

        if (viaje.getEstado() != EstadoViaje.INICIADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje debe estar iniciado para realizar el check-in");
        }

        if (viaje.getCheckin() == null || checkin == null || !viaje.getCheckin().equalsIgnoreCase(checkin.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkin inválido");
        }

        viaje.setEstado(EstadoViaje.EN_CURSO);
        viajeRepository.save(viaje);

        return convertirADTO(viaje);
    }

    private String generarCheckin() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void cancelarReservasYReembolsar(Viaje viaje, boolean reembolsar) {
        List<Reserva> reservasActivas = reservaRepository.findByViajeAndEstadoNot(viaje, EstadoReserva.CANCELADA);

        for (Reserva reserva : reservasActivas) {
            if (reserva.getEstado() != EstadoReserva.NO_PRESENTADO) {
                reserva.setEstado(EstadoReserva.CANCELADA);
                reservaRepository.save(reserva);
            }

            Pago pago = reserva.getPago();
            if (pago != null && reembolsar) {
                if (pago.getStripePaymentIntentId() != null) {
                    try {
                        pago.setEstado(stripeService.liberarFondos(pago.getStripePaymentIntentId()));
                    } catch (StripeException e) {
                        throw new RuntimeException("Error al reembolsar el pago en Stripe", e);
                    }
                } else {
                    pago.setEstado(EstadoPago.REEMBOLSADO);
                }

                pagoRepository.save(pago);
            }
            String msj = "El viaje de " + viaje.getSlug() + " ha sido cancelado por el conductor.";
            Notificacion noti = new Notificacion(msj, reserva.getPersona(), TipoNotificacion.VIAJE_CANCELADO);
            notificacionRepository.save(noti);
        }
    }

    private BigDecimal obtenerPrecioLitroConGemini(Vehiculo vehiculo) {
        try {
            String prompt = construirPromptPrecio(vehiculo);
            String json = calculoPrecioIA.pedirEstimacionJson(prompt);

            JsonNode node = objectMapper.readTree(json);
            BigDecimal precio = node.path("precio_combustible_litro").decimalValue();

            if (precio.compareTo(BigDecimal.valueOf(0.8)) < 0 || precio.compareTo(BigDecimal.valueOf(3.5)) > 0) {
                return null;
            }
            return precio;
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal obtenerCosteDesgasteConGemini(Vehiculo vehiculo, BigDecimal distanciaKm) {
        try {
            String prompt = """
            Devuelve SOLO JSON valido (sin markdown ni texto extra) con esta forma:
            {
            "coste_desgaste_por_km": number,
            "detalle": "string"
            }

            Contexto:
            - Pais: Espana
            - Vehiculo:
                - marca: %s
                - modelo: %s
                - tipo: %s
                - anio: %d

            Tarea:
            - Estimar el coste de desgaste POR KILÓMETRO del vehículo (mantenimiento, aceite, neumáticos, piezas, etc).
            - Incluye: desgaste de neumáticos, cambios de aceite, filtros, frenos, correas, etc.
            - Devuelve un coste por km en euros (ejemplo: 0.08 para 8 céntimos por km).
            - Si no estas seguro, usa una estimacion razonable de turismos en Espana (entre 0.06 y 0.12 euros por km).
            """.formatted(vehiculo.getMarca(), vehiculo.getModelo(), vehiculo.getTipo().name(), vehiculo.getAnio());

            String json = calculoPrecioIA.pedirEstimacionJson(prompt);

            JsonNode node = objectMapper.readTree(json);
            BigDecimal costeKm = node.path("coste_desgaste_por_km").decimalValue();

            if (costeKm.compareTo(BigDecimal.valueOf(0.02)) < 0 || costeKm.compareTo(BigDecimal.valueOf(0.30)) > 0) {
                // Fallback: 0.08€/km (estimación estándar)
                costeKm = BigDecimal.valueOf(0.08);
            }

            return costeKm.multiply(distanciaKm).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            // Fallback: 0.08€/km
            return BigDecimal.valueOf(0.08).multiply(distanciaKm).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String construirPromptPrecio(Vehiculo v) {
        return """
        Devuelve SOLO JSON valido (sin markdown ni texto extra) con esta forma:
        {
          "precio_combustible_litro": number,
          "detalle": "string"
        }

        Contexto:
        - Pais: Espana
        - Vehiculo:
          - marca: %s
          - modelo: %s
          - tipo: %s
          - anio: %d

        Tarea:
        - Estimar el precio actual por litro del combustible principal de ese vehiculo.
        - Si no estas seguro del combustible exacto, usa una estimacion razonable de turismos en Espana.
        """.formatted(v.getMarca(), v.getModelo(), v.getTipo().name(), v.getAnio());
    }

    private void validarParadas(Viaje viaje) {
        if (viaje.getParadas() == null || viaje.getParadas().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar al menos origen y destino");
        }

        long origenes = viaje.getParadas().stream().filter(p -> p.getTipo() == TipoParada.ORIGEN).count();
        long destinos = viaje.getParadas().stream().filter(p -> p.getTipo() == TipoParada.DESTINO).count();

        if (origenes != 1 || destinos != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe haber exactamente un ORIGEN y un DESTINO");
        }

        Set<Integer> ordenes = new HashSet<>();
        for (Parada parada : viaje.getParadas()) {
            if (parada.getLocalizacion() == null || parada.getLocalizacion().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas las paradas deben tener localizacion");
            }

            if (parada.getOrden() == null || parada.getOrden() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas las paradas deben tener orden valido");
            }

            if (!ordenes.add(parada.getOrden())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede haber dos paradas con el mismo orden");
            }
        }
    }

    private boolean coincideEnParadas(Viaje viaje, String origenNorm, String destinoNorm) {
        List<Parada> paradas = viaje.getParadas();
        if (paradas == null || paradas.isEmpty()) {
            return false;
        }

        boolean origenOk = origenNorm.isBlank() || paradas.stream()
            .map(Parada::getLocalizacion)
            .filter(loc -> loc != null && !loc.isBlank())
            .map(this::normalizar)
            .anyMatch(locNorm -> locNorm.contains(origenNorm));

        boolean destinoOk = destinoNorm.isBlank() || paradas.stream()
            .map(Parada::getLocalizacion)
            .filter(loc -> loc != null && !loc.isBlank())
            .map(this::normalizar)
            .anyMatch(locNorm -> locNorm.contains(destinoNorm));

        return origenOk && destinoOk;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ROOT).trim();
    }

    private ViajeDTO convertirADTO(Viaje viaje) {
        VehiculoDTO vehiculoDTO = new VehiculoDTO(
            viaje.getVehiculo().getId(),
            viaje.getVehiculo().getMarca(),
            viaje.getVehiculo().getModelo(),
            viaje.getVehiculo().getMatricula()
        );

        List<ParadaDTO> paradasDTO = viaje.getParadas().stream()
            .map(parada -> new ParadaDTO(
                parada.getId(),
                parada.getLocalizacion(),
                parada.getTipo().toString(),
                parada.getOrden()
            ))
            .toList();

        List<ReservaDTO> reservasDTO = viaje.getReservas() != null 
            ? viaje.getReservas().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .map(r -> new ReservaDTO(
                    r.getId(),
                    r.getEstado().toString(),
                    r.getFechaHoraReserva(),
                    r.getViaje().getId(),
                    r.getPersona().getId(),
                    r.getPersona().getNombre(),
                    r.getPersona().getSlug(),
                    r.getParadaSubida().getId(),
                    r.getParadaBajada().getId(),
                    r.getCantidadPlazas()
                )).toList()
            : List.of();

        return new ViajeDTO(
            viaje.getId(),
            viaje.getFechaHoraSalida(),
            viaje.getEstado().toString(),
            viaje.getPlazasDisponibles(),
            viaje.getPrecio(),
            vehiculoDTO,
            paradasDTO,
            viaje.getSlug(),
            viaje.getPersona().getId(),
            viaje.getPersona().getNombre(),
            viaje.getPersona().getSlug(),
            reservasDTO
        );
    }

    private String construirBaseSlug(Viaje viaje) {
        String origen = viaje.getParadas().stream()
            .filter(p -> p.getTipo() == TipoParada.ORIGEN)
            .map(Parada::getLocalizacion)
            .findFirst()
            .orElse("origen");

        String destino = viaje.getParadas().stream()
            .filter(p -> p.getTipo() == TipoParada.DESTINO)
            .map(Parada::getLocalizacion)
            .findFirst()
            .orElse("destino");

        // Truncar las localizaciones para evitar slugs demasiado largos
        if (origen.length() > 20) {
            origen = origen.substring(0, 20);
        }
        if (destino.length() > 20) {
            destino = destino.substring(0, 20);
        }

        String fecha = viaje.getFechaHoraSalida() != null
            ? viaje.getFechaHoraSalida().toLocalDate().toString()
            : "sin-fecha";

        String raw = origen + "-" + destino + "-" + fecha;
        return SlugUtils.toSlug(raw);
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (viajeRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
}
