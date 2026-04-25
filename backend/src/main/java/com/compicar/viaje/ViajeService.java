package com.compicar.viaje;

import java.time.LocalDate;
import java.util.List;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;

public interface ViajeService {

    Viaje crearViaje(String usuarioEmail, Viaje viaje);
    PrecioTrayectoResponseDTO calcularPrecioTrayecto(String usuarioEmail, CalcularPrecioTrayectoRequestDTO request);
    List<ViajeDTO> obtenerMisViajes(String email);
    List<ViajeDTO> obtenerViajesParticipados(String email);
    ViajeDTO obtenerViajePorSlug(String slug);
    List<ViajeDTO> buscarViajesPublicos(String origen, String destino, LocalDate fecha);
    List<ViajeDTO> obtenerViajesPublicosPorConductor(String conductorSlug);
    ViajeDTO cancelarViaje(String usuarioEmail, String slug);
    int cancelarViajesPendientesExpirados();
}