package com.compicar.parada;

import java.util.List;
import com.compicar.notificacion.Notificacion;
import com.compicar.parada.dto.SolicitudNuevaParadaRequest;
import com.compicar.viaje.Viaje;

public interface ParadaService {

    Parada crearParada(Long viajeId, Parada parada);

    Viaje anadirParadas(Long viajeId, List<Parada> paradas);

    List<Parada> obtenerParadasPorViaje(Viaje viaje);

    List<Notificacion> solicitarNuevaParada(String pasajeroEmail, SolicitudNuevaParadaRequest request);

    boolean tieneSolicitudNuevaParadaPendiente(String pasajeroEmail, Long reservaId);

    Notificacion aceptarSolicitudNuevaParada(String conductorEmail, Long notificacionId);

    Notificacion rechazarSolicitudNuevaParada(String conductorEmail, Long notificacionId);

    List<Object[]> obtenerTop5Localizaciones();
}

