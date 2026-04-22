package com.compicar.checkin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.parada.Parada;
import com.compicar.reserva.Reserva;

@RestController
@RequestMapping("/api/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    @Autowired
    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/crear/?tipo={tipoCheckin}")
    public ResponseEntity<Checkin> crearCheckin(@RequestBody Parada parada, @PathVariable TipoCheckin tipoCheckin) {
        Checkin checkin = checkinService.realizarCheckin(parada, tipoCheckin);
        return ResponseEntity.ok(checkin);
    }

}
