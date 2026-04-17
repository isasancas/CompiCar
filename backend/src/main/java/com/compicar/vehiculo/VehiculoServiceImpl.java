package com.compicar.vehiculo;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;
import com.compicar.vehiculo.dto.VehiculoResponseDTO;
import com.compicar.viaje.ViajeRepository;

@Service
@Transactional
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final ViajeRepository viajeRepository;

    @Autowired
    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository, PersonaRepository personaRepository,
         ViajeRepository viajeRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.personaRepository = personaRepository;
        this.viajeRepository = viajeRepository;
    }

    @Override
    public VehiculoResponseDTO crearVehiculo(String usuarioEmail, AltaVehiculoRequestDTO request) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        String matriculaNormalizada = request.getMatricula().trim().toUpperCase(Locale.ROOT);
        if (vehiculoRepository.existsByMatricula(matriculaNormalizada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La matricula ya existe");
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMatricula(matriculaNormalizada);
        vehiculo.setMarca(request.getMarca().trim());
        vehiculo.setModelo(request.getModelo().trim());
        vehiculo.setPlazas(request.getPlazas());
        vehiculo.setConsumo(request.getConsumo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPersona(persona);

        Vehiculo guardado = vehiculoRepository.save(vehiculo);
        return VehiculoResponseDTO.fromEntity(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponseDTO> obtenerMisVehiculos(String usuarioEmail) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        return vehiculoRepository.findByPersonaId(persona.getId())
            .stream()
            .map(VehiculoResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public VehiculoResponseDTO actualizarVehiculo(String usuarioEmail, Long vehiculoId, AltaVehiculoRequestDTO request) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        Vehiculo vehiculo = vehiculoRepository.findByIdAndPersonaId(vehiculoId, persona.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));

        String matriculaNormalizada = request.getMatricula().trim().toUpperCase(Locale.ROOT);
        if (vehiculoRepository.existsByMatriculaAndIdNot(matriculaNormalizada, vehiculoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La matricula ya existe");
        }

        vehiculo.setMatricula(matriculaNormalizada);
        vehiculo.setMarca(request.getMarca().trim());
        vehiculo.setModelo(request.getModelo().trim());
        vehiculo.setPlazas(request.getPlazas());
        vehiculo.setConsumo(request.getConsumo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setTipo(request.getTipo());

        Vehiculo actualizado = vehiculoRepository.save(vehiculo);
        return VehiculoResponseDTO.fromEntity(actualizado);
    }

    @Override
    public void borrarVehiculo(String usuarioEmail, Long vehiculoId) {
        Persona persona = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        Vehiculo vehiculo = vehiculoRepository.findByIdAndPersonaId(vehiculoId, persona.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));

        if (viajeRepository.existsByVehiculoId(vehiculo.getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se puede borrar el vehiculo porque tiene viajes asociados"
            );
        }

        vehiculoRepository.delete(vehiculo);
    }
    
}
