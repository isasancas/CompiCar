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
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "viaje")
public class Viaje extends ViajeBase {

    @Column(nullable = false, length = 6)
    private String checkin;

    private LocalDateTime fechaFinRecurrencia;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "viaje_dias_semana", joinColumns = @JoinColumn(name = "viaje_id"))
    @Column(name = "dia_semana")
    private List<String> diasSemana = new ArrayList<>();

    @OneToMany(mappedBy = "viajePadre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("viajePadre")
    private List<ViajeRecurrente> viajesRecurrentes = new ArrayList<>();

    @OneToMany(mappedBy = "viaje")
    @JsonIgnore
    private List<Reserva> reservas = new ArrayList<>();

    @OneToMany(mappedBy = "viaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @JsonIgnoreProperties("viaje")
    private List<Parada> paradas = new ArrayList<>();

    // Constructores
    public Viaje() {
    }

    public Viaje(LocalDateTime fechaHoraSalida, EstadoViaje estado, Integer plazasDisponibles, BigDecimal precio,
            Persona persona, Vehiculo vehiculo, String slug) {
        super(fechaHoraSalida, estado, plazasDisponibles, precio, persona, vehiculo, slug);
    }

    // Getters
    public String getCheckin() {
        return checkin;
    }

    public LocalDateTime getFechaFinRecurrencia() {
        return fechaFinRecurrencia;
    }

    public List<String> getDiasSemana() {
        return diasSemana;
    }

    public List<ViajeRecurrente> getViajesRecurrentes() {
        return viajesRecurrentes;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public List<Parada> getParadas() {
        return paradas;
    }
    
    // Setters
    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public void setFechaFinRecurrencia(LocalDateTime fechaFinRecurrencia) {
        this.fechaFinRecurrencia = fechaFinRecurrencia;
    }

    public void setDiasSemana(List<String> diasSemana) {
        this.diasSemana = diasSemana;
    }

    public void setViajesRecurrentes(List<ViajeRecurrente> viajesRecurrentes) {
        this.viajesRecurrentes = viajesRecurrentes;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    public void setParadas(List<Parada> paradas) {
        this.paradas = paradas;
    }

    @Override
    public String toString() {
        return "Viaje{id=" + getId() + ", fechaHoraSalida=" + getFechaHoraSalida() + ", estado=" + getEstado()
                + ", plazasDisponibles=" + getPlazasDisponibles() + ", precio=" + getPrecio() 
                + ", checkin=" + checkin + ", diasSemana=" + diasSemana + "}";
    }

}
