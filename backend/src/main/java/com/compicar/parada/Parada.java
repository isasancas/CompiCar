package com.compicar.parada;

import java.time.LocalDateTime;

import com.compicar.viaje.Viaje;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parada")
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String localizacion;

    @ManyToOne
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    // Constructores
    public Parada() {
    }

    public Parada(LocalDateTime fechaHora, String localizacion, Viaje viaje) {
        this.fechaHora = fechaHora;
        this.localizacion = localizacion;
        this.viaje = viaje;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getLocalizacion() {
        return localizacion;
    }

    public Viaje getViaje() {
        return viaje;
    }

    // Setters
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }

    public void setViaje(Viaje viaje) {
        this.viaje = viaje;
    }

    @Override
    public String toString() {
        return "Parada{" +
                "id=" + id +
                ", fechaHora=" + fechaHora +
                ", localizacion='" + localizacion + '\'' +
                ", viajeId=" + (viaje != null ? viaje.getId() : null) +
                '}';
    }
    
}
