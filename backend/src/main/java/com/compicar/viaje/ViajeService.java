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
    ViajeDTO actualizarViaje(String usuarioEmail, String slug, Viaje viajeEditado);
    ViajeDTO finalizarViaje(String usuarioEmail, String slug);
    ViajeDTO iniciarViaje(String usuarioEmail, String slug);
    ViajeDTO confirmarCheckin(String usuarioEmail, String slug, String checkin);
    ViajeDTO obtenerProximoViajeUsuario(String email);
    ViajeDTO ponerEnCursoAutomatico(String usuarioEmail, String slug);
    ViajeDTO cancelarViajeIncompareceConductor(String usuarioEmail, String slug);
    ViajeDTO cancelarViajeConjunto(String usuarioEmail, String slug);
    Integer contarKilometrosRecorridosPorUsuario(String usuarioEmail);

}