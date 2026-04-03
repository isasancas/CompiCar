package com.compicar.viaje;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ViajeServiceImpl implements ViajeService {

    private final ViajeRepository viajeRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoRepository vehiculoRepository;

    public ViajeServiceImpl(ViajeRepository viajeRepository,
                            PersonaRepository personaRepository,
                            VehiculoRepository vehiculoRepository) {
        this.viajeRepository = viajeRepository;
        this.personaRepository = personaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Override
    public Viaje crearViaje(String usuarioEmail, Viaje viaje) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (viaje.getVehiculo() == null || viaje.getVehiculo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje debe incluir un vehículo válido");
        }

        Vehiculo vehiculo = vehiculoRepository.findById(viaje.getVehiculo().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo no existe"));

        if (vehiculo.getPersona() == null || !vehiculo.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehículo no pertenece al usuario autenticado");
        }

        viaje.setPersona(conductor);
        viaje.setVehiculo(vehiculo);

        if (viaje.getParadas() != null) {
            viaje.getParadas().forEach(parada -> parada.setViaje(viaje));
        }

        return viajeRepository.save(viaje);
    }

}
