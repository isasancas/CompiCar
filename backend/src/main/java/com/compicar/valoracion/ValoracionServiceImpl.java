package com.compicar.valoracion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.valoracion.dto.ValoracionDTO;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ValoracionServiceImpl implements ValoracionService {

    private final ValoracionRepository valoracionRepository;
    private final PersonaRepository personaRepository;

    @Autowired
    public ValoracionServiceImpl(ValoracionRepository valoracionRepository, PersonaRepository personaRepository) {
        this.valoracionRepository = valoracionRepository;
        this.personaRepository = personaRepository;
    }

    @Override
    public ValoracionDTO crearValoracion(ValoracionDTO valoracionDTO) {
        Valoracion valoracion = new Valoracion();
        aplicarDatos(valoracion, valoracionDTO);

        Valoracion valoracionGuardada = valoracionRepository.save(valoracion);
        valoracionGuardada.setSlug("valoracion-" + valoracionGuardada.getId());
        valoracionGuardada = valoracionRepository.save(valoracionGuardada);

        return new ValoracionDTO(valoracionGuardada);
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
