package com.compicar.viajeRecurrente;

import java.util.List;
import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

public interface ViajeRecurrenteService {

    ViajeRecurrenteDTO mapearADTO(ViajeRecurrente viajeRecurrente);
    List<ViajeRecurrente> generarOcurrencias(Viaje viajePadre);
    ViajeRecurrenteDTO obtenerViajeRecurrentePorSlug(String slug);
    ViajeRecurrenteDTO iniciarViajeRecurrente(String usuarioEmail, String slug);
    ViajeRecurrenteDTO confirmarCheckinRecurrente(String usuarioEmail, String slug, String checkin);
    ViajeRecurrenteDTO finalizarViajeRecurrente(String usuarioEmail, String slug);
    ViajeRecurrenteDTO cancelarViajeRecurrente(String usuarioEmail, String slug);
    void cancelarViajesRecurrentesPendientesExpirados();
    ViajeRecurrenteDTO actualizarViajeRecurrente(String usuarioEmail, String slug, Viaje viajeEditado);
}
