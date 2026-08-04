package com.compicar.valoracion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

        Valoracion guardada = valoracionRepository.save(valoracion);
        guardada.setSlug("valoracion-" + guardada.getId());
        guardada = valoracionRepository.save(guardada);
        return new ValoracionDTO(guardada);
    }

    @Override
    public void eliminarValoracion(Long id) {
        valoracionRepository.deleteById(id);
    }

    @Override
    public ValoracionDTO actualizarValoracion(Long id, ValoracionDTO valoracionDTO) {
        Valoracion valoracionExistente = valoracionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valoración no encontrada"));

        aplicarDatos(valoracionExistente, valoracionDTO);
        valoracionExistente.setSlug(valoracionExistente.getSlug() != null ? valoracionExistente.getSlug() : "valoracion-" + id);

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

    private void aplicarDatos(Valoracion valoracion, ValoracionDTO valoracionDTO) {
        Persona autor = personaRepository.findById(valoracionDTO.getAutorId())
                .orElseThrow(() -> new IllegalArgumentException("Autor no encontrado"));
        Persona valorado = personaRepository.findById(valoracionDTO.getValoradoId())
                .orElseThrow(() -> new IllegalArgumentException("Persona valorada no encontrada"));

        valoracion.setPuntuacion(valoracionDTO.getPuntuacion());
        valoracion.setComentario(valoracionDTO.getComentario());
        valoracion.setAutor(autor);
        valoracion.setValorado(valorado);

        if (valoracionDTO.getSlug() != null && !valoracionDTO.getSlug().isBlank()) {
            valoracion.setSlug(valoracionDTO.getSlug());
        }
    }

    private List<ValoracionDTO> convertirAListaDTO(List<Valoracion> valoraciones) {
        List<ValoracionDTO> valoracionDTOs = new ArrayList<>();
        for (Valoracion valoracion : valoraciones) {
            valoracionDTOs.add(new ValoracionDTO(valoracion));
        }
        return valoracionDTOs;
    }

}
