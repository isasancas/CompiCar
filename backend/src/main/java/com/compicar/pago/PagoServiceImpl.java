package com.compicar.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
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

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, PersonaRepository personaRepository, 
        ReservaRepository reservaRepository, StripeService stripeService, ViajeRepository viajeRepository) {
        this.pagoRepository = pagoRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.stripeService = stripeService;
        this.viajeRepository = viajeRepository;
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
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Firma de Webhook inválida");
        }

        // 1. Filtramos rápido: Si no es un evento de PaymentIntent, lo ignoramos y salimos.
        if (!event.getType().startsWith("payment_intent.")) {
            return; 
        }

        System.out.println("👉 PROCESANDO EVENTO DE PAYMENT INTENT: " + event.getType());

        // 2. Extraemos el objeto de forma segura, a prueba de desajustes de versión
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (deserializer.getObject().isPresent()) {
        stripeObject = deserializer.getObject().get();
        } else {
            try {
                stripeObject = deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                System.out.println("❌ ERROR deserializando el objeto de Stripe: " + e.getMessage());
                return;
            }
        }

        PaymentIntent intent = (PaymentIntent) stripeObject;

        // 3. Ahora sí, ejecutamos nuestra lógica
        switch (event.getType()) {
            case "payment_intent.amount_capturable_updated":
                actualizarEstadoPago(intent.getId(), EstadoPago.AUTORIZADO);
                
                pagoRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(pago -> {
                    Reserva reserva = pago.getReserva();
                    
                    if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                        System.out.println("⚠️ La reserva no está PENDIENTE. Estado actual: " + reserva.getEstado());
                        return; 
                    }

                    Viaje viaje = reserva.getViaje();

                    if (viaje.getPlazasDisponibles() < reserva.getCantidadPlazas()) {
                        reserva.setEstado(EstadoReserva.CANCELADA); 
                        reservaRepository.save(reserva);
                        System.out.println("❌ Sobreaforo detectado. Reserva cancelada.");
                        
                        try {
                            actualizarEstadoPago(intent.getId(), EstadoPago.REEMBOLSADO);
                        } catch (Exception e) {
                            System.out.println("Error cancelando retención por sobreaforo");
                        }
                        return; 
                    }

                    // Todo OK: Restamos plazas y pasamos a PAGADA
                    viaje.setPlazasDisponibles(viaje.getPlazasDisponibles() - reserva.getCantidadPlazas());
                    viajeRepository.save(viaje);

                    reserva.setEstado(EstadoReserva.PAGADA);
                    reservaRepository.save(reserva);
                    System.out.println("✅ ÉXITO: Reserva PAGADA y plazas restadas.");
                });
                break;
                
            case "payment_intent.payment_failed":
                actualizarEstadoPago(intent.getId(), EstadoPago.FALLIDO);
                pagoRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(pago -> {
                    Reserva r = pago.getReserva();
                    r.setEstado(EstadoReserva.CANCELADA);
                    reservaRepository.save(r);
                });
                break;
                
            case "payment_intent.succeeded":
                actualizarEstadoPago(intent.getId(), EstadoPago.CAPTURADO);
                break;
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
