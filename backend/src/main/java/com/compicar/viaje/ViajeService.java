package com.compicar.viaje;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;

public interface ViajeService {

    Viaje crearViaje(String usuarioEmail, Viaje viaje);
    PrecioTrayectoResponseDTO calcularPrecioTrayecto(String usuarioEmail, CalcularPrecioTrayectoRequestDTO request);
    
}
