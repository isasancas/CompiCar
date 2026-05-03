package com.compicar.pago;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;
    private final StripeService stripeService; // La clase que creamos antes

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, PersonaRepository personaRepository, ReservaRepository reservaRepository, StripeService stripeService) {
        this.pagoRepository = pagoRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
        this.stripeService = stripeService;
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
        stripeService.liberarFondos(stripePaymentIntentId);
        
        Pago pago = pagoRepository.findByStripePaymentIntentId(stripePaymentIntentId).get();
        pago.setEstado(EstadoPago.FALLIDO);
        pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public String crearIntentoDePago(Reserva reserva) throws StripeException {
        // 1. Llamamos al StripeService que creamos antes para obtener el PaymentIntent
        // El capture_method ya es MANUAL en el StripeService
        PaymentIntent intent = stripeService.crearAutorizacion(reserva);

        // 2. Creamos o actualizamos la entidad Pago en nuestra DB
        Pago pago = reserva.getPago();
        if (pago == null) {
            pago = new Pago();
            pago.setReserva(reserva);
        }
        
        pago.setStripePaymentIntentId(intent.getId());
        pago.setEstado(EstadoPago.PENDIENTE); // Aún no sabemos si la tarjeta pasó
        pago.setFechaCreacion(LocalDateTime.now());
        
        pagoRepository.save(pago);

        // 3. Devolvemos el clientSecret que el Frontend necesita para completar el pago
        return intent.getClientSecret();
    }

    @Override
    public Pago crearPago(String usuarioEmail, Long reservaId) {
        
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + usuarioEmail));
        
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));
            
        if (!reserva.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("La reserva no pertenece al usuario con email: " + usuarioEmail); 
    }  
        
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setEstado(EstadoPago.PENDIENTE);
        return pagoRepository.save(pago);
    
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
            // Validamos que el mensaje realmente viene de Stripe
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Firma de Webhook inválida");
        }

        // Analizamos qué pasó
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (dataObjectDeserializer.getObject().isEmpty()) return;

        PaymentIntent intent = (PaymentIntent) dataObjectDeserializer.getObject().get();

        switch (event.getType()) {
            case "payment_intent.amount_capturable_updated":
                actualizarEstadoPago(intent.getId(), EstadoPago.AUTORIZADO);
                // ACTIVAR RESERVA
                pagoRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(pago -> {
                    Reserva r = pago.getReserva();
                    r.setEstado(EstadoReserva.CONFIRMADA); // O el estado que uses para "Válida"
                    reservaRepository.save(r);
                });
            break;
                
            case "payment_intent.payment_failed":
                // El banco rechazó la operación
                actualizarEstadoPago(intent.getId(), EstadoPago.FALLIDO);
                break;
                
            case "payment_intent.succeeded":
                // Esto ocurre después de que tú llamas a capturarPago() y sale bien
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
