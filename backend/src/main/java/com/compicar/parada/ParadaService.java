package com.compicar.parada;

import java.util.List;
import com.compicar.notificacion.Notificacion;
import com.compicar.parada.dto.SolicitudNuevaParadaRequest;
import com.compicar.viaje.Viaje;

public interface ParadaService {

    Parada crearParada(Long viajeId, Parada parada);

    Viaje anadirParadas(Long viajeId, List<Parada> paradas);

    List<Parada> obtenerParadasPorViaje(Viaje viaje);

    Notificacion solicitarNuevaParada(String pasajeroEmail, SolicitudNuevaParadaRequest request);

    Notificacion aceptarSolicitudNuevaParada(String conductorEmail, Long notificacionId);

    Notificacion rechazarSolicitudNuevaParada(String conductorEmail, Long notificacionId);
}

