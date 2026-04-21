package com.compicar.pago;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, PersonaRepository personaRepository, ReservaRepository reservaRepository) {
        this.pagoRepository = pagoRepository;
        this.personaRepository = personaRepository;
        this.reservaRepository = reservaRepository;
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
        pago.setEstado(EstadoPago.COMPLETADO);
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
    
}
