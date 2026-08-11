package com.compicar.viajeRecurrente;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compicar.parada.Parada;
import com.compicar.parada.dto.ParadaDTO;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.vehiculo.dto.VehiculoDTO;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

@Service
@Transactional
public class ViajeRecurrenteServiceImpl implements ViajeRecurrenteService {

    private final ViajeRecurrenteRepository viajeRecurrenteRepository;

    public ViajeRecurrenteServiceImpl(ViajeRecurrenteRepository viajeRecurrenteRepository) {
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
    }

    @Override
    public ViajeRecurrenteDTO mapearADTO(ViajeRecurrente vr) {
        if (vr == null) return null;

        VehiculoDTO vehiculoDTO = vr.getVehiculo() != null ? new VehiculoDTO(
            vr.getVehiculo().getId(),
            vr.getVehiculo().getMarca(),
            vr.getVehiculo().getModelo(),
            vr.getVehiculo().getMatricula()
        ) : null;

        List<ParadaDTO> paradasDTO = vr.getParadas() != null ? vr.getParadas().stream()
            .map(parada -> new ParadaDTO(
                parada.getId(),
                parada.getLocalizacion(),
                parada.getTipo().toString(),
                parada.getOrden()
            ))
            .toList() : List.of();

        List<ReservaDTO> reservasDTO = vr.getReservas() != null ? vr.getReservas().stream()
            .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
            .map(r -> new ReservaDTO(
                r.getId(),
                r.getEstado().toString(),
                r.getFechaHoraReserva(),
                r.getViaje() != null ? r.getViaje().getId() : null,
                r.getPersona().getId(),
                r.getPersona().getNombre(),
                r.getPersona().getSlug(),
                r.getParadaSubida() != null ? r.getParadaSubida().getId() : null,
                r.getParadaBajada() != null ? r.getParadaBajada().getId() : null,
                r.getCantidadPlazas()
            )).toList() : List.of();

        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        dto.setId(vr.getId());
        dto.setSlug(vr.getSlug());
        dto.setCheckin(vr.getCheckin());
        dto.setFechaHoraSalida(vr.getFechaHoraSalida());
        dto.setFechaHoraFin(vr.getFechaHoraFin());
        dto.setEstado(vr.getEstado() != null ? vr.getEstado().toString() : null);
        dto.setPlazasDisponibles(vr.getPlazasDisponibles());
        dto.setPrecio(vr.getPrecio());
        dto.setViajePadreId(vr.getViajePadre() != null ? vr.getViajePadre().getId() : null);

        if (vr.getPersona() != null) {
            dto.setConductorId(vr.getPersona().getId());
            dto.setConductorNombre(vr.getPersona().getNombre());
            dto.setConductorSlug(vr.getPersona().getSlug());
        }

        dto.setVehiculo(vehiculoDTO);
        dto.setParadas(paradasDTO);
        dto.setReservas(reservasDTO);

        return dto;
    }

    @Override
    public List<ViajeRecurrente> generarOcurrencias(Viaje viajePadre) {
        if (viajePadre.getDiasSemana() == null || viajePadre.getDiasSemana().isEmpty() || viajePadre.getFechaFinRecurrencia() == null) {
            return List.of();
        }

        Set<DayOfWeek> diasDeseados = viajePadre.getDiasSemana().stream()
            .map(this::mapearDiaSemana)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (diasDeseados.isEmpty()) {
            return List.of();
        }

        LocalDate fechaInicio = viajePadre.getFechaHoraSalida().toLocalDate().plusDays(1);
        
        LocalDate fechaFin = viajePadre.getFechaFinRecurrencia().toLocalDate();

        List<ViajeRecurrente> ocurrencias = new ArrayList<>();

        for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
            if (diasDeseados.contains(fecha.getDayOfWeek())) {
                LocalDateTime fechaSalidaOcurrencia = LocalDateTime.of(fecha, viajePadre.getFechaHoraSalida().toLocalTime());

                ViajeRecurrente vr = new ViajeRecurrente();
                vr.setViajePadre(viajePadre);
                vr.setFechaHoraSalida(fechaSalidaOcurrencia);
                vr.setEstado(EstadoViaje.PENDIENTE);
                vr.setPlazasDisponibles(viajePadre.getPlazasDisponibles());
                vr.setPrecio(viajePadre.getPrecio());
                vr.setPersona(viajePadre.getPersona());
                vr.setVehiculo(viajePadre.getVehiculo());
                vr.setCheckin(generarCheckin());
                vr.setSlug(generarSlugUnico(viajePadre.getSlug() + "-" + fecha.toString()));

                // Clonar paradas con ajuste temporal
                if (viajePadre.getParadas() != null) {
                    for (Parada pPadre : viajePadre.getParadas()) {
                        Parada pNueva = new Parada();
                        pNueva.setLocalizacion(pPadre.getLocalizacion());
                        pNueva.setTipo(pPadre.getTipo());
                        pNueva.setOrden(pPadre.getOrden());
                        pNueva.setLatitud(pPadre.getLatitud());
                        pNueva.setLongitud(pPadre.getLongitud());

                        if (pPadre.getFechaHora() != null && viajePadre.getFechaHoraSalida() != null) {
                            Duration offset = Duration.between(viajePadre.getFechaHoraSalida(), pPadre.getFechaHora());
                            pNueva.setFechaHora(fechaSalidaOcurrencia.plus(offset));
                        } else {
                            pNueva.setFechaHora(fechaSalidaOcurrencia);
                        }

                        vr.addParada(pNueva);
                    }
                }

                ocurrencias.add(vr);
            }
        }

        return viajeRecurrenteRepository.saveAll(ocurrencias);
    }

    private DayOfWeek mapearDiaSemana(String dia) {
        return switch (dia.trim().toUpperCase()) {
            case "L" -> DayOfWeek.MONDAY;
            case "M" -> DayOfWeek.TUESDAY;
            case "X" -> DayOfWeek.WEDNESDAY;
            case "J" -> DayOfWeek.THURSDAY;
            case "V" -> DayOfWeek.FRIDAY;
            case "S" -> DayOfWeek.SATURDAY;
            case "D" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private String generarCheckin() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (viajeRecurrenteRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
    
}
