package com.compicar.parada;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.checkin.Checkin;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

@Service
@Transactional
public class ParadaServiceImpl implements ParadaService {

    private final ParadaRepository paradaRepository;
    private final ViajeRepository viajeRepository;

    @Autowired
    public ParadaServiceImpl(ParadaRepository paradaRepository, ViajeRepository viajeRepository) {
        this.paradaRepository = paradaRepository;
        this.viajeRepository = viajeRepository;
    }

    @Override
    public Parada crearParada(Long viajeId, Parada parada) {
            Viaje viaje = viajeRepository.findById(viajeId)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    
            parada.setViaje(viaje);
            return paradaRepository.save(parada);   
    }

    @Override
    public Viaje anadirParadas(Long viajeId, List<Parada> paradas) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        if (viaje.getParadas() == null) {
            viaje.setParadas(new java.util.ArrayList<>());
        }

        paradas.forEach(p -> {
            p.setViaje(viaje);
            viaje.getParadas().add(p);
        });

        return paradaRepository.saveAll(paradas).isEmpty() ? viaje : viaje;
    }

    @Override
    public List<Parada> obtenerParadasPorViaje(Viaje viaje) {
        return paradaRepository.findByViaje(viaje);
    }
    
}
