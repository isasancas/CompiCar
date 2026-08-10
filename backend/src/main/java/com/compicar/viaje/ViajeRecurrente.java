package com.compicar.viaje;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.compicar.parada.Parada;
import com.compicar.persona.Persona;
import com.compicar.reserva.Reserva;
import com.compicar.vehiculo.Vehiculo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "viaje_recurrente")
public class ViajeRecurrente extends ViajeBase {

    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_padre_id", nullable = false)
    @JsonIgnoreProperties("viajesRecurrentes")
    private Viaje viajePadre;

    @OneToMany(mappedBy = "viajeRecurrente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @JsonIgnoreProperties("viajeRecurrente")
    private List<Parada> paradas = new ArrayList<>();

    @OneToMany(mappedBy = "viajeRecurrente")
    @JsonIgnore
    private List<Reserva> reservas = new ArrayList<>();

    public ViajeRecurrente() {
    }

    public ViajeRecurrente(LocalDateTime fechaHoraSalida, EstadoViaje estado, Integer plazasDisponibles, BigDecimal precio, 
        Persona persona, Vehiculo vehiculo, Viaje viajePadre, String slug) {
        super(fechaHoraSalida, estado, plazasDisponibles, precio, persona, vehiculo, slug);
        this.viajePadre = viajePadre;
    }

    // Getters
    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public Viaje getViajePadre() {
        return viajePadre;
    }

    public List<Parada> getParadas() {
        return paradas;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    // Setters
    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public void setViajePadre(Viaje viajePadre) {
        this.viajePadre = viajePadre;
    }

    public void setParadas(List<Parada> paradas) {
        this.paradas = paradas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }
    
}
