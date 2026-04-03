package com.compicar.parada;

import java.util.List;
import com.compicar.viaje.Viaje;

public interface ParadaService {

    Parada crearParada(Long viajeId, Parada parada);

    Viaje anadirParadas(Long viajeId, List<Parada> paradas);

    List<Parada> obtenerParadasPorViaje(Viaje viaje);
}

