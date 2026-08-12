package com.compicar.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;
    private final StripeService stripeService;
    private final ViajeRepository viajeRepository;
    private final NotificacionRepository notificacionRepository;

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, PersonaRepository personaRepository, 
        ReservaRepository reservaRepository, StripeService stripeService, ViajeRepository viajeRepository, NotificacionRepository notificacionRepository) {
        this.pagoRepository = pagoRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.stripeService = stripeService;
        this.viajeRepository = viajeRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    @Transactional
    public void capturarPago(String stripePaymentIntentId) throws StripeException {
        Pago pago = pagoRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado"));

        // Llamada a Stripe para cobrar el dinero congelado
        stripeService.confirmarCaptura(stripePaymentIntentId);

        // Actualizamos nuestra DB
        pago.setEstado(EstadoPago.CAPTURADO);
        pago.setFechaPago(LocalDateTime.now());
        pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public void cancelarPago(String stripePaymentIntentId) throws StripeException {
        // Lógica para liberar el dinero (Refund/Cancel)
        EstadoPago nuevoEstado = stripeService.liberarFondos(stripePaymentIntentId);
        
        Pago pago = pagoRepository.findByStripePaymentIntentId(stripePaymentIntentId).get();
        pago.setEstado(nuevoEstado);
        pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public String crearIntentoDePago(Reserva reserva) throws StripeException {
        // VALIDACIÓN DE SEGURIDAD
        if (reserva.getId() == null) {
            throw new IllegalStateException("No se puede crear un pago para una reserva que aún no ha sido guardada (ID nulo).");
        }

        // 1. Obtener el PaymentIntent de Stripe
        // Aquí es donde fallaba antes si dentro de crearAutorizacion usabas reserva.getPago()
        PaymentIntent intent = stripeService.crearAutorizacion(reserva);

        // 2. Gestión del Pago
        // Importante: Si la reserva es nueva, reserva.getPago() probablemente devuelva null
        // o intente disparar una consulta a la DB que falla por el ID nulo.
        Pago pago = reserva.getPago();
        
        if (pago == null) {
            pago = new Pago();
            pago.setReserva(reserva);
            
            // Calculamos el importe aquí si no viene en el objeto
            BigDecimal total = reserva.getViaje().getPrecio().multiply(new BigDecimal(reserva.getCantidadPlazas()));
            pago.setImporteTotal(total);
            
            // Rellena los campos obligatorios (Not Null) de tu tabla Pago
            pago.setComision(total.multiply(new BigDecimal("0.10"))); // Ejemplo 10%
            pago.setImporteConductor(total.subtract(pago.getComision()));
        }
        
        pago.setStripePaymentIntentId(intent.getId());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setFechaPago(null);
        
        pagoRepository.save(pago);

        return intent.getClientSecret();
    }

    @Override
    public List<Pago> obtenerPagosPorPersona(Persona persona) {
        Persona personaExistente = personaRepository.findById(persona.getId())
            .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada con ID: " + persona.getId()));
        return pagoRepository.findByPersona(personaExistente);
    }

    @Override
    public Pago obtenerPagoPorId(Long pagoId) {
        return pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
    }

    @Override
    public Pago actualizarPago(String usuarioEmail, Long reservaId, Pago pagoActualizado) {
        // Find the user by email
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        
        // Find the reservation by ID
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
        
        // Check if the reservation belongs to the user
        if (!reserva.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario con email: " + usuarioEmail);
        }
        
        // Find the pago associated with the reservation
        Pago pagoExistente = pagoRepository.findByReserva(reserva)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para la reserva con ID: " + reservaId));
        
        // Update the pago fields
        if (pagoActualizado.getImporteTotal() != null) {
            pagoExistente.setImporteTotal(pagoActualizado.getImporteTotal());
        }
        if (pagoActualizado.getImporteConductor() != null) {
            pagoExistente.setImporteConductor(pagoActualizado.getImporteConductor());
        }
        if (pagoActualizado.getComision() != null) {
            pagoExistente.setComision(pagoActualizado.getComision());
        }
        if (pagoActualizado.getEstado() != null) {
            pagoExistente.setEstado(pagoActualizado.getEstado());
        }
        
        // Save and return the updated pago
        return pagoRepository.save(pagoExistente);
    }

    @Override
    public Pago pagoCompletado(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
        pago.setEstado(EstadoPago.CAPTURADO);
        return pagoRepository.save(pago);
    }

    @Override
    public Pago pagoFallido(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
        pago.setEstado(EstadoPago.FALLIDO);
        return pagoRepository.save(pago);
    }

    @Override
    public Pago pagoReembolsado(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
        pago.setEstado(EstadoPago.REEMBOLSADO);
        return pagoRepository.save(pago);
    }
    
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Override
    @Transactional
    public void procesarEventoWebhook(String payload, String sigHeader) {
        String eventType;
        String paymentIntentId;

        // BYPASS PARA PRUEBAS LOCALES EN POSTMAN
        if ("t=123,v1=fake".equals(sigHeader) || "fake".equals(sigHeader)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(payload);
                eventType = root.path("type").asText();
                paymentIntentId = root.path("data").path("object").path("id").asText();
            } catch (Exception e) {
                throw new RuntimeException("Error parseando el JSON de prueba: " + e.getMessage());
            }
        } else {
            // PROCESAMIENTO REAL DE STRIPE EN PRODUCCIÓN
            try {
                Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
                eventType = event.getType();

                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                StripeObject stripeObject = deserializer.getObject().orElseGet(() -> {
                    try {
                        return deserializer.deserializeUnsafe();
                    } catch (EventDataObjectDeserializationException e) {
                        throw new RuntimeException("Error deserializando StripeObject", e);
                    }
                });

                PaymentIntent intent = (PaymentIntent) stripeObject;
                paymentIntentId = intent.getId();
            } catch (SignatureVerificationException e) {
                throw new RuntimeException("Firma de Webhook inválida");
            }
        }

        // 1. Filtramos rápido: Si no es un evento de PaymentIntent, salimos
        if (eventType == null || !eventType.startsWith("payment_intent.")) {
            return; 
        }

        System.out.println("👉 PROCESANDO EVENTO DE PAYMENT INTENT: " + eventType);

        // 2. Ejecutamos la lógica de negocio
        switch (eventType) {
            case "payment_intent.amount_capturable_updated":
                actualizarEstadoPago(paymentIntentId, EstadoPago.AUTORIZADO);
                
                pagoRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(pago -> {
                    List<Reserva> reservasDelPago = reservaRepository.findByPagoId(pago.getId());

                    for (Reserva reserva : reservasDelPago) {
                        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                            continue; 
                        }

                        // 1. Caso: Reserva de viaje puntual
                        if (reserva.getViaje() != null) {
                            Viaje viaje = reserva.getViaje();

                            if (viaje.getPlazasDisponibles() < reserva.getCantidadPlazas()) {
                                reserva.setEstado(EstadoReserva.CANCELADA); 
                                reservaRepository.save(reserva);
                                System.out.println("❌ Sobreaforo detectado en reserva " + reserva.getId() + ". Cancelada.");
                                continue;
                            }

                            viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - reserva.getCantidadPlazas());
                            viajeRepository.save(viaje);

                            reserva.setEstado(EstadoReserva.PAGADA);
                            reservaRepository.save(reserva);
                        } 
                        // 2. Caso: Reserva de viaje recurrente
                        else if (reserva.getViajeRecurrente() != null) {
                            var viajeRecurrente = reserva.getViajeRecurrente();

                            if (viajeRecurrente.getPlazasDisponibles() < reserva.getCantidadPlazas()) {
                                reserva.setEstado(EstadoReserva.CANCELADA);
                                reservaRepository.save(reserva);
                                System.out.println("❌ Sobreaforo en reserva recurrente " + reserva.getId() + ". Cancelada.");
                                continue;
                            }

                            viajeRecurrente.setPlazasDisponibles(viajeRecurrente.getPlazasDisponibles() - reserva.getCantidadPlazas());
                            // Si tienes repositorio de ViajeRecurrente, desmarcar para guardar:
                            // viajeRecurrenteRepository.save(viajeRecurrente);

                            reserva.setEstado(EstadoReserva.PAGADA);
                            reservaRepository.save(reserva);
                        }
                    }

                    // Notificación única al conductor
                    if (!reservasDelPago.isEmpty()) {
                        Reserva principal = reservasDelPago.get(0);
                        
                        // Obtener conductor según el tipo de reserva
                        Persona conductor = principal.getViaje() != null 
                            ? principal.getViaje().getPersona() 
                            : principal.getViajeRecurrente().getPersona();
                            
                        Persona pasajero = principal.getPersona();

                        String mensaje = pasajero.getNombre() + " ha pagado " + reservasDelPago.size() + " fecha(s) solicitada(s).";

                        notificacionRepository.save(new Notificacion(
                            mensaje,
                            conductor,
                            TipoNotificacion.NUEVA_RESERVA
                        ));
                    }

                    System.out.println("ÉXITO: Todas las reservas del paquete han sido procesadas a PAGADA.");
                });
                break;
                
            case "payment_intent.payment_failed":
                actualizarEstadoPago(paymentIntentId, EstadoPago.FALLIDO);
                pagoRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(pago -> {
                    Reserva r = pago.getReserva();
                    r.setEstado(EstadoReserva.CANCELADA);
                    reservaRepository.save(r);
                });
                break;
                
            case "payment_intent.succeeded":
                actualizarEstadoPago(paymentIntentId, EstadoPago.CAPTURADO);
                break;
        }
    }

    @Override
    @Transactional
    public void liberarPagoProgresivoPorViaje(Long viajeId) throws StripeException {
        // 1. Obtener las reservas asociadas a este viaje (o viaje recurrente)
        List<Reserva> reservasDelViaje = reservaRepository.findByViajeId(viajeId);

        for (Reserva reserva : reservasDelViaje) {
            if (reserva.getEstado() != EstadoReserva.PAGADA && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
                continue;
            }

            Pago pago = reserva.getPago();
            if (pago == null || pago.getStripePaymentIntentId() == null) {
                continue;
            }

            // 2. Si el dinero aún está en 'requires_capture', Stripe lo captura
            if (pago.getEstado() != EstadoPago.CAPTURADO) {
                stripeService.confirmarCaptura(pago.getStripePaymentIntentId());
                pago.setEstado(EstadoPago.CAPTURADO);
            }

            // 3. Obtener el precio por plaza según el tipo de viaje
            BigDecimal precioPorPlaza;
            Persona conductor;

            if (reserva.getViaje() != null) {
                precioPorPlaza = reserva.getViaje().getPrecio();
                conductor = reserva.getViaje().getPersona();
            } else if (reserva.getViajeRecurrente() != null) {
                precioPorPlaza = reserva.getViajeRecurrente().getPrecio();
                conductor = reserva.getViajeRecurrente().getPersona();
            } else {
                continue;
            }

            // 4. Calcular la parte NETA del conductor para ESTA fecha (Subtotal - 10% Comisión)
            BigDecimal subtotal = precioPorPlaza.multiply(BigDecimal.valueOf(reserva.getCantidadPlazas()));
            BigDecimal comisionReserva = subtotal.multiply(new BigDecimal("0.10"));
            BigDecimal parteConductorEstaReserva = subtotal.subtract(comisionReserva); // Ej: 13.50 €

            // 5. Acumular en el pago el saldo liberado al conductor
            BigDecimal nuevoSaldoLiberado = pago.getImporteLiberadoConductor().add(parteConductorEstaReserva);
            pago.setImporteLiberadoConductor(nuevoSaldoLiberado);

            // 6. Incrementar los fondos en la entidad Persona del conductor
            conductor.setFondosActuales(conductor.getFondosActuales().add(parteConductorEstaReserva));
            conductor.setFondosTotales(conductor.getFondosTotales().add(parteConductorEstaReserva));
            personaRepository.save(conductor);

            // 7. Si usa Stripe Connect, transferir el dinero neto a su cuenta bancaria
            if (conductor.getStripeConductorId() != null) {
                stripeService.transferirAConductor(conductor.getStripeConductorId(), parteConductorEstaReserva);
            }

            pagoRepository.save(pago);

            // 8. Notificar al conductor el importe neto abonado
            String msj = String.format("Se te han liberado %.2f € netos por la finalización del viaje.", parteConductorEstaReserva);
            notificacionRepository.save(new Notificacion(msj, conductor, TipoNotificacion.NUEVA_RESERVA));
        }
    }

    private void actualizarEstadoPago(String stripeId, EstadoPago nuevoEstado) {
        pagoRepository.findByStripePaymentIntentId(stripeId).ifPresent(pago -> {
            pago.setEstado(nuevoEstado);
            if (nuevoEstado == EstadoPago.CAPTURADO) {
                pago.setFechaPago(LocalDateTime.now());
            }
            pagoRepository.save(pago);
        });
    }

}
