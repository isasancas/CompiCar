package com.compicar.viaje;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final CalculoPrecioIA calculoPrecioIA;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pricing.fallback.fuel-price-eur-per-liter:1.65}")
    private BigDecimal fallbackFuelPrice;

    public ViajeServiceImpl(ViajeRepository viajeRepository,PersonaRepository personaRepository,
        VehiculoRepository vehiculoRepository,CalculoPrecioIA calculoPrecioIA) {
        this.viajeRepository = viajeRepository;
        this.personaRepository = personaRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.calculoPrecioIA = calculoPrecioIA;
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

        // Coste de combustible
        BigDecimal costeCombustible = litrosEstimados.multiply(precioLitro).setScale(2, RoundingMode.HALF_UP);

        // Coste de desgaste (Gemini estima coste por km)
        BigDecimal costeDesgaste = obtenerCosteDesgasteConGemini(vehiculo, distanciaKm);
        
        // Coste total: combustible + desgaste
        BigDecimal costeTotal = costeCombustible.add(costeDesgaste).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal precioMin = costeTotal.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioMax = costeTotal.multiply(BigDecimal.valueOf(1.20)).setScale(2, RoundingMode.HALF_UP);

        PrecioTrayectoResponseDTO response = new PrecioTrayectoResponseDTO();
        response.setLitrosEstimados(litrosEstimados.setScale(2, RoundingMode.HALF_UP));
        response.setPrecioCombustibleLitro(precioLitro.setScale(3, RoundingMode.HALF_UP));
        response.setCosteTotalCombustible(costeCombustible);
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

    @Override
    @Transactional(readOnly = true)
    public List<ViajeDTO> buscarViajesPublicos(String origen, String destino, LocalDate fecha) {
        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;

        Set<EstadoViaje> estadosPublicos = Set.of(EstadoViaje.PENDIENTE, EstadoViaje.INICIADO);

        List<Viaje> base = (inicio != null && fin != null)
            ? viajeRepository.buscarViajesPublicosConFecha(estadosPublicos, inicio, fin)
            : viajeRepository.buscarViajesPublicosSinFecha(estadosPublicos);

        String origenNorm = normalizar(origen);
        String destinoNorm = normalizar(destino);

        return base.stream()
            .filter(v -> coincideEnParadas(v, origenNorm, destinoNorm))
            .map(this::convertToDTO)
            .toList();
    }

    private BigDecimal obtenerPrecioLitroConGemini(Vehiculo vehiculo) {
        try {
            String prompt = construirPromptPrecio(vehiculo);
            String json = calculoPrecioIA.pedirEstimacionJson(prompt);

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

    private BigDecimal obtenerCosteDesgasteConGemini(Vehiculo vehiculo, BigDecimal distanciaKm) {
        try {
            String prompt = """
            Devuelve SOLO JSON valido (sin markdown ni texto extra) con esta forma:
            {
            "coste_desgaste_por_km": number,
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
            - Estimar el coste de desgaste POR KILÓMETRO del vehículo (mantenimiento, aceite, neumáticos, piezas, etc).
            - Incluye: desgaste de neumáticos, cambios de aceite, filtros, frenos, correas, etc.
            - Devuelve un coste por km en euros (ejemplo: 0.08 para 8 céntimos por km).
            - Si no estas seguro, usa una estimacion razonable de turismos en Espana (entre 0.06 y 0.12 euros por km).
            """.formatted(vehiculo.getMarca(), vehiculo.getModelo(), vehiculo.getTipo().name(), vehiculo.getAnio());

            String json = calculoPrecioIA.pedirEstimacionJson(prompt);

            JsonNode node = objectMapper.readTree(json);
            BigDecimal costeKm = node.path("coste_desgaste_por_km").decimalValue();

            if (costeKm.compareTo(BigDecimal.valueOf(0.02)) < 0 || costeKm.compareTo(BigDecimal.valueOf(0.30)) > 0) {
                // Fallback: 0.08€/km (estimación estándar)
                costeKm = BigDecimal.valueOf(0.08);
            }

            return costeKm.multiply(distanciaKm).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            // Fallback: 0.08€/km
            return BigDecimal.valueOf(0.08).multiply(distanciaKm).setScale(2, RoundingMode.HALF_UP);
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

    private boolean coincideEnParadas(Viaje viaje, String origenNorm, String destinoNorm) {
        List<Parada> paradas = viaje.getParadas();
        if (paradas == null || paradas.isEmpty()) {
            return false;
        }

        boolean origenOk = origenNorm.isBlank() || paradas.stream()
            .map(Parada::getLocalizacion)
            .filter(loc -> loc != null && !loc.isBlank())
            .map(this::normalizar)
            .anyMatch(locNorm -> locNorm.contains(origenNorm));

        boolean destinoOk = destinoNorm.isBlank() || paradas.stream()
            .map(Parada::getLocalizacion)
            .filter(loc -> loc != null && !loc.isBlank())
            .map(this::normalizar)
            .anyMatch(locNorm -> locNorm.contains(destinoNorm));

        return origenOk && destinoOk;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ROOT).trim();
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

        // Truncar las localizaciones para evitar slugs demasiado largos
        if (origen.length() > 20) {
            origen = origen.substring(0, 20);
        }
        if (destino.length() > 20) {
            destino = destino.substring(0, 20);
        }

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
