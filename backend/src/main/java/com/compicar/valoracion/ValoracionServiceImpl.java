package com.compicar.valoracion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.valoracion.dto.ValoracionDTO;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ValoracionServiceImpl implements ValoracionService {

    private final ValoracionRepository valoracionRepository;
    private final PersonaRepository personaRepository;
    private final ViajeRepository viajeRepository;

    @Autowired
    public ValoracionServiceImpl(ValoracionRepository valoracionRepository, PersonaRepository personaRepository, ViajeRepository viajeRepository) {
        this.valoracionRepository = valoracionRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
    }

    @Override
    public ValoracionDTO crearValoracion(ValoracionDTO valoracionDTO) {
        Persona autorAutenticado = obtenerPersonaAutenticada();
        if (!autorAutenticado.getId().equals(valoracionDTO.getAutorId())) {
            throw new AccessDeniedException("Solo puedes crear valoraciones con tu propia cuenta");
        }

        Viaje viaje = viajeRepository.findById(valoracionDTO.getViajeId())
                .orElseThrow(() -> new IllegalArgumentException("El viaje especificado no existe"));

        Persona conductor = viaje.getPersona();
        if (conductor == null) {
            throw new IllegalArgumentException("El viaje no tiene conductor asignado");
        }

        if (valoracionDTO.getValoradoId() != null && !conductor.getId().equals(valoracionDTO.getValoradoId())) {
            throw new IllegalArgumentException("Solo puedes valorar al conductor del viaje realizado");
        }

        boolean esPasajeroDelViaje = viaje.getReservas().stream()
                .anyMatch(reserva -> reserva.getPersona().getId().equals(valoracionDTO.getAutorId()));

        if (!esPasajeroDelViaje) {
            throw new IllegalArgumentException("Solo puedes valorar un viaje en el que hayas participado como pasajero");
        }

        boolean yaValorado = valoracionRepository.existePorAutorIdAndViajeId(
                valoracionDTO.getAutorId(), 
                valoracionDTO.getViajeId()
        );
        if (yaValorado) {
            throw new IllegalArgumentException("Ya has valorado este viaje anteriormente");
        }

        Valoracion valoracion = new Valoracion();
        valoracion.setPuntuacion(valoracionDTO.getPuntuacion());
        valoracion.setComentario(valoracionDTO.getComentario());
        valoracion.setFecha(LocalDateTime.now());
        valoracion.setAutor(personaRepository.findById(valoracionDTO.getAutorId())
                .orElseThrow(() -> new IllegalArgumentException("Autor no encontrado")));
        valoracion.setValorado(conductor);
        valoracion.setViaje(viaje);
        valoracion.setSlug(generarSlugValoracion(viaje.getId(), valoracion.getAutor().getId()));

        return new ValoracionDTO(valoracionRepository.save(valoracion));
    }

    @Override
    public void eliminarValoracion(Long id) {
        Valoracion valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valoración no encontrada"));

        asegurarAutorAutenticado(valoracion);
        valoracionRepository.delete(valoracion);
    }

    @Override
    public ValoracionDTO actualizarValoracion(Long id, ValoracionDTO valoracionDTO) {
        Valoracion valoracionExistente = valoracionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valoración no encontrada"));

        asegurarAutorAutenticado(valoracionExistente);

        if (valoracionDTO.getPuntuacion() != null) {
            valoracionExistente.setPuntuacion(valoracionDTO.getPuntuacion());
        }
        valoracionExistente.setComentario(valoracionDTO.getComentario());

        return new ValoracionDTO(valoracionRepository.save(valoracionExistente));
    }

    @Override
    public Double calcularReputacion(Long personaId) {
        return valoracionRepository.calcularReputacion(personaId);
    }

    @Override
    public List<ValoracionDTO> encontrarPorAutor(Long autorId) {
        return convertirAListaDTO(valoracionRepository.encontrarPorAutorId(autorId));
    }

    @Override
    public List<ValoracionDTO> encontrarPorValorado(Long valoradoId) {
        return convertirAListaDTO(valoracionRepository.encontrarPorValoradoId(valoradoId));
    }

    @Override
    public Optional<ValoracionDTO> encontrarPorId(Long id) {
        return valoracionRepository.findById(id).map(ValoracionDTO::new);
    }

    private List<ValoracionDTO> convertirAListaDTO(List<Valoracion> valoraciones) {
        List<ValoracionDTO> valoracionDTOs = new ArrayList<>();
        for (Valoracion valoracion : valoraciones) {
            valoracionDTOs.add(new ValoracionDTO(valoracion));
        }
        return valoracionDTOs;
    }

    private Persona obtenerPersonaAutenticada() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return personaRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void asegurarAutorAutenticado(Valoracion valoracion) {
        Persona autorAutenticado = obtenerPersonaAutenticada();
        if (valoracion.getAutor() == null || !autorAutenticado.getId().equals(valoracion.getAutor().getId())) {
            throw new AccessDeniedException("Solo puedes modificar o eliminar tus propias valoraciones");
        }
    }

    private String generarSlugValoracion(Long viajeId, Long autorId) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "valoracion-" + viajeId + "-" + autorId + "-" + random;
    }

}
