package com.compicar.valoracion;

import java.util.List;
import java.util.Optional;

import com.compicar.valoracion.dto.ValoracionDTO;

public interface ValoracionService {

    ValoracionDTO crearValoracion(ValoracionDTO valoracion);
    void eliminarValoracion(Long id);
    ValoracionDTO actualizarValoracion(Long id, ValoracionDTO valoracion);
    Double calcularReputacion(Long personaId);
    List<ValoracionDTO> encontrarPorAutor(Long autorId);
    List<ValoracionDTO> encontrarPorValorado(Long valoradoId);
    Optional<ValoracionDTO> encontrarPorId(Long id);
    
}
