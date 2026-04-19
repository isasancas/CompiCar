package com.compicar.vehiculo;

import java.util.List;

import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;
import com.compicar.vehiculo.dto.VehiculoResponseDTO;

public interface VehiculoService {

    VehiculoResponseDTO crearVehiculo(String usuarioEmail, AltaVehiculoRequestDTO request);
    List<VehiculoResponseDTO> obtenerMisVehiculos(String usuarioEmail);
    VehiculoResponseDTO actualizarVehiculo(String usuarioEmail, Long vehiculoId, AltaVehiculoRequestDTO request);
    void borrarVehiculo(String usuarioEmail, Long vehiculoId);
    VehiculoResponseDTO obtenerVehiculoPorSlug(String slug);
    
}
