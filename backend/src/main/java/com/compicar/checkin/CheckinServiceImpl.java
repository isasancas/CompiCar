package com.compicar.checkin;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.parada.Parada;
import com.compicar.reserva.Reserva;

@Service
@Transactional
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;

    @Autowired
    public CheckinServiceImpl(CheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    @Override
    public Checkin realizarCheckin(Parada parada, TipoCheckin tipoCheckin) {
        Checkin checkin = new Checkin();
        checkin.setFechaHoraConductor(LocalDateTime.now());
        checkin.setFechaHoraPasajero(LocalDateTime.now());
        checkin.setTipo(tipoCheckin);
        checkin.setEstado(EstadoCheckin.PENDIENTE);
        checkin.setParada(parada);

        return checkinRepository.save(checkin);
    }
    
}
