package com.compicar.checkin;

import com.compicar.parada.Parada;
import com.compicar.reserva.Reserva;

public interface CheckinService {
    
    Checkin realizarCheckin(Parada parada, TipoCheckin tipoCheckin);
    
}
