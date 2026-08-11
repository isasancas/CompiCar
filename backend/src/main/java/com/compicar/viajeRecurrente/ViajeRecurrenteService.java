package com.compicar.viajeRecurrente;

import java.util.List;

import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

public interface ViajeRecurrenteService {

    ViajeRecurrenteDTO mapearADTO(ViajeRecurrente viajeRecurrente);
    List<ViajeRecurrente> generarOcurrencias(Viaje viajePadre);
    
}
