package com.compicar.viaje;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.config.SlugUtils;
import com.compicar.parada.Parada;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import com.compicar.viaje.dto.VehiculoDTO;
import com.compicar.viaje.dto.ParadaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class ViajeServiceImpl implements ViajeService {

    private final ViajeRepository viajeRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final GeminiFuelPriceClient geminiFuelPriceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pricing.fallback.fuel-price-eur-per-liter:1.65}")
    private BigDecimal fallbackFuelPrice;

    public ViajeServiceImpl(
        ViajeRepository viajeRepository,
        PersonaRepository personaRepository,
        VehiculoRepository vehiculoRepository,
        GeminiFuelPriceClient geminiFuelPriceClient
    ) {
        this.viajeRepository = viajeRepository;
        this.personaRepository = personaRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.geminiFuelPriceClient = geminiFuelPriceClient;
    }

    @Override
    public Viaje crearViaje(String usuarioEmail, Viaje viaje) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (viaje.getVehiculo() == null || viaje.getVehiculo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El viaje debe incluir un vehículo válido");
        }

        if (viaje.getParadas() == null || viaje.getParadas().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar al menos origen y destino");
        }

        Vehiculo vehiculo = vehiculoRepository.findById(viaje.getVehiculo().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo no existe"));

        if (vehiculo.getPersona() == null || !vehiculo.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehículo no pertenece al usuario autenticado");
        }

        viaje.setPersona(conductor);
        viaje.setVehiculo(vehiculo);

        if (viaje.getParadas() != null) {
            for (int i = 0; i < viaje.getParadas().size(); i++) {
                Parada parada = viaje.getParadas().get(i);
                parada.setViaje(viaje);

                if (parada.getFechaHora() == null) {
                    parada.setFechaHora(viaje.getFechaHoraSalida());
                }

                if (parada.getOrden() == null) {
                    parada.setOrden(i + 1);
                }
            }
        }

        validarParadas(viaje);
        String baseSlug = construirBaseSlug(viaje);
        viaje.setSlug(generarSlugUnico(baseSlug));
        return viajeRepository.save(viaje);
    }

    @Override
    @Transactional(readOnly = true)
    public PrecioTrayectoResponseDTO calcularPrecioTrayecto(String usuarioEmail, CalcularPrecioTrayectoRequestDTO request) {
        Persona conductor = personaRepository.findByEmail(usuarioEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo no existe"));

        if (vehiculo.getPersona() == null || !vehiculo.getPersona().getId().equals(conductor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehículo no pertenece al usuario autenticado");
        }

        BigDecimal distanciaKm = BigDecimal.valueOf(request.getDistanciaKm());
        BigDecimal consumoL100 = BigDecimal.valueOf(vehiculo.getConsumo());

        BigDecimal litrosEstimados = consumoL100
            .multiply(distanciaKm)
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal precioLitro = obtenerPrecioLitroConGemini(vehiculo);
        String fuente = "GEMINI";
        String detalle = "Estimacion con Gemini";

        if (precioLitro == null) {
            precioLitro = fallbackFuelPrice;
            fuente = "FALLBACK";
            detalle = "Gemini no disponible, se usa precio fallback";
        }

        BigDecimal costeTotal = litrosEstimados.multiply(precioLitro).setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioMin = costeTotal.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioMax = costeTotal.multiply(BigDecimal.valueOf(1.20)).setScale(2, RoundingMode.HALF_UP);

        PrecioTrayectoResponseDTO response = new PrecioTrayectoResponseDTO();
        response.setLitrosEstimados(litrosEstimados.setScale(2, RoundingMode.HALF_UP));
        response.setPrecioCombustibleLitro(precioLitro.setScale(3, RoundingMode.HALF_UP));
        response.setCosteTotalCombustible(costeTotal);
        response.setPrecioMinimoPasajero(precioMin);
        response.setPrecioMaximoPasajero(precioMax);
        response.setFuente(fuente);
        response.setDetalle(detalle);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeDTO obtenerViajePorSlug(String slug) {
        Viaje viaje = viajeRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        return convertToDTO(viaje);
    }

    private BigDecimal obtenerPrecioLitroConGemini(Vehiculo vehiculo) {
        try {
            String prompt = construirPromptPrecio(vehiculo);
            String json = geminiFuelPriceClient.pedirEstimacionJson(prompt);

            JsonNode node = objectMapper.readTree(json);
            BigDecimal precio = node.path("precio_combustible_litro").decimalValue();

            if (precio.compareTo(BigDecimal.valueOf(0.8)) < 0 || precio.compareTo(BigDecimal.valueOf(3.5)) > 0) {
                return null;
            }
            return precio;
        } catch (Exception ex) {
            return null;
        }
    }

    private String construirPromptPrecio(Vehiculo v) {
        return """
        Devuelve SOLO JSON valido (sin markdown ni texto extra) con esta forma:
        {
          "precio_combustible_litro": number,
          "detalle": "string"
        }

        Contexto:
        - Pais: Espana
        - Vehiculo:
          - marca: %s
          - modelo: %s
          - tipo: %s
          - anio: %d

        Tarea:
        - Estimar el precio actual por litro del combustible principal de ese vehiculo.
        - Si no estas seguro del combustible exacto, usa una estimacion razonable de turismos en Espana.
        """.formatted(v.getMarca(), v.getModelo(), v.getTipo().name(), v.getAnio());
    }

    private void validarParadas(Viaje viaje) {
        if (viaje.getParadas() == null || viaje.getParadas().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar al menos origen y destino");
        }

        long origenes = viaje.getParadas().stream().filter(p -> p.getTipo() == TipoParada.ORIGEN).count();
        long destinos = viaje.getParadas().stream().filter(p -> p.getTipo() == TipoParada.DESTINO).count();

        if (origenes != 1 || destinos != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe haber exactamente un ORIGEN y un DESTINO");
        }

        Set<Integer> ordenes = new HashSet<>();
        for (Parada parada : viaje.getParadas()) {
            if (parada.getLocalizacion() == null || parada.getLocalizacion().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas las paradas deben tener localizacion");
            }

            if (parada.getOrden() == null || parada.getOrden() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas las paradas deben tener orden valido");
            }

            if (!ordenes.add(parada.getOrden())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede haber dos paradas con el mismo orden");
            }
        }
    }

    @Override
    public List<ViajeDTO> obtenerMisViajes(String email) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Viaje> viajes = viajeRepository.findByPersonaId(persona.getId());
        return viajes.stream().map(this::convertToDTO).toList();
    }

    @Override
    public List<ViajeDTO> obtenerViajesParticipados(String email) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Viaje> viajes = viajeRepository.findViajesParticipadosByPersonaId(persona.getId());
        return viajes.stream().map(this::convertToDTO).toList();
    }

    private ViajeDTO convertToDTO(Viaje viaje) {
        VehiculoDTO vehiculoDTO = new VehiculoDTO(
            viaje.getVehiculo().getId(),
            viaje.getVehiculo().getMarca(),
            viaje.getVehiculo().getModelo(),
            viaje.getVehiculo().getMatricula()
        );

        List<ParadaDTO> paradasDTO = viaje.getParadas().stream()
            .map(parada -> new ParadaDTO(
                parada.getId(),
                parada.getLocalizacion(),
                parada.getTipo().toString(),
                parada.getOrden()
            ))
            .toList();

        return new ViajeDTO(
            viaje.getId(),
            viaje.getFechaHoraSalida(),
            viaje.getEstado().toString(),
            viaje.getPlazasDisponibles(),
            viaje.getPrecio(),
            vehiculoDTO,
            paradasDTO,
            viaje.getSlug()
        );
    }

    private String construirBaseSlug(Viaje viaje) {
        String origen = viaje.getParadas().stream()
            .filter(p -> p.getTipo() == TipoParada.ORIGEN)
            .map(Parada::getLocalizacion)
            .findFirst()
            .orElse("origen");

        String destino = viaje.getParadas().stream()
            .filter(p -> p.getTipo() == TipoParada.DESTINO)
            .map(Parada::getLocalizacion)
            .findFirst()
            .orElse("destino");

        String fecha = viaje.getFechaHoraSalida() != null
            ? viaje.getFechaHoraSalida().toLocalDate().toString()
            : "sin-fecha";

        String raw = origen + "-" + destino + "-" + fecha;
        return SlugUtils.toSlug(raw);
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (viajeRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
}
